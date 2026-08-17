-- One scheduled nudge about one document. Specification section 10.6.
--
-- A reminder is a row rather than a calculation because section 12 requires it
-- to be cancellable, retryable and auditable: "changing valid_until cancels
-- obsolete PENDING reminders", "scheduler queries overdue PENDING work". None of
-- that is expressible about something computed on the fly.

CREATE TABLE reminders (
    id                  UUID        NOT NULL,
    vehicle_document_id UUID        NOT NULL,

    -- 30, 14, 7, 3, 1 or 0 days before valid_until.
    offset_days         INTEGER     NOT NULL,

    -- An instant, not a local time: computed once from the document's
    -- valid_until, the account's notification_local_time and its IANA zone.
    -- Storing the instant is what lets the scheduler ask one question of the
    -- whole table rather than one per account.
    scheduled_at        TIMESTAMPTZ NOT NULL,

    -- PENDING / PROCESSING / SENT / FAILED / CANCELLED.
    status              VARCHAR(16) NOT NULL,

    attempt_count       INTEGER     NOT NULL DEFAULT 0,
    last_attempt_at     TIMESTAMPTZ,
    sent_at             TIMESTAMPTZ,

    -- Section 27: a code, never a payload. Nothing here may carry a push token,
    -- a policy number or anything else about the person.
    last_error_code     VARCHAR(64),

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_reminders PRIMARY KEY (id),
    CONSTRAINT fk_reminders_document FOREIGN KEY (vehicle_document_id)
        REFERENCES vehicle_documents (id) ON DELETE CASCADE
);

-- The scheduler's only question: what is PENDING and due? Section 12 requires it
-- to pick up overdue work, so this is scanned on every tick and nothing else is.
CREATE INDEX ix_reminders_due ON reminders (status, scheduled_at);

-- Reconciliation's question: what is still pending for this document?
CREATE INDEX ix_reminders_document ON reminders (vehicle_document_id, status);