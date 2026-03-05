package com.notification.relay.api.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(apiInfo())
				.servers(servers());
	}

	private Info apiInfo() {
		return new Info()
				.title("Notification Relay API")
				.description("알림 중계 시스템 API 문서")
				.version("1.0.0");
	}

	private List<Server> servers() {
		return List.of(
				new Server()
						.url("http://localhost:8080")
						.description("Local Server")
		);
	}
}
