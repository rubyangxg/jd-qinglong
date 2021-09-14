package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @Value("${selenium.hub.url}")
    private String seleniumHubUrl;

    @Value("${selenium.hub.status.url}")
    private String seleniumHubStatusUrl;

    @Value("${op.timeout}")
    private int opTimeout;

    public static final String CLIENT_SESSION_ID_KEY = "client:session";

    private List<QLConfig> qlConfigs;

    private String qlUrl;
    private String qlUsername;
    private String qlPassword;
    private String qlClientID;
    private String qlClientSecret;
    private QLToken qlToken;
    private QLConfig.QLLoginType qlLoginType;

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
        String deleteUrl = String.format("%s/se/grid/node/session/%s", uri, sessionId);
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
        //老的逻辑，只支持单个青龙
        qlUrl = System.getenv("ql.url");
        qlUsername = System.getenv("ql.username");
        qlPassword = System.getenv("ql.password");
        qlClientID = System.getenv("ql.clientId");
        qlClientSecret = System.getenv("ql.clientSecret");

        boolean verify1 = !StringUtils.isEmpty(qlUrl);
        boolean verify2 = verify1 && !StringUtils.isEmpty(qlUsername) && !StringUtils.isEmpty(qlPassword);
        boolean verify3 = !StringUtils.isEmpty(qlClientID) && !StringUtils.isEmpty(qlClientSecret);

        //老的逻辑，支持多个青龙
        List<QLConfig> qlConfigs = parseMultiQLConfig();

        if (!verify1) {
            log.warn("请配置青龙面板地址!");
            throw new RuntimeException("请配置青龙面板地址!");
        }
        if (!verify2 && !verify3) {
            log.warn("请配置青龙面板用户名密码或openapi参数!");
            throw new RuntimeException("请配置青龙面板用户名密码或openapi参数!");
        }
        qlLoginType = verify2 ? QLConfig.QLLoginType.USERNAME_PASSWORD : QLConfig.QLLoginType.TOKEN;

        if (qlLoginType.equals(QLConfig.QLLoginType.TOKEN)) {
            boolean success = getToken(qlClientID, qlClientSecret);
            if (!success) {
                log.warn("获取token失败!");
                throw new RuntimeException("获取token失败!");
            }
        }

        if (qlUrl.endsWith("/")) {
            qlUrl = qlUrl.substring(0, qlUrl.length() - 1);
        }

        int result = initQingLong();
        log.info("初始化青龙面板" + (result == 1 ? "成功" : "失败"));
        log.info("青龙用户名：" + qlUsername);
        log.info("青龙密码：" + qlPassword);

        stopSchedule = true;

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
        File file = new File("/data/env.properties");
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(input);
                Set<String> keys = props.stringPropertyNames();
                for (int i = 1; i <= 5; i++) {
                    QLConfig config = new QLConfig();
                    for (String key : keys) {
                        String value = props.getProperty(key);
                        if (key.equals("QL_USERNAME_" + i)) {
                            config.setQlUsername(value);
                        } else if (key.equals("QL_URL_" + i)) {
                            config.setQlUrl(value);
                        } else if (key.equals("QL_PASSWORD_" + i)) {
                            config.setQlPassword(value);
                        } else if (key.equals("QL_CLIENTID_" + i)) {
                            config.setQlClientID(value);
                        } else if (key.equals("QL_SECRET_" + i)) {
                            config.setQlClientSecret(value);
                        }
                    }
                    if (config.isValid()) {
                        qlConfigs.add(config);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            log.info("/data/env.properties不存在，不解析多套青龙配置");
        }
        log.info("成功解析" + qlConfigs.size() + "套配置");
        return qlConfigs;
    }

    private boolean getToken(String qlClientID, String qlClientSecret) throws MalformedURLException {
        ResponseEntity<String> entity = restTemplate.getForEntity(qlUrl + "/open/auth/token?client_id=" + qlClientID + "&client_secret=" + qlClientSecret, String.class);
        if (entity.getStatusCodeValue() == 200) {
            String body = entity.getBody();
            //{
            //    "code": 200,
            //    "data": {
            //        "token": "f02afc91-24a1-40d6-b316-cf09a3d5ddd8",
            //        "token_type": "Bearer",
            //        "expiration": 1634203177
            //    }
            //}
            log.info("获取token " + body);
            JSONObject jsonObject = JSON.parseObject(body);
            Integer code = jsonObject.getInteger("code");
            if (code == 200) {
                JSONObject data = jsonObject.getJSONObject("data");
                String token = data.getString("token");
                String tokenType = data.getString("token_type");
                long expiration = data.getLong("expiration");
                log.info("获取token成功 " + token);
                log.info("获取tokenType成功 " + tokenType);
                log.info("获取expiration成功 " + expiration);
                qlToken = new QLToken(token, tokenType, expiration);
                return true;
            }
        }
        return false;
    }

    public int initQingLong() throws MalformedURLException {
        RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), getChromeOptions());
        try {
            webDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
            webDriver.get(qlUrl + "/login");
            log.info("initQingLong start : " + qlUrl + "/login");
            if (readPassword()) return 1;
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
                    String token = storage.getItem("token");
                    log.info("qinglong token " + token);
                    if (readPassword()) return 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            webDriver.quit();
        }
        return -1;
    }

    private boolean readPassword() throws IOException {
        File file = new File("/data/config/auth.json");
        if (file.exists()) {
            String s = FileUtils.readFileToString(file, "utf-8");
            JSONObject jsonObject = JSON.parseObject(s);
            String username = jsonObject.getString("username");
            String password = jsonObject.getString("password");
            if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password) && !"adminadmin".equals(password)) {
                this.qlUsername = username;
                this.qlPassword = password;
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
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
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
        for (MyChrome myChrome : chromes) {
            if (myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                return myChrome;
            }
        }
        return null;
    }

    public synchronized AssignSessionIdStatus assignSessionId(String clientSessionId, boolean create, String servletSessionId) {
        AssignSessionIdStatus status = new AssignSessionIdStatus();

        if (clientSessionId == null && servletSessionId != null) {
            String s = redisTemplate.opsForValue().get("servlet:session:" + servletSessionId);
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
                Long expire = redisTemplate.getExpire(CLIENT_SESSION_ID_KEY + ":" + clientSessionId);
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
            for (MyChrome myChrome : chromes) {
                String oldClientSessionId = myChrome.getClientSessionId() == null ? null : myChrome.getClientSessionId();
                log.info("当前sessionId = " + myChrome.getWebDriver().getSessionId().toString() + ", oldClientSessionId = " + oldClientSessionId);
                if (oldClientSessionId == null) {
                    String s = myChrome.getWebDriver().getSessionId().toString();
                    status.setAssignSessionId(s);
                    status.setNew(true);
                    redisTemplate.opsForValue().set("servlet:session:" + servletSessionId, s, 300, TimeUnit.SECONDS);
                    return status;
                }
            }
        }
        return status;
    }

    public synchronized void releaseWebDriver(String input) {
        redisTemplate.delete(CLIENT_SESSION_ID_KEY + ":" + input);
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
                redisTemplate.opsForValue().set(CLIENT_SESSION_ID_KEY + ":" + sessionId, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), opTimeout, TimeUnit.SECONDS);
                break;
            }
        }
    }

    public synchronized void unBindSessionId(String sessionId, String servletSessionId) {
        Iterator<MyChrome> iterator = chromes.iterator();
        redisTemplate.delete("servlet:session:" + servletSessionId);
        while (iterator.hasNext()) {
            MyChrome myChrome = iterator.next();
            if (myChrome != null && myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                myChrome.setClientSessionId(null);
                redisTemplate.delete(CLIENT_SESSION_ID_KEY + ":" + sessionId);
                iterator.remove();
                break;
            }
        }
    }

    public String getQlUrl() {
        return qlUrl;
    }

    public String getQlUsername() {
        return qlUsername;
    }

    public String getQlPassword() {
        return qlPassword;
    }

    public String getQlClientID() {
        return qlClientID;
    }

    public String getQlClientSecret() {
        return qlClientSecret;
    }

    public QLToken getQlToken() {
        return qlToken;
    }

    public QLConfig.QLLoginType getQlLoginType() {
        return qlLoginType;
    }
}
