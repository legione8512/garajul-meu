package ro.garajulmeu.auth.dto;

/**
 * @param expiresInSeconds relative rather than an absolute timestamp. A phone
 *                         with a wrong clock would misjudge an absolute expiry
 *                         and either refresh constantly or too late; a duration
 *                         is immune to clock skew. This is also what OAuth2 uses.
 */
public record LoginResponse(String accessToken, long expiresInSeconds) {
}