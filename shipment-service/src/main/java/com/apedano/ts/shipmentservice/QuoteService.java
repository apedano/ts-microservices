package com.apedano.ts.shipmentservice;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Service
public class QuoteService {

    private static final String DEFAULT_QUOTE = "Addhinucchiuni, cugghiennu cuttuni, essennu cu tia, cuttuni cugghia.";
    private static final String QUOTE_API_URI = "https://quotes15.p.rapidapi.com/quotes/random/";
    private static final String API_HOST_HEADER = "x-rapidapi-host";
    private static final String API_HOST = "quotes15.p.rapidapi.com";
    private static final String USE_QUERY_STRING_HEADER = "useQueryString";
    private static final String USE_QUERY_STRING = "true";
    private static final String API_KEY_HEADER = "x-rapidapi-key";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();


    @Value("${QUOTE_RAPIDAPI_KEY}")
    private String rapidApiKey;

    public String getRandomQuote(String languageCode) {
        try {
            return Optional.of(httpClient.send(createRequest(languageCode), HttpResponse.BodyHandlers.ofString()))
                    .filter(r -> r.statusCode() == 200)
                    .map(HttpResponse::body)
                    .orElseGet(() -> DEFAULT_QUOTE);
        } catch (IOException | InterruptedException e) {
            return DEFAULT_QUOTE;
        }
    }


    private HttpRequest createRequest(String languageCode) {
        return HttpRequest.newBuilder().uri(URI.create(String.format("%s%s", QUOTE_API_URI, determineQueryString(languageCode))))
                .header(API_HOST_HEADER, API_HOST)
                .headers(USE_QUERY_STRING_HEADER, USE_QUERY_STRING)
                .header(API_KEY_HEADER, rapidApiKey)
                .GET()
                .build();
    }

    private String determineQueryString(String languageCode) {
        return StringUtils.isBlank(languageCode) ?
                "" :
                String.format("?language_code=%s", languageCode);
    }

}
