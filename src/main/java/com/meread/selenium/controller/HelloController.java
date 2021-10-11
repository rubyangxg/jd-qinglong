package com.meread.selenium.controller;

import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.service.JDService;
import com.meread.selenium.service.WebDriverManager;
import com.meread.selenium.util.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private WebDriverManager factory;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @RequestMapping("/websocket")
    public String getWebSocket() {
        return "ws";
    }

//    @GetMapping(value = "/getScreen")
//    @ResponseBody
//    public JDScreenBean getScreen(HttpSession session) {
//        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
//        if (myChromeClient == null) {
//            return new JDScreenBean("", "", JDScreenBean.PageStatus.SESSION_EXPIRED);
//        }
//        JDScreenBean screen = service.getScreen(myChromeClient);
//        if (screen.getPageStatus().equals(JDScreenBean.PageStatus.SUCCESS_CK)) {
//            log.info("已经获取到ck了 " + myChromeClient + ", ck = " + screen.getCk());
//            String xddRes = service.doXDDNotify(screen.getCk().toString());
//            log.info("doXDDNotify res = " + xddRes);
//        }
//        return screen;
//    }

    @GetMapping(value = "/sendAuthCode")
    @ResponseBody
    public JDOpResultBean sendAuthCode(HttpSession session) {
        try {
            MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
            if (myChromeClient == null) {
                JDScreenBean screen = new JDScreenBean("", "", JDScreenBean.PageStatus.SESSION_EXPIRED);
                return new JDOpResultBean(screen, false);
            }
            boolean success = service.sendAuthCode(myChromeClient);
            JDScreenBean screen = service.getScreen(myChromeClient);
            if (screen.getPageStatus() != JDScreenBean.PageStatus.NORMAL) {
                success = false;
            }
            return new JDOpResultBean(screen, success);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new JDOpResultBean(new JDScreenBean("", "", JDScreenBean.PageStatus.INTERNAL_ERROR), false);
    }

    @GetMapping(value = "/crackCaptcha")
    @ResponseBody
    public JDOpResultBean crackCaptcha(HttpSession session) {
        boolean crackSuccess = false;
        //请求一个sessionId
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return new JDOpResultBean(service.getScreen(myChromeClient), false);
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                service.crackCaptcha(myChromeClient);
                JDScreenBean screen = service.getScreen(myChromeClient);
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
        return new JDOpResultBean(service.getScreen(myChromeClient), crackSuccess);
    }

    @GetMapping({"/"})
    public String index(
            @RequestParam(defaultValue = "phone") String jdLoginType,
            @RequestParam(defaultValue = "0") String reset,
            HttpSession session, Model model) {
        model.addAttribute("debug", this.debug);
        int qlUploadDirect = service.getQLUploadDirectConfig();
        model.addAttribute("qlUploadDirect", qlUploadDirect);
        model.addAttribute("qlConfigs", factory.getQlConfigs());
        model.addAttribute("initSuccess", factory.isInitSuccess());
        model.addAttribute("indexNotice", factory.getProperties().getProperty("INDEX.NOTICE"));
        model.addAttribute("indexTitle", factory.getProperties().getProperty("INDEX.TITLE"));

        try {
            JDLoginType.valueOf(jdLoginType);
        } catch (IllegalArgumentException e) {
            jdLoginType = "phone";
        }
        model.addAttribute("jdLoginType", jdLoginType);
        if (!factory.isInitSuccess()) {
            return "login";
        }

        String servletSessionId = session.getId();
        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(servletSessionId);
        if (cacheMyChromeClient == null) {
            cacheMyChromeClient = factory.createNewMyChromeClient(servletSessionId, LoginType.WEB, JDLoginType.valueOf(jdLoginType));
        }

        if (cacheMyChromeClient == null) {
            model.addAttribute("error", "1");
            return "login";
        } else {
            cacheMyChromeClient.setJdLoginType(JDLoginType.valueOf(jdLoginType));
        }

        if ("1".equals(reset)) {
            boolean b = service.toJDlogin(cacheMyChromeClient);
            if (!b) {
                log.error("跳转登录页失败");
            }
        }
        JDCookie ck;
        try {
            ck = service.getJDCookies(cacheMyChromeClient);
            if (!ck.isEmpty()) {
                model.addAttribute("ck", ck.toString());
            } else {
                boolean b = service.toJDlogin(cacheMyChromeClient);
                if (!b) {
                    log.error("跳转登录页失败");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getJDCookies " + cacheMyChromeClient.getUserTrackId() + " error!");
        }
        return "login";
    }

    @PostMapping({"/jdLogin"})
    @ResponseBody
    public String login(HttpSession session, @RequestParam("phone") String phone) {
        // 在session中保存用户信息
        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (cacheMyChromeClient == null) {
            return "-1";
        }
        try {
            service.jdLogin(cacheMyChromeClient);
            return phone;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "0";
        }
    }

    @PostMapping({"/uploadQingLong"})
    @ResponseBody
    public JSONObject uploadQingLong(@RequestParam(value = "chooseQLId", required = false) Set<Integer> chooseQLId,
                                     @RequestParam(value = "phone", defaultValue = "无手机号") String phone,
                                     @RequestParam(value = "remark", defaultValue = "") String remark,
                                     @RequestParam("ck") String ck,
                                     HttpSession httpSession) {
        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(httpSession.getId());
        if (cacheMyChromeClient == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", 0);
            return jsonObject;
        }
        int qlUploadDirect = service.getQLUploadDirectConfig();
        return service.uploadQingLong(chooseQLId, phone, remark, ck, cacheMyChromeClient.getChromeSessionId(), qlUploadDirect);
    }

    @PostMapping({"/chooseQingLong"})
    @ResponseBody
    public JSONObject chooseQingLong(@RequestParam(value = "phone", defaultValue = "无手机号") String phone,
                                     @RequestParam(value = "remark", defaultValue = "") String remark,
                                     @RequestParam("ck") String ck) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        Map<String, Object> map = new HashMap<>();
        if (factory.getQlConfigs() != null && !factory.getQlConfigs().isEmpty()) {
            map.put("qlConfigs", factory.getQlConfigs());
            map.put("phone", phone);
            map.put("ck", ck);
            map.put("remark", remark);
            try {
                Template template = freeMarkerConfigurer.getConfiguration().getTemplate("fragment/chooseQL.ftl");
                String process = FreemarkerUtils.process(template, map);
                log.debug(process);
                jsonObject.put("html", process);
                jsonObject.put("status", 1);
            } catch (IOException | TemplateException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    @GetMapping({"/releaseSession"})
    @ResponseBody
    public int releaseSession(HttpSession session) {
        // 在session中保存用户信息
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return 1;
        }
        factory.releaseWebDriver(myChromeClient.getChromeSessionId());
        return 1;
    }

    @PostMapping(value = "/control")
    @ResponseBody
    public int control(@RequestParam("currId") String currId, @RequestParam("currValue") String currValue, HttpSession session) {
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return -1;
        }
        service.controlChrome(myChromeClient, currId, currValue);
        return 1;
    }

}