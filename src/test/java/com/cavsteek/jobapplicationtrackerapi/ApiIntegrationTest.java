package com.cavsteek.jobapplicationtrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApiIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private long createApplication(String token, String company) throws Exception {
        String body = mockMvc.perform(post("/api/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "%s", "role": "Backend Developer",
                                 "jobUrl": "https://acme.com/jobs/1",
                                 "appliedDate": "2026-10-26", "notes": "via portal"}
                                """.formatted(company)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void patchStatus(String token, long id, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/applications/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "%s"}
                                """.formatted(newStatus)))
                .andExpect(status().isOk());
    }

    // ---------- auth ----------

    @Test
    void register_returnsCreatedWithBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(uniqueEmail(), PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "short"}
                                """.formatted(uniqueEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_withCorrectPassword_returnsToken() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- security ----------

    @Test
    void applications_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applications_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer("not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- applications ----------

    @Test
    void create_returns201_andDefaultsToApplied() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(post("/api/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme", "role": "Backend Developer",
                                 "appliedDate": "2026-10-26"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Acme"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void create_withBlankCompany_returns400() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(post("/api/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "", "role": "Dev", "appliedDate": "2026-10-26"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").exists());
    }

    @Test
    void cors_allowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_returnsOnlyTheCallersApplications() throws Exception {
        String tokenA = registerAndGetToken(uniqueEmail());
        String tokenB = registerAndGetToken(uniqueEmail());
        createApplication(tokenA, "Acme");
        createApplication(tokenB, "Globex");
        createApplication(tokenB, "Initech");

        mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Acme"));

        mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void list_filtersByStatus() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long first = createApplication(token, "Acme");
        createApplication(token, "Globex");
        patchStatus(token, first, "INTERVIEW");

        mockMvc.perform(get("/api/applications?status=INTERVIEW")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("INTERVIEW"));
    }

    @Test
    void anotherUser_cannotReadChangeOrDeleteMyApplication() throws Exception {
        String owner = registerAndGetToken(uniqueEmail());
        String intruder = registerAndGetToken(uniqueEmail());
        long id = createApplication(owner, "Acme");

        mockMvc.perform(get("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/applications/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OFFER"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/applications/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(status().isNotFound());

        // the owner still sees it, untouched
        mockMvc.perform(get("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(get("/api/applications/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_replacesFields() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long id = createApplication(token, "Acme");

        mockMvc.perform(put("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme Corp", "role": "Senior Developer",
                                 "appliedDate": "2026-10-27", "notes": "updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Acme Corp"))
                .andExpect(jsonPath("$.role").value("Senior Developer"));

        mockMvc.perform(get("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.company").value("Acme Corp"));
    }

    @Test
    void delete_returns204_thenApplicationIsGone() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long id = createApplication(token, "Acme");

        mockMvc.perform(delete("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ---------- status history ----------

    @Test
    void statusChange_isRecordedInHistory_newestFirst() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long id = createApplication(token, "Acme");
        patchStatus(token, id, "INTERVIEW");

        mockMvc.perform(get("/api/applications/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].oldStatus").value("APPLIED"))
                .andExpect(jsonPath("$[0].newStatus").value("INTERVIEW"))
                .andExpect(jsonPath("$[1].oldStatus").doesNotExist())
                .andExpect(jsonPath("$[1].newStatus").value("APPLIED"));
    }

    @Test
    void sameStatus_addsNoHistoryRow() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long id = createApplication(token, "Acme");
        patchStatus(token, id, "APPLIED");

        mockMvc.perform(get("/api/applications/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void patchStatus_withInvalidValue_returns400() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long id = createApplication(token, "Acme");

        mockMvc.perform(patch("/api/applications/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "BANANA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_withUnknownSortField_returns400() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        mockMvc.perform(get("/api/applications?sort=bogus")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}