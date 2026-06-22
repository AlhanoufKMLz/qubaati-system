package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Generic external-webhook client for the n8n "Parent Weekly Report" workflow.
 *
 * <p>This is NOT an AI integration — it is a plain HTTP POST to an n8n production webhook using {@link RestClient}
 * (the modern Spring HTTP client). Authentication is HTTP <b>Basic Auth</b> (username/password configured in n8n);
 * the password is only used to build the Authorization header and is never logged or returned.
 */
@Slf4j
@Service
public class N8nWebhookService {

    @Value("${n8n.parent-report-webhook-url:}")
    private String webhookUrl;

    @Value("${n8n.basic-auth-username:}")
    private String basicAuthUsername;

    @Value("${n8n.basic-auth-password:}")
    private String basicAuthPassword;

    private final RestClient restClient;

    public N8nWebhookService() {
        this.restClient = RestClient.create();
    }

    /** True only when the webhook URL and both Basic Auth credentials are all configured. */
    public boolean isConfigured() {
        return notBlank(webhookUrl) && notBlank(basicAuthUsername) && notBlank(basicAuthPassword);
    }

    /**
     * POSTs the given payload to the n8n parent-weekly-report webhook with Basic Auth and returns the parsed
     * JSON response. Throws a clear {@link ApiException} when the integration is not configured or the call fails.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateParentWeeklyReport(Map<String, Object> payload) {
        if (!notBlank(webhookUrl)) {
            throw new ApiException("n8n parent report webhook URL is not configured");
        }
        if (!notBlank(basicAuthUsername) || !notBlank(basicAuthPassword)) {
            throw new ApiException("n8n Basic Auth credentials are not configured");
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(webhookUrl)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new ApiException("n8n webhook returned an empty response");
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            // Remote returned a non-2xx status — surface the status, never the credentials.
            log.warn("n8n webhook call failed with HTTP status {}", e.getStatusCode().value());
            throw new ApiException("n8n webhook call failed with HTTP status " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("n8n webhook call failed: {}", e.getMessage());
            throw new ApiException("n8n webhook call failed: " + e.getMessage());
        }
    }

    // ---------- helpers ----------

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String basicAuthHeader() {
        String credentials = basicAuthUsername + ":" + basicAuthPassword;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
