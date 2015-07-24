package com.yubico.client.v2;

import com.yubico.client.v2.impl.ResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static rx.util.async.Async.fromCallable;

public class VerificationRequester {

    private static final Logger log = LoggerFactory.getLogger(VerificationRequester.class);

    public Optional<VerificationResponse> fetch(Collection<String> urls, String userAgent) {

        List<Observable<Optional<VerificationResponse>>> responses = urls.stream()
                .map(url -> request(url, userAgent))
                .collect(toList());

        return Observable.merge(responses)
                .filter(Optional::isPresent)
                .filter(r -> !r.get().isReplayed())
                .toBlocking()
                .firstOrDefault(Optional.empty());
    }

    private static Observable<Optional<VerificationResponse>> request(String urlString, String userAgent) {
        return fromCallable(() -> {
            URL url = new URL(urlString);
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                VerificationResponse value = new ResponseParser().parse(conn.getInputStream());
                return Optional.of(value);
            } catch (Exception e) {
                log.warn("Exception when requesting {}: {}", url.getHost(), e.getMessage());
                return Optional.empty();
            }
        });
    }
}
