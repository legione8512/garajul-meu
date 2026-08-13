package ro.garajulmeu.auth.dto;

/** Mirrors the transport of the request: {@code refreshToken} is null when the caller used the cookie. */
public record RefreshResponse(String accessToken, long expiresInSeconds, String refreshToken) {
}