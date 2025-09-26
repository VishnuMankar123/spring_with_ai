package com.example.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping
public class ChatController {

  /*  @Autowired
    private ChatClient openAiChatClient;*/

    @Autowired
    private ChatClient ollamaChatClient;



    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(value = "q") String q) {
        // Logic to process the chat message
        String content = ollamaChatClient.prompt(q).call().content();
        return ResponseEntity.ok(content);
    }

    @GetMapping("/")
    public String testing() {
        return "Hello World";
    }
}