-- V1__init.sql

CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           TEXT NOT NULL UNIQUE,
  name            TEXT,
  profile_image   TEXT,
  provider        TEXT NOT NULL,
  provider_id     TEXT NOT NULL,
  plan            TEXT NOT NULL DEFAULT 'free',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (provider, provider_id)
);

CREATE TABLE documents (
  id              TEXT PRIMARY KEY,
  slug            TEXT NOT NULL UNIQUE,
  user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
  title           TEXT,
  doc_type        TEXT NOT NULL,
  r2_key          TEXT NOT NULL,
  content_size    BIGINT NOT NULL,
  status          TEXT NOT NULL DEFAULT 'pending',
  expires_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE storage_usage (
  user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  total_bytes     BIGINT NOT NULL DEFAULT 0,
  document_count  INTEGER NOT NULL DEFAULT 0,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token           TEXT NOT NULL UNIQUE,
  is_revoked      BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at      TIMESTAMPTZ NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_documents_user_id ON documents(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_expires_at ON documents(expires_at)
  WHERE expires_at IS NOT NULL AND status = 'active';
CREATE INDEX idx_documents_pending_cleanup ON documents(created_at)
  WHERE status = 'pending';
CREATE INDEX idx_documents_user_status_updated ON documents(user_id, status, updated_at DESC)
  WHERE user_id IS NOT NULL;
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
