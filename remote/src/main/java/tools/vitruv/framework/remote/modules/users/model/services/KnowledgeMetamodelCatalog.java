package tools.vitruv.framework.remote.modules.users.model.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.config.VsumProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Individual metamodels a user can claim knowledge of.
 * Names come from the {@code name} attribute of each provider's {@code ecore-models/*.ecore} package,
 * not from the provider folder (for example {@code amalthea} and {@code ascet}, not {@code AmaltheaAscet}).
 * Package lists are cached until an {@code .ecore} file in that folder changes size or last-modified time.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeMetamodelCatalog {

  private static final Pattern PACKAGE_NAME = Pattern.compile(
      "<ecore:EPackage\\b[^>]*\\sname=\"([^\"]+)\""
  );

  private final VsumProperties vsumProperties;
  private final ConcurrentHashMap<Path, CachedPackages> packageCache = new ConcurrentHashMap<>();

  private record CachedPackages(long stamp, List<String> names) {
  }

  public record KnowledgeMetamodel(String name, String label) {
  }

  public List<KnowledgeMetamodel> listAvailable() {
    return listAvailable(Path.of(vsumProperties.vsumProvidersDir()));
  }

  public List<String> packageNamesForProvider(String providerName) {
    if (providerName == null || providerName.isBlank()) {
      return List.of();
    }
    Path ecoreDir = Path.of(vsumProperties.vsumProvidersDir())
        .resolve(providerName)
        .resolve("ecore-models");
    return cachedPackageNames(ecoreDir);
  }

  /**
   * Canonical catalog name for a requested name, or null when it is not a known package.
   */
  public String canonicalName(String requested) {
    if (requested == null || requested.isBlank()) {
      return null;
    }
    String normalized = requested.trim().toLowerCase(Locale.ROOT);
    return listAvailable().stream()
        .map(KnowledgeMetamodel::name)
        .filter(name -> name.equals(normalized))
        .findFirst()
        .orElse(null);
  }

  List<KnowledgeMetamodel> listAvailable(Path providersDir) {
    LinkedHashMap<String, KnowledgeMetamodel> byName = new LinkedHashMap<>();
    if (!Files.isDirectory(providersDir)) {
      return List.of();
    }
    try (Stream<Path> providers = Files.list(providersDir)) {
      providers.filter(Files::isDirectory)
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .forEach(provider -> {
            for (String name : cachedPackageNames(provider.resolve("ecore-models"))) {
              byName.putIfAbsent(name, new KnowledgeMetamodel(name, labelFor(name)));
            }
          });
    } catch (IOException e) {
      return List.of();
    }
    return new ArrayList<>(byName.values());
  }

  static String readPackageName(String xml) {
    Matcher matcher = PACKAGE_NAME.matcher(xml);
    if (!matcher.find()) {
      return null;
    }
    String name = matcher.group(1).trim().toLowerCase(Locale.ROOT);
    return name.isEmpty() ? null : name;
  }

  static String labelFor(String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  private List<String> cachedPackageNames(Path ecoreDir) {
    Path key;
    try {
      key = ecoreDir.toAbsolutePath().normalize();
    } catch (RuntimeException e) {
      key = ecoreDir;
    }
    long stamp = directoryStamp(key);
    CachedPackages cached = packageCache.get(key);
    if (cached != null && stamp != -1L && cached.stamp() == stamp) {
      return cached.names();
    }
    List<String> names = List.copyOf(readPackageNames(key));
    if (stamp != -1L) {
      packageCache.put(key, new CachedPackages(stamp, names));
    }
    return names;
  }

  /**
   * Changes when an ecore file is added, removed, or rewritten, which refreshes the cache.
   */
  private static long directoryStamp(Path ecoreDir) {
    if (!Files.isDirectory(ecoreDir)) {
      return 0L;
    }
    long stamp = 17L;
    try (Stream<Path> files = Files.list(ecoreDir)) {
      List<Path> ecoreFiles = files.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".ecore"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
      for (Path file : ecoreFiles) {
        stamp = stamp * 31 + file.getFileName().toString().hashCode();
        stamp = stamp * 31 + Files.getLastModifiedTime(file).toMillis();
        stamp = stamp * 31 + Files.size(file);
      }
      return stamp;
    } catch (IOException e) {
      return -1L;
    }
  }

  private static List<String> readPackageNames(Path ecoreDir) {
    if (!Files.isDirectory(ecoreDir)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(ecoreDir)) {
      return files.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".ecore"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .map(KnowledgeMetamodelCatalog::readPackageNameFromFile)
          .filter(name -> name != null && !name.isBlank())
          .distinct()
          .toList();
    } catch (IOException e) {
      return List.of();
    }
  }

  private static String readPackageNameFromFile(Path ecoreFile) {
    try {
      return readPackageName(Files.readString(ecoreFile));
    } catch (IOException e) {
      return null;
    }
  }
}
