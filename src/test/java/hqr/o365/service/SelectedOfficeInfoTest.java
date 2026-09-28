package hqr.o365.service;

import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaOfficeInfo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SelectedOfficeInfoTest {
    @Test
    void eachSessionResolvesItsOwnTenant() {
        TaOfficeInfoRepo repo = mock(TaOfficeInfoRepo.class);
        TaOfficeInfo first = new TaOfficeInfo();
        first.setSeqNo(1);
        TaOfficeInfo second = new TaOfficeInfo();
        second.setSeqNo(2);
        when(repo.findById(1)).thenReturn(Optional.of(first));
        when(repo.findById(2)).thenReturn(Optional.of(second));
        SelectedOfficeInfo service = new SelectedOfficeInfo();
        ReflectionTestUtils.setField(service, "repo", repo);
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.getSession().setAttribute(SelectedOfficeInfo.SESSION_KEY, 1);
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.getSession().setAttribute(SelectedOfficeInfo.SESSION_KEY, 2);

        try {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(firstRequest));
            assertEquals(1, service.current().get(0).getSeqNo());
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(secondRequest));
            assertEquals(2, service.current().get(0).getSeqNo());
            verify(repo, never()).findBySelected("是");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
