package ro.garajulmeu.feedback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackMessage, UUID> {

	/** The daily allowance: how many this account has sent since a moment. */
	long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

	List<FeedbackMessage> findByUserIdOrderByCreatedAt(UUID userId);
}
