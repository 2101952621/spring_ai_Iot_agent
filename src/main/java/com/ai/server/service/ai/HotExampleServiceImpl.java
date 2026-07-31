package com.ai.server.service.ai;

import com.ai.server.model.vo.HotExampleVO;
import com.ai.server.repository.HotExampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 热门示例消息 Service 实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HotExampleServiceImpl implements HotExampleService {

    private final HotExampleRepository hotExampleRepository;

    @Override
    public List<HotExampleVO> getHotExamples(Integer page, Integer size) {
        log.debug("分页查询热门示例消息, page={}, size={}", page, size);
        long totalCount = hotExampleRepository.count();
        if (totalCount == 0) {
            return Collections.emptyList();
        }
        int totalPages = (int) Math.ceil((double) totalCount / size);
        int actualPage = page % totalPages;
        log.debug("总记录数={}, 总页数={}, 请求页={}, 实际页={}", totalCount, totalPages, page, actualPage);
        PageRequest pageRequest = PageRequest.of(actualPage, size, Sort.by(Sort.Direction.ASC, "sortOrder"));
        return hotExampleRepository.findAll(pageRequest)
                .stream()
                .map(entity -> HotExampleVO.builder()
                        .title(entity.getTitle())
                        .describe(entity.getDescribe())
                        .build())
                .toList();
    }
}
