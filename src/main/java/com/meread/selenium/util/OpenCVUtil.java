package com.meread.selenium.util;

import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.opencv_core.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class OpenCVUtil {

    public static void test() throws IOException {
        for (char a = 'a'; a <= 'i'; a++) {
            System.out.println("初始化opencv start...");
            long tt1 = System.currentTimeMillis();
            System.out.println("0...");
            BufferedImage image = ImageIO.read(Objects.requireNonNull(OpenCVUtil.class.getClassLoader().getResourceAsStream("static/img/" + a + ".jpeg")));
            System.out.println("1...");
            BufferedImage imageSmall = ImageIO.read(Objects.requireNonNull(OpenCVUtil.class.getClassLoader().getResourceAsStream("static/img/" + a + "_small.png")));
            System.out.println("2...");
            Mat mat = Java2DFrameUtils.toMat(image);
            System.out.println("3...");
            long tt2 = System.currentTimeMillis();
            System.out.println("4...");
            Mat matSmall = Java2DFrameUtils.toMat(imageSmall);
            System.out.println("5...");
            Rect rect = getOffsetX(mat, matSmall, String.valueOf(a), false);
            System.out.println("init opencv calc " + a + " gap " + rect.x() + " end...耗时：" + (tt2 - tt1));
            System.out.println("初始化opencv end...");
        }
    }

    public static void test2() {
        for (char a = 'a'; a <= 'i'; a++) {
            String bigF = OpenCVUtil.class.getClassLoader().getResource("static/img/" + a + ".jpeg").getFile();
            String smallF = OpenCVUtil.class.getClassLoader().getResource("static/img/" + a + "_small.png").getFile();
            Rect rect = getOffsetX(bigF, smallF, true);
            System.out.println(rect.x());
        }
    }

    public static Rect getOffsetX(Mat source, Mat find, String baseName, boolean debug) {
        //read in image default colors
        Mat sourceGrey = new Mat(source.size(), CV_8UC1);
        cvtColor(source, sourceGrey, COLOR_BGR2GRAY);

        Mat template = new Mat();
        cvtColor(find, template, COLOR_BGR2GRAY);
        //Size for the result image
        Size size = new Size(sourceGrey.cols() - template.cols() + 1, sourceGrey.rows() - template.rows() + 1);
        Mat result = new Mat(size, CV_32FC1);
        matchTemplate(sourceGrey, template, result, TM_CCORR_NORMED);

        DoublePointer minVal = new DoublePointer();
        DoublePointer maxVal = new DoublePointer();
        Point min = new Point();
        Point max = new Point();
        minMaxLoc(result, minVal, maxVal, min, max, null);
        Rect rect = new Rect(max.x(), max.y(), template.cols(), template.rows());
        rectangle(source, rect, redColor(), 2, 0, 0);

        if (debug) {
            imwrite(CommonAttributes.TMPDIR + "/" + baseName + ".origin.marked.jpeg", source);
        }

        return rect;
    }

    public static Rect getOffsetX(String source, String find, boolean debug) {
        Mat sourceMat = imread(source);
        Mat findMat = imread(find);
        String namePrefix = FilenameUtils.getBaseName(source);
        return getOffsetX(sourceMat, findMat, namePrefix, debug);
    }

    // some usefull things.
    public static Scalar randColor() {
        int b, g, r;
        b = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        g = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        r = ThreadLocalRandom.current().nextInt(0, 255 + 1);
        return new Scalar(b, g, r, 0);
    }

    // some usefull things.
    public static Scalar redColor() {
        return new Scalar(0, 0, 255, 0);
    }

}