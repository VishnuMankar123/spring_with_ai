package com.example.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfEtlService {

    private static final Logger log = LoggerFactory.getLogger(PdfEtlService.class);

    private static final String PDF_PATH = "classpath:/documents/spring-ai-notes.pdf";

    private final VectorStore vectorStore;

    public PdfEtlService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Map<String, Object> ingestPdf() {

        long startTime = System.currentTimeMillis();

        log.info("========== ETL PIPELINE STARTED ==========");
        log.info("PDF source: {}", PDF_PATH);

        // -------------------------------------
        // STEP 1: EXTRACT
        // -------------------------------------

        log.info("[EXTRACT] Starting PDF extraction");

        PagePdfDocumentReader pdfReader =
                new PagePdfDocumentReader(
                        PDF_PATH,
                        PdfDocumentReaderConfig.builder()
                                .withPageTopMargin(0)
                                .withPagesPerDocument(1)
                                .build()
                );

        List<Document> extractedDocuments = pdfReader.read();

        log.info("[EXTRACT] Documents extracted: {}", extractedDocuments.size());

        if (extractedDocuments.isEmpty()) {
            throw new IllegalStateException("No text extracted from PDF. Check the PDF file.");
        }

        for (int i = 0; i < extractedDocuments.size(); i++) {

            Document document = extractedDocuments.get(i);

            log.info("[EXTRACT] Document number: {}", i + 1);
            log.info("[EXTRACT] Metadata: {}", document.getMetadata());

            log.debug("[EXTRACT] Extracted content:\n{}", document.getText());
        }


        // -------------------------------------
        // STEP 2: ADD METADATA
        // -------------------------------------

        log.info("[METADATA] Adding custom metadata");

        List<Document> enrichedDocuments = new ArrayList<>();

        for (Document document : extractedDocuments) {

            Map<String, Object> metadata = new HashMap<>(document.getMetadata());

            metadata.put("source", "spring-ai-notes.pdf");
            metadata.put("document_type", "PDF");
            metadata.put("ingested_at", Instant.now().toString());

            Document enrichedDocument =
                    new Document(
                            document.getText(),
                            metadata
                    );

            enrichedDocuments.add(enrichedDocument);

            log.info("[METADATA] Metadata added: {}", metadata);
        }

        log.info("[METADATA] Enriched documents: {}",
                enrichedDocuments.size());


        // -------------------------------------
        // STEP 3: TRANSFORM / CHUNKING
        // -------------------------------------

        log.info("[TRANSFORM] Starting text splitting");

        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(100)
                        .withMinChunkSizeChars(50)
                        .withMinChunkLengthToEmbed(10)
                        .withMaxNumChunks(1000)
                        .withKeepSeparator(true)
                        .build();

        List<Document> chunks =
                splitter.apply(enrichedDocuments);

        log.info("[TRANSFORM] Original documents: {}",
                enrichedDocuments.size());

        log.info("[TRANSFORM] Total chunks created: {}",
                chunks.size());

        if (chunks.isEmpty()) {
            throw new IllegalStateException("No chunks generated. Check extracted PDF content.");
        }

        for (int i = 0; i < chunks.size(); i++) {

            Document chunk = chunks.get(i);

            log.info("[CHUNK {}] ID: {}", i + 1, chunk.getId());

            log.info("[CHUNK {}] Metadata: {}",
                    i + 1, chunk.getMetadata());

            log.debug("[CHUNK {}] Content:\n{}",
                    i + 1, chunk.getText());
        }


        // -------------------------------------
        // STEP 4: LOAD INTO VECTOR STORE
        // -------------------------------------

        log.info("[LOAD] Sending {} chunks to MariaDB",
                chunks.size());

        vectorStore.write(chunks);

        log.info("[LOAD] Documents successfully saved");

        long totalTime =
                System.currentTimeMillis() - startTime;

        log.info("========== ETL PIPELINE COMPLETED ==========");
        log.info("Extracted documents: {}",
                extractedDocuments.size());
        log.info("Transformed chunks: {}", chunks.size());
        log.info("Total processing time: {} ms", totalTime);


        // -------------------------------------
        // STEP 5: RETURN SUMMARY
        // -------------------------------------

        Map<String, Object> response =
                new HashMap<>();

        response.put("status", "SUCCESS");
        response.put("source", PDF_PATH);
        response.put("extractedDocuments",
                extractedDocuments.size());
        response.put("generatedChunks", chunks.size());
        response.put("storedIn", "MariaDB Vector Store");
        response.put("processingTimeMs", totalTime);

        return response;
    }


    // -------------------------------------
    // SEARCH STORED DOCUMENTS
    // -------------------------------------

    public List<Map<String, Object>> searchDocuments(
            String question) {

        log.info("========== VECTOR SEARCH STARTED ==========");
        log.info("[SEARCH] User query: {}", question);

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .build();

        log.info("[SEARCH] Calling MariaDB Vector Store");

        List<Document> results =
                vectorStore.similaritySearch(searchRequest);

        log.info("[SEARCH] Documents returned: {}",
                results.size());

        List<Map<String, Object>> response =
                new ArrayList<>();

        for (Document document : results) {

            log.info("[SEARCH] Document ID: {}",
                    document.getId());

            log.info("[SEARCH] Similarity score: {}",
                    document.getScore());

            log.info("[SEARCH] Metadata: {}",
                    document.getMetadata());

            log.debug("[SEARCH] Content:\n{}",
                    document.getText());

            Map<String, Object> item = new HashMap<>();

            item.put("id", document.getId());
            item.put("score", document.getScore());
            item.put("metadata", document.getMetadata());
            item.put("content", document.getText());

            response.add(item);
        }

        log.info("========== VECTOR SEARCH COMPLETED ==========");

        return response;
    }
}