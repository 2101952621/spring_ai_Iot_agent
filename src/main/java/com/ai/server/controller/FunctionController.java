package com.ai.server.controller;

import com.ai.server.model.entity.WebFunctionEntity;
import com.ai.server.service.ai.WebFunctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class FunctionController {

    private final WebFunctionService webFunctionService;

    @GetMapping("/functions")
    public List<WebFunctionEntity> listFunctions() {
        return webFunctionService.listEnabledFunctions();
    }
}
