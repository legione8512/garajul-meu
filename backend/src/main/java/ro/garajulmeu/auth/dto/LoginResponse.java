package ro.garajulmeu.auth.dto;

/**
 * @param expiresInSeconds relative rather than an absolute timestamp, so a
 *                         device with a wrong clock still refreshes on time
 * @param refreshToken     null unless the caller asked for it. The web client
 *                         receives it as an HttpOnly cookie instead.
 */
public record LoginResponse(String accessToken, long expiresInSeconds, String refreshToken) {
}