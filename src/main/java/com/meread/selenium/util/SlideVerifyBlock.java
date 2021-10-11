package com.meread.selenium.util;

import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Base64Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class SlideVerifyBlock {
    //加速度
    private static final int A_SPEED = 150;

    //模拟手动的过程
    public static void moveWay1(WebDriver driver, WebElement slider, int gap) {
        log.info("gap = " + gap);
        Actions actions = new Actions(driver);
        actions.clickAndHold(slider);
        actions.perform();
        Rectangle cpc_img = driver.findElement(By.id("cpc_img")).getRect();
        gap = Math.toIntExact(Math.round(cpc_img.width / 275.0 * gap));
        List<Double> doubles = moveManualiy(gap);
        Double[] array = doubles.toArray(new Double[0]);
        double res = 0;
        for (int i = 0; i < array.length; i++) {
            // 由于经常会出现 forbidden
            int intValue = array[i].intValue();
            res += (array[i] - intValue);
            actions.moveByOffset(intValue, (i % 2));
            actions.perform();
        }
        actions.moveByOffset(new Double(res).intValue(), 0);
        actions.pause(200 + new Random().nextInt(300)).release(slider);
        actions.perform();
    }

    // 匀速移动
    public static void moveWay2(WebDriver driver, WebElement slider, int gap, String uuid,boolean isDebug) throws IOException {
        Actions actions = new Actions(driver);
        actions.clickAndHold(slider);
        actions.pause(200);
        actions.perform();
        int currx = 0;
        BufferedImage read = ImageIO.read(new File(CommonAttributes.TMPDIR + "/" + uuid + "_captcha.origin.marked.jpeg"));

        //275 * 170  --> 375 * 232
        //京东图片是等比例缩放的，所以原始图片计算出来的偏移量，需要和实际渲染图片的宽高进行比例计算
        Rectangle cpc_img = driver.findElement(By.id("cpc_img")).getRect();
        gap = Math.toIntExact(Math.round(cpc_img.width / 275.0 * gap));

        int maxX = cpc_img.x + gap - 3;
        for (int a = 0; a < 170; a++) {
            read.setRGB(maxX, a, Color.WHITE.getRGB());
            read.setRGB(maxX - 1, a, Color.WHITE.getRGB());
        }

        int step = 10;
        while (currx < gap) {
            try {
                Thread.sleep(100);
                for (int a = 0; a < 170; a++) {
                    read.setRGB(currx, a, Color.GREEN.getRGB());
                }

                actions.moveByOffset(step, 0);
                actions.perform();

                if (isDebug) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ImageIO.write(read, "png", outputStream);
                    String markedJpg = "data:image/jpg;base64," + Base64Utils.encodeToString(outputStream.toByteArray());
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('cpc_img').setAttribute('src','" + markedJpg + "')");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (currx + step > gap) {
                currx = gap;
            } else {
                currx += step;
            }
        }
        ImageIO.write(read, "jpg", new File(CommonAttributes.TMPDIR + "/" + uuid + "_captcha.origin.marked.jpeg"));
        actions.pause(200 + new Random().nextInt(300)).release(slider);
        actions.perform();
    }

    public static List<Double> moveManualiy(double distance) {
        DecimalFormat df = new DecimalFormat("######0.00");
        // 先加速移动 然后匀速移动  1/2*a*t0^2  + at0 * t1 =  distance
        double current = 0;
        ArrayList<Double> list = new ArrayList<>();
        int a = 0, cnt = 0;
        while (current < distance) {
            if (cnt < 10) {
                a = A_SPEED;
            } else {
                a = 0;
            }
            if (a == A_SPEED) {
                list.add(Double.valueOf(df.format(0.5 * a * Math.pow((cnt + 1) * (0.1), 2) - current)));
                current = 0.5 * a * Math.pow((cnt + 1) * (0.1), 2);
            } else {
                if (distance - current < A_SPEED * 0.1) {
                    break;
                } else {
                    current = current + A_SPEED * 0.1;
                    list.add(A_SPEED * 0.1);
                }
            }
            cnt++;
        }
        list.add(distance - current);
        return list;
    }
}