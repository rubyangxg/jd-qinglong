package com.meread.selenium.service;

import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import com.meread.selenium.util.OpenCVUtil;
import com.meread.selenium.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by yangxg on 2021/10/12
 *
 * @author yangxg
 */
@Slf4j
public abstract class BaseWebDriverManager implements WebDriverManager, InitializingBean, ApplicationListener<ContextClosedEvent> {

    protected String chromeDriverPath;
    protected RestTemplate restTemplate;
    protected ThreadPoolTaskExecutor threadPoolTaskExecutor;
    protected ResourceLoader resourceLoader;
    protected boolean headless;
    protected String envPath;
    protected int opTimeout;
    protected int chromeTimeout;
    protected String maxSessionFromProps;

    public BaseWebDriverManager(String chromeDriverPath, RestTemplate restTemplate, ThreadPoolTaskExecutor threadPoolTaskExecutor, ResourceLoader resourceLoader, boolean headless, String envPath, int opTimeout, int chromeTimeout, String maxSessionFromProps) {
        this.chromeDriverPath = chromeDriverPath;
        this.restTemplate = restTemplate;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.resourceLoader = resourceLoader;
        this.headless = headless;
        this.envPath = envPath;
        this.opTimeout = opTimeout;
        this.chromeTimeout = chromeTimeout;
        this.maxSessionFromProps = maxSessionFromProps;
    }

    /**
     * chromeSessionId-->MyChrome
     */
    public final Map<String, MyChrome> chromes = Collections.synchronizedMap(new HashMap<>());
    /**
     * userTrackId --> MyChromeClient
     */
    protected final Map<String, MyChromeClient> clients = Collections.synchronizedMap(new HashMap<>());

    protected int CAPACITY = 0;
    protected Properties properties = new Properties();
    protected ChromeOptions chromeOptions;
    protected String maxSessionFromEnvFile;
    protected List<QLConfig> qlConfigs;
    protected String xddUrl;
    protected String xddToken;

    protected RemoteWebDriver getDriverBySessionId(String chromeSessionId) {
        MyChrome myChromeBySessionId = getMyChromeBySessionId(chromeSessionId);
        if (myChromeBySessionId != null) {
            return myChromeBySessionId.getWebDriver();
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

    public MyChromeClient getCacheMyChromeClient(String userTrackId) {
        if (userTrackId != null) {
            return clients.get(userTrackId);
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
    public void afterPropertiesSet() {
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        CompletableFuture<Void> waitTask = CompletableFuture.runAsync(() -> {
            try {
                OpenCVUtil.test();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, threadPoolTaskExecutor);

        log.info("解析配置不初始化");
        parseConfig();
        createChromeOptions();

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
        try {
            waitTask.get();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    protected void parseConfig() {
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
                    }
                }
            }
        } catch (Exception e) {
            log.error(qlUrl + "测试登录失败，请检查配置");
        }
        return qlConfig.getQlToken() != null && qlConfig.getQlToken().getToken() != null;
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

    public Map<String, MyChrome> getChromes() {
        return chromes;
    }

    @Override
    public void destroyAll() {
        ApplicationContext context = SpringUtils.getApplicationContext();
        WSManager wsManager = context.getBean(WSManager.class);
        int count = 0;
        while (wsManager.runningSchedule) {
            if (count > 5) {
                break;
            }
            try {
                Thread.sleep(1000);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("wait wsManager schedule destroy...");
        }
        close();
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        destroyAll();
    }

    public abstract void heartbeat();

    public abstract void close();
}
