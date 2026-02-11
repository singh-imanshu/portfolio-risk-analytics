package com.himanshu.portfolio_risk_analytics.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a financial analyst expert in portfolio risk analysis. " +
                        "Provide concise, actionable insights based on quantitative metrics.")
                .build();
    }
}