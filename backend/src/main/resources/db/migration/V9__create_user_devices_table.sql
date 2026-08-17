-- Where a push notification is delivered. Specification section 10.7.
--
-- Empty for the whole of V1 web: section 18 makes push Android and iOS only,
-- and the native apps are phases 17 and 18. The table exists now because the
-- scheduler in 11.4 has to know what "nowhere to send it" looks like.

CREATE TABLE user_devices (
    id                    UUID        NOT NULL,
    user_id               UUID        NOT NULL,

    -- IOS or ANDROID. Section 18: no web push registrations, ever, in V1.
    platform              VARCHAR(16) NOT NULL,

    -- The one secret in this schema that must be readable again. Every other one
    -- is one-way - Argon2 for passwords, SHA-256 for refresh tokens, hashes for
    -- verification codes - because we only ever need to compare. FCM needs the
    -- value itself to deliver, so section 10.7 requires it retrievable and
    -- classifies it sensitive instead: excluded from logs and from every API
    -- response. Protected at rest by the database (Neon encrypts); application
    -- level encryption is the recorded upgrade path.
    push_token            TEXT        NOT NULL,

    device_name           VARCHAR(120),

    -- Device level, distinct from the account level switch in
    -- notification_preferences: one phone silenced is not the account silenced.
    notifications_enabled BOOLEAN     NOT NULL DEFAULT true,

    token_updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at          TIMESTAMPTZ,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_devices PRIMARY KEY (id),
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Global, not per user, and that is the point. An FCM token identifies one
-- installation of the app, not one account: if somebody signs out of a phone and
-- somebody else signs in, the token must move to the new account or that handset
-- keeps receiving the previous session's notifications.
CREATE UNIQUE INDEX ux_user_devices_token ON user_devices (push_token);

-- "Everything this account can be reached on", which is what sending asks.
CREATE INDEX ix_user_devices_user ON user_devices (user_id);