CREATE TABLE tags (
  id         BIGSERIAL PRIMARY KEY,
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       VARCHAR(50) NOT NULL,
  created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, name)
);

CREATE TABLE document_tags (
  document_id VARCHAR(12) NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  tag_id      BIGINT      NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (document_id, tag_id)
);

CREATE INDEX idx_document_tags_tag_id ON document_tags(tag_id);
CREATE INDEX idx_tags_user_id ON tags(user_id);
