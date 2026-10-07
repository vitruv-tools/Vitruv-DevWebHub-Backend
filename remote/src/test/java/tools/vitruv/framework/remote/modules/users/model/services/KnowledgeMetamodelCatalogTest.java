package tools.vitruv.framework.remote.modules.users.model.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.remote.modules.users.model.services.KnowledgeMetamodelCatalog.KnowledgeMetamodel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeMetamodelCatalogTest {

  @Test
  void readsPackageNameFromEcore() {
    String xml = """
        <ecore:EPackage name="amalthea" nsURI="http://example">
        </ecore:EPackage>
        """;
    assertEquals("amalthea", KnowledgeMetamodelCatalog.readPackageName(xml));
    assertEquals("Amalthea", KnowledgeMetamodelCatalog.labelFor("amalthea"));
    assertNull(KnowledgeMetamodelCatalog.readPackageName("<not-a-package/>"));
  }

  @Test
  void listsIndividualPackagesNotProviderFolders(@TempDir Path providers) throws Exception {
    writeEcore(providers, "AmaltheaAscet", "amalthea.ecore", "amalthea");
    writeEcore(providers, "AmaltheaAscet", "ascet.ecore", "ascet");
    writeEcore(providers, "SystemRootVsum", "model.ecore", "model");

    KnowledgeMetamodelCatalog catalog = new KnowledgeMetamodelCatalog(null);
    List<KnowledgeMetamodel> found = catalog.listAvailable(providers);

    assertEquals(List.of("amalthea", "ascet", "model"), found.stream().map(KnowledgeMetamodel::name).toList());
    assertEquals("Ascet", found.get(1).label());
  }

  private static void writeEcore(Path providers, String provider, String file, String packageName) throws Exception {
    Path dir = providers.resolve(provider).resolve("ecore-models");
    Files.createDirectories(dir);
    Files.writeString(dir.resolve(file), "<ecore:EPackage name=\"" + packageName + "\"></ecore:EPackage>");
  }
}
