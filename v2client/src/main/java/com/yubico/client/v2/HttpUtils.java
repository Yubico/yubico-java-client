package com.yubico.client.v2;

import java.util.Map;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static java.util.stream.Collectors.joining;

public class HttpUtils {
    public static String toQueryString(Map<String, String> requestMap) {
        return requestMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + urlFormParameterEscaper().escape(e.getValue()))
                .collect(joining("&"));
    }
}
