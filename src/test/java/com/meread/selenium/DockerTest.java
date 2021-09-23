package com.meread.selenium;

import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.UnixDocker;

import java.io.File;
import java.io.IOException;

/**
 * Created by yangxg on 2021/9/22
 *
 * @author yangxg
 */
public class DockerTest {
    public static void main(String[] args) throws IOException {
        Containers containers = new UnixDocker(new File("/var/run/docker.sock")).containers();
        for (Container container : containers) {
            String image = container.getString("Image");
            if ("selenoid/chrome:89.0".equals(image)) {
                container.remove(true, true, false);
            }
        }
    }
}
