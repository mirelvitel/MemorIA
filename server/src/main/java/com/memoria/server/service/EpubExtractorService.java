package com.memoria.server.service;

import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.domain.SpineReference;
import io.documentnode.epub4j.epub.EpubReader;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EpubExtractorService {

    private static final Logger log = LoggerFactory.getLogger(EpubExtractorService.class);

    private static final int MAX_PAGE_CHARS = 2000;

    public List<PdfExtractorService.PageText> extractPages(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        if (bytes.length < 2 || bytes[0] != 0x50 || bytes[1] != 0x4B) {
            throw new IllegalArgumentException("File is not a valid EPUB (invalid magic bytes)");
        }

        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(new ByteArrayInputStream(bytes));

        List<SpineReference> spineRefs = book.getSpine().getSpineReferences();
        log.info("EPUB loaded: {} spine items", spineRefs.size());

        List<PdfExtractorService.PageText> pages = new ArrayList<>();
        int pageNumber = 1;

        for (SpineReference ref : spineRefs) {
            Resource resource = ref.getResource();
            String html = new String(resource.getData());
            String text = Jsoup.parse(html).text().strip();

            if (text.isEmpty()) {
                log.warn("Spine item '{}' has no extractable text, skipping", resource.getId());
                continue;
            }

            // Split long chapters into page-sized sub-pages
            for (String subPage : splitIntoPages(text)) {
                pages.add(new PdfExtractorService.PageText(pageNumber, subPage));
                pageNumber++;
            }
        }

        if (pages.isEmpty()) {
            throw new IllegalArgumentException(
                    "No text could be extracted from this EPUB. The file may be image-based or malformed.");
        }

        log.info("Extracted text from {} EPUB pages (from {} spine items)", pages.size(), spineRefs.size());
        return pages;
    }

    private List<String> splitIntoPages(String text) {
        if (text.length() <= MAX_PAGE_CHARS) {
            return List.of(text);
        }

        List<String> pages = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + MAX_PAGE_CHARS, text.length());

            // Break at a word boundary if not at the end of the text
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            pages.add(text.substring(start, end).strip());
            start = end + 1;
        }

        return pages;
    }
}