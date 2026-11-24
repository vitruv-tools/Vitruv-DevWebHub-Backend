package tools.vitruv.framework.remote.modules.users.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;
import tools.vitruv.framework.remote.modules.users.model.services.MentionService.Mention;
import tools.vitruv.framework.remote.modules.users.model.services.MentionService.ResolvedMention;

class MentionServiceTest {

  private static final String ADA = "ada";
  private static final String ADA_LOVELACE = "Ada Lovelace";
  private static final String GRACE = "grace";
  private static final String ASCET = "ascet";
  private static final String AMALTHEA = "amalthea";

  private AppUserRepo appUserRepo;
  private MentionService service;

  @BeforeEach
  void setUp() {
    appUserRepo = mock(AppUserRepo.class);
    KnowledgeMetamodelCatalog catalog = mock(KnowledgeMetamodelCatalog.class);
    when(appUserRepo.findAllByOrderByUsernameAsc())
        .thenReturn(
            List.of(
                user(ADA, ADA_LOVELACE),
                user("ada.l", "Ada L"),
                user(AMALTHEA, "A user named like a metamodel"),
                user(GRACE, null)));
    when(catalog.listAvailable())
        .thenReturn(
            List.of(
                new KnowledgeMetamodel(AMALTHEA, "Amalthea"),
                new KnowledgeMetamodel(ASCET, "Ascet")));
    service = new MentionService(appUserRepo, catalog);
  }

  @Test
  void findsUsersAndMetamodelsIgnoringCaseAndDuplicates() {
    assertThat(service.parse("Hi @Ada, can @ASCET experts and @grace look? @ada again."))
        .containsExactly(
            new Mention(MentionKind.USER, ADA),
            new Mention(MentionKind.METAMODEL, ASCET),
            new Mention(MentionKind.USER, GRACE));
  }

  @Test
  void ignoresUnknownNamesEmailsAndPartialWords() {
    assertThat(service.parse("@nobody wrote to ada@ascet.example, see @adam and @@grace"))
        .isEmpty();
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
    assertThat(service.parse("thanks @ada.")).containsExactly(new Mention(MentionKind.USER, ADA));
    assertThat(service.parse("@ada, @ascet; @grace!"))
        .containsExactly(
            new Mention(MentionKind.USER, ADA),
            new Mention(MentionKind.METAMODEL, ASCET),
            new Mention(MentionKind.USER, GRACE));
  }

  @Test
  void prefersTheLongestMatchingName() {
    assertThat(service.parse("ping @ada.l now"))
        .containsExactly(new Mention(MentionKind.USER, "ada.l"));
  }

  // New usernames cannot take a metamodel's name. This covers a metamodel added after the user.
  @Test
  void anExistingUserKeepsTheNameWhenAMetamodelLaterAppearsUnderIt() {
    assertThat(service.parse("@amalthea")).containsExactly(new Mention(MentionKind.USER, AMALTHEA));
  }

  @Test
  void buildsTheCandidateListOnceUntilInvalidated() {
    service.parse("@ada");
    service.parse("@grace");
    service.resolve(List.of(new Mention(MentionKind.USER, ADA)));
    verify(appUserRepo, times(1)).findAllByOrderByUsernameAsc();

    service.invalidate();
    service.parse("@ada");
    verify(appUserRepo, times(2)).findAllByOrderByUsernameAsc();
  }

  @Test
  void insideATransactionInvalidatesOnlyAfterCommit() {
    service.parse("@ada");
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.invalidate();
      service.parse("@ada");
      verify(appUserRepo, times(1)).findAllByOrderByUsernameAsc();

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    service.parse("@ada");
    verify(appUserRepo, times(2)).findAllByOrderByUsernameAsc();
  }

  @Test
  void aRebuildThatRacedAnInvalidationIsNotReused() {
    // The first rebuild reads the users table, then a sign-in commits and invalidates
    // before that rebuild is cached.
    when(appUserRepo.findAllByOrderByUsernameAsc())
        .thenAnswer(
            call -> {
              service.invalidate();
              return List.of(user(ADA, ADA_LOVELACE));
            })
        .thenReturn(List.of(user(ADA, ADA_LOVELACE), user("alice", null)));

    assertThat(service.parse("@alice please check")).isEmpty();
    assertThat(service.parse("@alice please check"))
        .containsExactly(new Mention(MentionKind.USER, "alice"));
  }

  @Test
  void trailingPunctuationEndsTheTag() {
    assertThat(service.parse("(@grace)")).containsExactly(new Mention(MentionKind.USER, GRACE));
    assertThat(service.parse("thanks @ascet."))
        .containsExactly(new Mention(MentionKind.METAMODEL, ASCET));
  }

  @Test
  void resolvesLabelsAndExpandsMetamodelsToUsers() {
    when(appUserRepo.findAllKnowingMetamodel(ASCET))
        .thenReturn(List.of(user(ADA, ADA_LOVELACE), user(GRACE, null)));

    List<ResolvedMention> resolved =
        service.resolve(
            List.of(
                new Mention(MentionKind.USER, ADA),
                new Mention(MentionKind.USER, GRACE),
                new Mention(MentionKind.METAMODEL, ASCET)));

    assertThat(resolved)
        .containsExactly(
            new ResolvedMention(MentionKind.USER, ADA, ADA_LOVELACE, List.of(ADA)),
            new ResolvedMention(MentionKind.USER, GRACE, GRACE, List.of(GRACE)),
            new ResolvedMention(MentionKind.METAMODEL, ASCET, "Ascet", List.of(ADA, GRACE)));
  }

  private static AppUser user(String username, String displayName) {
    AppUser user = new AppUser();
    user.setUsername(username);
    user.setDisplayName(displayName);
    return user;
  }
}
