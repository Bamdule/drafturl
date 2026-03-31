CREATE TABLE inquiries (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type        TEXT NOT NULL,
  email       TEXT NOT NULL,
  name        TEXT,
  subject     TEXT NOT NULL,
  message     TEXT NOT NULL,
  document_id TEXT REFERENCES documents(id) ON DELETE SET NULL,
  status      TEXT NOT NULL DEFAULT 'pending',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inquiries_status ON inquiries(status);
