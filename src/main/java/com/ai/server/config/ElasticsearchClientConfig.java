package com.ai.server.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Elasticsearch 客户端配置
 * 自定义 RestClient，跳过 SSL 证书校验，适配 ES 8+/9+ 自签名证书。
 */
@Slf4j
@Configuration
public class ElasticsearchClientConfig {

    @Value("${spring.elasticsearch.uris:https://localhost:9200}")
    private String esUris;

    @Value("${spring.elasticsearch.username:elastic}")
    private String esUsername;

    @Value("${spring.elasticsearch.password:}")
    private String esPassword;

    @Bean
    @Primary
    public RestClient elasticsearchRestClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        RestClientBuilder builder = RestClient.builder(HttpHost.create(esUris));

        if (esUsername != null && !esUsername.isBlank() && esPassword != null && !esPassword.isBlank()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(esUsername, esPassword)
            );
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLContext(sslContext)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            );
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setSSLContext(sslContext)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            );
        }

        log.info("[Elasticsearch] 初始化 RestClient，连接地址: {} (SSL验证已关闭)", esUris);
        return builder.build();
    }

    /**
     * Spring AI VectorStore 需要的 Elasticsearch Java API Client
     */
    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
