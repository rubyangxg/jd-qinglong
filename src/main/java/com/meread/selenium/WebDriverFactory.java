package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yangxg
 * @date 2021/9/7
 */
@Component
@Slf4j
public class WebDriverFactory implements CommandLineRunner {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JDService jdService;

    @Value("${selenium.hub.url}")
    private String seleniumHubUrl;

    @Value("${selenium.hub.status.url}")
    private String seleniumHubStatusUrl;

    @Value("${op.timeout}")
    private int opTimeout;

    public static final String CLIENT_SESSION_ID_KEY = "client:session";

    private List<QLConfig> qlConfigs;

    private static int capacity = 0;

    public volatile boolean stopSchedule = false;
    public volatile boolean runningSchedule = false;

    private List<MyChrome> chromes;

    public List<MyChrome> getChromes() {
        return chromes;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void heartbeat() {
        if (CollectionUtils.isEmpty(chromes)) {
            return;
        }
        runningSchedule = true;
        if (!stopSchedule) {
            List<NodeStatus> nss = getGridStatus();
            Iterator<MyChrome> iterator = chromes.iterator();
            while (iterator.hasNext()) {
                MyChrome myChrome = iterator.next();
                SessionId s = myChrome.getWebDriver().getSessionId();
                if (s == null) {
                    iterator.remove();
                    log.warn("quit a chrome");
                    continue;
                }
                String sessionId = s.toString();
                boolean find = false;
                for (NodeStatus ns : nss) {
                    List<SlotStatus> slotStatus = ns.getSlotStatus();
                    for (SlotStatus ss : slotStatus) {
                        if (sessionId.equals(ss.getSessionId())) {
                            find = true;
                            break;
                        }
                    }
                    if (find) {
                        break;
                    }
                }
                //如果session不存在，则remove
                if (!find) {
                    iterator.remove();
                    log.warn("quit a chrome");
                }
            }
            int shouldCreate = capacity - chromes.size();
            if (shouldCreate > 0) {
                try {
                    RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), getChromeOptions());
                    MyChrome myChrome = new MyChrome();
                    myChrome.setWebDriver(webDriver);
                    log.warn("create a chrome " + webDriver.getSessionId().toString());
                    chromes.add(myChrome);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            inflate(chromes, getGridStatus());
        }
        runningSchedule = false;
    }

    @Autowired
    private RestTemplate restTemplate;

    public List<NodeStatus> getGridStatus() {
        String url = seleniumHubStatusUrl;
        String json = restTemplate.getForObject(url, String.class);
        JSONObject value = JSON.parseObject(json).getJSONObject("value");
        Boolean ready = value.getBoolean("ready");
        List<NodeStatus> res = new ArrayList<>();
        if (ready) {
            JSONArray nodes = value.getJSONArray("nodes");
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                NodeStatus status = new NodeStatus();

                String uri = node.getString("uri");
                String nodeStatusUrl = String.format("%s/status", uri);
                String nodeStatusJson = restTemplate.getForObject(nodeStatusUrl, String.class);
                boolean nodeReady = JSON.parseObject(nodeStatusJson).getJSONObject("value").getBooleanValue("ready");
                status.setFullSession(!nodeReady);
                status.setMaxSessions(node.getInteger("maxSessions"));

                String availability = node.getString("availability");
                status.setAvailability(availability);
                if ("UP".equals(availability)) {
                    JSONArray sessions = node.getJSONArray("slots");
                    List<SlotStatus> sss = new ArrayList<>();
                    for (int s = 0; s < sessions.size(); s++) {
                        JSONObject currSession = sessions.getJSONObject(s).getJSONObject("session");
                        SlotStatus ss = new SlotStatus();
                        Date start = null;
                        String sessionId = null;
                        if (currSession != null) {
                            String locald = currSession.getString("start");
                            locald = locald.substring(0, locald.lastIndexOf(".")) + " UTC";
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
                            try {
                                start = sdf.parse(locald);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            sessionId = currSession.getString("sessionId");
                        }
                        ss.setSessionStartTime(start);
                        ss.setSessionId(sessionId);
                        ss.setBelongsToUri(node.getString("uri"));
                        sss.add(ss);
                    }
                    status.setSlotStatus(sss);
                    status.setUri(uri);
                    status.setNodeId(node.getString("id"));
                }

                res.add(status);
            }
        }
        return res;
    }

    public void closeSession(String uri, String sessionId) {
        String deleteUrl = String.format("%s/session/%s", uri, sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-REGISTRATION-SECRET", "");
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = null;
        try {
            exchange = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, request, String.class);
            log.info("close sessionId : " + sessionId + " resp : " + exchange.getBody() + ", resp code : " + exchange.getStatusCode());
        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

    private void inflate(List<MyChrome> chromes, List<NodeStatus> statusList) {
        for (MyChrome chrome : chromes) {
            for (NodeStatus status : statusList) {
                String currSessionId = chrome.getWebDriver().getSessionId().toString();
                List<SlotStatus> slotStatus = status.getSlotStatus();
                boolean find = false;
                for (SlotStatus ss : slotStatus) {
                    if (currSessionId.equals(ss.getSessionId())) {
                        chrome.setSlotStatus(ss);
                        find = true;
                        break;
                    }
                }
                if (find) {
                    break;
                }
            }
        }
    }

    @Override
    public void run(String... args) throws MalformedURLException {
        stopSchedule = true;

        qlConfigs = parseMultiQLConfig();
        if (qlConfigs.isEmpty()) {
            log.warn("请配置至少一个青龙面板地址! 否则获取到的ck无法上传");
        }


        ChromeOptions chromeOptions = getChromeOptions();
        chromes = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<List<NodeStatus>> startSeleniumRes = CompletableFuture.supplyAsync(() -> {
            while (true) {
                List<NodeStatus> statusList = getGridStatus();
                if (statusList.size() > 0) {
                    return statusList;
                }
            }
        }, executor);
        List<NodeStatus> statusList = null;
        try {
            statusList = startSeleniumRes.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();
        if (statusList == null) {
            throw new RuntimeException("Selenium 浏览器组初始化失败");
        }

        for (NodeStatus status : statusList) {
            List<SlotStatus> sss = status.getSlotStatus();
            for (SlotStatus ss : sss) {
                if (ss != null && ss.getSessionId() != null) {
                    try {
                        closeSession(status.getUri(), ss.getSessionId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            capacity += status.getMaxSessions();
        }

        if (capacity <= 0) {
            log.error("capacity <= 0");
            throw new RuntimeException("无法创建浏览器实例");
        }

        for (int i = 0; i < capacity / 2; i++) {
            RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), chromeOptions);
            webDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
            MyChrome myChrome = new MyChrome();
            myChrome.setWebDriver(webDriver);
            chromes.add(myChrome);
        }

        inflate(chromes, getGridStatus());
        log.info("启动成功!");
        stopSchedule = false;
    }

    private List<QLConfig> parseMultiQLConfig() {
        List<QLConfig> qlConfigs = new ArrayList<>();
        Map<String, String> env = System.getenv();
        for (int i = 1; i <= 5; i++) {
            QLConfig config = new QLConfig();
            config.setId(i);
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.equals("QL_USERNAME_" + i)) {
                    config.setQlUsername(value);
                } else if (key.equals("QL_URL_" + i)) {
                    if (value.endsWith("/")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    config.setQlUrl(value);
                } else if (key.equals("QL_PASSWORD_" + i)) {
                    config.setQlPassword(value);
                } else if (key.equals("QL_CLIENTID_" + i)) {
                    config.setQlClientID(value);
                } else if (key.equals("QL_SECRET_" + i)) {
                    config.setQlClientSecret(value);
                } else if (key.equals("QL_LABEL_" + i)) {
                    config.setLabel(value);
                } else if (key.equals("QL_CAPACITY_" + i)) {
                    config.setCapacity(Integer.parseInt(value.trim()));
                }
            }
            if (config.isValid()) {
                qlConfigs.add(config);
            }
        }

        //兼容老的逻辑，只支持单个青龙
        QLConfig config = new QLConfig();
        config.setQlUrl(System.getenv("ql.url"));
        config.setQlUsername(System.getenv("ql.username"));
        config.setQlPassword(System.getenv("ql.password"));
        config.setQlClientID(System.getenv("ql.clientId"));
        config.setQlClientSecret(System.getenv("ql.clientSecret"));
        config.setLabel(System.getenv("ql.label"));
        if (config.isValid()) {
            qlConfigs.add(config);
        }

        log.info("解析" + qlConfigs.size() + "套配置");

        Iterator<QLConfig> iterator = qlConfigs.iterator();
        while (iterator.hasNext()) {
            QLConfig qlConfig = iterator.next();

            boolean verify1 = !StringUtils.isEmpty(qlConfig.getQlUrl());
            boolean verify2 = verify1 && !StringUtils.isEmpty(qlConfig.getQlUsername()) && !StringUtils.isEmpty(qlConfig.getQlPassword());
            boolean verify3 = verify1 && !StringUtils.isEmpty(qlConfig.getQlClientID()) && !StringUtils.isEmpty(qlConfig.getQlClientSecret());

            boolean result_token = false;
            boolean result_usernamepassword = false;
            if (verify3) {
                boolean success = getToken(qlConfig);
                if (success) {
                    result_token = true;
                    qlConfig.setQlLoginType(QLConfig.QLLoginType.TOKEN);
                    JSONArray currentCKS = jdService.getCurrentCKS(qlConfig, "");
                    if (currentCKS != null) {
                        qlConfig.setRemain(qlConfig.getCapacity() - currentCKS.size());
                    }
                } else {
                    log.warn(qlConfig.getQlUrl() + "获取token失败，获取到的ck无法上传，已忽略");
                }
            }
            if (verify2) {
                boolean result = false;
                try {
                    result = initInnerQingLong(qlConfig);
                    qlConfig.setQlLoginType(QLConfig.QLLoginType.USERNAME_PASSWORD);
                    JSONArray currentCKS = jdService.getCurrentCKS(qlConfig, "");
                    if (currentCKS != null) {
                        qlConfig.setRemain(qlConfig.getCapacity() - currentCKS.size());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (result) {
                    result_usernamepassword = true;
                } else {
                    log.info("初始化青龙面板" + qlConfig.getQlUrl() + "登录失败, 获取到的ck无法上传，已忽略");
                }
            }

            if (!result_token && !result_usernamepassword) {
                iterator.remove();
            }
        }

        log.info("成功添加" + qlConfigs.size() + "套配置");
        return qlConfigs;
    }

    private boolean getToken(QLConfig qlConfig) {
        String qlUrl = qlConfig.getQlUrl();
        String qlClientID = qlConfig.getQlClientID();
        String qlClientSecret = qlConfig.getQlClientSecret();
        try {
            ResponseEntity<String> entity = restTemplate.getForEntity(qlUrl + "/open/auth/token?client_id=" + qlClientID + "&client_secret=" + qlClientSecret, String.class);
            if (entity.getStatusCodeValue() == 200) {
                String body = entity.getBody();
                log.info("获取token " + body);
                JSONObject jsonObject = JSON.parseObject(body);
                Integer code = jsonObject.getInteger("code");
                if (code == 200) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    String token = data.getString("token");
                    String tokenType = data.getString("token_type");
                    long expiration = data.getLong("expiration");
                    log.info(qlUrl + "获取token成功 " + token);
                    log.info(qlUrl + "获取tokenType成功 " + tokenType);
                    log.info(qlUrl + "获取expiration成功 " + expiration);
                    qlConfig.setQlToken(new QLToken(token, tokenType, expiration));
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(qlUrl + "获取token失败，请检查配置");
        }
        return false;
    }

    public boolean initInnerQingLong(QLConfig qlConfig) throws MalformedURLException {
        String qlUrl = qlConfig.getQlUrl();
        RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), getChromeOptions());
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        try {
            String token = null;
            int retry = 0;
            while (StringUtils.isEmpty(token)) {
                retry++;
                if (retry > 2) {
                    break;
                }
                String qlUsername = qlConfig.getQlUsername();
                String qlPassword = qlConfig.getQlPassword();
                webDriver.get(qlUrl + "/login");
                log.info("initQingLong start : " + qlUrl + "/login");
                boolean b = WebDriverUtil.waitForJStoLoad(webDriver);
                if (b) {
                    webDriver.findElement(By.id("username")).sendKeys(qlUsername);
                    webDriver.findElement(By.id("password")).sendKeys(qlPassword);
                    webDriver.findElement(By.xpath("//button[@type='submit']")).click();
                    Thread.sleep(2000);
                    b = WebDriverUtil.waitForJStoLoad(webDriver);
                    if (b) {
                        RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(webDriver);
                        RemoteWebStorage webStorage = new RemoteWebStorage(executeMethod);
                        LocalStorage storage = webStorage.getLocalStorage();
                        token = storage.getItem("token");
                        log.info("qinglong token " + token);
                        qlConfig.setQlToken(new QLToken(token));
                        readPassword(qlConfig);
                    }
                }
            }
        } catch (Exception e) {
            log.error(qlUrl + "测试登录失败，请检查配置");
        } finally {
            webDriver.quit();
        }
        return qlConfig.getQlToken() != null && qlConfig.getQlToken().getToken() != null;
    }

    private boolean readPassword(QLConfig qlConfig) throws IOException {
        File file = new File("/data/config/auth.json");
        if (file.exists()) {
            String s = FileUtils.readFileToString(file, "utf-8");
            JSONObject jsonObject = JSON.parseObject(s);
            String username = jsonObject.getString("username");
            String password = jsonObject.getString("password");
            if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password) && !"adminadmin".equals(password)) {
                qlConfig.setQlUsername(username);
                qlConfig.setQlPassword(password);
                log.info("username = " + username + ", password = " + password);
                return true;
            }
        }
        return false;
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", true);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        chromeOptions.addArguments("disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--headless");
//        chromeOptions.addArguments("--no-sandbox");
//        chromeOptions.addArguments("--disable-extensions");
//        chromeOptions.addArguments("--disable-software-rasterizer");
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
//        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--window-size=500,700");
        return chromeOptions;
    }

    public RemoteWebDriver getDriverBySessionId(String sessionId) {
        for (MyChrome myChrome : chromes) {
            if (myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                return myChrome.getWebDriver();
            }
        }
        return null;
    }

    public MyChrome getMyChromeBySessionId(String sessionId) {
        if (chromes != null && chromes.size() > 0) {
            for (MyChrome myChrome : chromes) {
                if (myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                    return myChrome;
                }
            }
        }
        return null;
    }

    public synchronized AssignSessionIdStatus assignSessionId(String clientSessionId, boolean create, String servletSessionId) {
        AssignSessionIdStatus status = new AssignSessionIdStatus();

        if (clientSessionId == null && servletSessionId != null) {
//            String s = redisTemplate.opsForValue().get("servlet:session:" + servletSessionId);
            String s = CacheUtil.get("servlet:session:" + servletSessionId);
            if (!StringUtils.isEmpty(s)) {
                clientSessionId = s;
            }
        }

        if (clientSessionId != null) {
            status.setClientSessionId(clientSessionId);
            MyChrome myChrome = getMyChromeBySessionId(clientSessionId);
            if (myChrome != null && myChrome.getClientSessionId() != null) {
                status.setNew(false);
                status.setAssignSessionId(myChrome.getClientSessionId());
//                Long expire = redisTemplate.getExpire();
                Long expire = CacheUtil.getExpire(CLIENT_SESSION_ID_KEY + ":" + clientSessionId);
                if (expire != null && expire < 0) {
                    log.info("force expire " + status.getAssignSessionId());
                    //强制1分钟过期一个sessionId
                    closeSession(myChrome.getSlotStatus().getBelongsToUri(), status.getAssignSessionId());
                    status.setAssignSessionId(null);
                    myChrome.setClientSessionId(null);
                    create = false;
                } else {
                    return status;
                }
            }
        }
        if (create) {
            log.info("开始创建sessionId ");
            if (chromes != null) {
                for (MyChrome myChrome : chromes) {
                    String oldClientSessionId = myChrome.getClientSessionId() == null ? null : myChrome.getClientSessionId();
                    log.info("当前sessionId = " + myChrome.getWebDriver().getSessionId().toString() + ", oldClientSessionId = " + oldClientSessionId);
                    if (oldClientSessionId == null) {
                        String s = myChrome.getWebDriver().getSessionId().toString();
                        status.setAssignSessionId(s);
                        status.setNew(true);
//                    redisTemplate.opsForValue().set("servlet:session:" + servletSessionId, s, 300, TimeUnit.SECONDS);
                        CacheUtil.put("servlet:session:" + servletSessionId, new StringCache(System.currentTimeMillis(),s,300), 300);
                        return status;
                    }
                }
            }
        }
        return status;
    }

    public synchronized void releaseWebDriver(String input) {
//        redisTemplate.delete(CLIENT_SESSION_ID_KEY + ":" + input);
        CacheUtil.remove(CLIENT_SESSION_ID_KEY + ":" + input);
        log.info("releaseWebDriver " + input);
        Iterator<MyChrome> iterator = chromes.iterator();
        while (iterator.hasNext()) {
            MyChrome myChrome = iterator.next();
            String sessionId = myChrome.getWebDriver().getSessionId().toString();
            if (sessionId.equals(input)) {
                String uri = myChrome.getSlotStatus().getBelongsToUri();
                myChrome.getWebDriver().quit();
                iterator.remove();
                log.info("destroy chrome : " + uri + "-->" + sessionId);
                break;
            }
        }
    }

    public synchronized void bindSessionId(String sessionId) {
        for (MyChrome myChrome : chromes) {
            if (myChrome != null && myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                myChrome.setClientSessionId(sessionId);
//                redisTemplate.opsForValue().set(CLIENT_SESSION_ID_KEY + ":" + sessionId, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), opTimeout, TimeUnit.SECONDS);
                CacheUtil.put(CLIENT_SESSION_ID_KEY + ":" + sessionId, new StringCache(System.currentTimeMillis(),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),opTimeout), opTimeout);
                break;
            }
        }
    }

    public synchronized void unBindSessionId(String sessionId, String servletSessionId) {
        Iterator<MyChrome> iterator = chromes.iterator();
//        redisTemplate.delete("servlet:session:" + servletSessionId);
        CacheUtil.remove("servlet:session:" + servletSessionId);
        while (iterator.hasNext()) {
            MyChrome myChrome = iterator.next();
            if (myChrome != null && myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                myChrome.setClientSessionId(null);
//                redisTemplate.delete(CLIENT_SESSION_ID_KEY + ":" + sessionId);
                CacheUtil.remove(CLIENT_SESSION_ID_KEY + ":" + sessionId);
                iterator.remove();
                break;
            }
        }
    }

    public List<QLConfig> getQlConfigs() {
        return qlConfigs;
    }
}
