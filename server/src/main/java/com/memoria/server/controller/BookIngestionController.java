package com.memoria.server.controller;

import com.memoria.server.persistance.BookChunk;
import com.memoria.server.persistance.BookChunkRepository;
import com.memoria.server.service.ChunkingService;
import com.memoria.server.service.ChunkingService.Chunk;
import com.memoria.server.service.EmbeddingService;
import com.memoria.server.service.PdfExtractorService;
import com.memoria.server.service.PdfExtractorService.PageText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/books")
public class BookIngestionController {

    private static final Logger log = LoggerFactory.getLogger(BookIngestionController.class);

    private final PdfExtractorService pdfExtractorService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final BookChunkRepository bookChunkRepository;

    public BookIngestionController(PdfExtractorService pdfExtractorService,
                                   ChunkingService chunkingService,
                                   EmbeddingService embeddingService,
                                   BookChunkRepository bookChunkRepository) {
        this.pdfExtractorService = pdfExtractorService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.bookChunkRepository = bookChunkRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadBook(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        try {
            // 1. Extract pages
            List<PageText> pages = pdfExtractorService.extractPages(file);

            // 2. Chunk with overlap
            List<Chunk> chunks = chunkingService.chunkPages(pages);

            // 3. Embed all chunks
            List<String> texts = chunks.stream().map(Chunk::text).toList();
            List<float[]> embeddings = embeddingService.embedAll(texts);

            // 4. Store in pgvector
            // TODO: replace with real book ID once Book entity exists
            UUID bookId = UUID.randomUUID();
            List<BookChunk> entities = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                entities.add(new BookChunk(bookId, chunks.get(i).pageNumber(), chunks.get(i).text(), embeddings.get(i)));
            }
            bookChunkRepository.saveAll(entities);

            log.info("Stored {} chunks for book {} in pgvector", entities.size(), bookId);

            return ResponseEntity.ok(Map.of(
                    "message", "PDF ingested successfully",
                    "bookId", bookId.toString(),
                    "totalPages", pages.size(),
                    "totalChunks", chunks.size(),
                    "embeddingDimension", embeddings.getFirst().length
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process upload", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process file"));
        }
    }
}