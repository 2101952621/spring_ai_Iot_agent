package com.ai.server.model.dto;

import lombok.*;

@Getter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private String question;
    private String sessionId;
}
