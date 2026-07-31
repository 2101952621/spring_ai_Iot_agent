package com.ai.server.repository;

import com.ai.server.model.entity.HotExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 热门示例消息 Repository
 */
@Repository
public interface HotExampleRepository extends JpaRepository<HotExampleEntity, Long> {

}
