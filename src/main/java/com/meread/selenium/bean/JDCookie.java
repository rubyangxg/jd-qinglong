package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * @author yangxg
 * @date 2021/9/18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JDCookie {
    private String ptPin;
    private String ptKey;

    public static JDCookie parse(String ck) {
        JDCookie jdCookie = new JDCookie();
        String[] split = ck.split(";");
        for (String s : split) {
            if (s.startsWith("pt_key")) {
                jdCookie.setPtKey(s.split("=")[1]);
            }
            if (s.startsWith("pt_pin")) {
                jdCookie.setPtPin(s.split("=")[1]);
            }
        }
        return jdCookie;
    }

    @Override
    public String toString() {
        return "pt_key=" + ptKey + ";pt_pin=" + ptPin;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(ptPin) && StringUtils.isEmpty(ptKey);
    }
}
