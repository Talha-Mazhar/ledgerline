package com.ledgerline.onboarding;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.ledgerline.ApiRequests.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.ledgerline.ApiScenario;
import com.ledgerline.TestUsers;
import com.ledgerline.TestcontainersConfiguration;
import com.ledgerline.identity.SignedInUser;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Identity verification over HTTP, with the document stored in real S3-compatible storage (RustFS): upload, review queue,
 * approval, and the higher limits that follow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VerificationApiTests {

    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator.createForSpecificationUrl(
                    Path.of("../api/openapi.yaml").toAbsolutePath().toUri().toString())
            .build();

    /** The first bytes of a PDF are enough for the type check. */
    private static final byte[] PDF = "%PDF-1.7\n% passport scan\n".getBytes(StandardCharsets.US_ASCII);

    private final MockMvc mvc;
    private final ApiScenario scenario;
    private SignedInUser customer = TestUsers.DEMO;

    VerificationApiTests(@Autowired MockMvc mvc) {
        this.mvc = mvc;
        this.scenario = new ApiScenario(mvc);
    }

    @BeforeEach
    void aNewCustomer() throws Exception {
        customer = scenario.signUp("Vera Verify");
    }

    @Test
    void aNewCustomerIsNotVerifiedAndHasBasicLimits() throws Exception {
        mvc.perform(get("/api/v1/verification").with(TestUsers.signedInAs(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.tier").value("BASIC"))
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(get("/api/v1/limits").with(TestUsers.signedInAs(customer)))
                .andExpect(jsonPath("$.singlePayment").value("1000.00"))
                .andExpect(jsonPath("$.daily").value("2000.00"))
                .andExpect(openApi().isValid(CONTRACT));
    }

    @Test
    void staffReviewAnUploadedDocumentAndTheCustomerGetsHigherLimits() throws Exception {
        upload(PDF, "passport.pdf")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.documentName").value("passport.pdf"));

        String queue = mvc.perform(
                        get("/api/v1/back-office/verifications").with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(CONTRACT))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(queue).contains("\"userId\":\"" + customer.id() + "\"");
        mvc.perform(get("/api/v1/back-office/verifications/{id}/document", customer.id())
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));

        mvc.perform(post("/api/v1/back-office/verifications/{id}/approve", customer.id())
                        .with(TestUsers.signedInAs(TestUsers.SUPPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(openApi().isValid(CONTRACT));
        mvc.perform(get("/api/v1/limits").with(TestUsers.signedInAs(customer)))
                .andExpect(jsonPath("$.tier").value("VERIFIED"))
                .andExpect(jsonPath("$.singlePayment").value("10000.00"));
    }

    @Test
    void aRejectedCustomerCanTryAgain() throws Exception {
        upload(PDF, "blurry.pdf").andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/back-office/verifications/{id}/reject", customer.id())
                        .with(TestUsers.signedInAs(TestUsers.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"The photo is too blurry to read.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("The photo is too blurry to read."));

        upload(PDF, "sharp.pdf")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void onlyPdfJpegAndPngAreAccepted() throws Exception {
        upload("just text, renamed".getBytes(StandardCharsets.UTF_8), "passport.pdf")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Upload a PDF, JPEG or PNG file."));
    }

    @Test
    void customersCantSeeTheReviewQueue() throws Exception {
        mvc.perform(get("/api/v1/back-office/verifications").with(TestUsers.signedInAs(customer)))
                .andExpect(status().isForbidden());
    }

    private ResultActions upload(byte[] content, String name) throws Exception {
        return mvc.perform(multipart("/api/v1/verification")
                .file(new MockMultipartFile("document", name, "application/pdf", content))
                .cookie(new Cookie("XSRF-TOKEN", "test-csrf-token"))
                .header("X-XSRF-TOKEN", "test-csrf-token")
                .with(TestUsers.signedInAs(customer)));
    }
}
