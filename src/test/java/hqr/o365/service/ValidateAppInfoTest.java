package hqr.o365.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ValidateAppInfoTest {
    @Test
    void tokenBelongsToTheRequestedTenant() {
        ValidateAppInfo service = new ValidateAppInfo();
        RestTemplate http = new RestTemplate();
        ReflectionTestUtils.setField(service, "restTemplate", http);
        ReflectionTestUtils.setField(service, "ua", "test-client");
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(once(), requestTo("https://login.microsoftonline.com/tenant-a/oauth2/v2.0/token"))
                .andRespond(withSuccess("{\"access_token\":\"token-a\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://login.microsoftonline.com/tenant-b/oauth2/v2.0/token"))
                .andRespond(withSuccess("{\"access_token\":\"token-b\"}", MediaType.APPLICATION_JSON));

        assertEquals("token-a", service.getToken("tenant-a", "app-a", "secret-a"));
        assertEquals("token-b", service.getToken("tenant-b", "app-b", "secret-b"));
        server.verify();
    }
}
