package com.example.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

   /* @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(value = "q") String q) {
        String content = chatClient.prompt(q).call().content();
        return ResponseEntity.ok(content);
    }*/

    @GetMapping("/")
    public String testing() {
        return "Hello World";
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam("q") String q, @RequestParam("conversationId") String conversationId) {
        System.out.println("\n========== CHAT REQUEST ==========");
        System.out.println("Conversation ID : " + conversationId);
        System.out.println("User Request    : " + q);
        System.out.println("==================================");

        ChatResponse response = chatClient.prompt()
                .user(q)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

        String answer = response.getResult()
                .getOutput()
                .getText();

        Usage usage = response.getMetadata().getUsage();

        System.out.println("\n========== AI RESPONSE ===========");
        System.out.println("AI Response     : " + answer);

        if (usage != null) {
            System.out.println("Prompt Tokens   : " + usage.getPromptTokens());
            System.out.println("Response Tokens : " + usage.getCompletionTokens());
            System.out.println("Total Tokens    : " + usage.getTotalTokens());
        } else {
            System.out.println("Token usage unavailable");
        }

        System.out.println("==================================\n");

        return ResponseEntity.ok(answer);
    }

}
