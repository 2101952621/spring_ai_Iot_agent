package com.ai.server.service.ai;

import com.ai.server.model.entity.VersionRecordEntity;

import java.util.List;


public interface VersionRecordService {

    List<VersionRecordEntity> listAll();
}
