package org.sav.fornas.bff.service;


import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@Service
public class BffService {

	private final WebClient webClient;

	public BffService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public Mono<ForwardedResponse> forwardRequest(ServerRequest request, String backendUrl, String accessToken) {
		return request.bodyToMono(byte[].class)
				.defaultIfEmpty(new byte[0])
				.flatMap(body -> webClient.method(request.method())
						.uri(backendUrl)
						.headers(headers -> {
							headers.addAll(request.headers().asHttpHeaders());
							headers.setBearerAuth(accessToken);
						})
						.body(BodyInserters.fromValue(body))
						.exchangeToMono(response ->
								response.bodyToMono(String.class)
										.map(responseBody -> new ForwardedResponse(response.statusCode(), responseBody))
						)
				);
	}

	public record ForwardedResponse(HttpStatusCode status, String body) {}
}
