package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by yangxg on 2021/9/3
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
public class JDOpResultBean {
    private JDScreenBean screenBean;
    private boolean success;
}
