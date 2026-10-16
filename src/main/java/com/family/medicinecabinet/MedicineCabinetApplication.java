package com.family.medicinecabinet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 家庭智慧药箱管理系统 - 主应用类
 * 
 * @author Family Medicine Cabinet Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.family.medicinecabinet.mapper")
public class MedicineCabinetApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicineCabinetApplication.class, args);
    }
}
