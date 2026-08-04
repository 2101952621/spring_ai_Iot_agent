package com.ai.server.controller;

import com.ai.server.model.entity.VersionRecordEntity;
import com.ai.server.service.ai.VersionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class VersionRecordController {

    private final VersionRecordService versionRecordService;

    /**
     * 查询历史版本信息列表
     */
    @GetMapping("/versions")
    public List<VersionRecordEntity> listVersions() {
        return versionRecordService.listAll();
    }
}
