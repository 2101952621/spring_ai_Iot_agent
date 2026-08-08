package com.ai.server.agent.tools;

import com.ai.server.model.entity.IotDeviceEntity;
import com.ai.server.model.entity.IotDeviceRebootEntity;
import com.ai.server.repository.IotDeviceRebootRepository;
import com.ai.server.repository.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * 设备控制操作工具 — 供 SystemControlAgent 调用
 * 支持设备重启、查询设备状态等操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceControlTools {

    private final IotDeviceRepository iotDeviceRepository;
    private final IotDeviceRebootRepository iotDeviceRebootRepository;

    /**
     * 重启指定设备
     */
    @Tool(description = "重启指定设备。用户可以通过设备名称、设备ID、序列号(SN)、IP地址或MAC地址来标识目标设备。" +
            "例如：'重启路由器-001'、'重启SN为202591014061239的设备'、'重启192.168.0.109的设备'")
    public String rebootDevice(
            @ToolParam(description = "设备标识，可以是设备名称、设备ID(UUID)、设备序列号(SN)、IP地址或MAC地址") String deviceIdentifier,
            ToolContext toolContext) {
        Optional<IotDeviceEntity> deviceOpt = findDevice(deviceIdentifier);
        if (deviceOpt.isEmpty()) {
            log.warn("设备重启失败: 未找到设备, identifier={}", deviceIdentifier);
            return String.format("抱歉，未能找到与「%s」匹配的设备。请确认设备名称、序列号(SN)、IP地址或MAC地址是否正确，然后重试。", deviceIdentifier);
        }
        IotDeviceEntity device = deviceOpt.get();
        if (device.getIsOnline() == null || !device.getIsOnline()) {
            log.warn("设备重启被拒绝: 设备不在线, deviceName={}, deviceId={}", device.getName(), device.getId());
            IotDeviceRebootEntity record = IotDeviceRebootEntity.builder()
                    .id(UUID.randomUUID())
                    .deviceId(device.getId())
                    .deviceName(device.getName())
                    .deviceType(device.getType())
                    .deviceSn(device.getSn())
                    .deviceIp(device.getIp())
                    .rebootTime(System.currentTimeMillis())
                    .rebootStatus("OFFLINE_SKIP")
                    .resultDesc("设备不在线，无法下发重启指令")
                    .operator(resolveOperator(toolContext))
                    .remark("设备当前为离线状态")
                    .build();
            iotDeviceRebootRepository.save(record);
            return String.format("设备「%s」(SN: %s) 当前不在线，无法下发重启指令。" +
                    "请检查设备连接状态，待设备上线后再尝试重启操作。", device.getName(), device.getSn());
        }

        // ========================================
        // 第三步：下发重启指令实际场景可真是像设备发送MQTT或者其他类型消息
        // ========================================
        // TODO: 替换为真实的设备通信调用结果
        boolean responseStatus = true;

        // TODO: 替换为真实的设备通信调用结果
        String errorMsg = null;

        // ========================================
        // 第四步：记录重启操作并返回结果
        // ========================================
        if (responseStatus) {
            IotDeviceRebootEntity record = IotDeviceRebootEntity.builder()
                    .id(UUID.randomUUID())
                    .deviceId(device.getId())
                    .deviceName(device.getName())
                    .deviceType(device.getType())
                    .deviceSn(device.getSn())
                    .deviceIp(device.getIp())
                    .rebootTime(System.currentTimeMillis())
                    .rebootStatus("PENDING")
                    .resultDesc("重启指令已成功下发，设备正在重启中")
                    .operator(resolveOperator(toolContext))
                    .remark("通过AI智能助手发起重启")
                    .build();
            iotDeviceRebootRepository.save(record);

            log.info("设备重启指令已下发: deviceName={}, deviceId={}, sn={}, ip={}",
                    device.getName(), device.getId(), device.getSn(), device.getIp());

            return String.format("已成功向设备「%s」(SN: %s, IP: %s) 下发重启指令，设备正在重启中。" +
                            "重启后设备会短暂离线，预计1-2分钟后恢复正常上线。",
                    device.getName(), device.getSn(), device.getIp());
        } else {
            IotDeviceRebootEntity record = IotDeviceRebootEntity.builder()
                    .id(UUID.randomUUID())
                    .deviceId(device.getId())
                    .deviceName(device.getName())
                    .deviceType(device.getType())
                    .deviceSn(device.getSn())
                    .deviceIp(device.getIp())
                    .rebootTime(System.currentTimeMillis())
                    .rebootStatus("FAILED")
                    .resultDesc("重启指令下发失败: " + (errorMsg != null ? errorMsg : "未知错误"))
                    .operator(resolveOperator(toolContext))
                    .remark("通过AI智能助手发起重启")
                    .build();
            iotDeviceRebootRepository.save(record);

            log.error("设备重启指令下发失败: deviceName={}, deviceId={}, error={}",
                    device.getName(), device.getId(), errorMsg);

            return String.format("向设备「%s」(SN: %s) 下发重启指令失败，原因：%s。请稍后重试或联系运维人员处理。",
                    device.getName(), device.getSn(), errorMsg != null ? errorMsg : "未知错误");
        }
    }

    /**
     * 查询设备基本信息
     */
    @Tool(description = "查询设备的详细状态信息，包括在线状态、IP地址、软件版本等。" +
            "用户可通过设备名称、设备ID、序列号(SN)、IP地址或MAC地址来查询")
    public String queryDeviceStatus(
            @ToolParam(description = "设备标识，可以是设备名称、设备ID(UUID)、设备序列号(SN)、IP地址或MAC地址") String deviceIdentifier) {

        Optional<IotDeviceEntity> deviceOpt = findDevice(deviceIdentifier);

        if (deviceOpt.isEmpty()) {
            return String.format("抱歉，未能找到与「%s」匹配的设备。请确认设备标识是否正确。", deviceIdentifier);
        }

        IotDeviceEntity device = deviceOpt.get();
        String onlineStatus = (device.getIsOnline() != null && device.getIsOnline()) ? "在线" : "离线";

        return String.format("设备名称：%s\n类型：%s\nIP地址：%s\nMAC地址：%s\n序列号(SN)：%s\n" +
                        "软件版本：%s\n硬件版本：%s\n在线状态：%s",
                device.getName(),
                device.getType(),
                device.getIp(),
                device.getMac(),
                device.getSn(),
                device.getSoftwareVersion(),
                device.getFirmwareVersion(),
                onlineStatus);
    }

    private Optional<IotDeviceEntity> findDevice(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String trimmed = identifier.trim();

        try {
            UUID uuid = UUID.fromString(trimmed);
            return iotDeviceRepository.findById(uuid);
        } catch (IllegalArgumentException ignored) {
        }

        Optional<IotDeviceEntity> bySn = iotDeviceRepository.findBySn(trimmed);
        if (bySn.isPresent()) return bySn;

        Optional<IotDeviceEntity> byMac = iotDeviceRepository.findByMac(trimmed);
        if (byMac.isPresent()) return byMac;

        Optional<IotDeviceEntity> byIp = iotDeviceRepository.findByIp(trimmed);
        if (byIp.isPresent()) return byIp;

        Optional<IotDeviceEntity> byName = iotDeviceRepository.findByName(trimmed);
        if (byName.isPresent()) return byName;

        var byNameLike = iotDeviceRepository.findByNameContaining(trimmed);
        if (!byNameLike.isEmpty()) return Optional.of(byNameLike.get(0));

        return Optional.empty();
    }

    private String resolveOperator(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) return "system";
        Object v = toolContext.getContext().get("sessionId");
        return v != null ? v.toString() : "system";
    }
}
