package com.ai.server.service.ai;

import com.ai.server.model.entity.VersionRecordEntity;
import com.ai.server.repository.VersionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VersionRecordServiceImpl implements VersionRecordService {

    private final VersionRecordRepository versionRecordRepository;

    @Override
    public List<VersionRecordEntity> listAll() {
        return versionRecordRepository.findAllByOrderBySortOrderDesc();
    }
}
