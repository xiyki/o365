package hqr.o365.service;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.domain.TaMasterCd;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSystemInfoTest {
    @Test
    void hidesSensitiveValuesWithoutChangingStoredEntities() {
        TaMasterCdRepo repo = mock(TaMasterCdRepo.class);
        TaMasterCd secret = new TaMasterCd();
        secret.setKeyTy("CF_AUTH_KEY");
        secret.setCd("private-key");
        TaMasterCd normal = new TaMasterCd();
        normal.setKeyTy("GEN_APP_RPT");
        normal.setCd("Y");
        when(repo.count()).thenReturn(2L);
        when(repo.findAll()).thenReturn(Arrays.asList(secret, normal));
        GetSystemInfo service = new GetSystemInfo();
        ReflectionTestUtils.setField(service, "tmc", repo);

        String response = service.getAllSystemInfo();
        assertFalse(response.contains("private-key"));
        assertTrue(response.contains("GEN_APP_RPT"));
        assertEquals("private-key", secret.getCd());
    }
}
