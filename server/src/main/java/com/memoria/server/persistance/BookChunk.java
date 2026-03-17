package com.memoria.server.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "book_chunks")
public class BookChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] embedding;

    protected BookChunk() {}

    public BookChunk(UUID bookId, int pageNumber, String chunkText, float[] embedding) {
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.chunkText = chunkText;
        this.embedding = embedding;
    }

    public UUID getId() { return id; }
    public UUID getBookId() { return bookId; }
    public int getPageNumber() { return pageNumber; }
    public String getChunkText() { return chunkText; }
    public float[] getEmbedding() { return embedding; }
}