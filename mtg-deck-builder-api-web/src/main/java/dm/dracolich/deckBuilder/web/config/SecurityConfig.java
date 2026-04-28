package dm.dracolich.deckBuilder.web.config;

import dm.dracolich.forge.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SecurityConfig {
    @Bean
    public JwtAuthenticationWebFilter jwtFilter(JwtTokenValidator v) { return new JwtAuthenticationWebFilter(v); }

    @Bean
    public AnonCookieSigner anonCookieSigner(@Value("${dracolich.cookie.secret}") String s) { return new AnonCookieSigner(s); }

    @Bean
    public AnonCookieFilter anonCookieFilter(AnonCookieSigner s,
                                             @Value("${dracolich.cookie.lifetime}") Duration d,
                                             @Value("${dracolich.cookie.secure}") boolean secure) {
        return new AnonCookieFilter(s, d, secure);
    }

    @Bean
    public JwtTokenValidator jwtTokenValidator(@Value("${dracolich.jwt.public-key}") Resource pem) {
        return EcPublicKeyJwtValidator.fromResource(pem);
    }
}
