package com.xieh.springbootminio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author 谢辉
 * @Classname RouterController
 * @Description TODO
 * @Date 2020/10/13 21:54
 */
@Controller
public class RouterController {
    @GetMapping({"/", "/index.html"})
    public String index() {
        return "index";
    }

    @GetMapping({"/upload.html"})
    public String upload() {
        return "upload";
    }
}
