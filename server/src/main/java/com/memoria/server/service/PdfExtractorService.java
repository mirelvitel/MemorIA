package com.memoria.server.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);

    public record PageText(int pageNumber, String text) {}

    public List<PageText> extractPages(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        // Validate magic bytes: PDF starts with %PDF
        if (bytes.length < 4 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            throw new IllegalArgumentException("File is not a valid PDF (invalid magic bytes)");
        }

        List<PageText> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(bytes)) {
            int totalPages = document.getNumberOfPages();
            log.info("PDF loaded: {} pages", totalPages);

            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document).strip();

                if (text.isEmpty()) {
                    log.warn("Page {} has no extractable text (possibly scanned/image-based)", i);
                    continue;
                }

                pages.add(new PageText(i, text));
            }
        }

        if (pages.isEmpty()) {
            throw new IllegalArgumentException(
                    "No text could be extracted from this PDF. Scanned/image-based PDFs are not supported.");
        }

        log.info("Extracted text from {} pages", pages.size());
        return pages;
    }
}