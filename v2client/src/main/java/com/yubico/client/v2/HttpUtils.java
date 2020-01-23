package com.yubico.client.v2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {

    public static String toQueryString(Map<String, String> requestMap) throws UnsupportedEncodingException {
        StringBuilder paramStr = new StringBuilder();
        for(Map.Entry<String,String> entry : requestMap.entrySet()) {
            if(paramStr.length() > 0) {
                paramStr.append("&");
            }
            paramStr
                .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                .append("=")
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
        }
        return paramStr.toString();
    }

}
