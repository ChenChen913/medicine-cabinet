package com.family.medicinecabinet.controller;

import com.family.medicinecabinet.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主页控制器
 */
@RestController
public class HomeController {

    /**
     * 根路径欢迎信息
     */
    @GetMapping("/")
    public Result<String> home() {
        return Result.success("欢迎使用家庭智慧药箱管理系统API服务！");
    }
}