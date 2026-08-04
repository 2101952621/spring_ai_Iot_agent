package com.ai.server.service.ai;

import com.ai.server.model.entity.WebFunctionEntity;

import java.util.List;

public interface WebFunctionService {

    /**
     * 查询所有启用的功能，按排序权重升序
     *
     * @return 功能列表
     */
    List<WebFunctionEntity> listEnabledFunctions();
}
