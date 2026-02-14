CREATE TABLE sarifs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    vendor VARCHAR(255) NOT NULL,
    repository VARCHAR(255) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sarifs_filename ON sarifs(filename);
CREATE INDEX idx_sarifs_storage_key ON sarifs(storage_key);

CREATE INDEX idx_sarifs_repository ON sarifs(repository);
CREATE INDEX idx_sarifs_identifier ON sarifs(identifier);
