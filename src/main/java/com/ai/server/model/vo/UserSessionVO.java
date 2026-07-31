package com.ai.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionVO {
    private String sessionId;
    private String title;
    private String describe;
    private List<HotExampleVO> hotExamples;
}
