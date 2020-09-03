package com.example.jettyissuedemo;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.Exceptions;

@RestController
public class IssueController {

    // use URL to large file e.g. 19MB that takes at least more than 1sec to complete loading
    private static final String URI = "<your-large-file-url e.g. 19MB file on gist>";
    private final HttpClient httpClient;

    @Autowired
    public IssueController() {
        this.httpClient = configJetty();
    }

    @GetMapping("/")
    public int requestTest() throws URISyntaxException {


        try {
            return WebClient.builder()
                    .clientConnector(new JettyClientHttpConnector(this.httpClient))
                    .build()
                    .get()
                    .uri(new URI(URI))
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofMillis(1000)) // 1sec to "fail" fast
                    .block()
                    .getStatusCodeValue();
        } catch (RuntimeException e) {
            var ue = Exceptions.unwrap(e);
            if (ue instanceof TimeoutException) {
                // Timeout is considered as expected case here
                return 0;
            }
            throw e;
        }
    }


    public HttpClient configJetty() {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        HttpClient jettyHttpClient = new HttpClient(sslContextFactory);
        jettyHttpClient.setCookieStore(new HttpCookieStore.Empty());
        jettyHttpClient.setFollowRedirects(true);
        jettyHttpClient.setMaxRedirects(5);
        return jettyHttpClient;
    }
}
