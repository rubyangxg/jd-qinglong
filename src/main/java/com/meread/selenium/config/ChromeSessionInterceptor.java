package com.meread.selenium.config;

import com.meread.selenium.WebDriverManager;
import com.meread.selenium.bean.MyChromeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yangxg
 */
@Component
public class ChromeSessionInterceptor implements HandlerInterceptor {
    Logger logger = LoggerFactory.getLogger(ChromeSessionInterceptor.class);

    @Autowired
    private WebDriverManager driverFactory;

    /**
     * 在请求到达Controller控制器之前 通过拦截器执行一段代码
     * 如果方法返回true,继续执行后续操作
     * 如果返回false，执行中断请求处理，请求不会发送到Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String servletSessionId = request.getSession().getId();
        try {
            MyChromeClient cacheMyChromeClient = driverFactory.getCacheMyChromeClient(servletSessionId);
            if (cacheMyChromeClient != null && cacheMyChromeClient.isExpire()) {
                logger.info(cacheMyChromeClient.getChromeSessionId() + "过期了");
                driverFactory.releaseWebDriver(cacheMyChromeClient.getChromeSessionId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}