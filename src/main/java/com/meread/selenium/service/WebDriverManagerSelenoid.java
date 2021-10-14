package com.meread.selenium.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.UnixDocker;
import com.meread.selenium.bean.MyChrome;
import com.meread.selenium.bean.MyChromeClient;
import com.meread.selenium.bean.SelenoidStatus;
import com.meread.selenium.util.CommonAttributes;
import com.meread.selenium.util.OpenCVUtil;
import com.meread.selenium.util.WebDriverOpCallBack;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.SessionStorage;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by yangxg on 2021/10/12
 *
 * @author yangxg
 */
@Component
@Slf4j
@Profile({"debuglocal", "debugremote"})
public class WebDriverManagerSelenoid extends BaseWebDriverManager {

    @Value("${selenium.hub.url}")
    private String seleniumHubUrl;
    @Value("${selenium.hub.status.url}")
    private String seleniumHubStatusUrl;

    public WebDriverManagerSelenoid(@Value("${chrome.driver.path}") String chromeDriverPath,
                                    @Autowired RestTemplate restTemplate,
                                    @Autowired ThreadPoolTaskExecutor threadPoolTaskExecutor,
                                    @Autowired ResourceLoader resourceLoader,
                                    @Value("${chrome.headless}") boolean headless,
                                    @Value("${env.path}") String envPath,
                                    @Value("${op.timeout}") int opTimeout,
                                    @Value("${chrome.timeout}") int chromeTimeout,
                                    @Value("${SE_NODE_MAX_SESSIONS}") String maxSessionFromProps) {
        super(chromeDriverPath, restTemplate, threadPoolTaskExecutor, resourceLoader, headless, envPath, opTimeout, chromeTimeout, maxSessionFromProps);
    }

    @Override
    public void createChromeOptions() {
        chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("lang=zh-CN,zh,zh-TW,en-US,en");
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.72 Safari/537.36");
//        chromeOptions.addArguments("user-agent=UCWEB/2.0 (MIDP-2.0; U; Adr 9.0.0) UCBrowser U2/1.0.0 Gecko/63.0 Firefox/63.0 iPhone/7.1 SearchCraft/2.8.2 baiduboxapp/3.2.5.10 BingWeb/9.1 ALiSearchApp/2.4");
        chromeOptions.addArguments("--disable-blink-features");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--lang=zh-cn");
        chromeOptions.setCapability("browserName", "chrome");
        chromeOptions.setCapability("browserVersion", "89.0");
        chromeOptions.setCapability("screenResolution", "510x710x24");
        chromeOptions.setCapability("enableVideo", false);
        if (chromeTimeout < 60) {
            chromeTimeout = 60;
        }
        chromeOptions.setCapability("selenoid:options", Map.<String, Object>of(
                "enableVNC", CommonAttributes.debug,
                "enableVideo", false,
                "enableLog", false,
                "env", new String[]{"LANG=zh_CN.UTF-8", "LANGUAGE=zh:cn", "LC_ALL=zh_CN.UTF-8"},
                "timeZone", "Asia/Shanghai",
                "sessionTimeout", chromeTimeout + "s"
//                "applicationContainers", new String[]{"webapp"}
        ));
        if (!CommonAttributes.debug) {
            chromeOptions.addArguments("--headless");
        }
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-extensions");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--ignore-ssl-errors=yes");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--window-size=500,700");
    }

    /**
     * 和grid同步chrome状态，清理失效的session，并移除本地缓存
     */
    @Override
    public void heartbeat() {
        SelenoidStatus nss = getGridStatus();
        Iterator<Map.Entry<String, MyChrome>> iterator = chromes.entrySet().iterator();
        Set<String> removedChromeSessionId = new HashSet<>();
        while (iterator.hasNext()) {
            String chromeSessionId = iterator.next().getKey();
            Map<String, JSONObject> sessions = nss.getSessions();
            JSONObject jsonObject = sessions.get(chromeSessionId);
            if (jsonObject != null) {
                break;
            } else {
                //如果session不存在，则remove
                iterator.remove();
                removedChromeSessionId.add(chromeSessionId);
                log.warn("quit a chrome");
            }
        }
        cleanClients(removedChromeSessionId);
        int shouldCreate = CAPACITY - chromes.size();
        if (shouldCreate > 0) {
            try {
                RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), chromeOptions);
                webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(10, TimeUnit.SECONDS).setScriptTimeout(10, TimeUnit.SECONDS);
                MyChrome myChrome = new MyChrome(webDriver, null, System.currentTimeMillis() + (chromeTimeout - 10) * 1000L);
                //计算chrome实例的最大存活时间
                chromes.put(webDriver.getSessionId().toString(), myChrome);
                log.warn("create a chrome " + webDriver.getSessionId().toString() + " 总容量 = " + CAPACITY + ", 当前容量" + chromes.size());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            inflate(chromes, getGridStatus());
        }
    }

    @Override
    public <T> T exec(WebDriverOpCallBack<T> executor) {
        RemoteWebDriver webDriver = null;
        try {
            webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), chromeOptions);
            webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(10, TimeUnit.SECONDS).setScriptTimeout(10, TimeUnit.SECONDS);
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

    private void cleanClients(Set<String> removedChromeSessionId) {
        Iterator<Map.Entry<String, MyChromeClient>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MyChromeClient> entry = iterator.next();
            String chromeSessionId = entry.getValue().getChromeSessionId();
            if (chromeSessionId == null || removedChromeSessionId.contains(chromeSessionId)) {
                iterator.remove();
            }
        }
    }

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

    private void inflate(Map<String, MyChrome> chromes, SelenoidStatus statusList) {
        Map<String, JSONObject> sessions = statusList.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (MyChrome chrome : chromes.values()) {
            String currSessionId = chrome.getWebDriver().getSessionId().toString();
            JSONObject sessionInfo = sessions.get(currSessionId);
            if (sessionInfo != null) {
                String id = sessionInfo.getString("id");
                chrome.setSessionInfoJson(sessionInfo);
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        CompletableFuture<Void> waitTask = CompletableFuture.runAsync(() -> {
            try {
                OpenCVUtil.test();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, threadPoolTaskExecutor);

        parseConfig();
        createChromeOptions();
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
        createChrome();
        inflate(chromes, getGridStatus());
        //借助这一半chrome实例，初始化配置
        if (qlConfigs.isEmpty()) {
            log.warn("请配置至少一个青龙面板地址! 否则获取到的ck无法上传");
        }

        if (chromes.isEmpty()) {
            //防止配置资源数过少时(比如1)，因为初始化青龙后，导致无chrome实例,来不及等定时任务创建
            createChrome();
            inflate(chromes, getGridStatus());
        }

        try {
            waitTask.get();
        } catch (Exception e) {
            System.exit(0);
        }
    }

    @Override
    public void createChrome() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int create = CAPACITY == 1 ? 1 : (CAPACITY - chromes.size()) / 2;
        CountDownLatch cdl = new CountDownLatch(create);
        for (int i = 0; i < create; i++) {
            executorService.execute(() -> {
                try {
                    RemoteWebDriver webDriver = new RemoteWebDriver(new URL(seleniumHubUrl), chromeOptions);
                    webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS).pageLoadTimeout(10, TimeUnit.SECONDS).setScriptTimeout(10, TimeUnit.SECONDS);
                    MyChrome myChrome = new MyChrome(webDriver, null, System.currentTimeMillis() + (chromeTimeout - 10) * 1000L);
                    chromes.put(webDriver.getSessionId().toString(), myChrome);
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

    @Override
    public void releaseWebDriver(String removeChromeSessionId, boolean quit) {
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
                    if ((chromeExpireTime - clientExpireTime) / 1000 > opTimeout && !quit) {
                        myChrome.setUserTrackId(null);
                        clients.remove(userTrackId);
                        WebStorage webStorage = (WebStorage) new Augmenter().augment(myChrome.getWebDriver());
                        if (webStorage != null) {
                            LocalStorage localStorage = webStorage.getLocalStorage();
                            if (localStorage != null) {
                                localStorage.clear();
                            }
                            SessionStorage sessionStorage = webStorage.getSessionStorage();
                            if (sessionStorage != null) {
                                sessionStorage.clear();
                            }
                        }
                        myChrome.getWebDriver().manage().deleteAllCookies();
                        log.info("clean chrome binding: " + sessionId);
                    } else {
                        clients.remove(userTrackId);
                        iterator.remove();
                        threadPoolTaskExecutor.execute(() -> myChrome.getWebDriver().quit());
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
    public void close() {
        SelenoidStatus status = getGridStatus();
        Map<String, JSONObject> sessions = status.getSessions();
        if (sessions != null) {
            for (String sessionId : sessions.keySet()) {
                if (sessionId != null) {
                    log.info("destroy chrome " + sessionId);
                    closeSession(sessionId);
                }
            }
        }
        cleanDockerContainer();
    }

}
