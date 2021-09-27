package com.meread.selenium;

import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
    public JDScreenBean getScreen(HttpSession session) {
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return new JDScreenBean("", "", JDScreenBean.PageStatus.SESSION_EXPIRED);
        }
        JDScreenBean screen = service.getScreen(myChromeClient);
        if (screen.getPageStatus().equals(JDScreenBean.PageStatus.SUCCESS_CK)) {
            log.info("已经获取到ck了 " + myChromeClient + ", ck = " + screen.getCk());
            String xddRes = service.doXDDNotify(screen.getCk().toString());
            log.info("doXDDNotify res = " + xddRes);
        }
        return screen;
    }

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
    public String index(@RequestParam(value = "jdLoginType", defaultValue = "phone") String jdLoginType, @RequestAttribute HttpServletRequest request, Model model) {
        model.addAttribute("debug", this.debug);
        int qlUploadDirect = qlUploadDirect();
        model.addAttribute("qlUploadDirect", qlUploadDirect);
        model.addAttribute("qlConfigs", factory.getQlConfigs());
        model.addAttribute("initSuccess", factory.isInitSuccess());
        if (!factory.isInitSuccess()) {
            return "login";
        }

        try {
            JDLoginType.valueOf(jdLoginType);
        } catch (IllegalArgumentException e) {
            jdLoginType = "phone";
        }

        String servletSessionId = request.getSession().getId();
        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(servletSessionId);
        if (cacheMyChromeClient == null) {
            cacheMyChromeClient = factory.createNewMyChromeClient(servletSessionId, LoginType.WEB, JDLoginType.valueOf(jdLoginType));
        }

        String reset = request.getParameter("reset");
        if ("1".equals(reset)) {
            service.toJDlogin(cacheMyChromeClient);
        }
        JDCookie ck = null;
        try {
            ck = service.getJDCookies(cacheMyChromeClient);
            if (!ck.isEmpty()) {
                model.addAttribute("ck", ck.toString());
            } else {
                service.toJDlogin(cacheMyChromeClient);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getJDCookies " + cacheMyChromeClient.getUserTrackId() + " error!");
//            factory.unBindSessionId(cacheMyChromeClient.getUserTrackId());
        }
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
        if (factory.getQlConfigs() != null && factory.getQlConfigs().size() <= 1) {
            return 1;
        }
        return qlUploadDirect;
    }

    @PostMapping({"/jdLogin"})
    @ResponseBody
    public String login(@RequestParam("clientSessionId") String clientSessionId, HttpServletResponse response, @RequestParam("phone") String phone,
                        @RequestParam("sms_code") String sms_code, Model model) {
        // 在session中保存用户信息
        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(clientSessionId);
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);

        // 在session中保存用户信息

        MyChromeClient cacheMyChromeClient = factory.getCacheMyChromeClient(httpSession.getId());
        if (cacheMyChromeClient == null) {
            return jsonObject;
        }
        int qlUploadDirect = qlUploadDirect();

        if ((chooseQLId != null && chooseQLId.size() > 0) || qlUploadDirect == 1) {
            List<QLUploadStatus> uploadStatuses = new ArrayList<>();
            if (factory.getQlConfigs() != null) {
                for (QLConfig qlConfig : factory.getQlConfigs()) {
                    if (qlUploadDirect == 1 || chooseQLId.contains(qlConfig.getId())) {
                        if (qlConfig.getQlLoginType() == QLConfig.QLLoginType.TOKEN) {
                            QLUploadStatus status = service.uploadQingLongWithToken(cacheMyChromeClient, ck, phone, remark, qlConfig);
                            log.info("上传" + qlConfig.getQlUrl() + "结果" + status.getUploadStatus());
                            uploadStatuses.add(status);
                        }
                        if (qlConfig.getQlLoginType() == QLConfig.QLLoginType.USERNAME_PASSWORD) {
                            QLUploadStatus status = service.uploadQingLong(cacheMyChromeClient, ck, phone, remark, qlConfig);
                            log.info("上传" + qlConfig.getQlUrl() + "结果" + status.getUploadStatus());
                            uploadStatuses.add(status);
                        }
                    }
                }
            }

            factory.releaseWebDriver(cacheMyChromeClient);

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
                    String label = uploadStatus.getQlConfig().getLabel();
                    if (uploadStatus.getUploadStatus() <= 0) {
                        if (!StringUtils.isEmpty(label)) {
                            msg.append(label);
                        } else {
                            msg.append("QL_URL_").append(uploadStatus.getQlConfig().getId());
                        }
                        msg.append("上传失败<br/>");
                    }
                    if (uploadStatus.isFull()) {
                        if (!StringUtils.isEmpty(label)) {
                            msg.append(label);
                        } else {
                            msg.append("QL_URL_").append(uploadStatus.getQlConfig().getId());
                        }
                        msg.append("超容量了<br/>");
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
    public JSONObject chooseQingLong(@RequestParam("clientSessionId") String clientSessionId,
                                     @RequestParam(value = "phone", defaultValue = "无手机号") String phone,
                                     @RequestParam(value = "remark", defaultValue = "") String remark,
                                     @RequestParam("ck") String ck) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", 0);
        Map<String, Object> map = new HashMap<>();
        if (factory.getQlConfigs() != null && !factory.getQlConfigs().isEmpty()) {
            map.put("qlConfigs", factory.getQlConfigs());
            map.put("clientSessionId", clientSessionId);
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
            return -1;
        }
        factory.releaseWebDriver(myChromeClient);
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