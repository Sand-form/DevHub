package com.github.devhub.forum.service.notify.help;

import com.github.devhub.forum.api.model.enums.NotifyTypeEnum;
import com.github.devhub.forum.api.model.vo.notify.NotifyMsgEvent;
import com.github.devhub.forum.core.util.SpringUtil;
import org.springframework.stereotype.Service;

/**
 * @author TTT
 * @date 2024/11/27
 */
@Service
public class MsgNotifyHelper {

    /**
     * 消息广播通知
     *
     * @param type    消息类型
     * @param content 消息内容
     * @param <T>     消息类型
     */
    public <T> void publishMsg(NotifyTypeEnum type, T content) {
        SpringUtil.publishEvent(new NotifyMsgEvent<>(this, type, content));
    }


    /**
     * 静态方法使用方式，简化调用方使用
     *
     * @param type 消息类型
     *             * @param content 消息内容
     *             * @param <T>     消息类型
     */
    public static <T> void publish(NotifyTypeEnum type, T content) {
        SpringUtil.getBean(MsgNotifyHelper.class).publishMsg(type, content);
    }
}
