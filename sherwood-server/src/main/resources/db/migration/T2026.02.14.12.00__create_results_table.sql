CREATE TABLE results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sarif UUID NOT NULL,
    location VARCHAR(255) NOT NULL,
    line_number INT NOT NULL,
    fingerprint VARCHAR(255),
    snippet TEXT,
    description TEXT,
    rule_id VARCHAR(255) NOT NULL,
    confidence DECIMAL DEFAULT 0.0,
    reachable BOOLEAN DEFAULT false,
    graph TEXT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_results_sarif FOREIGN KEY (sarif) REFERENCES sarifs(id) ON DELETE CASCADE
);

CREATE INDEX idx_results_sarif ON results(sarif);
CREATE INDEX idx_results_location ON results(location);
CREATE INDEX idx_results_line_number ON results(line_number);
CREATE INDEX idx_results_rule_id ON results(rule_id);
