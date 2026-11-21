package com.example.dto;

import java.util.List;

public record AskResponse(
        String question,
        String answer,
        List<String> retrievedDocuments
) {
}
