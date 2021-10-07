package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by yangxg on 2021/9/3
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
public class JDScreenBean {
    public enum PageStatus {
        EMPTY_URL("空白页面 data:"),
        VERIFY_FAILED("验证失败，请重新验证"),
        SESSION_EXPIRED("浏览器sessionId失效，请重新获取"),
        REQUIRE_VERIFY("安全验证"),

        REQUIRE_SCANQR("需要扫码"),
        REQUIRE_REFRESH("二维码失效，请点击刷新"),
        WAIT_QR_CONFIRM("扫描成功，请在手机确认"),

        VERIFY_CODE_MAX("对不起，短信验证码发送次数已达上限，请24小时后再试"),
        VERIFY_FAILED_MAX("验证码错误多次，请重新获取"),
        SUCCESS_CK("已经获取到ck了"),
        NORMAL("正常页面"),
        SWITCH_SMS_LOGIN("需要切换到短信验证码登录"),
        AGREE_AGREEMENT("若您输入的手机号未注册，将为您直接注册，注册即视为同意"),
        INTERNAL_ERROR("抛异常了");
        private final String desc;
        PageStatus(String s) {
            this.desc = s;
        }

        public String getDesc() {
            return desc;
        }
    }
    private String screen;
    private String qr;
    private JDCookie ck;
    private PageStatus pageStatus;
    //手机验证码重新获取倒计时
    private int authCodeCountDown;
    //登录按钮是否可点击
    private boolean canClickLogin;
    //获取验证码是否可点击
    private boolean canSendAuth;
    //为了防止前端一直占用webdriver，5分钟要释放掉
    private Long sessionTimeOut;
    //统计信息
    private StatClient statClient;
    //截屏时间
    private long snapshotTime;

    public JDScreenBean(String screen,String qr, PageStatus pageStatus) {
        this.screen = screen;
        this.qr = qr;
        this.pageStatus = pageStatus;
        this.snapshotTime = System.currentTimeMillis();
    }

    public JDScreenBean(String screen,String qr, PageStatus pageStatus,JDCookie ck) {
        this.screen = screen;
        this.qr = qr;
        this.pageStatus = pageStatus;
        this.ck = ck;
        this.snapshotTime = System.currentTimeMillis();
    }
}
