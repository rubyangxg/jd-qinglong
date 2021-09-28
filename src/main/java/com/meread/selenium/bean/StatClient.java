package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by yangxg on 2021/9/28
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
public class StatClient {
    private int availChromeCount;
    private int webSessionCount;
    private int qqSessionCount;
    private int totalChromeCount;
}
