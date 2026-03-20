package com.audit.server;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class ServerApplication {

    @Bean
    public CommandLineRunner runner(ChatClient.Builder builder) {
        return args -> {
            ChatClient chatClient = builder.build();
        };
    }

    public static void main(String[] args) throws IOException {

        Connection.Response response = Jsoup.connect("https://api.groq.com/openai/v1/models")
                .ignoreContentType(true)
                .header("Authorization", "Bearer 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")
                .execute();

        System.out.println(response.body());


        SpringApplication.run(ServerApplication.class, args);
    }

}
