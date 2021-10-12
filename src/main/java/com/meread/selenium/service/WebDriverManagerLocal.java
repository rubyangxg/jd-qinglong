package com.meread.selenium.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import com.meread.selenium.util.WebDriverOpCallBack;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yangxg
 * @date 2021/9/7
 */
@Component
@Slf4j
@Profile("default")
public class WebDriverManagerLocal implements WebDriverManager, CommandLineRunner, InitializingBean, ApplicationListener<ContextClosedEvent> {

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${chrome.headless}")
    private boolean headless;

    @Autowired
    private JDService jdService;

    @Value("${env.path}")
    private String envPath;

    @Value("${op.timeout}")
    private int opTimeout;

    @Value("${chrome.timeout}")
    private int chromeTimeout;

    @Value("${SE_NODE_MAX_SESSIONS}")
    private String maxSessionFromProps;

    private String maxSessionFromEnvFile;

    private List<QLConfig> qlConfigs;

    public Properties properties = new Properties();

    private String xddUrl;

    private String xddToken;

    private static int CAPACITY = 0;

    public volatile boolean stopSchedule = false;
    public volatile boolean initSuccess = false;
    public volatile boolean runningSchedule = false;

    /**
     * chromeSessionId-->MyChrome
     */
    private static final Map<String, MyChrome> chromes = Collections.synchronizedMap(new HashMap<>());

    /**
     * userTrackId --> MyChromeClient
     */
    private static final Map<String, MyChromeClient> clients = Collections.synchronizedMap(new HashMap<>());

    public ChromeOptions chromeOptions;

    public void init() {
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", true);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("--disable-blink-features");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
//        chromeOptions.addArguments("user-agent=Mozilla/5.0 (iPhone; CPU iPhone OS 15_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.setCapability("enableVideo", false);
        chromeOptions.addArguments("--lang=zh-cn");
        chromeOptions.addArguments("lang=zh_CN.UTF-8");
        chromeOptions.setCapability("enableVideo", false);
        if (chromeTimeout < 60) {
            chromeTimeout = 60;
        }
        if (headless) {
            chromeOptions.addArguments("--headless");
        }
        //ssl证书支持
        chromeOptions.setCapability("acceptSslCerts", true);
        //截屏支持
        chromeOptions.setCapability("takesScreenshot", true);
        //css搜索支持
        chromeOptions.setCapability("cssSelectorsEnabled", true);
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-extensions");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--disable-software-rasterizer");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--window-size=500,700");
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 30 * 60000)
    public void syncCK_count() {
        if (qlConfigs != null) {
            for (QLConfig qlConfig : qlConfigs) {
                int oldSize = qlConfig.getRemain();
                Boolean exec = exec(webDriver -> {
                    jdService.fetchCurrentCKS_count(webDriver, qlConfig, "");
                    return true;
                });
                if (exec != null && exec) {
                    int newSize = qlConfig.getRemain();
                    log.info(qlConfig.getQlUrl() + " 容量从 " + oldSize + "变为" + newSize);
                } else {
                    log.error("syncCK_count 执行失败");
                }
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

    /**
     * 和grid同步chrome状态，清理失效的session，并移除本地缓存
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void heartbeat() {
        runningSchedule = true;
        if (!stopSchedule) {
            Iterator<Map.Entry<String, MyChrome>> iterator = chromes.entrySet().iterator();
            while (iterator.hasNext()) {
                MyChrome chrome = iterator.next().getValue();
                if (chrome.getUserTrackId() == null) {
                    if (chrome.isExpire()) {
                        iterator.remove();
                        quit(chrome);
                    }
                }
            }
            int shouldCreate = CAPACITY - chromes.size();
            if (shouldCreate > 0) {
                ChromeDriverService chromeDriverService = ChromeDriverService.createDefaultService();
                ChromeDriver webDriver = new ChromeDriver(chromeDriverService, chromeOptions);
                webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(20, TimeUnit.SECONDS).setScriptTimeout(20, TimeUnit.SECONDS);
                MyChrome myChrome = new MyChrome(webDriver, chromeDriverService, System.currentTimeMillis() + (chromeTimeout - 10) * 1000L);
                //计算chrome实例的最大存活时间
                chromes.put(webDriver.getSessionId().toString(), myChrome);
                log.warn("create a chrome " + webDriver.getSessionId().toString() + " 总容量 = " + CAPACITY + ", 当前容量" + chromes.size());
            }
        }
        runningSchedule = false;
    }

    private void quit(MyChrome chrome) {
        ChromeDriverService chromeDriverService = chrome.getChromeDriverService();
        int port = chromeDriverService.getUrl().getPort();
        log.info("kill port = " + port);
        String[] cmd = new String[]{"sh", "-c", "kill -9 $(lsof -n -i :" + port + " | awk '/LISTEN/{print $2}')"};
        try {
            chrome.getWebDriver().quit();
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Value("${chrome.driver.path}")
    private String chromeDriverPath;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(String... args) throws MalformedURLException {
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        stopSchedule = true;

        log.info("解析配置不初始化");
        parseMultiQLConfig();

        //清理未关闭的session
        log.info("清理未关闭的session，获取最大容量");
        try {
            long pid = ProcessHandle.current().pid();
            //排除当前java进程
            String[] cmd = new String[]{"sh", "-c", "kill -9 $(ps aux | grep -v " + pid + " | grep -i '[c]hrome' | awk '{print $2}')"};
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //初始化一半Chrome实例
        log.info("初始化一半Chrome实例");
        createChrome();
        //借助这一半chrome实例，初始化配置
        initQLConfig();
        if (qlConfigs.isEmpty()) {
            log.warn("请配置至少一个青龙面板地址! 否则获取到的ck无法上传");
        }

        log.info("启动成功!");
        stopSchedule = false;
        initSuccess = true;
    }

    private void createChrome() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int create = CAPACITY - chromes.size();
        if (create <= 0) {
            return;
        }
        CountDownLatch cdl = new CountDownLatch(create);
        for (int i = 0; i < create; i++) {
            executorService.execute(() -> {
                try {
                    ChromeDriverService chromeDriverService = ChromeDriverService.createDefaultService();
                    ChromeDriver webDriver = new ChromeDriver(chromeDriverService, chromeOptions);
                    webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(20, TimeUnit.SECONDS).setScriptTimeout(20, TimeUnit.SECONDS);
                    MyChrome myChrome = new MyChrome(webDriver, chromeDriverService, System.currentTimeMillis() + (chromeTimeout - 10) * 1000L);
                    chromes.put(webDriver.getSessionId().toString(), myChrome);
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
        int customChromeTimeout = Integer.parseInt(properties.getProperty("chrome.timeout", "0"));
        if (customChromeTimeout > 0) {
            chromeTimeout = customChromeTimeout;
        }
        CommonAttributes.debug = Boolean.parseBoolean(properties.getProperty("jd.debug", "false"));

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

        log.info("最大资源数配置: maxSessionFromEnvFile = " + maxSessionFromEnvFile + " maxSessionFromProps = " + maxSessionFromProps);

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
        int res1 = 0;
        int res2 = 0;

        if (!StringUtils.isEmpty(maxSessionFromEnvFile)) {
            try {
                res1 = Integer.parseInt(maxSessionFromEnvFile);
            } catch (NumberFormatException e) {
            }
        }

        if (res1 > 0) {
            return res1;
        }

        if (!StringUtils.isEmpty(maxSessionFromProps)) {
            try {
                res2 = Integer.parseInt(maxSessionFromProps);
            } catch (NumberFormatException e) {
            }
        }
        return res2;
    }

    private void initQLConfig() {
        Iterator<QLConfig> iterator = qlConfigs.iterator();
        RemoteWebDriver driver = null;
        try {
            for (MyChrome chrome : chromes.values()) {
                if (chrome.getUserTrackId() == null) {
                    driver = chrome.getWebDriver();
                    break;
                }
            }
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
                        jdService.fetchCurrentCKS_count(driver, qlConfig, "");
                    } else {
                        log.warn(qlConfig.getQlUrl() + "获取token失败，获取到的ck无法上传，已忽略");
                    }
                } else if (verify2) {
                    boolean result = false;
                    try {
                        result = initInnerQingLong(driver, qlConfig);
                        if (result) {
                            qlConfig.setQlLoginType(QLConfig.QLLoginType.USERNAME_PASSWORD);
                            jdService.fetchCurrentCKS_count(driver, qlConfig, "");
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
        } finally {
            if (driver != null && driver.getSessionId() != null) {
                releaseWebDriver(driver.getSessionId().toString());
            }
        }

        log.info("成功添加" + qlConfigs.size() + "套配置");
    }

    @Override
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
                    qlConfig.setQlToken(new QLToken(token, tokenType, expiration));
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(qlUrl + "获取token失败，请检查配置");
        }
        return false;
    }

    public boolean initInnerQingLong(RemoteWebDriver webDriver, QLConfig qlConfig) {
        String qlUrl = qlConfig.getQlUrl();
        try {
            String token = null;
            int retry = 0;
            while (StringUtils.isEmpty(token)) {
                retry++;
                log.info("initInnerQingLong-->" + qlConfig.getQlUrl() + "第" + retry + "次尝试");
                if (retry > 2) {
                    break;
                }
                String qlUsername = qlConfig.getQlUsername();
                String qlPassword = qlConfig.getQlPassword();
                webDriver.get(qlUrl + "/login");
                new RemoteWebStorage(new RemoteExecuteMethod(webDriver)).getLocalStorage().clear();
                webDriver.get(qlUrl + "/login");
                WebElement firstResult = new WebDriverWait(webDriver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@type='submit']")));
                if (firstResult != null) {
                    webDriver.findElement(By.id("username")).sendKeys(qlUsername);
                    webDriver.findElement(By.id("password")).sendKeys(qlPassword);
                    webDriver.findElement(By.xpath("//button[@type='submit']")).click();
                    Boolean until = new WebDriverWait(webDriver, Duration.ofSeconds(10)).until(
                            driver -> driver.findElement(By.tagName("body")).getText().contains("定时任务")
                    );
                    if (until) {
                        RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(webDriver);
                        RemoteWebStorage webStorage = new RemoteWebStorage(executeMethod);
                        LocalStorage storage = webStorage.getLocalStorage();
                        token = storage.getItem("token");
                        qlConfig.setQlToken(new QLToken(token));
                        readPassword(qlConfig);
                    }
                }
            }
        } catch (Exception e) {
            log.error(qlUrl + "测试登录失败，请检查配置");
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

    @Override
    public RemoteWebDriver getDriverBySessionId(String chromeSessionId) {
        MyChrome myChromeBySessionId = getMyChromeBySessionId(chromeSessionId);
        if (myChromeBySessionId != null) {
            return myChromeBySessionId.getWebDriver();
        }
        return null;
    }

    public MyChrome getMyChromeBySessionId(String chromeSessionId) {
        if (chromes.size() > 0) {
            return chromes.get(chromeSessionId);
        }
        return null;
    }

    @Override
    public synchronized MyChromeClient createNewMyChromeClient(String userTrackId, LoginType loginType, JDLoginType jdLoginType) {
        //客户端传过来的sessionid为空，可能是用户重新刷新了网页，或者新开了一个浏览器，那么就需要跟踪会话缓存来找到之前的ChromeSessionId
        MyChromeClient myChromeClient = null;
        myChromeClient = new MyChromeClient();
        myChromeClient.setLoginType(loginType);
        myChromeClient.setJdLoginType(jdLoginType);
        myChromeClient.setUserTrackId(userTrackId);
        boolean success = false;
        if (chromes.size() < CAPACITY) {
            createChrome();
        }
        for (MyChrome myChrome : chromes.values()) {
            if (myChrome.getUserTrackId() == null) {
                //双向绑定
                myChromeClient.setExpireTime(System.currentTimeMillis() + opTimeout * 1000L);
                myChromeClient.setChromeSessionId(myChrome.getChromeSessionId());
                myChrome.setUserTrackId(userTrackId);
                success = true;
                break;
            }
        }
        if (success) {
            clients.put(userTrackId, myChromeClient);
            return myChromeClient;
        } else {
            return null;
        }
    }

    @Override
    public MyChromeClient getCacheMyChromeClient(String userTrackId) {
        if (userTrackId != null) {
            return clients.get(userTrackId);
        }
        return null;
    }

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void releaseWebDriver(String removeChromeSessionId) {
        Iterator<Map.Entry<String, MyChrome>> iterator = chromes.entrySet().iterator();
        while (iterator.hasNext()) {
            MyChrome myChrome = iterator.next().getValue();
            String sessionId = myChrome.getWebDriver().getSessionId().toString();
            if (sessionId.equals(removeChromeSessionId)) {
                try {
                    //获取chrome的失效时间
                    long chromeExpireTime = myChrome.getExpireTime();
                    long clientExpireTime = 0;
                    //获取客户端的失效时间
                    String userTrackId = myChrome.getUserTrackId();
                    if (userTrackId != null) {
                        MyChromeClient client = clients.get(userTrackId);
                        clientExpireTime = client.getExpireTime();
                    }
                    //chrome的存活时间不够一个opTime时间，则chrome不退出，只清理客户端引用
                    if ((chromeExpireTime - clientExpireTime) / 1000 > opTimeout) {
                        myChrome.setUserTrackId(null);
                        clients.remove(userTrackId);
                        myChrome.getWebDriver().manage().deleteAllCookies();
                        log.info("clean chrome binding: " + sessionId);
                    } else {
                        iterator.remove();
                        threadPoolTaskExecutor.execute(() -> quit(myChrome));
                        log.info("destroy chrome : " + sessionId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        Iterator<Map.Entry<String, MyChromeClient>> iterator2 = clients.entrySet().iterator();
        while (iterator2.hasNext()) {
            MyChromeClient curr = iterator2.next().getValue();
            if (curr.getChromeSessionId().equals(removeChromeSessionId)) {
                try {
                    iterator2.remove();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("remove MyChromeClient : " + removeChromeSessionId);
                break;
            }
        }
    }

    @Override
    public String getXddUrl() {
        return xddUrl;
    }

    @Override
    public String getXddToken() {
        return xddToken;
    }

    @Override
    public List<QLConfig> getQlConfigs() {
        return qlConfigs;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public boolean isInitSuccess() {
        return initSuccess;
    }

    @Override
    public void afterPropertiesSet() {
        init();
    }

    public <T> T exec(WebDriverOpCallBack<T> executor) {
        RemoteWebDriver webDriver = null;
        try {
            webDriver = new ChromeDriver(chromeOptions);
            webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(20, TimeUnit.SECONDS).setScriptTimeout(20, TimeUnit.SECONDS);
            return executor.doBusiness(webDriver);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
        return null;
    }

    @Override
    public StatClient getStatClient() {
        int availChromeCount = 0;
        int webSessionCount = 0;
        int qqSessionCount = 0;
        int totalChromeCount = CAPACITY;
        for (MyChrome chrome : chromes.values()) {
            if (chrome.getUserTrackId() == null) {
                availChromeCount++;
            } else {
                String userTrackId = chrome.getUserTrackId();
                MyChromeClient client = clients.get(userTrackId);
                if (client != null) {
                    LoginType loginType = client.getLoginType();
                    if (loginType == LoginType.WEB) {
                        webSessionCount++;
                    } else if (loginType == LoginType.QQBOT) {
                        qqSessionCount++;
                    }
                }
            }
        }
        return new StatClient(availChromeCount, webSessionCount, qqSessionCount, totalChromeCount);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        WebDriverManagerLocal webDriverManager = context.getBean(WebDriverManagerLocal.class);
        WSManager wsManager = context.getBean(WSManager.class);

        webDriverManager.stopSchedule = true;
        wsManager.stopSchedule = true;
        for (MyChrome myChrome : chromes.values()) {
            myChrome.getWebDriver().quit();
        }
    }
}
