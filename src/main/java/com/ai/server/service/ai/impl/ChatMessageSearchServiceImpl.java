package com.ai.server.service.ai.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.ai.server.model.document.ChatMessageSearchDocument;
import com.ai.server.model.entity.ChatMessageEntity;
import com.ai.server.model.entity.ChatSessionEntity;
import com.ai.server.model.vo.ChatMessageSearchVO;
import com.ai.server.repository.ChatMessageRepository;
import com.ai.server.repository.ChatSessionRepository;
import com.ai.server.service.ai.ChatMessageSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageSearchServiceImpl implements ChatMessageSearchService {

    public static final String INDEX_NAME = "chat_message_search";

    private final ElasticsearchClient elasticsearchClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostConstruct
    public void init() {
        try {
            ensureIndex();
            reindexAllFromDatabase();
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] 初始化失败: {}", e.getMessage());
        }
    }

    @Override
    public void ensureIndex() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(b -> b.index(INDEX_NAME)))
                    .value();
            if (exists) {
                log.info("[CHAT SEARCH] ES 索引已存在: {}", INDEX_NAME);
                return;
            }
            elasticsearchClient.indices().create(CreateIndexRequest.of(b -> b
                    .index(INDEX_NAME)
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("sessionId", p -> p.keyword(k -> k))
                            .properties("conversationId", p -> p.keyword(k -> k))
                            .properties("userId", p -> p.keyword(k -> k))
                            .properties("messageType", p -> p.keyword(k -> k))
                            .properties("messageIndex", p -> p.integer(i -> i))
                            .properties("sessionTitle", p -> p.text(t -> t
                                    .analyzer("standard")))
                            .properties("messageContent", p -> p.text(t -> t
                                    .analyzer("standard")))
                            .properties("createTime", p -> p.date(d -> d
                                    .format("yyyy-MM-dd HH:mm:ss||epoch_millis")))
                    )
            ));
            log.info("[CHAT SEARCH] 成功创建 ES 索引: {}", INDEX_NAME);
        } catch (Exception e) {
            log.error("[CHAT SEARCH] 创建 ES 索引失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动时将PG中的历史消息全量同步到ES。
     * 若ES与数据库条数不一致，先清空ES再全量写入。
     */
    private void reindexAllFromDatabase() {
        try {
            long esCount = elasticsearchClient.count(c -> c.index(INDEX_NAME)).count();
            long dbCount = chatMessageRepository.count();
            if (esCount == dbCount) {
                log.info("[CHAT SEARCH] ES 与数据库一致（各 {} 条），跳过全量同步", esCount);
                return;
            }
            if (esCount > 0) {
                log.info("[CHAT SEARCH] ES({}) 与数据库({}) 条数不一致，清空 ES 后重新全量同步", esCount, dbCount);
                elasticsearchClient.deleteByQuery(d -> d
                        .index(INDEX_NAME)
                        .query(q -> q.matchAll(m -> m))
                        .refresh(true));
            } else {
                log.info("[CHAT SEARCH] ES 为空，数据库有 {} 条，开始全量同步...", dbCount);
            }
            long totalIndexed = 0;
            int page = 0;
            int batchSize = 500;
            while (true) {
                Page<ChatMessageEntity> pgPage = chatMessageRepository.findAll(PageRequest.of(page, batchSize));
                List<ChatMessageEntity> content = pgPage.getContent();
                if (content.isEmpty()) {
                    break;
                }
                indexBatch(content);
                totalIndexed += content.size();
                log.info("[CHAT SEARCH] 全量同步进度: 已索引 {} 条", totalIndexed);
                if (!pgPage.hasNext()) {
                    break;
                }
                page++;
            }
            log.info("[CHAT SEARCH] 全量同步完成，共索引 {} 条历史消息", totalIndexed);
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] 全量同步历史消息失败（不影响主流程）: {}", e.getMessage());
        }
    }

    @Override
    public void indexMessage(ChatMessageEntity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        try {
            ChatMessageSearchDocument doc = toDocument(entity);
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(entity.getId()))
                    .document(doc));
            log.debug("[CHAT SEARCH] 索引消息成功: id={}", entity.getId());
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] 索引消息失败: id={}, error={}", entity.getId(), e.getMessage());
        }
    }

    @Override
    public void indexBatch(List<ChatMessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        List<ChatMessageEntity> valid = entities.stream()
                .filter(e -> e != null && e.getId() != null)
                .toList();
        if (valid.isEmpty()) {
            return;
        }
        try {
            List<BulkOperation> ops = new ArrayList<>(valid.size());
            for (ChatMessageEntity e : valid) {
                ChatMessageSearchDocument doc = toDocument(e);
                ops.add(BulkOperation.of(op -> op.index(idx -> idx
                        .index(INDEX_NAME)
                        .id(String.valueOf(e.getId()))
                        .document(doc))));
            }
            BulkResponse resp = elasticsearchClient.bulk(BulkRequest.of(b -> b
                    .index(INDEX_NAME)
                    .operations(ops)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor)));
            if (resp.errors()) {
                long failed = resp.items().stream()
                        .filter(it -> it.error() != null)
                        .count();
                log.warn("[CHAT SEARCH] 批量索引部分失败: success={}, failed={}",
                        resp.items().size() - failed, failed);
            } else {
                log.debug("[CHAT SEARCH] 批量索引成功: count={}", valid.size());
            }
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] 批量索引失败: error={}", e.getMessage());
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t
                            .field("conversationId")
                            .value(conversationId)))
                    .refresh(true));
            log.debug("[CHAT SEARCH] 删除会话消息成功: conversationId={}", conversationId);
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] 删除会话消息失败: conversationId={}, error={}",
                    conversationId, e.getMessage());
        }
    }

    @Override
    public List<ChatMessageSearchVO> searchByKeyword(String userId, String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank() || userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = (safePage - 1) * safeSize;
        try {
            SearchResponse<ChatMessageSearchDocument> resp = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .from(from)
                            .size(safeSize)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.multiMatch(mm -> mm
                                            .query(keyword)
                                            .fields("messageContent", "sessionTitle")
                                            .type(TextQueryType.BestFields)
                                            .operator(Operator.And)))
                                    .filter(f -> f.term(t -> t
                                            .field("userId")
                                            .value(userId)))))
                            .highlight(h -> h
                                    .fields("messageContent", hf -> hf
                                            .preTags("<em>")
                                            .postTags("</em>")
                                            .fragmentSize(150)
                                            .numberOfFragments(1)))
                            .sort(so -> so.score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                            .sort(so -> so.field(f -> f.field("createTime")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                    ChatMessageSearchDocument.class);

            return resp.hits().hits().stream()
                    .map(this::toSearchVO)
                    .collect(Collectors.toList());
        } catch (ElasticsearchException e) {
            log.warn("[CHAT SEARCH] 搜索失败: keyword={}, error={}", keyword, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[CHAT SEARCH] 搜索异常: keyword={}, error={}", keyword, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countByKeyword(String userId, String keyword) {
        if (keyword == null || keyword.isBlank() || userId == null || userId.isBlank()) {
            return 0L;
        }
        try {
            return elasticsearchClient.count(c -> c
                    .index(INDEX_NAME)
                    .query(q -> q.bool(b -> b
                            .must(m -> m.multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields("messageContent", "sessionTitle")
                                    .type(TextQueryType.BestFields)
                                    .operator(Operator.And)))
                            .filter(f -> f.term(t -> t
                                    .field("userId")
                                    .value(userId)))))).count();
        } catch (Exception e) {
            log.warn("[CHAT SEARCH] count 失败: keyword={}, error={}", keyword, e.getMessage());
            return 0L;
        }
    }

    private ChatMessageSearchDocument toDocument(ChatMessageEntity entity) {
        String conversationId = entity.getConversationId();
        String sessionId = parseSessionId(conversationId);
        String userId = parseUserId(conversationId);

        LocalDateTime createTime = entity.getCreateTime() != null
                ? entity.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();

        return ChatMessageSearchDocument.builder()
                .id(String.valueOf(entity.getId()))
                .conversationId(conversationId)
                .sessionId(sessionId)
                .userId(userId)
                .messageType(entity.getMessageType())
                .messageContent(entity.getMessageContent())
                .messageIndex(entity.getMessageIndex())
                .sessionTitle(resolveSessionTitle(sessionId))
                .createTime(createTime)
                .build();
    }

    private ChatMessageSearchVO toSearchVO(Hit<ChatMessageSearchDocument> hit) {
        ChatMessageSearchDocument src = hit.source();
        if (src == null) {
            return null;
        }
        String highlight = null;
        Map<String, List<String>> hl = hit.highlight();
        if (hl != null) {
            List<String> contentHl = hl.get("messageContent");
            if (contentHl != null && !contentHl.isEmpty()) {
                highlight = contentHl.get(0);
            }
        }
        return ChatMessageSearchVO.builder()
                .messageId(src.getId())
                .sessionId(src.getSessionId())
                .sessionTitle(src.getSessionTitle())
                .messageType(src.getMessageType())
                .messageContent(src.getMessageContent())
                .highlight(highlight != null ? highlight : src.getMessageContent())
                .createTime(src.getCreateTime())
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    private String parseSessionId(String conversationId) {
        if (conversationId == null) return null;
        int idx = conversationId.indexOf('_');
        return idx >= 0 ? conversationId.substring(idx + 1) : conversationId;
    }

    private String parseUserId(String conversationId) {
        if (conversationId == null) return null;
        int idx = conversationId.indexOf('_');
        return idx >= 0 ? conversationId.substring(0, idx) : conversationId;
    }

    private String resolveSessionTitle(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        try {
            Optional<ChatSessionEntity> session = Optional.ofNullable(chatSessionRepository.findBySessionId(sessionId));
            return session.map(ChatSessionEntity::getTitle).orElse("");
        } catch (Exception e) {
            log.debug("[CHAT SEARCH] 读取会话标题失败: sessionId={}, error={}", sessionId, e.getMessage());
            return "";
        }
    }
}