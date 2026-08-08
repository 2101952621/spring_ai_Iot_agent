package com.ai.server.common.enums;

/**
 * 设备类型枚举
 */
public enum DeviceTypeEnum {

    /**
     * 路由器
     */
    ROUTER("1"),

    /**
     * 中继器
     */
    REPEATER("2"),

    /**
     * 交换机
     */
    SWITCHBOARD("3"),

    /**
     * IPC
     */
    IPC("4"),

    /**
     * CPE
     */
    CPE("5"),

    /**
     * AP
     */
    AP("6");


    private final String status;


    DeviceTypeEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
