-- Application accounts. Specification section 10.1.
--
-- Flyway owns this schema. Hibernate runs with ddl-auto=validate and may only
-- check that entity mappings match these columns; it never alters them.

CREATE TABLE users (
    id                 UUID         NOT NULL,
    full_name          VARCHAR(120) NOT NULL,
    email              VARCHAR(320) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    email_verified_at  TIMESTAMPTZ,
    preferred_language VARCHAR(5)   NOT NULL DEFAULT 'ro',
    timezone           VARCHAR(64)  NOT NULL DEFAULT 'Europe/Bucharest',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_preferred_language CHECK (preferred_language IN ('ro', 'en'))
);

-- The application stores the address already normalised, so a plain unique
-- index is enough to enforce the global uniqueness required by section 9.
CREATE UNIQUE INDEX ux_users_email ON users (email);
