package com.li.bot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication

@EnableScheduling
@MapperScan("com.li.bot.mapper")
public class ProcessBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessBotApplication.class, args);

    }

}
