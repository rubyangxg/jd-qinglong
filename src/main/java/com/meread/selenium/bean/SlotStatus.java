package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Created by yangxg on 2021/9/10
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotStatus {
    private String sessionId;
    private Date sessionStartTime;
    private String belongsToUri;
}
