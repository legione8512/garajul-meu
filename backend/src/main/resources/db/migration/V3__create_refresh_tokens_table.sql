-- Rotating opaque refresh tokens. Specification section 10.9.
--
-- Only the SHA-256 of the token is stored. The token itself has 256 bits of
-- entropy, so a fast digest is safe here; a slow one would also be unusable,
-- because a per-hash salt makes lookup by value impossible.

CREATE TABLE refresh_tokens (
    id                   UUID         NOT NULL,
    user_id              UUID         NOT NULL,
    token_hash           VARCHAR(64)  NOT NULL,
    family_id            UUID         NOT NULL,
    expires_at           TIMESTAMPTZ  NOT NULL,
    revoked_at           TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Every refresh is a lookup by hash, so this index is on the hot path.
-- Unique also guarantees two tokens can never collide onto one row.
CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Reuse detection revokes a whole family at once.
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);