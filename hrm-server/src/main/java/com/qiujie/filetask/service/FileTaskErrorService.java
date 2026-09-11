package com.qiujie.filetask.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.mapper.FileTaskErrorMapper;
import org.springframework.stereotype.Service;

@Service
public class FileTaskErrorService extends ServiceImpl<FileTaskErrorMapper, FileTaskError> {
}
