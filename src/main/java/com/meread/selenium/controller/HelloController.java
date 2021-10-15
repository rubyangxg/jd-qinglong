package com.meread.selenium.controller;

import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.service.BaseWebDriverManager;
import com.meread.selenium.service.JDService;
import com.meread.selenium.util.CommonAttributes;
import com.meread.selenium.util.FreemarkerUtils;
import com.meread.selenium.util.OpenCVUtil;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

@Controller
@Slf4j
public class HelloController {

    public static byte[] exampleBig = new byte[0];
    public static byte[] exampleSmall = new byte[0];

    static {
        try {
            exampleBig = IOUtils.toByteArray(Objects.requireNonNull(OpenCVUtil.class.getClassLoader().getResourceAsStream("static/img/a.jpeg")));
            exampleSmall = IOUtils.toByteArray(Objects.requireNonNull(OpenCVUtil.class.getClassLoader().getResourceAsStream("static/img/a_small.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    private JDService service;

    @Autowired
    private BaseWebDriverManager factory;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

//    @RequestMapping("/websocket")
//    public String getWebSocket() {
//        return "ws";
//    }
//
//    @RequestMapping("/mock")
//    public String mock() {
//        return "mock";
//    }
//
//    @RequestMapping("/mock2")
//    public String mock2() {
//        return "mock2";
//    }

//    @GetMapping(value = "/manualCrack/{type}", produces = MediaType.IMAGE_JPEG_VALUE)
//    @ResponseBody
//    public byte[] manualCrackBig(@PathVariable("type") String type, HttpSession session) {
//        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
//        if (myChromeClient == null) {
//            if ("small".equals(type)) {
//                return exampleSmall;
//            } else if ("big".equals(type)) {
//                return exampleBig;
//            }
//        }
//        JDScreenBean screen = service.getScreen(myChromeClient);
//        if (screen.getPageStatus() == JDScreenBean.PageStatus.REQUIRE_VERIFY) {
//            if ("small".equals(type)) {
//                return screen.getCaptchaImg().getSmall();
//            } else if ("big".equals(type)) {
//                return screen.getCaptchaImg().getBig();
//            }
//        }
//        return null;
//    }

    @RequestMapping("/verifyCaptcha")
    @ResponseBody
    public boolean verifyCaptcha(@RequestParam String datas, HttpSession session) {
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return false;
        }
        if (!StringUtils.isEmpty(datas)) {
            String[] split = datas.split("\\|");
            List<Point> pointList = new ArrayList<>();
            long currTime = 0;
            for (int i = 0; i < split.length; i++) {
                String[] points = split[i].substring(1, split[i].length() - 1).split(",");
                Point point = new Point(Integer.parseInt(points[0].trim()), Integer.parseInt(points[1].trim()), Long.parseLong(points[2].trim()));
                if (currTime == 0 || (point.getTime() - currTime > 50) || i == split.length - 1) {
                    pointList.add(point);
                    currTime = point.getTime();
                }
            }
            log.info("filter size = " + pointList.size());
            if (CommonAttributes.mockCaptcha) {
                service.manualCrackCaptchaMock(myChromeClient, pointList);
                return true;
            }
            return service.manualCrackCaptcha(myChromeClient, pointList);
        }
        return true;
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
        MyChromeClient myChromeClient = factory.getCacheMyChromeClient(session.getId());
        if (myChromeClient == null) {
            return new JDOpResultBean(null, false);
        }
        if (!CommonAttributes.mockCaptcha) {
            service.manualCrackCaptcha(myChromeClient, null);
            JDScreenBean screen = service.getScreen(myChromeClient);
            if (screen.getPageStatus() != JDScreenBean.PageStatus.REQUIRE_VERIFY) {
                crackSuccess = true;
            }
        }
        return new JDOpResultBean(service.getScreen(myChromeClient), crackSuccess);
    }

    @GetMapping({"/"})
    public String index(
            @RequestParam(defaultValue = "phone") String jdLoginType,
            @RequestParam(defaultValue = "0") String reset,
            HttpSession session, Model model) {
        model.addAttribute("debug", CommonAttributes.debug);
        int qlUploadDirect = service.getQLUploadDirectConfig();
        model.addAttribute("qlUploadDirect", qlUploadDirect);
        model.addAttribute("qlConfigs", factory.getQlConfigs());
        model.addAttribute("initSuccess", service.isInitSuccess());
        model.addAttribute("mockCaptcha", CommonAttributes.mockCaptcha);
        model.addAttribute("indexNotice", factory.getProperties().getProperty("INDEX.NOTICE"));
        model.addAttribute("indexTitle", factory.getProperties().getProperty("INDEX.TITLE"));

        try {
            JDLoginType.valueOf(jdLoginType);
        } catch (IllegalArgumentException e) {
            jdLoginType = "phone";
        }
        model.addAttribute("jdLoginType", jdLoginType);
        if (!service.isInitSuccess()) {
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
            String chromeSessionId = cacheMyChromeClient.getChromeSessionId();
            factory.releaseWebDriver(chromeSessionId, true);
            return "redirect:/";
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
        factory.releaseWebDriver(myChromeClient.getChromeSessionId(), false);
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