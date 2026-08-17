-- Statutory and insurance documents, and the expiry dates the product exists to
-- remind about. Specification section 10.4.
--
-- No is_current column, deliberately: section 11 forbids one. Which record
-- covers a vehicle is a question about *today*, and a stored boolean would be a
-- second source of truth that goes stale at midnight without anybody touching
-- the row. Overlapping records are expected rather than prevented - section 11
-- describes how to choose between them - so there is no uniqueness constraint
-- on vehicle and type.

CREATE TABLE vehicle_documents (
    id               UUID         NOT NULL,
    vehicle_id       UUID         NOT NULL,

    -- RCA, CASCO, ITP, ROVINIETA. Held as text and validated by the Java enum
    -- rather than by a CHECK constraint: the set will grow, and a constraint
    -- would turn every new document type into a schema migration for no gain
    -- that the enum does not already give.
    type             VARCHAR(16)  NOT NULL,

    valid_from       DATE,
    valid_until      DATE         NOT NULL,
    provider         VARCHAR(160),
    reference_number VARCHAR(64),
    notes            TEXT,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_vehicle_documents PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_documents_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,

    -- Section 12 rejects a period that ends before it starts. Held here as well
    -- as in the service because it is an invariant of the data, not a rule about
    -- one request - a row that violates it is wrong however it arrived.
    CONSTRAINT ck_vehicle_documents_period
        CHECK (valid_from IS NULL OR valid_from <= valid_until)
);

-- Both shapes the application asks for: every document of one vehicle, and the
-- records of one type ordered by when they end.
CREATE INDEX ix_vehicle_documents_vehicle_type
    ON vehicle_documents (vehicle_id, type, valid_until DESC);