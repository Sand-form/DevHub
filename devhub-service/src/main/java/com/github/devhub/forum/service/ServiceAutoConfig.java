package com.github.devhub.forum.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author TTT
 * @date 2022/7/6
 */
@Configuration
@ComponentScan("com.github.devhub.forum.service")
@MapperScan(basePackages = {
        "com.github.devhub.forum.service.article.repository.mapper",
        "com.github.devhub.forum.service.user.repository.mapper",
        "com.github.devhub.forum.service.comment.repository.mapper",
        "com.github.devhub.forum.service.config.repository.mapper",
        "com.github.devhub.forum.service.statistics.repository.mapper",
        "com.github.devhub.forum.service.notify.repository.mapper",
        "com.github.devhub.forum.service.shortlink.repository.mapper",
})
public class ServiceAutoConfig {


}
