package org.sav.fornas.bff.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 60*60*24*2, redisNamespace = "spring:session:bff")
@Slf4j
public class SecurityConfig {

	@Value("${app-props.url.iot-manager}")
	private String iotManagerBaseUrl;

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

		http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/api/**", "/user-info").authenticated()
						.anyExchange().permitAll()
				)
				.exceptionHandling(exceptions -> exceptions
						// 2. Встановлюємо 401 EntryPoint для захищених шляхів
						.authenticationEntryPoint(
								new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
						)
				)
				.oauth2Login(oauth2 -> oauth2
						// Передаємо репозиторій в customSuccessHandler
						.authenticationSuccessHandler(customSuccessHandler())
				)
//				.requestCache(cache -> cache.requestCache(requestCache))
				.oauth2Client(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ServerOAuth2AuthorizedClientRepository authorizedClientRepository
	) {
		// Менеджер для WebFlux
		ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
				ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
						.authorizationCode()
						.refreshToken()
						.build();

		DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
				new DefaultReactiveOAuth2AuthorizedClientManager(
						clientRegistrationRepository,
						authorizedClientRepository
				);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}






	private ServerAuthenticationSuccessHandler customSuccessHandler() {
		return (webFilterExchange, authentication) -> {
			var exchange = webFilterExchange.getExchange();
			var request = exchange.getRequest();
			var response = exchange.getResponse();

			String redirectUri = iotManagerBaseUrl;

			// зчитуємо cookie redirectAfterLogin
			var cookies = request.getCookies().get("bffRedirectAfterLogin");
			if (cookies != null && !cookies.isEmpty()) {
				try {
					var cookieValue = cookies.getFirst().getValue();
					redirectUri = new String(Base64.getDecoder().decode(cookieValue), StandardCharsets.UTF_8);
				} catch (Exception e) {
					log.warn("Invalid redirectAfterLogin cookie", e);
				}
			}

			// очищаємо cookie
			response.addCookie(ResponseCookie.from("bffRedirectAfterLogin", "")
					.path("/")
					.maxAge(0)
					.build()
			);

			response.setStatusCode(HttpStatus.FOUND);
			response.getHeaders().setLocation(URI.create(redirectUri));

			return response.setComplete();
		};
	}

}
