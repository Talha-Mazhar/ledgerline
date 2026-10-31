package com.ledgerline.identity;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-up, sign-in, sign-out and the security rules around them, over HTTP. These tests start
 * without a session, like a browser that has just opened the app.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    private final MockMvc mvc;

    AuthApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void everyApiNeedsASession() throws Exception {
        mvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Sign-in required"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void handsOutTheCsrfCookieOnTheFirstRequest() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void refusesAPostWithoutTheCsrfToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(TestUsers.DEMO.email(), TestUsers.PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Request refused"));
    }

    @Test
    void signsInAndKeepsTheUserInTheSession() throws Exception {
        MockHttpSession session = signIn(TestUsers.DEMO.email(), TestUsers.PASSWORD);

        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestUsers.DEMO.id().toString()))
                .andExpect(jsonPath("$.displayName").value("Demo user"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void ignoresTheCaseOfTheEmail() throws Exception {
        mvc.perform(signInRequest("Demo@Ledgerline.DEV", TestUsers.PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void refusesAWrongPasswordWithoutSayingWhichPartWasWrong() throws Exception {
        mvc.perform(signInRequest(TestUsers.DEMO.email(), "not-the-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."))
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(signInRequest("nobody@ledgerline.dev", TestUsers.PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."));
    }

    @Test
    void signsUpAndStartsASession() throws Exception {
        MockHttpSession session = (MockHttpSession) Objects.requireNonNull(mvc.perform(
                                signUp("pavel@example.com", "Pavel Dvořák", "a-long-password"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.email").value("pavel@example.com"))
                        .andExpect(openApi().isValid(CONTRACT))
                        .andReturn()
                        .getRequest()
                        .getSession(false));

        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(jsonPath("$.displayName").value("Pavel Dvořák"));
        mvc.perform(signInRequest("pavel@example.com", "a-long-password")).andExpect(status().isOk());
    }

    @Test
    void refusesASecondAccountForTheSameEmail() throws Exception {
        mvc.perform(signUp("JANA@ledgerline.dev", "Jana", "a-long-password"))
                .andExpect(status().isConflict())
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void refusesAShortPassword() throws Exception {
        mvc.perform(signUp("short@example.com", "Short", "too-short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Use a password of 10 to 64 characters."))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void signingOutEndsTheSession() throws Exception {
        MockHttpSession session = signIn(TestUsers.DEMO.email(), TestUsers.PASSWORD);

        mvc.perform(post("/api/v1/auth/sign-out").session(session))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    private MockHttpSession signIn(String email, String password) throws Exception {
        return (MockHttpSession) Objects.requireNonNull(mvc.perform(signInRequest(email, password))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(CONTRACT))
                .andReturn()
                .getRequest()
                .getSession(false));
    }

    private static RequestBuilder signInRequest(String email, String password) {
        return post("/api/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password));
    }

    private static RequestBuilder signUp(String email, String displayName, String password) {
        return post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"displayName\":\"" + displayName + "\",\"password\":\""
                        + password + "\"}");
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
