package com.example.controller;

import com.example.dto.AskRequest;
import com.example.dto.AskResponse;
import com.example.dto.IngestRequest;
import com.example.service.RagService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    // API 1: Ingest documents
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        List<String> documents = List.of("Our company name is ABC Technologies.",
                "Our company uses Java as its primary programming language.",
                "We use Spring Boot to develop backend microservices.",
                "Our primary relational database is MySQL.",
                "We use Apache Kafka for asynchronous communication between microservices.",
                "Our backend applications follow microservices architecture.",
                "We use Redis for caching frequently accessed data.",
                "Docker is used to containerize our applications.",
                "Kubernetes is used for container orchestration.",
                "Our applications are deployed on AWS cloud infrastructure.");

        IngestRequest request = new IngestRequest(documents);
        int count = ragService.ingestDocuments(request.documents());
        return Map.of("message", "Documents stored successfully", "totalDocuments", count);
    }

    // API 2: Ask question
    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return ragService.askQuestion(request.question());
    }

    @PostMapping("/advisor/ask")
    public AskResponse askUsingAdvisor(@RequestBody AskRequest request) {
        return ragService.askQuestionUsingAdvisor(request.question());
    }
}
