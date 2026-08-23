-- Push tokens become ciphertext, and gain a blind index. Specification section
-- 10.7: "user_devices must retain a retrievable token value (for example
-- encrypted at application/infrastructure level), not a one-way hash only."
--
-- Encryption takes two things away that this table depended on. An
-- authenticated cipher writes different bytes every time, so a row can no
-- longer be found by its token, and a unique index on the token can never
-- collide - which would let one handset register a thousand times. The hash
-- column restores both.

-- Existing rows are discarded rather than migrated, and that is safe for a
-- reason particular to this column: no native application exists yet, so every
-- row here was written by a test or by hand. Even in production it would be
-- safe - an FCM registration token is re-obtained by the client on every launch,
-- so a deleted row reappears the next time the app opens. The delete cascades
-- into notification_deliveries by the foreign key declared in V10, which takes
-- the delivery history of devices that no longer exist with it.
DELETE FROM user_devices;

-- VARCHAR, not CHAR, and the distinction is load-bearing rather than stylistic.
-- A SHA-256 hex string is always exactly sixty-four characters, so CHAR(64)
-- reads as the more precise choice - but PostgreSQL implements it as `bpchar`,
-- which pads on write and strips trailing blanks on comparison, and which
-- Hibernate reports as a type mismatch against a plain String field. With
-- `ddl-auto: validate` that is not a warning: the SessionFactory refuses to
-- build and the application does not start at all.
--
-- NOT NULL with no default, which is only possible because of the delete above.
ALTER TABLE user_devices
    ADD COLUMN push_token_hash VARCHAR(64) NOT NULL;

-- The uniqueness moves with the searchability. Dropping this before creating
-- the replacement matters: a unique index on ciphertext would silently permit
-- exactly the duplicates it looks like it prevents.
DROP INDEX ux_user_devices_token;

CREATE UNIQUE INDEX ux_user_devices_token_hash ON user_devices (push_token_hash);