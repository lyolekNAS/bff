package org.sav.fornas.bff.service;


import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@Service
public class BffService {

	private final WebClient webClient;

	public BffService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public Mono<String> forwardRequest(ServerRequest request, String backendUrl, String accessToken) {
		return webClient.method(request.method())
				.uri(backendUrl)
				.headers(headers -> {
					headers.addAll(request.headers().asHttpHeaders());
					headers.setBearerAuth(accessToken);
				})
				.retrieve()
				.bodyToMono(String.class);
	}
}
