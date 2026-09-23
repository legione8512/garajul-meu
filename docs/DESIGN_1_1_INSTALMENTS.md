# 1.1 — Instalment reminders: design

Drafted 2026-09-23 from the developer's answers. **Nothing here is built yet.**
It is the design to agree before any code, as the deferred-work row in
`PROJECT_STATE.md` asks.

## What the developer decided (2026-09-23)

| Question | Decision |
|---|---|
| Which payments | **Two kinds:** the car-loan or leasing instalment, and the CASCO instalment |
| Amounts | **Not stored**, at least not yet. Only dates |
| Frequency | **Chosen per payment:** monthly, quarterly, every six months, yearly |
| End | **Required:** the date of the last instalment, or the number of instalments. The reminders stop on their own after the last one |
| Reminder lead | **Chosen per payment.** If nothing is chosen, **3 days and 1 day before** |

## What the person sees

On the vehicle's own screen, a new section **"Rate"** ("Instalments"), below the
documents:

- **Add** opens a short form:
  - **type**: *Leasing / credit auto* or *CASCO*;
  - **first instalment**: a date. Its day of the month is the anchor for every later instalment;
  - **how often**: *lunar*, *trimestrial*, *semestrial*, *anual*;
  - **until**: either *the last instalment's date* or *the number of instalments*, one switch between the two;
  - **remind me**: checkboxes *7 zile*, *3 zile*, *1 zi*, *în ziua scadenței*, with **3 and 1 ticked** by default.
- The list shows, for each payment: its type, the **next due date** in words
  ("Următoarea rată: 15 octombrie"), and **"rata 4 din 36"**.
  After the last one it reads "Toate ratele au trecut" and sends nothing more.
- Edit and delete, like a document. Deleting the vehicle deletes its payments.

**On *Acasă*** (decided by the developer, 2026-09-23): an instalment due **within the next
7 days**, today included, appears on its vehicle's card below the document lines, in the same
shape: *"Rată leasing — peste 3 zile (15 octombrie)."*, *"Rată CASCO — mâine."*, *"… — azi."*
Further off, it stays on the vehicle's screen only, so the card does not grow with every contract.
`DashboardVehicle` gains a list of these, computed in `DashboardService` with the documents; an
application built before 1.1 ignores a field it does not know, so the iOS and Android versions
already installed keep working unchanged.

**The reminder text** carries no amount and no account detail:
*"Rata la leasing pentru B 123 ABC este peste 3 zile (15 octombrie)."*
The Romanian plural rule of 1.0.1 (`countForm` / `ReminderMessage.needsDe`) applies,
and "mâine" / "azi" for 1 and 0.

## Rules that need an answer in code

1. **Day-of-month clamping.** An instalment anchored on the 31st falls on the last day of shorter
   months, and returns to the 31st when the month allows: 31 January → 28/29 February → 31 March.
   The anchor is the first instalment's day, never the previous clamped one, or a
   31st would drift to the 28th for good after February.
2. **End by count.** "36 instalments" is converted once, at save, into the last due date,
   so only one rule decides where the series ends. The form may show either.
3. **A ceiling.** At most **120 instalments**. Ten years of monthly payments covers every
   Romanian car loan and leasing contract, and it bounds the rows one payment can create.
4. **Past instalments.** A series whose first date is in the past is accepted. Reminders are made
   only for instalments still ahead, the same "only when that instant is still in the future"
   rule the documents follow.
5. **The account-level switch** (*Trimite-mi notificări*) and the notification time and zone
   apply to instalments as they do to documents. **The per-offset switches on screen 18**
   (30 / 14 / 7 / 3 / 1 / 0 days) **stay for documents only**: an instalment carries its
   own choice, which is what the developer asked for.

## Data

**V13** adds one table:

```sql
CREATE TABLE vehicle_payments (
    id                 UUID        NOT NULL,
    vehicle_id         UUID        NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    kind               VARCHAR(16) NOT NULL,   -- LOAN / CASCO
    first_due_date     DATE        NOT NULL,
    frequency          VARCHAR(16) NOT NULL,   -- MONTHLY / QUARTERLY / SEMIANNUAL / ANNUAL
    last_due_date      DATE        NOT NULL,   -- from the date or the count, decided at save
    remind_days_before VARCHAR(16) NOT NULL DEFAULT '3,1',  -- a subset of 7, 3, 1, 0
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_vehicle_payments PRIMARY KEY (id)
);
```

and widens `reminders` so that a row belongs to **either** a document **or** one instalment
of a payment:

- `vehicle_document_id` becomes nullable;
- new `vehicle_payment_id UUID REFERENCES vehicle_payments (id) ON DELETE CASCADE` and
  `due_date DATE` (which instalment it is about);
- `CHECK` that exactly one of the two parents is set.

The scheduler, the dispatcher, `notification_deliveries`, retries and the dead-token path are
unchanged: they deal in reminder rows and never ask what a row is about. Only the generation
and the message are new.

**Generation: all at once, at save.** Saving or editing a payment cancels its PENDING
reminders and generates the whole series, as `POST` and `PATCH` already do for documents.
At the ceiling of 120 instalments with all four offsets ticked, that is 480 rows, which is
small. The alternative, a rolling window topped up by a daily job, is more moving parts for
no gain at this size.

## Endpoints

Under the vehicle, as documents are:

- `GET    /api/v1/vehicles/{vehicleId}/payments`
- `POST   /api/v1/vehicles/{vehicleId}/payments`
- `PATCH  /api/v1/vehicles/{vehicleId}/payments/{paymentId}`
- `DELETE /api/v1/vehicles/{vehicleId}/payments/{paymentId}`

Each answers the next due date, the index of that instalment and the total, so the screen does no
date arithmetic of its own.

## Privacy and the stores — to check, not assume

No amount is stored. **But the fact that somebody has a car loan and when it is due may still be
"Other financial info"** in Google Play's Data safety form, and a comparable category in
Apple's App Privacy. The safer course is to declare it (collected, optional, not shared, deletable)
and add one sentence to the privacy policy. That is a form and a sentence, not a reason to change
the design. Decide it with the developer before 1.1 is submitted.

## Not in this version

- amounts, and anything that adds them up;
- marking an instalment as paid;
- linking a CASCO instalment to the CASCO policy's expiry (a possible convenience later:
  the policy's `valid_until` as the default last date);

## Rough size

Backend: V13, entity, repository, service with the clamping and the series, controller,
reminder generation and message, and tests for the 31st, the count, the ceiling and
cancellation on edit. Frontend: the section, the form, both locales and tests.
Then a release on both stores, **1.1.0**, and iOS needs the Mac again.
