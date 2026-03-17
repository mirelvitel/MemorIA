CREATE EXTENSION IF NOT EXISTS vector;

-- HNSW index for fast cosine similarity search on embeddings
-- "IF NOT EXISTS" prevents errors on repeated app restarts
CREATE INDEX IF NOT EXISTS book_chunks_embedding_idx
    ON book_chunks USING hnsw (embedding vector_cosine_ops);