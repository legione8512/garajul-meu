package ro.garajulmeu.vehicledocument.dto;

import java.time.LocalDate;

/**
 * What adding and renewing have in common: a period and the details hanging off
 * it. The type is deliberately absent - it is the one thing the two differ on,
 * because a renewal takes it from the record it supersedes.
 *
 * <p>This exists so that section 12's rule about the pair of dates lives in one
 * method rather than two. A rule stated twice is a rule that can drift, and this
 * one decides whether a period is accepted at all.
 */
public interface DocumentPeriod {

	LocalDate validFrom();

	LocalDate validUntil();

	String provider();

	String referenceNumber();

	String notes();
}