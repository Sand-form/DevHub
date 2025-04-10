package com.github.devhub.forum.service.notify.service.impl;

import com.github.devhub.forum.api.model.enums.NotifyTypeEnum;
import com.github.devhub.forum.core.common.CommonConstants;
import com.github.devhub.forum.core.rabbitmq.RabbitmqConnection;
import com.github.devhub.forum.core.rabbitmq.RabbitmqConnectionPool;
import com.github.devhub.forum.core.util.JsonUtil;
import com.github.devhub.forum.core.util.SpringUtil;
import com.github.devhub.forum.service.notify.service.NotifyService;
import com.github.devhub.forum.service.notify.service.RabbitmqService;
import com.github.devhub.forum.service.user.repository.entity.UserFootDO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class RabbitmqServiceImpl implements RabbitmqService {

    @Autowired
    private NotifyService notifyService;

    @Override
    public boolean enabled() {
        return "true".equalsIgnoreCase(SpringUtil.getConfig("rabbitmq.switchFlag"));
    }

    /**
     * 发布消息到 RabbitMQ 交换机。
     *
     * @param exchange      交换机的名称
     * @param exchangeType  交换机的类型，使用 BuiltinExchangeType 枚举
     * @param toutingKey    路由键，用于将消息路由到指定的队列
     * @param message       要发布的消息内容
     */
    @Override
    public void publishMsg(String exchange,
                           BuiltinExchangeType exchangeType,
                           String toutingKey,
                           String message) {
        try {
            // 创建连接，从连接池中获取一个 RabbitmqConnection 对象
            RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();
            // 从 RabbitmqConnection 对象中获取实际的 Connection 对象
            Connection connection = rabbitmqConnection.getConnection();
            // 创建消息通道，用于与 RabbitMQ 服务器进行通信
            Channel channel = connection.createChannel();
            // 声明 exchange 中的消息为可持久化，不自动删除
            // 这样可以确保在 RabbitMQ 服务器重启后，交换机仍然存在
//            channel.exchangeDeclare(exchange, exchangeType, true, false, null);
            channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, true, false, null);
            // 发布消息到指定的交换机，并使用路由键进行路由
            channel.basicPublish(exchange, toutingKey, null, message.getBytes());
            // 记录日志，表明消息已成功发布
            log.info("Publish msg: {}", message);
            // 关闭消息通道，释放资源
            channel.close();
            // 将使用完的 RabbitmqConnection 对象返回到连接池中
            RabbitmqConnectionPool.returnConnection(rabbitmqConnection);
        } catch (InterruptedException | IOException | TimeoutException e) {
            log.error("rabbitMq消息发送异常: exchange: {}, msg: {}", exchange, message, e);
        }

    }

    /**
     * 从 RabbitMQ 队列中消费消息，并处理消息。
     *
     * @param exchange    交换机的名称
     * @param queueName   要消费消息的队列名称
     * @param routingKey  绑定队列到交换机的路由键
     */
    @Override
    public void consumerMsg(String exchange,
                            String queueName,
                            String routingKey) {

        try {
            // 创建连接，从连接池中获取一个 RabbitmqConnection 对象
            RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();
            // 从 RabbitmqConnection 对象中获取实际的 Connection 对象
            Connection connection = rabbitmqConnection.getConnection();
            // 创建消息信道，用于与 RabbitMQ 服务器进行通信
            final Channel channel = connection.createChannel();
            // 声明消息队列，设置为持久化、非排他、非自动删除
            channel.queueDeclare(queueName, true, false, false, null);
            // 将队列绑定到指定的交换机，并使用路由键进行关联
            channel.queueBind(queueName, exchange, routingKey);

            // 创建一个消费者对象，用于处理接收到的消息
            Consumer consumer = new DefaultConsumer(channel) {
                /**
                 * 处理接收到的消息的方法。
                 * 从RabbitMQ获取到的消息首先被转换为对象（UserFootDO），
                 * 然后调用 notifyService 的 saveArticleNotify 方法将其保存到数据库。
                 *
                 * @param consumerTag 消费者标签
                 * @param envelope    消息信封，包含消息的元数据
                 * @param properties  消息的属性
                 * @param body        消息的字节数组
                 * @throws IOException 如果在处理消息时发生 I/O 异常
                 */
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    // 将接收到的消息字节数组转换为 UTF-8 编码的字符串
                    String message = new String(body, "UTF-8");
                    // 记录日志，表明接收到了消息
                    log.info("Consumer msg: {}", message);

                    // 获取 Rabbitmq 消息，并保存到数据库
                    // 说明：这里仅作为示例，如果有多种类型的消息，可以根据消息判定，简单的用 if...else 处理，复杂的用工厂 + 策略模式
                    notifyService.saveArticleNotify(JsonUtil.toObj(message, UserFootDO.class), NotifyTypeEnum.PRAISE);

                    // 手动确认消息已被消费，告诉 RabbitMQ 可以将该消息从队列中移除
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };
            // 取消自动确认消息（auto-ack），改为手动确认
            channel.basicConsume(queueName, false, consumer);
            // 关闭消息通道，释放资源
            channel.close();
            // 将使用完的 RabbitmqConnection 对象返回到连接池中
            RabbitmqConnectionPool.returnConnection(rabbitmqConnection);
        } catch (InterruptedException | IOException | TimeoutException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 处理从 RabbitMQ 队列消费消息的逻辑。
     * 此方法会循环调用 consumerMsg 方法来消费消息，并且在达到指定步数后会休眠一段时间。
     * 目前采用的是轮询方式，后续计划改造成阻塞 I/O 模式以提高效率。
     */
    @Override
    public void processConsumerMsg() {
        // 记录日志，表明开始处理消费消息的逻辑
        log.info("Begin to processConsumerMsg.");

        // 定义循环的总步数，这里设置为 1 表示每执行一次 consumerMsg 方法就进行一次休眠操作
        Integer stepTotal = 1;
        // 初始化当前步数为 0
        Integer step = 0;

        // TODO: 这种方式非常 Low，后续会改造成阻塞 I/O 模式
        // 进入一个无限循环，持续处理消息消费逻辑
        while (true) {
            // 每循环一次，当前步数加 1
            step++;
            try {
                // 记录日志，表明进入一次消费消息的循环
                log.info("processConsumerMsg cycle.");
                // 调用 consumerMsg 方法，从指定的交换机、队列和路由键消费消息
                consumerMsg(CommonConstants.EXCHANGE_NAME_DIRECT, CommonConstants.QUERE_NAME_PRAISE,
                        CommonConstants.QUERE_KEY_PRAISE);
                // 检查当前步数是否达到总步数
                if (step.equals(stepTotal)) {
                    // 如果达到总步数，线程休眠 10000 毫秒（即 10 秒）
                    Thread.sleep(10000);
                    // 休眠结束后，将当前步数重置为 0，以便下一轮计数
                    step = 0;
                }
            } catch (Exception e) {
                // 捕获所有异常，但这里没有做任何处理，后续可以根据需求添加日志记录或其他处理逻辑
            }
        }
    }
}
