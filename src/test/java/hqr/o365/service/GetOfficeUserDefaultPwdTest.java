package hqr.o365.service;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.domain.TaMasterCd;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetOfficeUserDefaultPwdTest {
    @Test
    void replacesThePublishedDefaultPassword() {
        TaMasterCdRepo repo = mock(TaMasterCdRepo.class);
        TaMasterCd setting = new TaMasterCd();
        setting.setKeyTy("DEFAULT_PASSWORD");
        setting.setCd("Mjj@1234");
        when(repo.findById("DEFAULT_PASSWORD")).thenReturn(Optional.of(setting));
        GetOfficeUserDefaultPwd service = new GetOfficeUserDefaultPwd();
        ReflectionTestUtils.setField(service, "tmc", repo);

        String password = service.getDefaultPwd();
        assertEquals(20, password.length());
        assertNotEquals("Mjj@1234", password);
        assertEquals(password, setting.getCd());
        verify(repo).saveAndFlush(setting);
    }
}
