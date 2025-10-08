package org.sav.fornas.bff.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sav.fornas.bff.service.BffService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BffHandler {

	private final ReactiveOAuth2AuthorizedClientManager reactiveAuthorizedClientManager;
	private final WebClient webClient;
	private final BffService bffService;

	@Value("${app-props.url.iot-back}")
	private String resourceApiBaseUrl;

	/**
	 * Проксіює будь-який запит /api/** до Resource Server
	 */
	public Mono<ServerResponse> proxy(ServerRequest request) {
		String path = request.path().substring("/api/".length());
		String url = resourceApiBaseUrl + path;
		log.debug(">>> path:{}", path);
		log.debug(">>> url:{}", url);

		return request.principal()
				.cast(Authentication.class)
				.flatMap(auth -> {
					OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
							.withClientRegistrationId("iot-manager")
							.principal(auth)
							.build();

					return reactiveAuthorizedClientManager.authorize(authorizeRequest)
							.flatMap(client -> bffService.forwardRequest(request, url, client.getAccessToken().getTokenValue())
									.flatMap(forwardedResponse -> {
										// forwardedResponse містить статус і body
										return ServerResponse.status(forwardedResponse.status())
												.contentType(MediaType.APPLICATION_JSON)
												.bodyValue(forwardedResponse.body());
									})
									.onErrorResume(e -> ServerResponse.status(HttpStatus.BAD_GATEWAY)
											.bodyValue(Map.of("error", e.getMessage())))
							)
							.switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED)
									.bodyValue(Map.of("error", "Not authorized")));

				})
				.switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED)
						.bodyValue(Map.of("error", "User not authenticated")));

	}


	/**
	 * Повертає інформацію про поточного користувача після логіну
	 */
	public Mono<ServerResponse> userInfo(ServerRequest request) {
		return request.principal()
				.cast(Authentication.class)
				.flatMap(auth -> {
					if (auth.getPrincipal() instanceof OAuth2User user) {
						return ServerResponse.ok()
								.contentType(MediaType.APPLICATION_JSON)
								.bodyValue(user.getAttributes());
					}
					return ServerResponse.status(HttpStatus.UNAUTHORIZED)
							.bodyValue(Map.of("error", "User not authenticated"));
				});
	}
}
