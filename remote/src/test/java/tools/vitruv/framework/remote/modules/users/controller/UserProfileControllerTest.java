package tools.vitruv.framework.remote.modules.users.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.users.usecases.UserProfileUseCases;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserProfileControllerTest {

    private static final String TOKEN = UserProfileUseCases.PROFILE_TOKEN_HEADER;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void pointCatalogAtTestProviders(DynamicPropertyRegistry registry) {
        Path fromModule = Path.of("src/test/resources/vsum-providers");
        Path fromBackendRoot = Path.of("remote/src/test/resources/vsum-providers");
        Path providers = Files.isDirectory(fromModule) ? fromModule : fromBackendRoot;
        registry.add("app.vsums.vsum-providers-dir", providers.toAbsolutePath()::toString);
    }

    @Test
    void signInReissuesALostTokenAndLaterCallsMustPresentTheNewOne() throws Exception {
        String first = signIn("ada", "Ada Lovelace", "ada@example.com");

        mvc.perform(get("/v1/users/ada"))
                .andExpect(status().isUnauthorized());

        String second = signIn("ada", "Ada Again", "ada@example.com");
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);

        mvc.perform(get("/v1/users/ada").header(TOKEN, first))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/users/ada").header(TOKEN, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada Again"))
                .andExpect(jsonPath("$.email").value("ada@example.com"));

        mvc.perform(post("/v1/users/session")
                        .header(TOKEN, second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody("ada", "Ada", "ada@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada"))
                .andExpect(jsonPath("$.profileToken").value(second));

        mvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ada"))
                .andExpect(jsonPath("$[0].email").value(nullValue()))
                .andExpect(jsonPath("$[0].metamodels", empty()))
                .andExpect(jsonPath("$[0].profileToken").value(nullValue()));
    }

    @Test
    void metamodelUpdatesReplaceDedupeRejectAndClear() throws Exception {
        String token = signIn("ada", "Ada", "ada@example.com");

        mvc.perform(put("/v1/users/ada/metamodels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\"]}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/v1/users/ada/metamodels")
                        .header(TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\",\"model\",\"model2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metamodels", contains("model", "model2")));

        mvc.perform(put("/v1/users/ada/metamodels")
                        .header(TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\",\"model2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metamodels", contains("model", "model2")));

        mvc.perform(put("/v1/users/ada/metamodels")
                        .header(TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"not-a-metamodel\"]}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/v1/users/ada").header(TOKEN, token))
                .andExpect(jsonPath("$.metamodels", contains("model", "model2")));

        mvc.perform(put("/v1/users/ada/metamodels")
                        .header(TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metamodels", empty()));

        mvc.perform(put("/v1/users/ada/metamodels")
                        .header(TOKEN, "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\"]}"))
                .andExpect(status().isUnauthorized());
    }

    private String signIn(String username, String displayName, String email) throws Exception {
        MvcResult result = mvc.perform(post("/v1/users/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody(username, displayName, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileToken", not(emptyString())))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"profileToken\":\"") + "\"profileToken\":\"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    private static String sessionBody(String username, String displayName, String email) {
        return "{\"username\":\"" + username + "\",\"displayName\":\"" + displayName + "\",\"email\":\"" + email + "\"}";
    }
}
