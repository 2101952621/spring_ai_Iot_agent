package com.ai.server.agent.tools.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 网页功能信息（Tool 返回对象 / CARD 事件数据载体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebFunctionInfo implements Serializable {

    /** 功能ID */
    private Long id;

    /** 功能编码 */
    private String functionCode;

    /** 功能名称 */
    private String functionName;

    /** 所属模块 */
    private String module;

    /** 模块基础路径 */
    private String openUrl;

    /** 按钮文案 */
    private String buttonText;

    /** 图标 */
    private String icon;

    /** 功能描述 */
    private String description;

    /** 卡片类型（前端识别渲染样式） */
    private String cardType;

    /** 注意事项 */
    private String precautions;

    /** 配置方式（用于前端展示） */
    private String configMethod;
}