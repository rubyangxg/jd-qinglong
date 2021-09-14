package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.meread.selenium.bean.AssignSessionIdStatus;
import com.meread.selenium.bean.JDOpResultBean;
import com.meread.selenium.bean.JDScreenBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
public class HelloController {

    @Value("${jd.debug}")
    private boolean debug;

    @Autowired
    private JDService service;

    @Autowired
    private WebDriverFactory factory;

    @GetMapping(value = "/getScreen")
    @ResponseBody
    public JDScreenBean getScreen(@RequestParam("clientSessionId") String clientSessionId) {
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return new JDScreenBean("", JDScreenBean.PageStatus.SESSION_EXPIRED);
        }
        JDScreenBean screen = service.getScreen(sessionId);
        if (screen.getPageStatus().equals(JDScreenBean.PageStatus.SUCCESS_CK)) {
            log.info("已经获取到ck了 " + sessionId + ", ck = " + screen.getCk());
        }
        return screen;
    }

    @GetMapping(value = "/sendAuthCode")
    @ResponseBody
    public JDOpResultBean sendAuthCode(@RequestParam("clientSessionId") String clientSessionId) {
        try {
            String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
            if (sessionId == null) {
                JDScreenBean screen = new JDScreenBean("", JDScreenBean.PageStatus.SESSION_EXPIRED);
                return new JDOpResultBean(screen, false);
            }
            boolean success = service.sendAuthCode(sessionId);
            JDScreenBean screen = service.getScreen(sessionId);
            if (screen.getPageStatus() != JDScreenBean.PageStatus.NORMAL) {
                success = false;
            }
            return new JDOpResultBean(screen, success);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new JDOpResultBean(new JDScreenBean("", JDScreenBean.PageStatus.INTERNAL_ERROR), false);
    }

    @GetMapping(value = "/crackCaptcha")
    @ResponseBody
    public JDOpResultBean crackCaptcha(@RequestParam("clientSessionId") String clientSessionId) {
        boolean crackSuccess = false;
        //请求一个sessionId
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return new JDOpResultBean(service.getScreen(sessionId), false);
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                service.crackCaptcha(sessionId);
                JDScreenBean screen = service.getScreen(sessionId);
                if (screen.getPageStatus() != JDScreenBean.PageStatus.REQUIRE_VERIFY) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });
        try {
            crackSuccess = future.get(10, TimeUnit.SECONDS);
            if (!crackSuccess) {
                crackSuccess = true;
            }
        } catch (Exception ignored) {
        }
        return new JDOpResultBean(service.getScreen(sessionId), crackSuccess);
    }

    @GetMapping({"/"})
    public String index(HttpServletRequest httpRequest, Model model) {
        String debug = httpRequest.getParameter("debug");
        String servletSessionId = httpRequest.getSession().getId();
        model.addAttribute("debug", this.debug);
        if (!StringUtils.isEmpty(debug)) {
            int i = Integer.parseInt(debug);
            model.addAttribute("debug", i == 1);
        }
        //请求一个sessionId
        AssignSessionIdStatus status = factory.assignSessionId(httpRequest.getParameter("clientSessionId"), true, servletSessionId);
        log.info("index : " + JSON.toJSONString(status));
        if (status.getAssignSessionId() == null) {
            //分配sessionid失败
            //使前端垃圾cookie失效
            if (status.getClientSessionId() != null) {
                log.info("pre release : " + JSON.toJSONString(status));
                factory.releaseWebDriver(status.getClientSessionId());
            }
            model.addAttribute("error", true);
            return "login";
        } else {
            if (status.isNew()) {
                factory.bindSessionId(status.getAssignSessionId());
            }
        }

        String reset = httpRequest.getParameter("reset");
        if ("1".equals(reset)) {
            service.reset(status.getAssignSessionId());
        }
        String ck = null;
        try {
            ck = service.getJDCookies(status.getAssignSessionId());
            if (!StringUtils.isEmpty(ck)) {
                model.addAttribute("ck", ck);
            } else {
                service.toJDlogin(status.getAssignSessionId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getJDCookies " + status.getAssignSessionId() + " error!");
            factory.unBindSessionId(status.getAssignSessionId(), servletSessionId);
        }
        model.addAttribute("clientSessionId", status.getAssignSessionId());
        return "login";
    }

    @PostMapping({"/jdLogin"})
    @ResponseBody
    public String login(@RequestParam("clientSessionId") String clientSessionId, HttpServletResponse response, @RequestParam("phone") String phone,
                        @RequestParam("sms_code") String sms_code, Model model) {
        // 在session中保存用户信息
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return "-1";
        }
        try {
            service.jdLogin(sessionId);
            return phone;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "0";
        }
    }

    @PostMapping({"/uploadQingLong"})
    @ResponseBody
    public int uploadQingLong(@RequestParam("clientSessionId") String clientSessionId, @RequestParam("phone") String phone, @RequestParam("ck") String ck, HttpServletResponse response) {
        // 在session中保存用户信息
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return -1;
        }
        try {
            if (factory.getQlLoginType() == WebDriverFactory.QLLoginType.TOKEN) {
                return service.uploadQingLongWithToken(ck, phone, factory.getQlToken().getToken());
            } else {
                return service.uploadQingLong(sessionId, ck, phone);
            }
        } finally {
            factory.releaseWebDriver(sessionId);
        }
    }

    @PostMapping({"/uploadQingLongWithToken"})
    @ResponseBody
    public int uploadQingLongWithToken(@RequestParam("clientSessionId") String clientSessionId, @RequestParam("phone") String phone, @RequestParam("ck") String ck, HttpServletResponse response) {
        // 在session中保存用户信息
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return -1;
        }
        try {
            return service.uploadQingLong(sessionId, ck, phone);
        } finally {
            factory.releaseWebDriver(sessionId);
        }
    }

    @GetMapping({"/releaseSession"})
    @ResponseBody
    public int releaseSession(@RequestParam("clientSessionId") String clientSessionId) {
        // 在session中保存用户信息
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return -1;
        }
        factory.releaseWebDriver(sessionId);
        return 1;
    }

    @PostMapping(value = "/control")
    @ResponseBody
    public int control(@RequestParam("currId") String currId, @RequestParam("currValue") String currValue, @RequestParam("clientSessionId") String clientSessionId) {
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return -1;
        }
        service.controlChrome(sessionId, currId, currValue);
        return 1;
    }

}