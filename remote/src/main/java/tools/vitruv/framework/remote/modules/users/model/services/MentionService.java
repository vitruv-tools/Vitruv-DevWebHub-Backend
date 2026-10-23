package tools.vitruv.framework.remote.modules.users.model.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUser;
import tools.vitruv.framework.remote.modules.users.model.entities.AppUserRepo;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds {@code @tags} in comment text and resolves them to users.
 * <p>
 * A tag is only recognised when it matches a real username or a known metamodel name,
 * so {@code @} in an email address or an unknown word is ignored. Matching ignores case.
 * When a username and a metamodel share a name, the user wins. A tag on a metamodel
 * stands for every user who knows that metamodel.
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

  private final AppUserRepo appUserRepo;
  private final KnowledgeMetamodelCatalog knowledgeMetamodelCatalog;

  /**
   * Tags in {@code text}, in order of first appearance, without duplicates.
   */
  public List<Mention> parse(String text) {
    if (text == null || text.indexOf('@') < 0) {
      return List.of();
    }
    Map<String, Mention> candidates = new HashMap<>();
    for (AppUser user : appUserRepo.findAllByOrderByUsernameAsc()) {
      candidates.putIfAbsent(key(user.getUsername()), new Mention(MentionKind.USER, user.getUsername()));
    }
    for (KnowledgeMetamodel metamodel : knowledgeMetamodelCatalog.listAvailable()) {
      candidates.putIfAbsent(key(metamodel.name()), new Mention(MentionKind.METAMODEL, metamodel.name()));
    }
    if (candidates.isEmpty()) {
      return List.of();
    }

    // Longest names first, so "@ada.lovelace" is not read as "@ada".
    List<String> names = new ArrayList<>(candidates.keySet());
    names.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
    StringBuilder alternatives = new StringBuilder();
    for (String name : names) {
      if (!alternatives.isEmpty()) {
        alternatives.append('|');
      }
      alternatives.append(Pattern.quote(name));
    }
    Pattern pattern = Pattern.compile(
        "(?<![\\p{L}\\p{N}_@])@(" + alternatives + ")(?![\\p{L}\\p{N}_])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    Set<Mention> found = new LinkedHashSet<>();
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      Mention mention = candidates.get(key(matcher.group(1)));
      if (mention != null) {
        found.add(mention);
      }
    }
    return new ArrayList<>(found);
  }

  /**
   * Adds labels and the users each tag reaches. Reflects current metamodel knowledge.
   */
  public List<ResolvedMention> resolve(Collection<Mention> mentions) {
    if (mentions.isEmpty()) {
      return List.of();
    }
    Map<String, AppUser> usersByName = new HashMap<>();
    for (AppUser user : appUserRepo.findAllByOrderByUsernameAsc()) {
      usersByName.put(key(user.getUsername()), user);
    }
    Map<String, String> metamodelLabels = new HashMap<>();
    for (KnowledgeMetamodel metamodel : knowledgeMetamodelCatalog.listAvailable()) {
      metamodelLabels.put(metamodel.name(), metamodel.label());
    }
    Map<String, List<String>> knowledgeCache = new LinkedHashMap<>();

    List<ResolvedMention> resolved = new ArrayList<>();
    for (Mention mention : mentions) {
      if (mention.kind() == MentionKind.USER) {
        AppUser user = usersByName.get(key(mention.name()));
        String label = user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank()
            ? user.getDisplayName()
            : mention.name();
        resolved.add(new ResolvedMention(mention.kind(), mention.name(), label, List.of(mention.name())));
      } else {
        List<String> users = knowledgeCache.computeIfAbsent(mention.name(), this::usernamesKnowing);
        String label = metamodelLabels.getOrDefault(mention.name(), mention.name());
        resolved.add(new ResolvedMention(mention.kind(), mention.name(), label, users));
      }
    }
    return resolved;
  }

  /** Usernames of everyone who knows the metamodel, sorted by username. */
  public List<String> usernamesKnowing(String metamodelName) {
    return appUserRepo.findDistinctByMetamodels_MetamodelNameOrderByUsernameAsc(metamodelName).stream()
        .map(AppUser::getUsername)
        .toList();
  }

  private static String key(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }
}
