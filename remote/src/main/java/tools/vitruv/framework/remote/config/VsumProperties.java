package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File system settings for VSUM providers and storage bound from {@code app.vsums}.
 *
 * @param vsumProvidersDir directory containing the VSUM providers
 * @param vsumProviderJar file name of a provider's JAR
 * @param vsumProviderLibDirName name of a provider's library directory
 * @param vsumStorageDir directory where VSUMs are stored
 */
@ConfigurationProperties(prefix = "app.vsums")
public record VsumProperties(
    String vsumProvidersDir,
    String vsumProviderJar,
    String vsumProviderLibDirName,
    String vsumStorageDir) {}
