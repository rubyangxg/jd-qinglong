package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.UnixDocker;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author yangxg
 * @date 2021/9/7
 */
@Component
@Slf4j
public class WebDriverFactory implements CommandLineRunner, InitializingBean {

    @Autowired
    ResourceLoader resourceLoader;

    @Value("${jd.debug}")
    private boolean debug;

    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    private JDService jdService;

    @Value("${selenium.hub.url}")
    private String seleniumHubUrl;

    @Value("${env.path}")
    private String envPath;

    @Value("${selenium.hub.status.url}")
    private String seleniumHubStatusUrl;

    @Value("${op.timeout}")
    private int opTimeout;

    @Value("#{environment.SE_NODE_MAX_SESSIONS}")
    private String maxSessionFromSystemEnv;

    @Value("${SE_NODE_MAX_SESSIONS}")
    private String maxSessionFromProps;

    private String maxSessionFromEnvFile;

    public static final String CLIENT_SESSION_ID_KEY = "client:session";

    private List<QLConfig> qlConfigs;

    public Properties properties = new Properties();

    private String xddUrl;

    private String xddToken;

    private static int CAPACITY = 0;

    public volatile boolean stopSchedule = false;
    public volatile boolean initSuccess = false;
    public volatile boolean runningSchedule = false;

    private List<MyChrome> chromes;

    public List<MyChrome> getChromes() {
        return chromes;
    }

    public ChromeOptions chromeOptions;

    public void init(){
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", true);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        chromeOptions.addArguments("disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.setCapability("browserName", "chrome");
        chromeOptions.setCapability("browserVersion", "89.0");
        chromeOptions.setCapability("screenResolution", "1280x1024x24");
        chromeOptions.setCapability("enableVideo", false);
        chromeOptions.setCapability("selenoid:options", Map.<String, Object>of(
                "enableVNC", debug,
                "enableVideo", false,
                "enableLog", debug,
                "sessionTimeout", "5m"
//                "applicationContainers", new String[]{"webapp"}
        ));
        if (!debug) {
            chromeOptions.addArguments("--headless");
        }
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-software-rasterizer");
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--window-size=500,700");
    }

    public MutableCapabilities getOptions() {
        return chromeOptions;
    }

    @Scheduled(initialDelay = 180000, fixedDelay = 60000)
    public void syncCK_count() {
        if (qlConfigs != null) {
            for (QLConfig qlConfig : qlConfigs) {
                int oldSize = qlConfig.getRemain();
                jdService.fetchCurrentCKS_count(qlConfig, "");
                int newSize = qlConfig.getRemain();
                log.info(qlConfig.getQlUrl() + " 容量从 " + oldSize + "变为" + newSize);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
//    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public void refreshOpenIdToken() {
        if (qlConfigs != null) {
            for (QLConfig qlConfig : qlConfigs) {
                if (qlConfig.getQlLoginType() == QLConfig.QLLoginType.TOKEN) {
                    QLToken qlTokenOld = qlConfig.getQlToken();
                    jdService.fetchNewOpenIdToken(qlConfig);
                    log.info(qlConfig.getQlToken() + " token 从" + qlTokenOld + " 变为 " + qlConfig.getQlToken());
                }
            }
        }
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void heartbeat() {
        runningSchedule = true;
        if (!stopSchedule) {
            SelenoidStatus nss = getGridStatus();
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
                Map<String, JSONObject> sessions = nss.getSessions();
                if (sessions != null) {
                    for (String ss : sessions.keySet()) {
                        if (sessionId.equals(ss)) {
                            find = true;
                            break;
                        }
                    }
                }
                //如果session不存在，则remove
                if (!find) {
                    iterator.remove();
                    log.warn("quit a chrome");
                }
            }
            int shouldCreate = CAPACITY - chromes.size();
            if (shouldCreate > 0) {
                try {
                    RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), getOptions());
                    MyChrome myChrome = new MyChrome();
                    myChrome.setWebDriver(webDriver);
                    chromes.add(myChrome);
                    log.warn("create a chrome " + webDriver.getSessionId().toString() + " 总容量 = " + CAPACITY + ", 当前容量" + chromes.size());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                inflate(chromes, getGridStatus());
            }
        }
        runningSchedule = false;
    }

    @Autowired
    private RestTemplate restTemplate;

    public SelenoidStatus getGridStatus() {
        SelenoidStatus status = new SelenoidStatus();
        Map<String, JSONObject> sessions = new HashMap<>();
        String url = seleniumHubStatusUrl;
        String json = restTemplate.getForObject(url, String.class);
        JSONObject data = JSON.parseObject(json);
        int total = data.getIntValue("total");
        int used = data.getIntValue("used");
        int queued = data.getIntValue("queued");
        int pending = data.getIntValue("pending");
        List<JSONObject> read = (List<JSONObject>) JSONPath.read(json, "$.browsers.chrome..sessions");
        if (read != null) {
            for (JSONObject jo : read) {
                sessions.put(jo.getString("id"), jo);
            }
        }
        status.setSessions(sessions);
        status.setUsed(used);
        status.setTotal(total);
        status.setQueued(queued);
        status.setPending(pending);
        return status;
    }

    public void closeSession(String sessionId) {
        String deleteUrl = String.format("%s/session/%s", seleniumHubUrl, sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-REGISTRATION-SECRET", "");
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = null;
        try {
            exchange = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, request, String.class);
            log.info("close sessionId : " + deleteUrl + " resp : " + exchange.getBody() + ", resp code : " + exchange.getStatusCode());
        } catch (RestClientException e) {
            e.printStackTrace();
        }
    }

    private void inflate(List<MyChrome> chromes, SelenoidStatus statusList) {
        Map<String, JSONObject> sessions = statusList.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (MyChrome chrome : chromes) {
            String currSessionId = chrome.getWebDriver().getSessionId().toString();
            JSONObject sessionInfo = sessions.get(currSessionId);
            if (sessionInfo != null) {
                String id = sessionInfo.getString("id");
                chrome.setSessionInfoJson(sessionInfo);
                chrome.setSelenoidSessionId(id);
            }
        }
    }

    @Override
    public void run(String... args) throws MalformedURLException {

        stopSchedule = true;
        chromes = Collections.synchronizedList(new ArrayList<>());

        log.info("解析配置不初始化");
        parseMultiQLConfig();

        //获取hub-node状态
        SelenoidStatus status = getNodeStatuses();
        if (status == null) {
            throw new RuntimeException("Selenium 浏览器组初始化失败");
        }

        //清理未关闭的session
        log.info("清理未关闭的session，获取最大容量");
        Map<String, JSONObject> sessions = status.getSessions();
        if (sessions != null && sessions.size() > 0) {
            for (String sessionId : sessions.keySet()) {
                if (sessionId != null) {
                    try {
                        closeSession(sessionId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //清理未关闭的session
        log.info("清理未关闭的docker container");
        cleanDockerContainer();

        //初始化一半Chrome实例
        log.info("初始化一半Chrome实例");
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int create = (CAPACITY == 1 ? 2 : CAPACITY) / 2;
        CountDownLatch cdl = new CountDownLatch(create);
        for (int i = 0; i < create; i++) {
            executorService.execute(() -> {
                try {
                    RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), getOptions());
                    webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
                    MyChrome myChrome = new MyChrome();
                    myChrome.setWebDriver(webDriver);
                    chromes.add(myChrome);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } finally {
                    cdl.countDown();
                }
            });
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("无法创建浏览器实例");
        }
        executorService.shutdown();

        if (chromes.isEmpty()) {
            throw new RuntimeException("无法创建浏览器实例");
        }
        inflate(chromes, getGridStatus());
        //借助这一半chrome实例，初始化配置
        initQLConfig();
        if (qlConfigs.isEmpty()) {
            log.warn("请配置至少一个青龙面板地址! 否则获取到的ck无法上传");
        }

        log.info("启动成功!");
        stopSchedule = false;
        initSuccess = true;
    }

    public void cleanDockerContainer() {
        Containers containers = new UnixDocker(new File("/var/run/docker.sock")).containers();
        for (Container container : containers) {
            String image = container.getString("Image");
            if (image.startsWith("selenoid/chrome")) {
                try {
                    log.info("关闭残留容器" + container.containerId());
                    container.remove(true, true, false);
                } catch (Exception e) {
                }
            }
        }
    }

    private SelenoidStatus getNodeStatuses() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<SelenoidStatus> startSeleniumRes = CompletableFuture.supplyAsync(() -> {
            while (true) {
                SelenoidStatus ss = getGridStatus();
                if (ss.getTotal() > 0) {
                    return ss;
                }
            }
        }, executor);
        SelenoidStatus status = null;
        try {
            status = startSeleniumRes.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();
        return status;
    }

    private void parseMultiQLConfig() {
        qlConfigs = new ArrayList<>();
        if (envPath.startsWith("classpath")) {
            Resource resource = resourceLoader.getResource(envPath);
            try (InputStreamReader inputStreamReader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                properties.load(inputStreamReader);
            } catch (IOException e) {
                throw new RuntimeException("env.properties配置有误");
            }
        } else {
            File envFile = new File(envPath);
            if (!envFile.exists()) {
                throw new RuntimeException("env.properties配置有误");
            }
            try (BufferedReader br = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                properties.load(br);
            } catch (IOException e) {
                throw new RuntimeException("env.properties配置有误");
            }
        }

        maxSessionFromEnvFile = properties.getProperty("SE_NODE_MAX_SESSIONS");
        String opTime = properties.getProperty("OP_TIME");
        if (!StringUtils.isEmpty(opTime)) {
            try {
                int i = Integer.parseInt(opTime);
                if (i > 0) {
                    opTimeout = i;
                }
            } catch (NumberFormatException e) {
            }
        }

        log.info("最大资源数配置: maxSessionFromEnvFile = " + maxSessionFromEnvFile + ", maxSessionFromSystemEnv = " + maxSessionFromSystemEnv + ", maxSessionFromProps = " + maxSessionFromProps);

        CAPACITY = parseCapacity();
        log.info("最大资源数配置: CAPACITY = " + CAPACITY);
        if (CAPACITY <= 0) {
            throw new RuntimeException("最大资源数配置有误");
        }

        xddUrl = properties.getProperty("XDD_URL");
        xddToken = properties.getProperty("XDD_TOKEN");

        for (int i = 1; i <= 5; i++) {
            QLConfig config = new QLConfig();
            config.setId(i);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
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
                    config.setCapacity(Integer.parseInt(value));
                }
            }
            if (config.isValid()) {
                qlConfigs.add(config);
            }
        }
        log.info("解析" + qlConfigs.size() + "套配置");
    }

    private int parseCapacity() {
        int res = 0;
        if (!StringUtils.isEmpty(maxSessionFromSystemEnv)) {
            try {
                res = Integer.parseInt(maxSessionFromSystemEnv);
            } catch (NumberFormatException e) {
            }
        }

        if (res <= 0) {
            if (!StringUtils.isEmpty(maxSessionFromEnvFile)) {
                try {
                    res = Integer.parseInt(maxSessionFromEnvFile);
                } catch (NumberFormatException e) {
                }
            }
        }

        if (res <= 0) {
            if (!StringUtils.isEmpty(maxSessionFromProps)) {
                try {
                    res = Integer.parseInt(maxSessionFromProps);
                } catch (NumberFormatException e) {
                }
            }
        }
        return res;
    }

    private void initQLConfig() {
        Iterator<QLConfig> iterator = qlConfigs.iterator();
        while (iterator.hasNext()) {
            QLConfig qlConfig = iterator.next();
            if (StringUtils.isEmpty(qlConfig.getLabel())) {
                qlConfig.setLabel("请配置QL_LABEL_" + qlConfig.getId() + "");
            }

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
                    jdService.fetchCurrentCKS_count(qlConfig, "");
                } else {
                    log.warn(qlConfig.getQlUrl() + "获取token失败，获取到的ck无法上传，已忽略");
                }
            }
            if (verify2) {
                boolean result = false;
                try {
                    result = initInnerQingLong(qlConfig);
                    qlConfig.setQlLoginType(QLConfig.QLLoginType.USERNAME_PASSWORD);
                    jdService.fetchCurrentCKS_count(qlConfig, "");
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
    }

    public boolean getToken(QLConfig qlConfig) {
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
        String sessionId = assignSessionId(null, true, null).getAssignSessionId();
        MyChrome chrome = null;
        if (sessionId != null) {
            chrome = getMyChromeBySessionId(sessionId);
        }
        if (chrome == null) {
            throw new RuntimeException("请检查资源配置，资源数太少");
        }
        RemoteWebDriver webDriver = chrome.getWebDriver();
        webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
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
            releaseWebDriver(sessionId);
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
                log.debug("username = " + username + ", password = " + password);
                return true;
            }
        }
        return false;
    }

    public RemoteWebDriver getDriverBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
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

    public AssignSessionIdStatus assignSessionId(String clientSessionId, boolean create, HttpSession session) {
        AssignSessionIdStatus status = new AssignSessionIdStatus();
        String servletSessionId = null;
        if (session != null) {
            servletSessionId = session.getId();
        }
        if (clientSessionId == null && servletSessionId != null) {
            String s = cacheUtil.get("servlet:session:" + servletSessionId);
            if (!StringUtils.isEmpty(s)) {
                clientSessionId = s;
            } else {
                log.info("servletSessionId " + servletSessionId + " invalidate!");
                create = true;
            }
        }

        if (clientSessionId != null) {
            status.setClientSessionId(clientSessionId);
            MyChrome myChrome = getMyChromeBySessionId(clientSessionId);
            if (myChrome != null && myChrome.getClientSessionId() != null) {
                status.setNew(false);
                status.setAssignSessionId(myChrome.getClientSessionId());
                Long expire = cacheUtil.getExpire(CLIENT_SESSION_ID_KEY + ":" + clientSessionId);
                if (expire != null && expire < 0) {
                    log.info("force expire " + status.getAssignSessionId());
                    //强制1分钟过期一个sessionId
                    closeSession(myChrome.getClientSessionId());
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
                        cacheUtil.put("servlet:session:" + servletSessionId, new StringCache(System.currentTimeMillis(), s, 300), 300);
                        return status;
                    }
                }
            }
        }
        return status;
    }

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void releaseWebDriver(String input) {
        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                cacheUtil.remove(CLIENT_SESSION_ID_KEY + ":" + input);
                log.info("releaseWebDriver " + input);
                Iterator<MyChrome> iterator = chromes.iterator();
                while (iterator.hasNext()) {
                    MyChrome myChrome = iterator.next();
                    String sessionId = myChrome.getWebDriver().getSessionId().toString();
                    if (sessionId.equals(input)) {
                        try {
                            myChrome.getWebDriver().quit();
                            iterator.remove();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        log.info("destroy chrome : " + sessionId);
                        break;
                    }
                }
            }
        });
    }

    public synchronized void bindSessionId(String sessionId) {
        for (MyChrome myChrome : chromes) {
            if (myChrome != null && myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                myChrome.setClientSessionId(sessionId);
                cacheUtil.put(CLIENT_SESSION_ID_KEY + ":" + sessionId, new StringCache(System.currentTimeMillis(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), opTimeout), opTimeout);
                break;
            }
        }
    }

    public synchronized void unBindSessionId(String sessionId, HttpSession httpSession) {
        Iterator<MyChrome> iterator = chromes.iterator();
        String servletSessionId = httpSession.getId();
        cacheUtil.remove("servlet:session:" + servletSessionId);
        while (iterator.hasNext()) {
            MyChrome myChrome = iterator.next();
            if (myChrome != null && myChrome.getWebDriver().getSessionId().toString().equals(sessionId)) {
                myChrome.setClientSessionId(null);
                cacheUtil.remove(CLIENT_SESSION_ID_KEY + ":" + sessionId);
                iterator.remove();
                break;
            }
        }
    }

    public String getXddUrl() {
        return xddUrl;
    }

    public String getXddToken() {
        return xddToken;
    }

    public List<QLConfig> getQlConfigs() {
        return qlConfigs;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean isInitSuccess() {
        return initSuccess;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
