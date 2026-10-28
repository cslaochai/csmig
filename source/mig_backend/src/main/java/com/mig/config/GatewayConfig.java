package com.mig.config;

import com.mig.service.GatewayForwardService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayConfig {

    @Bean
    @ConfigurationProperties(prefix = "mig.upstream")
    public UpstreamProps upstreamProps() {
        return new UpstreamProps();
    }

    @Bean("upstreamRestClient")
    public RestClient upstreamRestClient(UpstreamProps props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        return RestClient.builder().baseUrl(props.getBaseUrl()).requestFactory(factory).build();
    }

    @Bean
    public GatewayForwardService gatewayForwardService(RestClient upstreamRestClient,
                                                       com.mig.mapper.GatewayLogMapper logMapper,
                                                       UpstreamProps props,
                                                       com.mig.config.CryptoProperties cryptoProps) {
        return new GatewayForwardService(upstreamRestClient, logMapper, props, cryptoProps);
    }

    public static class UpstreamProps {
        private String baseUrl;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
}
