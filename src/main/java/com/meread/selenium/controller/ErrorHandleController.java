package com.meread.selenium.controller;

import com.meread.selenium.service.BaseWebDriverManager;
import com.meread.selenium.service.JDService;
import com.meread.selenium.service.WebDriverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorHandleController implements ErrorController {

    @Autowired
    private JDService jdService;

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        //获取statusCode
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        model.addAttribute("initSuccess", jdService.isInitSuccess());
        if (statusCode == 404) {
            return "/error/404";
        } else {
            return "/error/500";
        }
    }
}
