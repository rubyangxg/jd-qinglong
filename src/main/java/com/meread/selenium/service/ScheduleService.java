package com.meread.selenium.service;

import com.meread.selenium.bean.QLConfig;
import com.meread.selenium.bean.QLToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by yangxg on 2021/10/12
 *
 * @author yangxg
 */
@Slf4j
@Service
public class ScheduleService {

    @Autowired
    private BaseWebDriverManager driverFactory;

    @Autowired
    private JDService jdService;

    /**
     * 和grid同步chrome状态，清理失效的session，并移除本地缓存
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void heartbeat() {
        driverFactory.heartbeat();
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 30 * 60000)
    public void syncCK_count() {
        List<QLConfig> qlConfigs = driverFactory.getQlConfigs();
        if (qlConfigs != null) {
            for (QLConfig qlConfig : qlConfigs) {
                int oldSize = qlConfig.getRemain();
                Boolean exec = driverFactory.exec(webDriver -> {
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
    public void refreshOpenIdToken() {
        List<QLConfig> qlConfigs = driverFactory.getQlConfigs();
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


}
