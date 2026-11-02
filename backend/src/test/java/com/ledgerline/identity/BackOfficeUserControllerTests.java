package com.ledgerline.identity;

import static com.ledgerline.ApiRequests.put;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgerline.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A slice test: only the web layer (this controller, the security rules, JSON and error handling)
 * starts, with the service replaced by a mock. No database, so it runs in a fraction of a second
 * and checks exactly one thing: who may call what.
 */
@WebMvcTest(BackOfficeUserController.class)
@Import(SecurityConfiguration.class)
class BackOfficeUserControllerTests {

    private static final UserSummary JANA = new UserSummary(
            TestUsers.JANA.id(), TestUsers.JANA.email(), TestUsers.JANA.displayName(), Role.CUSTOMER);

    private final MockMvc mvc;

    @MockitoBean
    @SuppressWarnings("NullAway.Init")
    UserAccounts accounts;

    BackOfficeUserControllerTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void needsASession() throws Exception {
        mvc.perform(get("/api/v1/back-office/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersAreRefused() throws Exception {
        mvc.perform(get("/api/v1/back-office/users").with(TestUsers.signedInAs(TestUsers.DEMO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportStaffCanListUsers() throws Exception {
        when(accounts.search(anyString(), anyInt())).thenReturn(List.of(JANA));

        mvc.perform(get("/api/v1/back-office/users")
                        .param("query", "jana")
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("jana@ledgerline.dev"))
                .andExpect(jsonPath("$.items[0].role").value("CUSTOMER"));
        verify(accounts).search("jana", 50);
    }

    @Test
    void onlyAdminsChangeRoles() throws Exception {
        mvc.perform(put("/api/v1/back-office/users/{id}/role", JANA.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPPORT\"}")
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isForbidden());
        verify(accounts, never()).changeRole(any(), any(), any());
    }

    @Test
    void anAdminChangesARole() throws Exception {
        when(accounts.changeRole(TestUsers.ADMIN.id(), JANA.id(), Role.SUPPORT))
                .thenReturn(new UserSummary(JANA.id(), JANA.email(), JANA.displayName(), Role.SUPPORT));

        mvc.perform(put("/api/v1/back-office/users/{id}/role", JANA.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPPORT\"}")
                        .with(TestUsers.signedInAs(TestUsers.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPPORT"));
        verify(accounts).changeRole(eq(TestUsers.ADMIN.id()), eq(JANA.id()), eq(Role.SUPPORT));
    }
}
