package com.example.service;

import com.example.dto.AskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        SearchRequest searchRequest =
                SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build();

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();

        this.chatClient = chatClientBuilder.defaultAdvisors(qaAdvisor).build();
        this.vectorStore = vectorStore;
    }

    // STEP 1: Store documents in MariaDB Vector Store
    public int ingestDocuments(List<String> data) {

        log.info("Starting document ingestion...");

        // Convert String into Document objects
        List<Document> documents = data.stream()
                .map(text -> new Document(
                        text,
                        Map.of("source", "company-documents")
                ))
                .toList();

        log.info("Total documents: {}", documents.size());
        // Generate embeddings and store documents
        vectorStore.add(documents);
        log.info("Documents successfully stored!");
        return documents.size();
    }

    // STEP 2: Search relevant documents and generate answer
    public AskResponse askQuestion(String question) {
        log.info("User question: {}", question);
        // Search similar documents from MariaDB
        List<Document> retrievedDocuments =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(4)
                                .build()
                );

        log.info("Retrieved documents: {}", retrievedDocuments.size());

        // If no relevant documents found
        if (retrievedDocuments.isEmpty()) {
            return new AskResponse(question, "I don't have relevant information in my documents.", List.of());
        }

        // Convert retrieved documents into context
        String context = retrievedDocuments.stream()
                .map(Document::getText)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        log.info("Retrieved Context:\n{}", context);

        // Send context + question to Ollama
        String answer = chatClient.prompt()

                .system("""
                        You are a company knowledge assistant.

                        Answer the user's question ONLY using
                        the provided context.

                        Rules:
                        1. Do not use outside knowledge.
                        2. Do not invent information.
                        3. If the answer is not available in
                           the context, say:
                           "I don't know based on the provided documents."
                        4. Give clear and simple answers.
                        """)

                .user("""
                        Context:
                        %s

                        Question:
                        %s
                        """.formatted(context, question))

                .call()
                .content();

        log.info("Generated Answer: {}", answer);

        // Return answer + retrieved documents
        List<String> sources = retrievedDocuments.stream()
                .map(Document::getText)
                .toList();

        return new AskResponse(
                question,
                answer,
                sources
        );
    }

    public AskResponse askQuestionUsingAdvisor(String question) {
        log.info("User question: {}", question);
        // No manual similarity search
        // No manual context creation
        // No manual prompt injection

        String answer = chatClient.prompt().user(question).call().content();
        log.info("Generated Answer: {}", answer);

        return new AskResponse(question, answer, List.of());
    }
}
