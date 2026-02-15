CREATE TABLE IF NOT EXISTS vector_store_ollama_rules (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(768)
);

CREATE INDEX ON vector_store_ollama_rules USING HNSW (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_ollama_vendor ON vector_store_ollama_rules ((metadata->>'vendor'));
CREATE INDEX IF NOT EXISTS idx_ollama_rule_id ON vector_store_ollama_rules ((metadata->>'id'));