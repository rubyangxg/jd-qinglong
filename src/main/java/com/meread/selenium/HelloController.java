package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.guieffect.qual.UIPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
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

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

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
        int qlUploadDirect = qlUploadDirect();
        model.addAttribute("qlUploadDirect", qlUploadDirect);
        model.addAttribute("qlConfigs", factory.getQlConfigs());
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

    private int qlUploadDirect() {
        String ql_upload_direct = System.getenv("QL_UPLOAD_DIRECT");
        int qlUploadDirect = 0;
        if (!StringUtils.isEmpty(ql_upload_direct)) {
            try {
                qlUploadDirect = Integer.parseInt(ql_upload_direct);
            } catch (NumberFormatException e) {
            }
        }
        if (factory.getQlConfigs().size() <= 1) {
            return 1;
        }
        return qlUploadDirect;
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
    public JSONObject uploadQingLong(@RequestParam(value = "chooseQLId", required = false) Set<Integer> chooseQLId, @RequestParam("clientSessionId") String clientSessionId, @RequestParam(value = "phone", defaultValue = "无手机号") String phone, @RequestParam("ck") String ck, HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);

        // 在session中保存用户信息
        String sessionId = factory.assignSessionId(clientSessionId, false, null).getAssignSessionId();
        if (sessionId == null) {
            return jsonObject;
        }
        int qlUploadDirect = qlUploadDirect();

        if ((chooseQLId != null && chooseQLId.size() > 0) || qlUploadDirect == 1) {
            List<QLUploadStatus> uploadStatuses = new ArrayList<>();
            try {
                if (factory.getQlConfigs() != null) {
                    for (QLConfig qlConfig : factory.getQlConfigs()) {
                        if (qlUploadDirect == 1 || chooseQLId.contains(qlConfig.getId())) {
                            if (qlConfig.getQlLoginType() == QLConfig.QLLoginType.TOKEN) {
                                int i = service.uploadQingLongWithToken(ck, phone, qlConfig);
                                uploadStatuses.add(new QLUploadStatus(qlConfig, i > 0,qlConfig.getRemain() <= 0));
                            }
                            if (qlConfig.getQlLoginType() == QLConfig.QLLoginType.USERNAME_PASSWORD) {
                                int i = service.uploadQingLong(sessionId, ck, phone, qlConfig);
                                uploadStatuses.add(new QLUploadStatus(qlConfig, i > 0,qlConfig.getRemain() <= 0));
                            }
                        }
                    }
                }
            } finally {
                factory.releaseWebDriver(sessionId);
            }

            if (qlUploadDirect != 1) {
                Map<String, Object> map = new HashMap<>();
                map.put("uploadStatuses", uploadStatuses);
                try {
                    Template template = freeMarkerConfigurer.getConfiguration().getTemplate("fragment/uploadRes.ftl");
                    String process = FreemarkerUtils.process(template, map);
                    log.debug(process);
                    jsonObject.put("html", process);
                    jsonObject.put("status", 1);
                } catch (IOException | TemplateException e) {
                    e.printStackTrace();
                }
            } else {
                StringBuilder msg = new StringBuilder();
                for (QLUploadStatus uploadStatus : uploadStatuses) {
                    if (!uploadStatus.isUploadStatus() ) {
                        msg.append("QL_URL_").append(uploadStatus.getQlConfig().getId()).append("上传失败<br/>");
                    }
                    if (uploadStatus.isFull()) {
                        msg.append("QL_URL_").append(uploadStatus.getQlConfig().getId()).append("超容量了<br/>");
                    }
                }
                if (msg.length() > 0) {
                    jsonObject.put("status", -2);
                    jsonObject.put("html", msg.toString());
                    return jsonObject;
                }
                jsonObject.put("status", 2);
            }

        } else {
            jsonObject.put("status", 0);
        }
        return jsonObject;
    }

    @PostMapping({"/chooseQingLong"})
    @ResponseBody
    public JSONObject chooseQingLong(@RequestParam("clientSessionId") String clientSessionId, @RequestParam(value = "phone", defaultValue = "无手机号") String phone, @RequestParam("ck") String ck) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        Map<String, Object> map = new HashMap<>();
        if (factory.getQlConfigs() != null && !factory.getQlConfigs().isEmpty()) {
            map.put("qlConfigs", factory.getQlConfigs());
            map.put("clientSessionId", clientSessionId);
            map.put("phone", phone);
            map.put("ck", ck);
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