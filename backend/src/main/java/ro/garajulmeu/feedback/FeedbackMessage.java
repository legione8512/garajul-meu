package ro.garajulmeu.feedback;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/** One message from the Sugestii tab. V14. Written once and never changed. */
@Entity
@Table(name = "feedback_messages")
public class FeedbackMessage {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 16, updatable = false)
	private FeedbackCategory category;

	@Column(name = "message", nullable = false, length = 2000, updatable = false)
	private String message;

	@Column(name = "platform", length = 16, updatable = false)
	private String platform;

	@Column(name = "app_version", length = 32, updatable = false)
	private String appVersion;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected FeedbackMessage() {
		// Required by JPA.
	}

	public FeedbackMessage(UUID userId, FeedbackCategory category, String message, String platform,
			String appVersion) {
		this.userId = userId;
		this.category = category;
		this.message = message;
		this.platform = platform;
		this.appVersion = appVersion;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public FeedbackCategory getCategory() {
		return category;
	}

	public String getMessage() {
		return message;
	}

	public String getPlatform() {
		return platform;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FeedbackMessage feedback)) {
			return false;
		}
		return id != null && id.equals(feedback.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
