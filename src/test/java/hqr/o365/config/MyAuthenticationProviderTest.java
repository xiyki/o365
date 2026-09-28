package hqr.o365.config;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.dao.TaUserRepo;
import hqr.o365.domain.TaUser;
import hqr.o365.service.TaUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyAuthenticationProviderTest {
    @Test
    void legacyPasswordIsUpgradedAfterSuccessfulLogin() {
        TaUserDetailsService userService = mock(TaUserDetailsService.class);
        TaUserRepo userRepo = mock(TaUserRepo.class);
        TaMasterCdRepo codes = mock(TaMasterCdRepo.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        MyAuthenticationProvider provider = provider(userService, userRepo, codes, encoder);
        TaUser stored = new TaUser();
        stored.setUserId("admin");
        stored.setPasswd("legacy-password");
        when(userService.loadUserByUsername("admin"))
                .thenReturn(new User("admin", "legacy-password", Collections.emptyList()));
        when(userRepo.findByUserId("admin")).thenReturn(stored);
        when(codes.findById("WX_CALLBACK_IND")).thenReturn(Optional.empty());

        Authentication result = provider.authenticate(
                new UsernamePasswordAuthenticationToken("admin", "legacy-password"));
        assertTrue(result.isAuthenticated());
        assertNull(result.getCredentials());
        assertTrue(encoder.matches("legacy-password", ((UserDetails) result.getPrincipal()).getPassword()));
        assertTrue(encoder.matches("legacy-password", stored.getPasswd()));
        verify(userRepo).save(stored);
    }

    @Test
    void hashedPasswordDoesNotGetRehashedOnLogin() {
        TaUserDetailsService userService = mock(TaUserDetailsService.class);
        TaUserRepo userRepo = mock(TaUserRepo.class);
        TaMasterCdRepo codes = mock(TaMasterCdRepo.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        MyAuthenticationProvider provider = provider(userService, userRepo, codes, encoder);
        when(userService.loadUserByUsername("admin"))
                .thenReturn(new User("admin", encoder.encode("password"), Collections.emptyList()));
        when(codes.findById("WX_CALLBACK_IND")).thenReturn(Optional.empty());

        assertTrue(provider.authenticate(new UsernamePasswordAuthenticationToken("admin", "password"))
                .isAuthenticated());
        verifyNoInteractions(userRepo);
    }

    private MyAuthenticationProvider provider(TaUserDetailsService userService, TaUserRepo userRepo,
                                              TaMasterCdRepo codes, BCryptPasswordEncoder encoder) {
        MyAuthenticationProvider provider = new MyAuthenticationProvider();
        ReflectionTestUtils.setField(provider, "userDetailsService", userService);
        ReflectionTestUtils.setField(provider, "userRepo", userRepo);
        ReflectionTestUtils.setField(provider, "tmc", codes);
        ReflectionTestUtils.setField(provider, "passwordEncoder", encoder);
        return provider;
    }
}
