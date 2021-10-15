package com.meread.selenium.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ScriptPython {
    Process mProcess;

    public static void main(String[] args) {
        ScriptPython scriptPython = new ScriptPython();
        scriptPython.runScript();
    }

    public void runScript() {
        Process process;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/Users/yangxg/java/my-project-2021/selenium-test/docker-arm/gatgap.py", "arg1", "arg2"});
            mProcess = process;
        } catch (Exception e) {
            System.out.println("Exception Raised" + e.toString());
        }
        InputStream stdout = mProcess.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                System.out.println("stdout: " + line);
            }
        } catch (IOException e) {
            System.out.println("Exception in reading output" + e.toString());
        }
    }
}
