package com.notification.relay.worker.config;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

	private final SenderProperties senderProperties;

	@Bean
	public RestClient restClient() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(Timeout.of(senderProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS))
				.setResponseTimeout(Timeout.of(senderProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
				.build();

		HttpClientConnectionManager connectionManager =
				PoolingHttpClientConnectionManagerBuilder.create().build();

		HttpComponentsClientHttpRequestFactory factory =
				new HttpComponentsClientHttpRequestFactory(
						HttpClients.custom()
								.setDefaultRequestConfig(requestConfig)
								.setConnectionManager(connectionManager)
								.build()
				);

		return RestClient.builder()
				.requestFactory(factory)
				.build();
	}
}
