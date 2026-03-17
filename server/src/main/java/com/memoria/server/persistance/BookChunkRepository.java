package com.memoria.server.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookChunkRepository extends JpaRepository<BookChunk, UUID> {

    List<BookChunk> findByBookIdOrderByPageNumber(UUID bookId);

    void deleteByBookId(UUID bookId);
}