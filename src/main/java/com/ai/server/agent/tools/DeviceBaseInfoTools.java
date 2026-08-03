package com.ai.server.agent.tools;

import com.ai.server.agent.tools.result.DeviceBaseInfo;
import com.ai.server.config.Constant;
import com.ai.server.model.entity.DeviceBaseInfoEntity;
import com.ai.server.repository.DeviceBaseInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 设备基础信息查询工具
 */
@Component
@RequiredArgsConstructor
public class DeviceBaseInfoTools {

    private final DeviceBaseInfoRepository deviceBaseInfoRepository;

    @Tool(description = "根据设备id查询设备基础信息")
    public DeviceBaseInfo queryDeviceById(@ToolParam(description = "设备基础信息id") Long deviceBaseId) {
        Optional<DeviceBaseInfoEntity> opt = deviceBaseInfoRepository.findById(deviceBaseId);
        return opt.map(this::convertToDeviceBaseInfo).orElse(null);
    }

    private DeviceBaseInfo convertToDeviceBaseInfo(DeviceBaseInfoEntity entity) {
        return DeviceBaseInfo.builder()
                .id(entity.getId())
                .name(entity.getDeviceName())
                .model(entity.getDeviceModel())
                .type(entity.getDeviceType())
                .price(entity.getPrice() != null ? entity.getPrice() : BigDecimal.ZERO)
                .detail(entity.getDetail())
                .core(entity.getCore())
                .suitableScenarios(entity.getSuitableScenarios())
                .productUrl(entity.getProductUrl())
                .build();
    }
}
