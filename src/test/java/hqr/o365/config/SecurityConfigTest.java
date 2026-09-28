package hqr.o365.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.initialization-mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void registrationRequiresCsrfToken() throws Exception {
        mvc.perform(get("/reg")).andExpect(status().isOk());
        mvc.perform(post("/reg").param("userid", "admin").param("pwd", "password"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/getConfig").with(user("admin")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void registrationStylesheetIsPublic() throws Exception {
        mvc.perform(get("/modern.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }
}
