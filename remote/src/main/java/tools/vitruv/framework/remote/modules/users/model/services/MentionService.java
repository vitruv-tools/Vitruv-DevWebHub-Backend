package tools.vitruv.framework.remote.modules.users.model.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;

/**
 * Finds {@code @tags} in comment text and resolves them to users.
 *
 * <p>A tag is only recognised when it matches a real username or a known metamodel name,
 * so {@code @} in an email address or an unknown word is ignored. Matching ignores case.
 * Usernames are unique across users and metamodels, so a tag is never ambiguous. A tag on a
 * metamodel stands for every user who knows that metamodel.
 */
@Service
@RequiredArgsConstructor
public class MentionService {

  /** A tag as written in a comment. {@code name} is the canonical username or metamodel name. */
  public record Mention(MentionKind kind, String name) {
  }

  /** A tag together with its label and the usernames it reaches today. */
  public record ResolvedMention(MentionKind kind, String name, String label, List<String> users) {
  }

  /**
   * How long the metamodel half of the index is trusted. Metamodels come from files that change
   * outside the application, so there is no event to listen to.
   */
  private static final long METAMODEL_REFRESH_NANOS = TimeUnit.SECONDS.toNanos(30);

  // A tag must not sit inside an email address, so the "@" may not follow a character that can
  // appear in an email's local part. It must also end the token: "@ada.later" is not "@ada".
  private static final String BEFORE_TAG = "(?<![\\p{L}\\p{N}_@.+%-])@(";
  private static final String AFTER_TAG = ")(?![\\p{L}\\p{N}_@])(?![.+-]+[\\p{L}\\p{N}_])";

  /**
   * Everything needed to find and label tags, built once and reused until a user is added or
   * changed, or the metamodel refresh interval passes.
   */
  private record Index(
      Map<String, Mention> candidates,
      Map<String, String> userLabels,
      Map<String, String> metamodelLabels,
      Pattern pattern,
      long builtAtNanos
  ) {
  }

  private final AppUserRepo appUserRepo;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;
  private volatile Index index;

  /**
   * Drops the cached index. Call this whenever a user is created or their display name changes.
   */
  public void invalidate() {
    index = null;
  }

  /**
   * Tags in {@code text}, in order of first appearance, without duplicates.
   */
  public List<Mention> parse(String text) {
    if (text == null || text.indexOf('@') < 0) {
      return List.of();
    }
    Index current = currentIndex();
    if (current.pattern() == null) {
      return List.of();
    }
    Set<Mention> found = new LinkedHashSet<>();
    Matcher matcher = current.pattern().matcher(text);
    while (matcher.find()) {
      Mention mention = current.candidates().get(key(matcher.group(1)));
      if (mention != null) {
        found.add(mention);
      }
    }
    return new ArrayList<>(found);
  }

  /**
   * Adds labels and the users each tag reaches. Labels come from the cached index. Who knows a
   * metamodel is looked up here, once per metamodel in {@code mentions}, so callers should pass
   * all tags of a request together. That keeps later changes to anyone's knowledge visible.
   */
  public List<ResolvedMention> resolve(Collection<Mention> mentions) {
    if (mentions.isEmpty()) {
      return List.of();
    }
    Index current = currentIndex();
    Map<String, List<String>> knowledgeCache = new HashMap<>();

    List<ResolvedMention> resolved = new ArrayList<>();
    for (Mention mention : mentions) {
      if (mention.kind() == MentionKind.USER) {
        String label = current.userLabels().getOrDefault(key(mention.name()), mention.name());
        resolved.add(new ResolvedMention(
            mention.kind(), mention.name(), label, List.of(mention.name())));
      } else {
        List<String> users = knowledgeCache.computeIfAbsent(mention.name(), this::usernamesKnowing);
        String label = current.metamodelLabels().getOrDefault(mention.name(), mention.name());
        resolved.add(new ResolvedMention(mention.kind(), mention.name(), label, users));
      }
    }
    return resolved;
  }

  private Index currentIndex() {
    Index snapshot = index;
    if (snapshot == null
        || System.nanoTime() - snapshot.builtAtNanos() > METAMODEL_REFRESH_NANOS) {
      snapshot = buildIndex();
      index = snapshot;
    }
    return snapshot;
  }

  private Index buildIndex() {
    Map<String, Mention> candidates = new HashMap<>();
    Map<String, String> userLabels = new HashMap<>();
    for (AppUser user : appUserRepo.findAllByOrderByUsernameAsc()) {
      String userKey = key(user.getUsername());
      candidates.putIfAbsent(userKey, new Mention(MentionKind.USER, user.getUsername()));
      String displayName = user.getDisplayName();
      boolean hasDisplayName = displayName != null && !displayName.isBlank();
      userLabels.put(userKey, hasDisplayName ? displayName : user.getUsername());
    }
    // Usernames cannot take a metamodel's name when they are created. If a metamodel
    // appears later under an existing username, the user keeps the name, because
    // putIfAbsent never replaces it.
    Map<String, String> metamodelLabels = new HashMap<>();
    for (KnowledgeMetamodel metamodel : knowledgeMetamodelCatalog.listAvailable()) {
      candidates.putIfAbsent(
          key(metamodel.name()), new Mention(MentionKind.METAMODEL, metamodel.name()));
      metamodelLabels.put(metamodel.name(), metamodel.label());
    }

    Pattern pattern = null;
    if (!candidates.isEmpty()) {
      // Longest names first, so "@ada.lovelace" is not read as "@ada".
      List<String> names = new ArrayList<>(candidates.keySet());
      names.sort(Comparator.comparingInt(String::length).reversed()
          .thenComparing(Comparator.naturalOrder()));
      StringBuilder alternatives = new StringBuilder();
      for (String name : names) {
        if (!alternatives.isEmpty()) {
          alternatives.append('|');
        }
        alternatives.append(Pattern.quote(name));
      }
      pattern = Pattern.compile(
          BEFORE_TAG + alternatives + AFTER_TAG,
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
      );
    }
    return new Index(candidates, userLabels, metamodelLabels, pattern, System.nanoTime());
  }

  /** Usernames of everyone who knows the metamodel, sorted by username. */
  public List<String> usernamesKnowing(String metamodelName) {
    return appUserRepo.findAllKnowingMetamodel(metamodelName).stream()
        .map(AppUser::getUsername)
        .toList();
  }

  private static String key(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }
}
