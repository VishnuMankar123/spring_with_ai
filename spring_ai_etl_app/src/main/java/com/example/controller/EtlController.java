package com.example.controller;


import com.example.service.PdfEtlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etl")
public class EtlController {

    private final PdfEtlService etlService;

    public EtlController(PdfEtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingestPdf() {
        return etlService.ingestPdf();
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String question) {
        return etlService.searchDocuments(question);
    }
}