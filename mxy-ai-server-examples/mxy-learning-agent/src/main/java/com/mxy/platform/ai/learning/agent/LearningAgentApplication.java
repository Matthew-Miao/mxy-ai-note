package com.mxy.platform.ai.learning.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 渐进式学习助手应用启动类
 * 基于SpringAI Alibaba Graph的智能学习系统
 *
 * @author mxy
 * @date 2025-01-23
 */
@SpringBootApplication
public class LearningAgentApplication {

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LearningAgentApplication.class, args);
    }
}