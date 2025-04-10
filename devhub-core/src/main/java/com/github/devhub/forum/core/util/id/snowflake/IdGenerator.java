package com.github.devhub.forum.core.util.id.snowflake;

/**
 * @author TTT
 * @date 2023/10/17
 */
public interface IdGenerator {
    /**
     * 生成分布式id
     *
     * @return
     */
    Long nextId();
}
