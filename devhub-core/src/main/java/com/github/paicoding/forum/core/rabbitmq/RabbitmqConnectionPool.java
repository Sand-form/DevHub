package com.github.devhub.forum.core.rabbitmq;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * RabbitMQ 连接池类，用于管理 RabbitMQ 连接对象。
 *
 * @author TTT
 */
public class RabbitmqConnectionPool {

    /**
     * 用于存储 RabbitMQ 连接对象的阻塞队列
     */
    private static BlockingQueue<RabbitmqConnection> pool;

    /**
     * 初始化 RabbitMQ 连接池
     *
     * @param host        RabbitMQ 服务器主机名
     * @param port        RabbitMQ 服务器端口号
     * @param userName    连接 RabbitMQ 的用户名
     * @param password    连接 RabbitMQ 的密码
     * @param virtualhost RabbitMQ 的虚拟主机名
     * @param poolSize    连接池的大小
     */
    public static void initRabbitmqConnectionPool(String host, int port, String userName, String password,
                                                  String virtualhost,
                                                  Integer poolSize) {
        // 初始化阻塞队列，指定队列容量
        pool = new LinkedBlockingQueue<>(poolSize);
        // 循环创建指定数量的连接并添加到队列中
        for (int i = 0; i < poolSize; i++) {
            pool.add(new RabbitmqConnection(host, port, userName, password, virtualhost));
        }
    }

    /**
     * 从连接池中获取一个 RabbitMQ 连接
     *
     * @return RabbitMQ 连接对象
     * @throws InterruptedException 如果在等待获取连接时被中断
     */
    public static RabbitmqConnection getConnection() throws InterruptedException {
        // 从队列中取出一个连接，如果队列为空则阻塞等待
        return pool.take();
    }

    /**
     * 将使用完的 RabbitMQ 连接返回到连接池中
     *
     * @param connection 要返回的 RabbitMQ 连接对象
     */
    public static void returnConnection(RabbitmqConnection connection) {
        // 将连接添加到队列中
        pool.add(connection);
    }

    /**
     * 关闭连接池中的所有 RabbitMQ 连接
     */
    public static void close() {
        // 遍历队列中的所有连接并调用关闭方法
        pool.forEach(RabbitmqConnection::close);
    }
}