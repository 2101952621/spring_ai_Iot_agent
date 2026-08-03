package com.ai.server.agent.tools.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceBaseInfo implements Serializable {
    private Long id;
    private String name;
    private String model;
    private String type;
    private BigDecimal price;
    private String detail;
    private String core;
    private String suitableScenarios;
    private String productUrl;
}
