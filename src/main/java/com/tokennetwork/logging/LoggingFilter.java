package com.tokennetwork.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final LoggingFilterConfig config;
    private final ObjectMapper mapper;
    private static final String MASKED_STRING = "***masked***";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        byte[] requestBodyBytes = request.getInputStream().readAllBytes();
        HttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, requestBodyBytes);

        try {
            logRequest(wrappedRequest, requestBodyBytes);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request, byte[] bodyBytes) {
        HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
        maskHeaders(headers, config.getRequestHeaderMaskPredicate());

        try {
            String body = new String(bodyBytes, getCharsetOrDefault(request.getCharacterEncoding()));

            if (isJson(request.getContentType())) {
                body = maskJsonFields(body, config.getRequestBodyFieldMaskPredicate(), config.getRequestBodyJsonPathsToMask());
            }

            log.info("Received Request: {} {}", request.getMethod(), request.getRequestURL());
            log.info("Request Headers: {}", headers);
            log.info("Request Body: {}", body);
        } catch (Exception e) {
            log.error("An error occurred while logging request", e);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response) throws IOException {
        try (ServletServerHttpResponse servletServerHttpResponse = new ServletServerHttpResponse(response)) {
            HttpHeaders headers = servletServerHttpResponse.getHeaders();
            maskHeaders(headers, config.getResponseHeaderMaskPredicate());

            String body = new String(response.getContentAsByteArray(), getCharsetOrDefault(response.getCharacterEncoding()));

            if (isJson(response.getContentType())) {
                body = maskJsonFields(body, config.getResponseBodyFieldMaskPredicate(), config.getResponseBodyJsonPathsToMask());
            }

            log.info("Response Status: {}", response.getStatus());
            log.info("Response Headers: {}", headers);
            log.info("Response Body: {}", body);
        }
    }

    private void maskHeaders(HttpHeaders headers, Predicate<String> predicate) {
        headers.forEach((key, value) -> {
            if (predicate.test(key)) {
                headers.set(key, MASKED_STRING);
            }
        });
    }

    private String maskJsonFields(String json, Predicate<String> fieldPredicate, Iterable<String> jsonPaths) {
        try {
            JsonNode root = mapper.readTree(json);

            if (root.isObject()) {
                ObjectNode obj = (ObjectNode) root;
                obj.fieldNames().forEachRemaining(f -> {
                    if (fieldPredicate.test(f)) {
                        obj.put(f, MASKED_STRING);
                    }
                });
            }

            Object document = Configuration.defaultConfiguration().jsonProvider().parse(root.toString());
            for (String path : jsonPaths) {
                parseJsonPath(path, document);
            }

            return Configuration.defaultConfiguration().jsonProvider().toJson(document);
        } catch (Exception e) {
            log.warn("Failed to parse body as JSON: {}", e.getMessage());
            return json;
        }
    }

    private static void parseJsonPath(String path, Object document) {
        try {
            JsonPath.parse(document).set(path, MASKED_STRING);
        } catch (Exception e) {
            log.warn("Failed to apply JSONPath {}: {}", path, e.getMessage());
        }
    }

    private String getCharsetOrDefault(String encoding) {
        return encoding != null ? encoding : StandardCharsets.UTF_8.name();
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}
