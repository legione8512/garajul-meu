-- The vehicle and its registration certificate. Specification sections 10.2 and
-- 10.3, under the identity rules in section 9.
--
-- The two tables arrive together because a vehicle has no identity of its own.
-- Section 9 makes the certificate the source of truth for registration number,
-- VIN, make and commercial description, and leaves the vehicle holding only the
-- owner, an optional nickname and image metadata - so a vehicle without a
-- certificate would be a row nobody could name.

CREATE TABLE vehicles (
    id                 UUID         NOT NULL,
    user_id            UUID         NOT NULL,
    display_name       VARCHAR(120),
    image_object_key   VARCHAR(255),
    image_content_type VARCHAR(100),
    image_size_bytes   BIGINT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Every garage listing is "all vehicles of one account", on every load.
CREATE INDEX ix_vehicles_user ON vehicles (user_id);

CREATE TABLE registration_certificates (
    id                          UUID          NOT NULL,
    vehicle_id                  UUID          NOT NULL,

    -- The owner, denormalised from vehicles. Section 9 requires that one account
    -- cannot hold the same VIN twice while different accounts may, and that
    -- constraint spans two tables - which PostgreSQL cannot index. Carrying the
    -- owner here makes the database the arbiter instead of a service check that
    -- two simultaneous requests can both pass. It cannot drift: V1 implements no
    -- ownership transfer, so this value never changes for a given row. Note that
    -- the identity fields stay in one place; only the owner is duplicated.
    user_id                     UUID          NOT NULL,

    registration_number         VARCHAR(32)   NOT NULL,   -- A
    first_registration_date     DATE,                     -- B
    vehicle_category            VARCHAR(16),              -- J
    make                        VARCHAR(64)   NOT NULL,   -- D.1
    type_variant_version        VARCHAR(128),             -- D.2
    commercial_description      VARCHAR(128)  NOT NULL,   -- D.3
    vin                         VARCHAR(32)   NOT NULL,   -- E
    type_approval_number        VARCHAR(64),              -- K
    validity_period             VARCHAR(64),              -- H
    registration_date           DATE,                     -- I
    certificate_issue_date      DATE,                     -- I.1
    maximum_permissible_mass_kg INTEGER,                  -- F.1
    vehicle_mass_kg             INTEGER,                  -- G
    engine_capacity_cc          INTEGER,                  -- P.1
    maximum_power_kw            DECIMAL(8, 2),            -- P.2
    fuel_type                   VARCHAR(32),              -- P.3
    power_weight_ratio          DECIMAL(8, 3),            -- Q
    colour                      VARCHAR(64),              -- R
    seats                       INTEGER,                  -- S.1
    standing_places             INTEGER,                  -- S.2
    civ_number                  VARCHAR(64),              -- Y
    issuing_authority           VARCHAR(128),             -- Z
    observations                TEXT,
    certificate_number          VARCHAR(64),

    -- C.2 and C.3. Sensitive and optional throughout: section 24 keeps personal
    -- data to what the product actually needs, and none of these are required to
    -- track an expiry date.
    owner_name_or_company       VARCHAR(160),             -- C.2.1
    owner_first_name            VARCHAR(80),              -- C.2.2
    owner_address               VARCHAR(255),             -- C.2.3
    c2_equals_c1                BOOLEAN,
    user_name_or_company        VARCHAR(160),             -- C.3.1
    user_first_name             VARCHAR(80),              -- C.3.2
    user_address                VARCHAR(255),             -- C.3.3
    c3_equals_c1                BOOLEAN,

    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_registration_certificates PRIMARY KEY (id),
    CONSTRAINT fk_registration_certificates_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT fk_registration_certificates_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- One certificate per vehicle, enforced rather than assumed.
CREATE UNIQUE INDEX ux_registration_certificates_vehicle
    ON registration_certificates (vehicle_id);

-- Section 9: the same VIN twice in one account is refused; across accounts it is
-- allowed, because V1 has no shared or transferred ownership.
CREATE UNIQUE INDEX ux_registration_certificates_user_vin
    ON registration_certificates (user_id, vin);