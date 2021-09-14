package com.meread.selenium;

import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.*;

import java.util.concurrent.ThreadLocalRandom;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class OpenCVUtil {

    public static void main(String[] args) {
        for (char a = 'a'; a <= 'i'; a++) {
            String bigF = OpenCVUtil.class.getClassLoader().getResource("static/img/" + a + ".jpeg").getFile();
            String smallF = OpenCVUtil.class.getClassLoader().getResource("static/img/" + a + "_small.png").getFile();
            Rect rect = getOffsetX(bigF, smallF);
            System.out.println(rect.x());
        }
    }

    public static Rect getOffsetX(String source, String find) {
        //read in image default colors
        Mat sourceColor = imread(source);
        Mat sourceGrey = new Mat(sourceColor.size(), CV_8UC1);
        cvtColor(sourceColor, sourceGrey, COLOR_BGR2GRAY);
        //load in template in grey
        Mat template = imread(find, IMREAD_GRAYSCALE);//int = 0
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
        rectangle(sourceColor, rect, redColor(), 2, 0, 0);

        String namePrefix = FilenameUtils.getBaseName(source);

        imwrite(namePrefix + ".origin.marked.jpeg", sourceColor);
//        imwrite(prefix + ".template.jpeg", template);
//        imwrite(prefix + ".result.jpeg", result);
        return rect;
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