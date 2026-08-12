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
    CARD(1004),

    /**
     * 调用系统下载事件
     */
    SYSTEM_DOWNLOAD(1005),

    /**
     * 分析预览事件 — 前端渲染日志分析洞察卡片（含统计概览 + 智能建议）
     */
    SYSTEM_ANALYSIS(1006);

    private final int value;
}
