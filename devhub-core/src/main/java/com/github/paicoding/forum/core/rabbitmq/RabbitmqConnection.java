package com.github.devhub.forum.core.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * 该类用于创建和管理与 RabbitMQ 服务器的连接。
 *
 * @author TTT
 * @date 2023/5/10
 */
public class RabbitmqConnection {

    // 存储与 RabbitMQ 服务器的连接对象
    private Connection connection;

    /**
     * 构造函数，用于初始化与 RabbitMQ 服务器的连接。
     *
     * @param host  RabbitMQ 服务器的主机名
     * @param port  RabbitMQ 服务器的端口号
     * @param userName  连接 RabbitMQ 服务器的用户名
     * @param password  连接 RabbitMQ 服务器的密码
     * @param virtualhost  RabbitMQ 服务器的虚拟主机名
     */
    public RabbitmqConnection(String host, int port, String userName, String password, String virtualhost) {
        // 创建一个 ConnectionFactory 实例，用于配置和创建连接
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // 设置 RabbitMQ 服务器的主机名
        connectionFactory.setHost(host);
        // 设置 RabbitMQ 服务器的端口号
        connectionFactory.setPort(port);
        // 设置连接 RabbitMQ 服务器的用户名
        connectionFactory.setUsername(userName);
        // 设置连接 RabbitMQ 服务器的密码
        connectionFactory.setPassword(password);
        // 设置 RabbitMQ 服务器的虚拟主机名
        connectionFactory.setVirtualHost(virtualhost);
        try {
            // 通过 ConnectionFactory 创建与 RabbitMQ 服务器的连接
            connection = connectionFactory.newConnection();
        } catch (IOException | TimeoutException e) {
            // 若创建连接过程中出现 IO 异常或超时异常，打印异常堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 获取与 RabbitMQ 服务器的连接对象。
     *
     * @return 与 RabbitMQ 服务器的连接对象
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 关闭与 RabbitMQ 服务器的连接。
     */
    public void close() {
        try {
            // 调用 Connection 对象的 close 方法关闭连接
            connection.close();
        } catch (IOException e) {
            // 若关闭连接过程中出现 IO 异常，打印异常堆栈信息
            e.printStackTrace();
        }
    }
}
