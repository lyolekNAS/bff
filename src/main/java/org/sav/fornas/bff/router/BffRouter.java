package org.sav.fornas.bff.router;

import org.sav.fornas.bff.handler.BffHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.*;

@Configuration
public class BffRouter {

	@Bean
	public RouterFunction<ServerResponse> apiProxyRouter(BffHandler handler) {
		return RouterFunctions.route()
				.GET("/user-info", handler::userInfo)
				.path("/api", builder -> builder
						.GET(RequestPredicates.all(), handler::proxy)
						.POST(RequestPredicates.all(), handler::proxy)
						.PUT(RequestPredicates.all(), handler::proxy)
						.DELETE(RequestPredicates.all(), handler::proxy)
				).build();
	}
}
