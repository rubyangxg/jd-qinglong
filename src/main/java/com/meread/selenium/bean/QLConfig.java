package com.meread.selenium.bean;

import com.meread.selenium.WebDriverFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.springframework.util.StringUtils;

/**
 * @author yangxg
 * @date 2021/9/14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QLConfig {
    private String qlUrl;
    private String qlUsername;
    private String qlPassword;

    private String qlClientID;
    private String qlClientSecret;

    private QLToken qlToken;

    public boolean isValid() {
        boolean verify1 = !StringUtils.isEmpty(qlUrl);
        boolean verify2 = verify1 && !StringUtils.isEmpty(qlUsername) && !StringUtils.isEmpty(qlPassword);
        boolean verify3 = !StringUtils.isEmpty(qlClientID) && !StringUtils.isEmpty(qlClientSecret);
        return verify1 && (verify2 || verify3);
    }

    public enum QLLoginType {
        USERNAME_PASSWORD,
        TOKEN;
    }

    private QLConfig.QLLoginType qlLoginType;
}
