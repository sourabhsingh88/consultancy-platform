package com.consultancy.platform.modules.auth;

import com.consultancy.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GoogleOAuthTokenVerifier implements OAuthTokenVerifier {
    private final RestClient googleClient = RestClient.create("https://oauth2.googleapis.com");
    private final RestClient facebookClient = RestClient.create("https://graph.facebook.com");
    private final String googleClientId;
    private final String appleClientId;
    private final String facebookAppId;
    private final String facebookAppSecret;
    private final NimbusJwtDecoder appleDecoder = NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys").build();

    public GoogleOAuthTokenVerifier(@Value("${app.oauth.google-client-id}") String googleClientId,
                                    @Value("${app.oauth.apple-client-id}") String appleClientId,
                                    @Value("${app.oauth.facebook-app-id}") String facebookAppId,
                                    @Value("${app.oauth.facebook-app-secret}") String facebookAppSecret) {
        this.googleClientId = googleClientId;
        this.appleClientId = appleClientId;
        this.facebookAppId = facebookAppId;
        this.facebookAppSecret = facebookAppSecret;
    }

    @Override
    public VerifiedOAuthUser verify(AuthDtos.Provider provider, String providerToken) {
        return switch (provider) {
            case GOOGLE -> verifyGoogle(providerToken);
            case APPLE -> verifyApple(providerToken);
            case FACEBOOK -> verifyFacebook(providerToken);
        };
    }

    private VerifiedOAuthUser verifyGoogle(String providerToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE_CLIENT_ID is not configured");
        }
        Map<?, ?> tokenInfo = googleClient.get()
                .uri(uri -> uri.path("/tokeninfo").queryParam("id_token", providerToken).build())
                .retrieve()
                .body(Map.class);
        if (tokenInfo == null || !googleClientId.equals(tokenInfo.get("aud"))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid Google token audience");
        }
        boolean emailVerified = Boolean.parseBoolean(String.valueOf(tokenInfo.get("email_verified")));
        if (!emailVerified) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }
        Object name = tokenInfo.get("name");
        Object picture = tokenInfo.get("picture");
        return new VerifiedOAuthUser(
                String.valueOf(tokenInfo.get("sub")),
                String.valueOf(tokenInfo.get("email")),
                true,
                name == null ? "User" : String.valueOf(name),
                picture == null ? "" : String.valueOf(picture)
        );
    }

    private VerifiedOAuthUser verifyApple(String providerToken) {
        if (appleClientId == null || appleClientId.isBlank()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "APPLE_CLIENT_ID is not configured");
        }
        Jwt jwt = appleDecoder.decode(providerToken);
        if (!"https://appleid.apple.com".equals(jwt.getIssuer().toString()) || !jwt.getAudience().contains(appleClientId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid Apple token");
        }
        String email = jwt.getClaimAsString("email");
        String sub = jwt.getSubject();
        return new VerifiedOAuthUser(sub, email, true, email == null ? "Apple User" : email, "");
    }

    private VerifiedOAuthUser verifyFacebook(String providerToken) {
        if (facebookAppId == null || facebookAppId.isBlank() || facebookAppSecret == null || facebookAppSecret.isBlank()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FACEBOOK_APP_ID and FACEBOOK_APP_SECRET are not configured");
        }
        String appToken = facebookAppId + "|" + facebookAppSecret;
        Map<?, ?> debug = facebookClient.get()
                .uri(uri -> uri.path("/debug_token").queryParam("input_token", providerToken).queryParam("access_token", appToken).build())
                .retrieve()
                .body(Map.class);
        Map<?, ?> data = debug == null ? null : (Map<?, ?>) debug.get("data");
        if (data == null || !Boolean.TRUE.equals(data.get("is_valid")) || !facebookAppId.equals(String.valueOf(data.get("app_id")))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid Facebook token");
        }
        Map<?, ?> profile = facebookClient.get()
                .uri(uri -> uri.path("/me").queryParam("fields", "id,name,email,picture").queryParam("access_token", providerToken).build())
                .retrieve()
                .body(Map.class);
        if (profile == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unable to load Facebook profile");
        }
        Object name = profile.get("name");
        return new VerifiedOAuthUser(String.valueOf(profile.get("id")), (String) profile.get("email"), profile.get("email") != null,
                name == null ? "Facebook User" : String.valueOf(name), "");
    }
}
