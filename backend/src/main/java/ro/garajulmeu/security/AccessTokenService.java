package ro.garajulmeu.security;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues the short-lived access token.
 *
 * <p>The token carries the account identifier and nothing else. No email, no
 * name: a JWT is only base64, readable by anyone who holds it, and it is stored
 * on the client. Specification section 26 asks for minimised personal data
 * everywhere, and a token is one of the easiest places to leak it by accident.
 */
@Service
public class AccessTokenService {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	AccessTokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public IssuedAccessToken issueFor(UUID accountId) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(properties.accessTokenValidity());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(accountId.toString())
				.build();

		String value = jwtEncoder
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();

		return new IssuedAccessToken(value, expiresAt);
	}

	public record IssuedAccessToken(String value, Instant expiresAt) {
	}
}