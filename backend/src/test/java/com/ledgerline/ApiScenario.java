package com.ledgerline;

import static com.ledgerline.ApiRequests.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ledgerline.identity.Role;
import com.ledgerline.identity.SignedInUser;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Steps that many API tests share, done through the HTTP API like a real client: sign up, open a
 * wallet, add money, send money. Tests that commit their data use fresh users from {@link #signUp},
 * so they never change what other tests see.
 */
public final class ApiScenario {

    private final MockMvc mvc;

    public ApiScenario(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** A new customer with a unique email, signed up through the API. */
    public SignedInUser signUp(String displayName) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"" + displayName
                                + "\",\"password\":\"a-long-password\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new SignedInUser(UUID.fromString(JsonPath.read(body, "$.id")), email, displayName, Role.CUSTOMER);
    }

    public String openWallet(SignedInUser user, String currency) throws Exception {
        String body = mvc.perform(post("/api/v1/wallets")
                        .with(TestUsers.signedInAs(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"" + currency + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    public void deposit(SignedInUser user, String walletId, String amount) throws Exception {
        mvc.perform(post("/api/v1/wallets/{walletId}/deposits", walletId)
                        .with(TestUsers.signedInAs(user))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"" + amount + "\"}"))
                .andExpect(status().isCreated());
    }

    public ResultActions withdraw(SignedInUser user, String walletId, String amount) throws Exception {
        return mvc.perform(post("/api/v1/wallets/{walletId}/withdrawals", walletId)
                .with(TestUsers.signedInAs(user))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"" + amount + "\"}"));
    }

    public ResultActions send(SignedInUser from, String walletId, String recipientEmail, String amount)
            throws Exception {
        return mvc.perform(post("/api/v1/transfers")
                .with(TestUsers.signedInAs(from))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromWalletId\":\"" + walletId + "\",\"recipientEmail\":\"" + recipientEmail
                        + "\",\"amount\":\"" + amount + "\"}"));
    }

    /** The id in a JSON response, e.g. a payment's. */
    public static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
