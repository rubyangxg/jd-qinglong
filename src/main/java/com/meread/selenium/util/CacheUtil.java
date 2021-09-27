package com.meread.selenium.util;

import com.meread.selenium.WebDriverFactory;
import com.meread.selenium.bean.QQCache;
import com.meread.selenium.bean.StringCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.meread.selenium.WebDriverFactory.SERVLET_OR_QQ_SESSION_ID_KEY;

/**
 * <pre>
 *     基于concurrentHash的本地缓存工具类
 *     缓存删除基于timer定时器
 * <pre>
 */
@Component
@Slf4j
public class CacheUtil {

    @Autowired
    private WebDriverFactory factory;

    //默认大小
    private static final int DEFAULT_CAPACITY = 1024;
  
    // 最大缓存大小
    private static final int MAX_CAPACITY = 10000;
  
    //1000毫秒
    private static final long SECOND_TIME = 1000;
  
    //存储缓存的Map
    private static final ConcurrentHashMap<String, StringCache> map;
  
    private static final Timer timer;
  
    static {
        map = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
        timer = new Timer();
    }
  
    public Long getExpire(String key) {
        StringCache cache = getCache(key);
        if (cache != null) {
            return cache.getRemainSeconds();
        }
        return -1L;
    }

    public void updateQQCache(String assignChromeSessionId, QQCache qqCache) {
        StringCache cache = getCache(assignChromeSessionId);
        if (cache != null) {
            cache.setQqCache(qqCache);
        }
    }

    /**
     * <pre>
     *     缓存任务清除类
     * <pre>
     */
     class ClearTask extends TimerTask {
        private String key;
        private StringCache object;

        public ClearTask(String key, StringCache object) {
            this.key = key;
            this.object = object;
        }
  
        @Override
        public void run() {
            String value = object.getChromeSessionId();
            factory.releaseWebDriver(value);
            remove(key);
            log.warn("timer clear session " + key + " value = " + object);
        }
  
    }
  
    //==================缓存的增删改查
  
    /**
     * <pre>
     *     添加缓存
     * <pre>
     */
    public boolean put(String key, StringCache object, int time_out) {
        if (checkCapacity()) {
            map.put(key, object);
            //默认缓存时间
            timer.schedule(new ClearTask(key, object), time_out * SECOND_TIME);
        }
        return false;
    }
  
    /**
     * <pre>
     *     判断容量大小
     * <pre>
     */
    public boolean checkCapacity() {
        return map.size() < MAX_CAPACITY;
    }
  
    /**
     * <pre>
     *     删除缓存
     * <pre>
     */
    public void remove(String key) {
        map.remove(key);
    }
  
    /**
     * <pre>
     *     清除所有缓存
     * <pre>
     */
    @PreDestroy
    public void clearAll() {
        if (map.size() > 0) {
            map.clear();
        }
        timer.cancel();
    }
  
    /**
     * <pre>
     *     获取缓存
     * <pre>
     */
    private String get(String key) {
        StringCache cache = map.get(key);
        return cache == null ? null : cache.getChromeSessionId();
    }

    /**
     * 根据用户标识取分配给用户的chrome driver
     * 如果网页途径，则用户标识是HttpSession的id
     * 如果qq途径，则用户标识是QQ号
     */
    public String getAssociatedChromeSessionIdByUser(HttpSession session, long qq) {
        String cacheChromeSessionId = null;
        if (session != null) {
            cacheChromeSessionId = get(SERVLET_OR_QQ_SESSION_ID_KEY + ":"  + session.getId());
        } else if (qq > 0) {
            cacheChromeSessionId = get(SERVLET_OR_QQ_SESSION_ID_KEY + ":"  + qq);
        }
        return cacheChromeSessionId;
    }

    /**
     * <pre>
     *     获取缓存
     * <pre>
     */
    public StringCache getCache(String key) {
        return map.get(key);
    }
  
}