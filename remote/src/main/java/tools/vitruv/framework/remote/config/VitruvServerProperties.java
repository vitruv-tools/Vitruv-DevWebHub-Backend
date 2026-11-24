package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the Vitruvius server bound from {@code app.vitruv-server}.
 *
 * @param url the URL of the Vitruvius server
 */
@ConfigurationProperties(prefix = "app.vitruv-server")
public record VitruvServerProperties(String url) {}
