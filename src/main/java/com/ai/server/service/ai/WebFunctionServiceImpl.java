package com.ai.server.service.ai;

import com.ai.server.model.entity.WebFunctionEntity;
import com.ai.server.repository.WebFunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebFunctionServiceImpl implements WebFunctionService {

    private final WebFunctionRepository webFunctionRepository;

    @Override
    public List<WebFunctionEntity> listEnabledFunctions() {
        return webFunctionRepository.findByIsEnabledTrueOrderBySortOrderAsc();
    }
}
