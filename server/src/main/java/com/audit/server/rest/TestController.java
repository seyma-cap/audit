package com.audit.server.rest;

import com.audit.server.service.AiService;
import com.audit.server.service.JSoupService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    JSoupService jSoupService;

    private final OpenAiChatModel chatModel;

    @Autowired
    public TestController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/generate")
    public Map<String, Object> generate(@RequestParam String message) throws JsonProcessingException {
        String engineeredMessage = "Get a fresh perspective! Simply provide a message, and rephrase it into one big sentence for my ecommerce application.Only give me the result. I don't need extra details. Here's the input: " + message;
        Prompt prompt = new Prompt(new UserMessage(engineeredMessage));
        ChatResponse response = this.chatModel.call(prompt);
        return Map.of("result", response);
    }

    @GetMapping(path = "/jsoup", produces = "application/json")
    public ResponseEntity<String> testApi(){
        jSoupService.getData();
        String response = "crawler works";
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

//    @GetMapping(path = "/ai", produces = "application/json")
//    public ResponseEntity<String> testAi(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message){
//        return ResponseEntity.status(HttpStatus.OK).body(aiService.getResponse(message));
//    }
}
