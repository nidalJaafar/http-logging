package com.tokennetwork.logging;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Getter
@Setter
public class LoggingFilterConfig {

    private Predicate<String> requestHeaderMaskPredicate = h -> false;
    private Predicate<String> responseHeaderMaskPredicate = h -> false;
    private Predicate<String> requestBodyFieldMaskPredicate = k -> false;
    private Predicate<String> responseBodyFieldMaskPredicate = k -> false;

    private final List<String> requestBodyJsonPathsToMask = new ArrayList<>();
    private final List<String> responseBodyJsonPathsToMask = new ArrayList<>();

    public LoggingFilterConfig maskRequestHeaders(Predicate<String> predicate) {
        this.requestHeaderMaskPredicate = this.requestHeaderMaskPredicate.or(predicate);
        return this;
    }

    public LoggingFilterConfig maskResponseHeaders(Predicate<String> predicate) {
        this.responseHeaderMaskPredicate = this.responseHeaderMaskPredicate.or(predicate);
        return this;
    }

    public LoggingFilterConfig maskRequestBodyFields(Predicate<String> predicate) {
        this.requestBodyFieldMaskPredicate = this.requestBodyFieldMaskPredicate.or(predicate);
        return this;
    }

    public LoggingFilterConfig maskResponseBodyFields(Predicate<String> predicate) {
        this.responseBodyFieldMaskPredicate = this.responseBodyFieldMaskPredicate.or(predicate);
        return this;
    }

    public LoggingFilterConfig maskRequestBodyJsonPath(String jsonPath) {
        this.requestBodyJsonPathsToMask.add(jsonPath);
        return this;
    }

    public LoggingFilterConfig maskResponseBodyJsonPath(String jsonPath) {
        this.responseBodyJsonPathsToMask.add(jsonPath);
        return this;
    }
}
