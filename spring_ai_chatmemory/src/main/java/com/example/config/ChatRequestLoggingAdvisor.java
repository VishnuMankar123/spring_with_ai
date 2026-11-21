package com.example.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public class ChatRequestLoggingAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "ChatRequestLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        // Run after the memory advisor
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        System.out.println("\n========== ACTUAL LLM REQUEST ==========");
        request.prompt().getInstructions().forEach(message -> {
            System.out.println("\nRole: " + message.getMessageType());
            System.out.println("Content: " + message.getText());
        });

        System.out.println("\n========================================");
        ChatClientResponse response = chain.nextCall(request);
        System.out.println("========================================\n");
        return response;
    }
}
