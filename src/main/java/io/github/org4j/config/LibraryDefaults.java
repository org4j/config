package io.github.org4j.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Adds every library's own YAML files below the app's configuration, so no app imports them. A
 * library lists its files in {@code
 * META-INF/spring/io.github.org4j.config.LibraryDefaults.imports}, one per line, and depends on
 * this library at runtime.
 *
 * <p>Runs right after Boot's config data and adds each file last: the app's files, profiles,
 * imports, environment variables and arguments all override it. A file already added, by a parent
 * context, is added once. A listed file that is missing, or that holds more than one YAML document,
 * stops startup: a document's {@code spring.config.activate} would not be honoured here.
 */
public class LibraryDefaults implements EnvironmentPostProcessor, Ordered {

  private final YamlPropertySourceLoader yaml = new YamlPropertySourceLoader();

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    var sources = environment.getPropertySources();
    var classLoader = application.getClassLoader();
    for (var file : ImportCandidates.load(LibraryDefaults.class, classLoader)) {
      if (!sources.contains(file)) {
        sources.addLast(load(file, new ClassPathResource(file, classLoader)));
      }
    }
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }

  private PropertySource<?> load(String file, ClassPathResource resource) {
    if (!resource.exists()) {
      throw new IllegalStateException(
          "Library defaults file %s is listed but not on the classpath".formatted(file));
    }
    try {
      var documents = yaml.load(file, resource);
      if (documents.size() != 1) {
        throw new IllegalStateException(
            "Library defaults file %s must hold one YAML document, not %d"
                .formatted(file, documents.size()));
      }
      return documents.getFirst();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
