package com.consultancy.platform.modules.auth;

public interface OAuthTokenVerifier {
    VerifiedOAuthUser verify(AuthDtos.Provider provider, String providerToken);

    record VerifiedOAuthUser(String providerSubject, String email, boolean emailVerified, String displayName, String avatarUrl) {
    }
}
