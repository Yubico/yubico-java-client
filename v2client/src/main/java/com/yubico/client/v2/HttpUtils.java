package com.yubico.client.v2;

import com.yubico.client.v2.exceptions.YubicoValidationException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtils {
    public static String toQueryString(Map<String, String> requestMap) throws UnsupportedEncodingException {
        String paramStr = "";
        for(Map.Entry<String,String> entry : requestMap.entrySet()) {
            if(!paramStr.isEmpty()) {
                paramStr += "&";
            }
            paramStr += entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
        }
        return paramStr;
    }


}
