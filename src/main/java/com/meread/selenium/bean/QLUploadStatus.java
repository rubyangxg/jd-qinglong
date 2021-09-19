package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by yangxg on 2021/9/15
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
public class QLUploadStatus {
    private QLConfig qlConfig;
    private int uploadStatus;
    private boolean isFull;
    private String pushRes;
    private String xddRes;
}
