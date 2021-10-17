package com.meread.selenium;

import com.meread.selenium.util.OpenCVUtil;
import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;

/**
 * @author yangxg
 * @date 2021/10/9
 */
public class LocalDriverTest {
    @Test
    public void test1() throws IOException {
        OpenCVUtil.test();
    }

    @Test
    public void test2() throws IOException {
        OpenCVUtil.test2();
    }

}
