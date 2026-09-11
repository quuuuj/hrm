package com.qiujie.docs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterCommand;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService.RegisterResult;
import com.qiujie.knowledge.mapper.DocsMapper;
import com.qiujie.filetask.spi.UploadSessionInfo;
import com.qiujie.common.storage.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DocsUploadCompletionHandler 单元测试")
class DocsUploadCompletionHandlerUnitTest {

    private DocsUploadCompletionHandler handler;
    private DocsMapper docsMapper;
    private MinioStorageService storageService;
    private DocumentLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        docsMapper = mock(DocsMapper.class);
        storageService = mock(MinioStorageService.class);
        lifecycle = mock(DocumentLifecycleService.class);

        handler = new DocsUploadCompletionHandler();
        ReflectionTestUtils.setField(handler, "docsMapper", docsMapper);
        ReflectionTestUtils.setField(handler, "storageService", storageService);
        ReflectionTestUtils.setField(handler, "lifecycle", lifecycle);
    }

    @Test
    @DisplayName("checkDedup：命中存活文档时返回去重元数据")
    void checkDedup_Hit_ShouldReturnMetadata() {
        Docs existingDoc = new Docs()
                .setId(100)
                .setName("docs/test.txt")
                .setOldName("测试.txt")
                .setKbStatus("READY");

        when(docsMapper.selectList(any())).thenReturn(List.of(existingDoc));

        Map<String, Object> result = handler.checkDedup("hash123");
        assertNotNull(result);
        assertEquals(100, result.get("id"));
        assertEquals("docs/test.txt", result.get("name"));
        assertEquals("测试.txt", result.get("oldName"));
        assertEquals("READY", result.get("kbStatus"));
    }

    @Test
    @DisplayName("checkDedup：无匹配或已被软删除时返回 null")
    void checkDedup_Miss_ShouldReturnNull() {
        when(docsMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = handler.checkDedup("hashNotFound");
        assertNull(result);
    }

    @Test
    @DisplayName("onComplete：ingest=true 时调用 lifecycle.register 走知识库摄入")
    void onComplete_IngestTrue_ShouldRegisterKnowledge() {
        byte[] content = "知识库测试文件内容段落".getBytes(StandardCharsets.UTF_8);
        when(storageService.get("docs/kb_file.txt")).thenReturn(new ByteArrayInputStream(content));

        UploadSessionInfo session = new UploadSessionInfo(
                "upload-1", "知识文档.txt", "txt", (long) content.length, "hash-kb", 5, 1, true);

        when(lifecycle.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(501L, "UPLOADED"));

        Map<String, Object> result = handler.onComplete("docs/kb_file.txt", session);

        assertNotNull(result);
        assertEquals(501L, result.get("documentId"));
        assertEquals("UPLOADED", result.get("kbStatus"));

        ArgumentCaptor<RegisterCommand> captor = ArgumentCaptor.forClass(RegisterCommand.class);
        verify(lifecycle).register(captor.capture());
        RegisterCommand cmd = captor.getValue();
        assertEquals("docs/kb_file.txt", cmd.name());
        assertEquals("知识文档.txt", cmd.oldName());
        assertEquals("txt", cmd.type());
        assertEquals("hash-kb", cmd.fileHash());
        assertEquals((long) content.length, cmd.fileSize());
        assertEquals(5, cmd.staffId());

        verify(docsMapper, never()).insert(any(Docs.class));
    }

    @Test
    @DisplayName("onComplete：ingest=false 时直接写入 sys_docs 通用存储")
    void onComplete_IngestFalse_ShouldInsertDocs() {
        byte[] content = "通用文件内容".getBytes(StandardCharsets.UTF_8);
        when(storageService.get("docs/normal_file.txt")).thenReturn(new ByteArrayInputStream(content));

        UploadSessionInfo session = new UploadSessionInfo(
                "upload-2", "普通表格.xlsx", "xlsx", (long) content.length, "hash-normal", 9, 1, false);

        doAnswer(invocation -> {
            Docs doc = invocation.getArgument(0);
            doc.setId(201);
            return 1;
        }).when(docsMapper).insert(any(Docs.class));

        Map<String, Object> result = handler.onComplete("docs/normal_file.txt", session);

        assertNotNull(result);
        assertEquals(201, result.get("id"));
        assertEquals("docs/normal_file.txt", result.get("name"));
        assertNotNull(result.get("compressed"));

        ArgumentCaptor<Docs> captor = ArgumentCaptor.forClass(Docs.class);
        verify(docsMapper).insert(captor.capture());
        Docs inserted = captor.getValue();
        assertEquals("docs/normal_file.txt", inserted.getName());
        assertEquals("普通表格.xlsx", inserted.getOldName());
        assertEquals("xlsx", inserted.getType());
        assertEquals("hash-normal", inserted.getFileHash());
        assertEquals(9, inserted.getStaffId());
        assertNull(inserted.getKbStatus());

        verify(lifecycle, never()).register(any());
    }
}
