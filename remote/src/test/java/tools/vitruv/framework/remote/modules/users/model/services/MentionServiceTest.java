package tools.vitruv.framework.remote.modules.users.model.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;
import tools.vitruv.framework.remote.modules.users.model.services.MentionService.Mention;
import tools.vitruv.framework.remote.modules.users.model.services.MentionService.ResolvedMention;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MentionServiceTest {

    private AppUserRepo appUserRepo;
    private MentionService service;

    @BeforeEach
    void setUp() {
        appUserRepo = mock(AppUserRepo.class);
        KnowledgeMetamodelCatalog catalog = mock(KnowledgeMetamodelCatalog.class);
        when(appUserRepo.findAllByOrderByUsernameAsc()).thenReturn(List.of(
                user("ada", "Ada Lovelace"),
                user("ada.l", "Ada L"),
                user("amalthea", "A user named like a metamodel"),
                user("grace", null)));
        when(catalog.listAvailable()).thenReturn(List.of(
                new KnowledgeMetamodel("amalthea", "Amalthea"),
                new KnowledgeMetamodel("ascet", "Ascet")));
        service = new MentionService(appUserRepo, catalog);
    }

    @Test
    void findsUsersAndMetamodelsIgnoringCaseAndDuplicates() {
        assertThat(service.parse("Hi @Ada, can @ASCET experts and @grace look? @ada again."))
                .containsExactly(
                        new Mention(MentionKind.USER, "ada"),
                        new Mention(MentionKind.METAMODEL, "ascet"),
                        new Mention(MentionKind.USER, "grace"));
    }

    @Test
    void ignoresUnknownNamesEmailsAndPartialWords() {
        assertThat(service.parse("@nobody wrote to ada@ascet.example, see @adam and @@grace")).isEmpty();
        assertThat(service.parse("no tags here")).isEmpty();
        assertThat(service.parse(null)).isEmpty();
    }

    @Test
    void rejectsPrefixesOfLongerHandles() {
        assertThat(service.parse("@ada.later")).isEmpty();
        assertThat(service.parse("@ada-lovelace and @grace+x")).isEmpty();
    }

    @Test
    void rejectsEmailAddressesWithPlusOrDotInTheLocalPart() {
        assertThat(service.parse("ada+@ascet.example")).isEmpty();
        assertThat(service.parse("first.@grace.example")).isEmpty();
    }

    @Test
    void keepsTagsBeforeSentencePunctuation() {
        assertThat(service.parse("thanks @ada.")).containsExactly(new Mention(MentionKind.USER, "ada"));
        assertThat(service.parse("@ada, @ascet; @grace!")).containsExactly(
                new Mention(MentionKind.USER, "ada"),
                new Mention(MentionKind.METAMODEL, "ascet"),
                new Mention(MentionKind.USER, "grace"));
    }

    @Test
    void prefersTheLongestMatchingName() {
        assertThat(service.parse("ping @ada.l now")).containsExactly(new Mention(MentionKind.USER, "ada.l"));
    }

    // New usernames cannot take a metamodel's name. This covers a metamodel added after the user.
    @Test
    void anExistingUserKeepsTheNameWhenAMetamodelLaterAppearsUnderIt() {
        assertThat(service.parse("@amalthea")).containsExactly(new Mention(MentionKind.USER, "amalthea"));
    }

    @Test
    void buildsTheCandidateListOnceUntilInvalidated() {
        service.parse("@ada");
        service.parse("@grace");
        service.resolve(List.of(new Mention(MentionKind.USER, "ada")));
        verify(appUserRepo, times(1)).findAllByOrderByUsernameAsc();

        service.invalidate();
        service.parse("@ada");
        verify(appUserRepo, times(2)).findAllByOrderByUsernameAsc();
    }

    @Test
    void trailingPunctuationEndsTheTag() {
        assertThat(service.parse("(@grace)")).containsExactly(new Mention(MentionKind.USER, "grace"));
        assertThat(service.parse("thanks @ascet.")).containsExactly(new Mention(MentionKind.METAMODEL, "ascet"));
    }

    @Test
    void resolvesLabelsAndExpandsMetamodelsToUsers() {
        when(appUserRepo.findDistinctByMetamodels_MetamodelNameOrderByUsernameAsc("ascet"))
                .thenReturn(List.of(user("ada", "Ada Lovelace"), user("grace", null)));

        List<ResolvedMention> resolved = service.resolve(List.of(
                new Mention(MentionKind.USER, "ada"),
                new Mention(MentionKind.USER, "grace"),
                new Mention(MentionKind.METAMODEL, "ascet")));

        assertThat(resolved).containsExactly(
                new ResolvedMention(MentionKind.USER, "ada", "Ada Lovelace", List.of("ada")),
                new ResolvedMention(MentionKind.USER, "grace", "grace", List.of("grace")),
                new ResolvedMention(MentionKind.METAMODEL, "ascet", "Ascet", List.of("ada", "grace")));
    }

    private static AppUser user(String username, String displayName) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        return user;
    }
}
