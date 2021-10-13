package com.meread.selenium.util;

import com.meread.selenium.bean.Point;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.touch.TouchActions;
import org.openqa.selenium.remote.RemoteWebDriver;
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
    private static final int A_SPEED = 300;

    //模拟手动的过程
    public static void moveWay1(WebDriver driver, WebElement slider, int gap, String uuid, boolean debug) throws IOException {
        Rectangle cpc_img = driver.findElement(By.id("cpc_img")).getRect();
        log.info("gap old is " + gap);
        gap = Math.toIntExact(Math.round(cpc_img.width / 275.0 * gap));
        log.info("gap new is " + gap + " cpc_img.width  = " + cpc_img.width);

        List<Double> doubles = moveManualiy2(gap);
        Double[] array = doubles.toArray(new Double[0]);
        double res = 0;

        BufferedImage read = null;
        if (debug) {
            read = ImageIO.read(new File(CommonAttributes.TMPDIR + "/" + uuid + ".origin.marked.jpeg"));
        }

        Actions actions = new Actions(driver);
        actions.clickAndHold(slider);
        actions.perform();

        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            int intValue = array[i].intValue();
            sum += intValue;
            res += (array[i] - intValue);
            long myRandomLong = (long) (Math.random() * 8 * (Math.random() > 0.5 ? 1 : -1));
            log.info("random y " + myRandomLong);
            actions.moveByOffset(intValue, i % 2).perform();
            if (debug) {
                try {
                    for (int a = 0; a < 170; a++) {
                        read.setRGB(sum, a, Color.GREEN.getRGB());
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ImageIO.write(read, "png", outputStream);
                    String markedJpg = "data:image/jpg;base64," + Base64Utils.encodeToString(outputStream.toByteArray());
                    ((JavascriptExecutor) driver).executeScript("document.getElementById('cpc_img').setAttribute('src','" + markedJpg + "')");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        actions.moveByOffset(new Double(res).intValue(), 0);
//        if (debug) {
//            try {
//                sum += new Double(res).intValue();
//                for (int a = 0; a < 170; a++) {
//                    read.setRGB(sum, a, Color.GREEN.getRGB());
//                }
//                ImageIO.write(read, "jpg", new File(CommonAttributes.TMPDIR + "/" + uuid + "_captcha.origin.marked.jpeg"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        actions.pause(100 + new Random().nextInt(100)).release(slider);
        actions.perform();
    }

    // 匀速移动
    @Deprecated
    public static void moveWay2(WebDriver driver, WebElement slider, int gap, String uuid, boolean isDebug) throws IOException {
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
                Thread.sleep(50);
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
            if (cnt < 6) {
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

    /**
     * 拿到移动轨迹，模仿人的滑动行为，先匀加速后匀减速
     * 匀变速运动基本公式：
     * ①v=v0+at
     * ②s=v0t+½at²
     * ③v²-v0²=2as
     * :param distance: 需要移动的距离
     * :return: 存放每0.2秒移动的距离
     *
     * @param distance
     * @return
     */
    public static List<Double> moveManualiy2(double distance) {
        DecimalFormat df = new DecimalFormat("######0.00");

        // 初速度
        double v = 0;
        // 单位时间为0.2s来统计轨迹，轨迹即0.2内的位移
        double t = 0.2;
        // 位移/轨迹列表，列表内的一个元素代表0.2s的位移
        ArrayList<Double> tracks = new ArrayList<>();
        tracks.add(distance * 0.1);
        distance = distance * 0.9;
        // 当前的位移
        int current = 0;
        // 到达mid值开始减速
        double mid = distance * 5 / 8;
        //加速度
        int a = 0;
        Random random = new Random();
        while (current < distance) {
            if (current < mid) {
                // 加速度越小，单位时间的位移越小,模拟的轨迹就越多越详细
                a = random.nextInt(200) + 1;
            } else {
                a = -random.nextInt(200) + 2;
            }
            // 初速度
            double v0 = v;
            // 0.2秒时间内的位移
            double s = v0 * t + 0.5 * a * Math.pow(t, 2);
            double d = Double.parseDouble(df.format(s));
            // 当前的位置
            current += d;
            // 添加到轨迹列表
            tracks.add(d);
            int i1 = random.nextInt(8);
            if (i1 % 3 == 1) {
                int i2 = random.nextInt(8);
                tracks.add((double) (-i2 / 2));
                tracks.add((double) (i2 / 2));
            }

            // 速度已经达到v,该速度作为下次的初速度
            v = v0 + a * t;
        }
        tracks.add(distance - current);
        tracks.add(-4D);
        return tracks;
    }

    public static void main(String[] args) {
        List<Double> dis1 = SlideVerifyBlock.moveManualiy2(100);
        System.out.println(dis1);
    }

    public static void manualWay(RemoteWebDriver webDriver, WebElement slider, List<Point> pointList) {
        Actions actions = new Actions(webDriver);
        actions.clickAndHold(slider);
        actions.perform();
        for (Point point : pointList) {
            actions.moveByOffset(point.getX(), point.getY()).perform();
        }
        actions.pause(100 + new Random().nextInt(100)).release(slider);
        actions.perform();
    }
}