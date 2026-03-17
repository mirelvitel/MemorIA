package com.memoria.server.service;

import com.memoria.server.service.PdfExtractorService.PageText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    public record Chunk(int pageNumber, String text) {}

    /**
     * Chunks pages with ~10% overlap between adjacent pages.
     * Each chunk is one page's text, prefixed with the tail of the previous page
     * and suffixed with the head of the next page.
     */
    public List<Chunk> chunkPages(List<PageText> pages) {
        if (pages.isEmpty()) {
            return List.of();
        }

        List<Chunk> chunks = new ArrayList<>(pages.size());

        for (int i = 0; i < pages.size(); i++) {
            PageText current = pages.get(i);
            String currentText = current.text();

            String overlapBefore = "";
            if (i > 0) {
                String prevText = pages.get(i - 1).text();
                overlapBefore = tail(prevText, overlapSize(prevText));
            }

            String overlapAfter = "";
            if (i < pages.size() - 1) {
                String nextText = pages.get(i + 1).text();
                overlapAfter = head(nextText, overlapSize(nextText));
            }

            String chunkText = buildChunk(overlapBefore, currentText, overlapAfter);
            chunks.add(new Chunk(current.pageNumber(), chunkText));
        }

        log.info("Created {} chunks from {} pages", chunks.size(), pages.size());
        return chunks;
    }

    private int overlapSize(String text) {
        // 10% of the text length, in characters
        return (int) (text.length() * 0.10);
    }

    /** Last n characters, breaking at a word boundary. */
    private String tail(String text, int n) {
        if (n >= text.length()) return text;
        String sub = text.substring(text.length() - n);
        int firstSpace = sub.indexOf(' ');
        return firstSpace > 0 ? sub.substring(firstSpace + 1) : sub;
    }

    /** First n characters, breaking at a word boundary. */
    private String head(String text, int n) {
        if (n >= text.length()) return text;
        String sub = text.substring(0, n);
        int lastSpace = sub.lastIndexOf(' ');
        return lastSpace > 0 ? sub.substring(0, lastSpace) : sub;
    }

    private String buildChunk(String before, String main, String after) {
        StringBuilder sb = new StringBuilder();
        if (!before.isEmpty()) {
            sb.append(before).append(" ");
        }
        sb.append(main);
        if (!after.isEmpty()) {
            sb.append(" ").append(after);
        }
        return sb.toString();
    }
}