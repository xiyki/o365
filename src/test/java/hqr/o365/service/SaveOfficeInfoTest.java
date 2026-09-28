package hqr.o365.service;

import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaOfficeInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SaveOfficeInfoTest {
    @Test
    void updatePreservesExistingCredentialsAndSelectionWhenBlank() {
        TaOfficeInfoRepo repo = mock(TaOfficeInfoRepo.class);
        SaveOfficeInfo service = new SaveOfficeInfo();
        ReflectionTestUtils.setField(service, "repo", repo);
        TaOfficeInfo existing = new TaOfficeInfo();
        existing.setSeqNo(7);
        existing.setPasswd("old-password");
        existing.setSecretId("old-secret");
        existing.setSelected("是");
        Date created = new Date();
        existing.setCreateDt(created);
        when(repo.findById(7)).thenReturn(Optional.of(existing));

        TaOfficeInfo update = new TaOfficeInfo();
        update.setSeqNo(7);
        update.setSelected("否");
        update.setPasswd("");
        update.setSecretId("");

        assertTrue(service.save(update));
        assertEquals("old-password", update.getPasswd());
        assertEquals("old-secret", update.getSecretId());
        assertEquals("是", update.getSelected());
        assertEquals(created, update.getCreateDt());
        verify(repo).save(update);
    }

    @Test
    void newConfigurationRequiresSecret() {
        TaOfficeInfoRepo repo = mock(TaOfficeInfoRepo.class);
        SaveOfficeInfo service = new SaveOfficeInfo();
        ReflectionTestUtils.setField(service, "repo", repo);

        assertFalse(service.save(new TaOfficeInfo()));
        verify(repo, never()).save(any(TaOfficeInfo.class));
    }
}
