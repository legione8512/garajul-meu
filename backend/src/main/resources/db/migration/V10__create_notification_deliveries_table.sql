-- What actually left the building, and to which device. Specification section 10.8.
--
-- One row per (reminder, device). The reminder says "this account should be
-- told"; these rows say what was attempted. Both are needed: an account with
-- three phones has one reminder and three deliveries, and two of them can fail
-- while the third arrives.
--
-- The tenth and last table of section 10.

CREATE TABLE notification_deliveries (
    id              UUID        NOT NULL,
    reminder_id     UUID        NOT NULL,
    user_device_id  UUID        NOT NULL,

    -- PENDING / SENT / FAILED / CANCELLED. Deliberately one state short of the
    -- reminder's set: there is no PROCESSING here, because a delivery is never
    -- claimed on its own. Claiming the reminder claims everything under it.
    status          VARCHAR(16) NOT NULL,

    attempt_count   INTEGER     NOT NULL DEFAULT 0,

    -- The provider accepted it. Not "the phone showed it" - nobody can know that.
    sent_at         TIMESTAMPTZ,

    -- A provider's code and nothing else. Section 27: no payloads, no tokens, no
    -- message bodies in a column that ends up in a support conversation.
    last_error_code VARCHAR(64),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_notification_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_notification_deliveries_reminder FOREIGN KEY (reminder_id)
        REFERENCES reminders (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_device FOREIGN KEY (user_device_id)
        REFERENCES user_devices (id) ON DELETE CASCADE
);

-- Section 10.8 calls this the "database idempotency guarantee", and it is what
-- makes a retry safe: a second pass over the same reminder finds the row it
-- wrote last time and increments it rather than sending twice. Section 19 says
-- two passes never run at once in V1 - which is precisely the assumption most
-- likely to stop being true, and this constraint is what survives it.
--
-- No separate index on reminder_id: this one leads with it, so "what happened to
-- this reminder" - the question the 11.5 view asks - is already served.
CREATE UNIQUE INDEX ux_notification_deliveries_reminder_device
    ON notification_deliveries (reminder_id, user_device_id);