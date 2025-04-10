package com.github.devhub.forum.api.model.exception;

import com.github.devhub.forum.api.model.vo.constants.StatusEnum;

/**
 * @author TTT
 * @date 2022/9/2
 */
public class ExceptionUtil {

    public static ForumException of(StatusEnum status, Object... args) {
        return new ForumException(status, args);
    }

}
