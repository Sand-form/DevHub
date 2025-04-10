package com.github.devhub.forum.web;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.devhub.forum.core.config.RabbitmqProperties;
import com.github.devhub.forum.core.rabbitmq.RabbitmqConnectionPool;
import com.github.devhub.forum.core.util.SocketUtil;
import com.github.devhub.forum.core.util.SpringUtil;
import com.github.devhub.forum.service.notify.service.RabbitmqService;
import com.github.devhub.forum.web.config.GlobalViewConfig;
import com.github.devhub.forum.web.global.ForumExceptionHandler;
import com.github.devhub.forum.web.hook.interceptor.GlobalViewInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 入口，直接运行即可
 *
 * @author yihui
 * @date 2022/7/6
 */
@Slf4j
@EnableAsync
@EnableScheduling
@EnableCaching
@ServletComponentScan
@SpringBootApplication
public class QuickForumApplication implements WebMvcConfigurer, ApplicationRunner {
    @Value("${server.port:8080}")
    private Integer webPort;

    @Resource
    private GlobalViewInterceptor globalViewInterceptor;

    @Resource
    @Qualifier(value = "taskExecutor")
    private Executor taskExecutor;

    @Resource
    private RabbitmqService rabbitmqService;

    @Autowired
    private RabbitmqProperties rabbitmqProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalViewInterceptor).addPathPatterns("/**");
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(0, new ForumExceptionHandler());
    }

    public static void main(String[] args) {
        SpringApplication.run(QuickForumApplication.class, args);
    }

    /**
     * 兼容本地启动时8080端口被占用的场景; 只有dev启动方式才做这个逻辑
     *
     * @return
     */
    @Bean
    @ConditionalOnExpression(value = "#{'dev'.equals(environment.getProperty('env.name'))}")
    public TomcatConnectorCustomizer customServerPortTomcatConnectorCustomizer() {
        // 开发环境时，首先判断8080d端口是否可用；若可用则直接使用，否则选择一个可用的端口号启动
        int port = SocketUtil.findAvailableTcpPort(8000, 10000, webPort);
        if (port != webPort) {
            log.info("默认端口号{}被占用，随机启用新端口号: {}", webPort, port);
            webPort = port;
        }
        return connector -> connector.setPort(port);
    }

    /**
     * 配置 Prometheus 中显示的服务名称
     * @param applicationName
     * @return
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(@Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    @Override
    public void run(ApplicationArguments args) {
        // 设置类型转换, 主要用于mybatis读取varchar/json类型数据据，并写入到json格式的实体Entity中
        JacksonTypeHandler.setObjectMapper(new ObjectMapper());
        // 应用启动之后执行
        GlobalViewConfig config = SpringUtil.getBean(GlobalViewConfig.class);
        if (webPort != null) {
            config.setHost("http://127.0.0.1:" + webPort);
        }
        // 启动 RabbitMQ 进行消费
        if (rabbitmqProperties.getSwitchFlag()) {
            String host = rabbitmqProperties.getHost();
            Integer port = rabbitmqProperties.getPort();
            String userName = rabbitmqProperties.getUsername();
            String password = rabbitmqProperties.getPassport();
            String virtualhost = rabbitmqProperties.getVirtualhost();
            Integer poolSize = rabbitmqProperties.getPoolSize();
            RabbitmqConnectionPool.initRabbitmqConnectionPool(host, port, userName, password, virtualhost, poolSize);
            taskExecutor.execute(() -> rabbitmqService.processConsumerMsg());
        }
        log.info("启动成功，点击进入首页: {}", config.getHost());
    }
}
