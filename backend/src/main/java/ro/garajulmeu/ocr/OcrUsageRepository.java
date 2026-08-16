package ro.garajulmeu.ocr;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcrUsageRepository extends JpaRepository<OcrUsage, UUID> {

	/**
	 * Makes today's row exist without racing anybody else who is doing the same.
	 *
	 * <p>Two simultaneous first requests of the day would otherwise both find no
	 * row and both insert one, and the second would fail on the unique index -
	 * turning a perfectly ordinary request into an internal error. The database
	 * resolves it instead, and doing nothing on conflict is exactly right: the
	 * row only has to be there, not to be ours.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into ocr_usage (id, user_id, usage_date, request_count)
			values (gen_random_uuid(), :userId, :day, 0)
			on conflict (user_id, usage_date) do nothing
			""", nativeQuery = true)
	void ensureRowFor(@Param("userId") UUID userId, @Param("day") LocalDate day);

	/**
	 * Locked, so two requests arriving together cannot both read the same count
	 * and both write count + 1 - which is how an allowance of ten quietly becomes
	 * eleven. Contention is only ever with the same account.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<OcrUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

	/** The month is the sum of its days, so there is no monthly row to keep in step. */
	@Query("""
			select coalesce(sum(usage.requestCount), 0) from OcrUsage usage
			where usage.userId = :userId and usage.usageDate between :from and :to
			""")
	long totalBetween(@Param("userId") UUID userId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);
}