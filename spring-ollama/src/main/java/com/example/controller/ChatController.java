package com.example.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping
public class ChatController {

    private ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(value = "q") String q) {
        // Logic to process the chat message
        String content = chatClient.prompt(q).call().content();
        return ResponseEntity.ok(content);
    }

    @GetMapping("/")
    public String testing() {
        return "Hello World";
    }


    @PostMapping(value = "/parse-resume-ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> parseResume(@RequestPart("file") MultipartFile file) throws Exception {
        // 1. Extract raw text (simple version – plain text read)
        String resumeText = new String(file.getBytes(), StandardCharsets.UTF_8);

        // (better: use Apache Tika to handle PDF/DOCX properly)
        // String resumeText = tika.parseToString(file.getInputStream());

        // 2. Prepare AI prompt
        String prompt = """
                Extract the following fields from the resume text:

                - fullName
                - email
                - phone
                - education (list)
                - experience (list)
                - skills (list)

                Return output strictly in JSON format.

                Resume Text:
                """ + resumeText;

        // 3. Send to Ollama
        String aiResponse = chatClient.prompt(prompt).call().content();

        // 4. Return JSON back to frontend
        return ResponseEntity.ok(aiResponse);
    }
}
