package com.qiujie.chat.service;

import com.qiujie.chat.entity.KbQaCitation;
import com.qiujie.chat.entity.KbQaRecord;
import com.qiujie.chat.mapper.KbQaCitationMapper;
import com.qiujie.chat.mapper.KbQaRecordMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 问答审计落库——一次问答写一行 {@code kb_qa_record} 与 N 行 {@code kb_qa_citation}。
 * <p>
 * 记录与引用明细同事务写入：引用行失去主表会变成无法归属的垃圾数据，宁可整体回滚。
 * 失败只记日志不回抛——审计不是问答主流程的必要环节，用户不该因为审计写失败而看不到回答。
 * </p>
 * <p>
 * 拿不到提问者（未登录或档案缺失）时整条不写：{@code staff_id} 非空，
 * 而「不知道是谁问的」不该被伪造成某个账号。
 * </p>
 */
@Component
public class QaRecordWriter {

    private static final Logger log = LoggerFactory.getLogger(QaRecordWriter.class);

    /** 引用快照截断长度；全文可凭 chunkId 回查 */
    private static final int SNAPSHOT_MAX_CHARS = 500;

    @Autowired
    private KbQaRecordMapper recordMapper;

    @Autowired
    private KbQaCitationMapper citationMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 写入一条问答记录及其引用明细。
     *
     * @param question    用户原始问题
     * @param answer      模型回答（兜底文案也算）
     * @param staffId     提问者；为 null 时整条跳过
     * @param evidenceLevel 证据等级；非知识分支传 null
     * @param answered    是否真正作答（知识分支且证据非 NONE）
     * @param usedResults 实际进入提示词的检索结果；非知识分支传空列表
     */
    public void save(String question, String answer, Integer staffId, String evidenceLevel,
                     boolean answered, List<SearchResult> usedResults) {
        if (staffId == null) {
            log.debug("Skip QA record: unknown operator");
            return;
        }
        List<SearchResult> cited = citable(usedResults);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                KbQaRecord record = new KbQaRecord()
                        .setQuestion(question)
                        .setAnswer(answer)
                        .setStaffId(staffId)
                        .setEvidenceLevel(evidenceLevel)
                        .setAnswered(answered)
                        .setCitationCount(cited.size())
                        .setEndpoint("chat/chat")
                        .setSuccess(true)
                        .setCreateTime(LocalDateTime.now());
                recordMapper.insert(record);

                for (SearchResult sr : cited) {
                    citationMapper.insert(toCitation(record.getId(), sr));
                }
            });
        } catch (Exception e) {
            log.warn("Failed to save QA record: {}", e.getMessage(), e);
        }
    }

    /** 只有真正被检索命中的切片算引用；邻窗扩展只是让上下文连贯，不是模型依据。 */
    private List<SearchResult> citable(List<SearchResult> usedResults) {
        if (usedResults == null || usedResults.isEmpty()) {
            return List.of();
        }
        List<SearchResult> result = new ArrayList<>(usedResults.size());
        for (SearchResult sr : usedResults) {
            if (sr == null || "neighbor".equals(sr.source())) {
                continue;
            }
            result.add(sr);
        }
        return result;
    }

    private KbQaCitation toCitation(Long recordId, SearchResult sr) {
        SearchResult.ChannelScores channels = sr.channels();
        return new KbQaCitation()
                .setQaRecordId(recordId)
                .setChunkId(sr.chunkId())
                .setDocumentName(sr.documentName())
                .setChunkText(truncate(sr.chunkText()))
                .setRelevanceScore(sr.relevance())
                .setRetrievalSource(channels == null ? null : channels.retrievalSource())
                .setVectorScore(channels == null ? null : channels.vector())
                .setKeywordScore(channels == null ? null : channels.keyword())
                .setHybridScore(channels == null ? null : channels.hybrid());
    }

    private String truncate(String text) {
        if (text == null || text.length() <= SNAPSHOT_MAX_CHARS) {
            return text;
        }
        return text.substring(0, SNAPSHOT_MAX_CHARS);
    }
}
