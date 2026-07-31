package com.ai.server.agent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserChatEventType {
    /**
     * 数据事件
     */
    DATA(1001),

    /**
     * 停止事件
     */
    STOP(1002),

    /**
     * 参数事件
     */
    PARAM(1003);

    private final int value;
}
