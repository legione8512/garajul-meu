-- A suggestion, a problem or a wished-for feature, sent from the application's
-- Sugestii tab. Added in 1.1 at the owner's request.
--
-- Stored as well as emailed to the operator, by the owner's decision: the email
-- is how it is read and answered, the row is what survives a mail that never
-- arrived. The platform and version say what the person was running, which is
-- most of what a problem report needs.
--
-- The account's messages go with the account: ON DELETE CASCADE, as everything
-- else a person wrote does.

CREATE TABLE feedback_messages (
    id           UUID          NOT NULL,
    user_id      UUID          NOT NULL,

    -- IDEA / PROBLEM / FEATURE.
    category     VARCHAR(16)   NOT NULL,

    message      VARCHAR(2000) NOT NULL,

    -- WEB / ANDROID / IOS, and the application's version where it has one.
    platform     VARCHAR(16),
    app_version  VARCHAR(32),

    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_feedback_messages PRIMARY KEY (id),
    CONSTRAINT fk_feedback_messages_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- The daily allowance's question: how many has this account sent lately?
CREATE INDEX ix_feedback_messages_user_created ON feedback_messages (user_id, created_at);
