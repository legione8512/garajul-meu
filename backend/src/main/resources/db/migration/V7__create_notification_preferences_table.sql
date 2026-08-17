-- What a person wants to be told about, and when. Specification section 10.5.
--
-- One row per account, created the first time somebody saves something rather
-- than at registration: that would mean changing the registration transaction
-- and backfilling every account that already exists, to store values identical
-- to the defaults. A missing row and a row holding the defaults mean the same
-- thing, and only one of them costs a write.

CREATE TABLE notification_preferences (
    id                      UUID        NOT NULL,
    user_id                 UUID        NOT NULL,

    -- The global gate, applied at delivery rather than at generation: reminders
    -- are still calculated, so turning notifications back on does not need the
    -- history rebuilt. Section 18 makes push native-only, and V1 web registers
    -- no devices at all, so nothing is sent either way today.
    notifications_enabled   BOOLEAN     NOT NULL DEFAULT true,

    remind_30_days          BOOLEAN     NOT NULL DEFAULT true,
    remind_14_days          BOOLEAN     NOT NULL DEFAULT true,
    remind_7_days           BOOLEAN     NOT NULL DEFAULT true,
    remind_3_days           BOOLEAN     NOT NULL DEFAULT true,
    remind_1_day            BOOLEAN     NOT NULL DEFAULT true,
    remind_on_expiry        BOOLEAN     NOT NULL DEFAULT true,

    -- Section 12: 09:00 in the account's own timezone, which lives on users.
    -- A TIME rather than a TIMESTAMPTZ because it is a time of day and not an
    -- instant - the instant is calculated per reminder, from this and the zone.
    notification_local_time TIME        NOT NULL DEFAULT '09:00',

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_notification_preferences PRIMARY KEY (id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Section 10.5 says one-to-one, so the database says it too.
CREATE UNIQUE INDEX ux_notification_preferences_user
    ON notification_preferences (user_id);