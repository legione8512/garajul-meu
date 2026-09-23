-- Recurring payments on a vehicle: the car-loan or leasing instalment, and the
-- CASCO instalment. Added in 1.1 at the owner's request; the design is
-- docs/DESIGN_1_1_INSTALMENTS.md.
--
-- Dates only, by the owner's decision: no amount is stored, so nothing here
-- adds up to what somebody owes.
--
-- last_due_date is stored even when the person gave a number of instalments:
-- the count is turned into a date once, at save, so exactly one rule decides
-- where a series ends. Kind and frequency are text rather than CHECK
-- constraints, for the reason V6 gives for document types.

CREATE TABLE vehicle_payments (
    id                 UUID        NOT NULL,
    vehicle_id         UUID        NOT NULL,

    -- LOAN / CASCO.
    kind               VARCHAR(16) NOT NULL,

    -- The first instalment. Its day of the month anchors every later one.
    first_due_date     DATE        NOT NULL,

    -- MONTHLY / QUARTERLY / SEMIANNUAL / ANNUAL.
    frequency          VARCHAR(16) NOT NULL,

    last_due_date      DATE        NOT NULL,

    -- Days before each instalment to remind, a subset of 7, 3, 1 and 0,
    -- written largest first. Per payment, by the owner's decision: the
    -- account's own offsets on screen 18 stay the documents'.
    remind_days_before VARCHAR(16) NOT NULL DEFAULT '3,1',

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_vehicle_payments PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_payments_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_payments_period CHECK (last_due_date >= first_due_date)
);

CREATE INDEX ix_vehicle_payments_vehicle ON vehicle_payments (vehicle_id);

-- A reminder is now about a document or about one instalment of a payment,
-- never both and never neither. The scheduler, the dispatcher and the delivery
-- rows deal in reminder rows and are indifferent to which.
ALTER TABLE reminders
    ALTER COLUMN vehicle_document_id DROP NOT NULL;

ALTER TABLE reminders
    ADD COLUMN vehicle_payment_id UUID
        CONSTRAINT fk_reminders_payment REFERENCES vehicle_payments (id) ON DELETE CASCADE;

-- Which instalment a payment reminder is about. Null for a document's.
ALTER TABLE reminders
    ADD COLUMN due_date DATE;

ALTER TABLE reminders
    ADD CONSTRAINT ck_reminders_one_subject CHECK (
        (vehicle_document_id IS NOT NULL AND vehicle_payment_id IS NULL AND due_date IS NULL)
        OR (vehicle_document_id IS NULL AND vehicle_payment_id IS NOT NULL AND due_date IS NOT NULL));

CREATE INDEX ix_reminders_payment ON reminders (vehicle_payment_id, status);
