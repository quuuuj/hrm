package com.qiujie.util;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties // 指明前缀，因为没有前缀，所以不用指明，默认为”“即可
public class HolidayConfig {
    private List<String> holidays;

    /**
     * 周末补班（调休上班）日——国家因法定节假日调休把周六/周日设为工作日的日期，yyyy-MM-dd
     */
    private List<String> makeupWorkdays;
}
