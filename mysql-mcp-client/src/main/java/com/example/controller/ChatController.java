package com.example.controller;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        log.info("User question: {}", request.message());

        String systemPrompt = """
                You are an AI MySQL database assistant.

                You have access to MySQL database tools through MCP.

                Instructions:

                1. Understand the user's question.
                2. Use the available MySQL MCP tools when database
                   information is required.
                3. Use listTables to list available tables.
                4. Use describeTable to inspect table structure.
                5. Use executeReadOnlyQuery for database queries.
                6. Only execute read-only SQL queries.
                7. Never generate INSERT, UPDATE, DELETE or DROP queries.
                8. Do not invent database records or query results.
                9. Explain the result in simple language.
                10. If a tool returns an error, explain the error.

                You are connected to the MySQL database configured on the MCP server (currently micro_yt).
                """;

        String answer = chatClient
                .prompt()
                .system(systemPrompt)
                .user(request.message())
                .call()
                .content();

        log.info("AI response: {}", answer);

        return new ChatResponse(answer);
    }
}
