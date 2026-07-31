package com.ai.server.controller;

import com.ai.server.model.vo.HotExampleVO;
import com.ai.server.service.ai.HotExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class HotExampleController {

    private final HotExampleService hotExampleService;

    @GetMapping("/hot")
    public List<HotExampleVO> getHostMessage(@RequestParam(value = "page", defaultValue = "0") Integer page) {
        return this.hotExampleService.getHotExamples(page, 3);
    }
}
