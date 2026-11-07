package com.qiujie.filetask.spi;

/**
 * 导入行 DTO 的行号契约。
 * <p>
 * 引擎读入每一行时回填 Excel 行号，业务校验失败时据此定位原始行。
 * </p>
 */
public interface ImportRow {

    void setRowNum(Integer rowNum);
}
