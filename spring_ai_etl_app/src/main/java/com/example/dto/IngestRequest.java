package com.example.dto;

import java.util.List;

public record IngestRequest(List<String> documents) {
}
