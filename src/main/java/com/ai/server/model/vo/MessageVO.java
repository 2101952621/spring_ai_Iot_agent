package com.ai.server.model.vo;

import com.ai.server.agent.enums.SystemMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {
    private SystemMessageType type;
    private String content;
    private Map<String, Object> params;
}
