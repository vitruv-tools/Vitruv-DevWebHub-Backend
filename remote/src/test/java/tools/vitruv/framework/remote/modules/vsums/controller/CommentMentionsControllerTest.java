package tools.vitruv.framework.remote.modules.vsums.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistency;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CommentMentionsControllerTest {

    private static final String ADA = "ada";
    private static final String GRACE = "grace";
    private static final String MODEL = "model";
    private static final String GRACE_NAME = "Grace Hopper";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OpenInconsistencyRepo openInconsistencyRepo;

    @DynamicPropertySource
    static void pointCatalogAtTestProviders(DynamicPropertyRegistry registry) {
        Path fromModule = Path.of("src/test/resources/vsum-providers");
        Path fromBackendRoot = Path.of("remote/src/test/resources/vsum-providers");
        Path providers = Files.isDirectory(fromModule) ? fromModule : fromBackendRoot;
        registry.add("app.vsums.vsum-providers-dir", providers.toAbsolutePath()::toString);
    }

    @Test
    void commentsStoreUserAndMetamodelTagsAndExpandMetamodelsToUsers() throws Exception {
        String adaToken = signIn(ADA, "Ada Lovelace");
        String graceToken = signIn(GRACE, GRACE_NAME);
        mvc.perform(put("/v1/users/ada/metamodels")
                        .header("X-Profile-Token", adaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\"]}"))
                .andExpect(status().isOk());
        OpenInconsistency inconsistency = saveInconsistency();

        String body = "@grace please check, and @Model people too. Mail ada@model.example is not a tag, @nobody neither.";
        mvc.perform(post(commentsUrl(inconsistency))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"ada\",\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mentions", hasSize(2)))
                .andExpect(jsonPath("$.mentions[0].kind").value("USER"))
                .andExpect(jsonPath("$.mentions[0].name").value(GRACE))
                .andExpect(jsonPath("$.mentions[0].label").value(GRACE_NAME))
                .andExpect(jsonPath("$.mentions[0].users", contains(GRACE)))
                .andExpect(jsonPath("$.mentions[1].kind").value("METAMODEL"))
                .andExpect(jsonPath("$.mentions[1].name").value(MODEL))
                .andExpect(jsonPath("$.mentions[1].users", contains(ADA)));

        mvc.perform(post(commentsUrl(inconsistency))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"grace\",\"body\":\"No tags in this one\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mentions", empty()));

        mvc.perform(get(commentsUrl(inconsistency)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mentions", hasSize(2)))
                .andExpect(jsonPath("$[0].mentions[0].name").value(GRACE))
                .andExpect(jsonPath("$[0].mentions[1].name").value(MODEL))
                .andExpect(jsonPath("$[1].mentions", empty()));

        // Metamodel tags follow who knows the metamodel now, not when the comment was written.
        mvc.perform(put("/v1/users/grace/metamodels")
                        .header("X-Profile-Token", graceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metamodels\":[\"model\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get(commentsUrl(inconsistency)))
                .andExpect(jsonPath("$[0].mentions[1].users", contains(ADA, GRACE)));
    }

    @Test
    void resolutionCommentsStoreTagsFromTheResolutionMessage() throws Exception {
        signIn(GRACE, GRACE_NAME);
        OpenInconsistency inconsistency = saveInconsistency();

        mvc.perform(post("/v1/inconsistencies/" + inconsistency.getId() + "/resolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolvedBy\":\"ada\",\"choice\":\"keep\","
                                + "\"comment\":\"@grace please double check, ask @model people\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(commentsUrl(inconsistency)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mentions", hasSize(2)))
                .andExpect(jsonPath("$[0].mentions[0].name").value(GRACE))
                .andExpect(jsonPath("$[0].mentions[1].name").value(MODEL));
    }

    @Test
    void aMetamodelNameCannotBecomeAUsername() throws Exception {
        mvc.perform(post("/v1/users/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Model\",\"displayName\":\"Impostor\"}"))
                .andExpect(status().isConflict());
    }

    private static String commentsUrl(OpenInconsistency inconsistency) {
        return "/v1/inconsistencies/" + inconsistency.getId() + "/comments";
    }

    private String signIn(String username, String displayName) throws Exception {
        String response = mvc.perform(post("/v1/users/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"displayName\":\"" + displayName + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = response.indexOf("\"profileToken\":\"") + "\"profileToken\":\"".length();
        return response.substring(start, response.indexOf('"', start));
    }

    private OpenInconsistency saveInconsistency() {
        OpenInconsistency entity = new OpenInconsistency();
        entity.setVsumId(UUID.randomUUID());
        entity.setTaskId(UUID.randomUUID());
        entity.setViewId(UUID.randomUUID());
        entity.setVsumName("Test VSUM");
        entity.setTitle("Title");
        entity.setMessage("Title");
        entity.setInteractionJson("{\"message\":\"Title\"}");
        entity.setState(OpenInconsistencyState.RESOLVED);
        entity.setCreatedAt(Instant.now());
        return openInconsistencyRepo.save(entity);
    }
}
