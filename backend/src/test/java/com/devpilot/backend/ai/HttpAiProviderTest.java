package com.devpilot.backend.ai;

import com.devpilot.backend.exception.AiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpAiProviderTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private HttpAiProvider httpAiProvider;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://api.openai.com/v1");
        properties.setKey("test-key");
        properties.setModel("gpt-4o");

        httpAiProvider = new HttpAiProvider(webClientBuilder, properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockWebClientPost(Mono<?> responseMono) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        if (responseMono != null) {
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(responseMono);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGenerateResponseAuthenticationFailure() {
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized", HttpHeaders.EMPTY, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(ex);

        AiException aiEx = assertThrows(AiException.class, () -> httpAiProvider.generateResponse("prompt"));
        assertEquals(HttpStatus.UNAUTHORIZED, aiEx.getStatus());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGenerateResponseRateLimit() {
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", HttpHeaders.EMPTY, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(ex);

        AiException aiEx = assertThrows(AiException.class, () -> httpAiProvider.generateResponse("prompt"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, aiEx.getStatus());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGenerateResponseProvider5xx() {
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", HttpHeaders.EMPTY, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(ex);

        AiException aiEx = assertThrows(AiException.class, () -> httpAiProvider.generateResponse("prompt"));
        assertEquals(HttpStatus.BAD_GATEWAY, aiEx.getStatus());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGenerateResponseNetworkFailure() {
        WebClientRequestException ex = new WebClientRequestException(new RuntimeException("Connection refused"), org.springframework.http.HttpMethod.POST, java.net.URI.create("http://localhost"), new HttpHeaders());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri((Function) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(ex);

        AiException aiEx = assertThrows(AiException.class, () -> httpAiProvider.generateResponse("prompt"));
        assertEquals(HttpStatus.BAD_GATEWAY, aiEx.getStatus());
        assertEquals("Network error while communicating with AI provider.", aiEx.getMessage());
    }

    @Test
    void testGenerateResponseMalformedResponse() {
        // Return null to simulate empty or unparseable response body
        mockWebClientPost(Mono.empty());

        AiException aiEx = assertThrows(AiException.class, () -> httpAiProvider.generateResponse("prompt"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, aiEx.getStatus());
        assertEquals("Received malformed or empty response from AI provider.", aiEx.getMessage());
    }
}
