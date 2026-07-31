package com.ai.server.service.ai;

import com.ai.server.model.vo.HotExampleVO;

import java.util.List;

/**
 * 热门示例消息 Service 接口
 */
public interface HotExampleService {

    /**
     * 分页获取热门示例消息
     *
     * @param page 页码（从0开始）
     * @param size 每页条数
     * @return 热门示例列表
     */
    List<HotExampleVO> getHotExamples(Integer page, Integer size);
}
