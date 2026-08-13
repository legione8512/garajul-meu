package ro.garajulmeu.auth.dto;

/**
 * The body of POST /api/v1/auth/refresh and /logout, for clients that hold the
 * token themselves. Web clients send no body at all and rely on the cookie.
 */
public record RefreshRequest(String refreshToken) {
}