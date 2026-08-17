package ro.garajulmeu.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

import ro.garajulmeu.vehicledocument.DocumentStatus;
import ro.garajulmeu.vehicledocument.DocumentType;

/**
 * One type of document for one vehicle, as of the reader's today.
 *
 * <p>All four types are reported for every vehicle, including the ones nothing
 * has ever been entered for. Which of them a screen chooses to show - CASCO is
 * optional insurance and most people do not hold it - is the screen's decision,
 * the same way the garage sends both the nickname and the certificate and lets
 * the client pick a label.
 *
 * <p><strong>The status answers one question: are you covered today?</strong>
 * Everything nullable here exists to explain the answer rather than to change
 * it. A line with no {@code documentId} has never been configured; a line with
 * {@code upcomingFrom} set and no {@code validUntil} is a policy that has been
 * bought but has not started; one with both is a lapse with cover already
 * arranged.
 *
 * <p>Section 11 gives six statuses and none of them means "not started yet",
 * which is thinner than reality allows - see the decision of 2026-08-17. A
 * future-only record therefore reports EXPIRED, because on the only question the
 * dashboard asks the honest answer is no, and the two dates tell the screen
 * which sentence to write.
 */
public record DocumentStatusLine(
		DocumentType type,
		DocumentStatus status,
		UUID documentId,
		LocalDate validUntil,
		Long daysRemaining,
		LocalDate upcomingFrom) {
}