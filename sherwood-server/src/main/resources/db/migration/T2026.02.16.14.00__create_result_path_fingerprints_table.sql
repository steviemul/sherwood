CREATE TABLE result_path_fingerprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id UUID NOT NULL,
    fingerprint VARCHAR(255) NOT NULL,
    fingerprint_order INT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_path_fingerprints_result FOREIGN KEY (result_id) REFERENCES results(id) ON DELETE CASCADE
);

CREATE INDEX idx_path_fingerprints_result_id ON result_path_fingerprints(result_id);
CREATE INDEX idx_path_fingerprints_order ON result_path_fingerprints(result_id, fingerprint_order);
CREATE INDEX idx_path_fingerprints_value ON result_path_fingerprints(fingerprint);
