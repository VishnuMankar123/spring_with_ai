package com.example.controller;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        System.out.println("=================================");
        System.out.println("USER MESSAGE: " + request.message());
        System.out.println("=================================");

        String response = chatClient
                .prompt()
                .system("""
                    You are an AI assistant connected to a
                    filesystem MCP server.

                    For file creation, reading and listing
                    requests, use the available filesystem tools.

                    Work only inside the configured workspace.

                    Do not claim that a file was created or read
                    unless the corresponding tool successfully
                    executes.

                    Do not delete or move files.
                    If the user requests deletion or moving,
                    explain that this demo does not allow it.
                    """)
                .user(request.message())
                .call()
                .content();

        System.out.println("=================================");
        System.out.println("AI RESPONSE: " + response);
        System.out.println("=================================");

        return new ChatResponse(response);
    }
}
