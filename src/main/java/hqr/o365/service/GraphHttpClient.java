package hqr.o365.service;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/** Common HTTP settings for Microsoft Graph calls. Each service gets its own client. */
final class GraphHttpClient {
    private GraphHttpClient() {
    }

    static RestTemplate create() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        RestTemplate client = new RestTemplate(factory);
        client.getInterceptors().add((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            if (request.getMethod() != HttpMethod.GET || response.getRawStatusCode() != 429) {
                return response;
            }
            String retryAfter = response.getHeaders().getFirst("Retry-After");
            try {
                long seconds = Long.parseLong(retryAfter);
                if (seconds < 0 || seconds > 5) {
                    return response;
                }
                Thread.sleep(seconds * 1000);
            } catch (NumberFormatException e) {
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return response;
            }
            response.close();
            return execution.execute(request, body);
        });
        return client;
    }
}
