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
    PARAM(1003),

    /**
     * 卡片事件 — 前端渲染"打开网页"等功能卡片（带按钮）
     */
    CARD(1004);

    private final int value;
}
