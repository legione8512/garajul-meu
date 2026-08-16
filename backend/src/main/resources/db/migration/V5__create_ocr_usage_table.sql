-- What each account has spent of its OCR allowance. Specification section 13.
--
-- This is deliberately not the rate limiter. AuthRateLimit counts attempts in
-- memory, and a restart forgiving somebody their failed logins costs nothing.
-- An OCR request costs money at Google, and section 13 sets an allowance of ten
-- a day and thirty a month - so a restart must not hand everybody a fresh
-- budget, and a month is far too long a window to keep in a map that grows with
-- every user who ever appears.
--
-- One row per account per day. The month is the sum of its days, which is why
-- there is no second table and no monthly row to keep in step with the daily
-- one.

CREATE TABLE ocr_usage (
    id            UUID        NOT NULL,
    user_id       UUID        NOT NULL,
    usage_date    DATE        NOT NULL,
    request_count INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_ocr_usage PRIMARY KEY (id),
    CONSTRAINT fk_ocr_usage_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- One row per account per day, enforced rather than assumed - two rows for the
-- same day would each hold part of the count and neither would be the answer.
-- The same index serves the monthly sum, which is a range scan over one
-- account's days.
CREATE UNIQUE INDEX ux_ocr_usage_user_date ON ocr_usage (user_id, usage_date);