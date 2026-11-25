package tools.vitruv.framework.remote.modules.users.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog;
import tools.vitruv.framework.remote.modules.users.model.services.MentionService;
import tools.vitruv.framework.remote.modules.users.usecases.dtos.UpsertUserRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileUseCasesTest {

    private static final String ALICE = "alice";
    private static final String TOKEN = "secret-token";

    private AppUserRepo appUserRepo;
    private MentionService mentionService;
    private UserProfileUseCases useCases;

    @BeforeEach
    void setUp() {
        appUserRepo = mock(AppUserRepo.class);
        mentionService = mock(MentionService.class);
        useCases = new UserProfileUseCases(appUserRepo, mock(KnowledgeMetamodelCatalog.class), mentionService);
        when(appUserRepo.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void creatingAUserRefreshesMentions() {
        when(appUserRepo.findByUsernameIgnoreCase(ALICE)).thenReturn(Optional.empty());

        useCases.signIn(new UpsertUserRequest(ALICE, "Alice", null), null);

        verify(mentionService).invalidate();
    }

    @Test
    void signingInWithTheSameDisplayNameKeepsMentionsCached() throws NoSuchAlgorithmException {
        when(appUserRepo.findByUsernameIgnoreCase(ALICE)).thenReturn(Optional.of(existingAlice("Alice")));

        useCases.signIn(new UpsertUserRequest(ALICE, "Alice", "new@example.org"), TOKEN);

        verify(mentionService, never()).invalidate();
    }

    @Test
    void changingTheDisplayNameRefreshesMentions() throws NoSuchAlgorithmException {
        when(appUserRepo.findByUsernameIgnoreCase(ALICE)).thenReturn(Optional.of(existingAlice("Alice")));

        useCases.signIn(new UpsertUserRequest(ALICE, "Alice Smith", null), TOKEN);

        verify(mentionService).invalidate();
    }

    private static AppUser existingAlice(String displayName) throws NoSuchAlgorithmException {
        AppUser user = new AppUser();
        user.setUsername(ALICE);
        user.setDisplayName(displayName);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8));
        user.setProfileTokenHash(HexFormat.of().formatHex(digest));
        return user;
    }
}
