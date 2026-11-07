package com.qiujie.filetask.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskModuleEnum {

    ATTENDANCE("ATTENDANCE"),
    SALARY("SALARY"),
    STAFF_OVERTIME("STAFF_OVERTIME");

    @EnumValue
    private final String value;
}
