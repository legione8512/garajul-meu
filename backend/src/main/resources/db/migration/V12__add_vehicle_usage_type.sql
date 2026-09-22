-- How a vehicle is used: NORMAL, TAXI or TRANSPORT. Added in 1.0.2 at the
-- owner's request, as plain information that later features - the ITP
-- interval first - may read. See VehicleUsage.
--
-- On the vehicle, not the certificate: the registration certificate does not
-- state it, so it is something the owner declares, like the nickname beside it.
--
-- NOT NULL with a default, so every vehicle that already exists becomes NORMAL
-- in the same statement that adds the column - which is what nearly every car
-- is - and no row is ever left in a state the application would have to
-- interpret. Text rather than a CHECK constraint, for the reason V6 gives for
-- document types: a fourth use should be a constant, not a migration.

ALTER TABLE vehicles
    ADD COLUMN usage_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL';
