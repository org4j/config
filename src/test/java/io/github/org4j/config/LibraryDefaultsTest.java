package io.github.org4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

class LibraryDefaultsTest {

  @Configuration(proxyBeanMethods = false)
  static class App {}

  private static SpringApplicationBuilder app() {
    return new SpringApplicationBuilder(App.class).web(WebApplicationType.NONE);
  }

  @Test
  void theDefaultsLoadWithoutAnImport() {
    try (var context = app().run()) {
      assertThat(context.getEnvironment().getProperty("demo.value")).isEqualTo("library");
    }
  }

  @Test
  void anAppConfigFileOverridesThem() {
    try (var context = app().run("--spring.config.location=classpath:/override.yaml")) {
      assertThat(context.getEnvironment().getProperty("demo.value")).isEqualTo("file");
      assertThat(context.getEnvironment().getProperty("demo.other")).isEqualTo("library");
    }
  }

  @Test
  void aCommandLineArgumentOverridesThem() {
    try (var context = app().run("--demo.other=arg")) {
      assertThat(context.getEnvironment().getProperty("demo.other")).isEqualTo("arg");
    }
  }

  @Test
  void aParentAndChildAddThemOnce() {
    try (var context = app().child(App.class).web(WebApplicationType.NONE).run()) {
      var sources = context.getEnvironment().getPropertySources().stream();
      assertThat(sources.filter(source -> source.getName().contains("test-defaults.yaml")))
          .hasSize(1);
    }
  }

  @Nested
  class ListedFiles {

    @TempDir Path classpath;

    private ConfigurableEnvironment load(String imports, Map<String, String> files)
        throws IOException {
      var listing =
          classpath.resolve("META-INF/spring/" + LibraryDefaults.class.getName() + ".imports");
      Files.createDirectories(listing.getParent());
      Files.writeString(listing, imports);
      for (var file : files.entrySet()) {
        Files.writeString(classpath.resolve(file.getKey()), file.getValue());
      }
      try (var loader = new URLClassLoader(new URL[] {classpath.toUri().toURL()}, null)) {
        var application = new SpringApplication();
        application.setResourceLoader(new DefaultResourceLoader(loader));
        var environment = new StandardEnvironment();
        new LibraryDefaults().postProcessEnvironment(environment, application);
        return environment;
      }
    }

    @Test
    void areAddedInTheirOrderBelowEverythingElse() throws IOException {
      var environment =
          load(
              "# comments are allowed\na.yaml\nb.yaml\n",
              Map.of("a.yaml", "demo:\n  value: a\n", "b.yaml", "demo:\n  value: b\n  b: b\n"));
      assertThat(environment.getProperty("demo.value")).isEqualTo("a");
      assertThat(environment.getProperty("demo.b")).isEqualTo("b");
      var names = environment.getPropertySources().stream().map(PropertySource::getName).toList();
      assertThat(names).endsWith("a.yaml", "b.yaml");
    }

    @Test
    void aMissingOneStopsStartupNamingIt() {
      assertThatThrownBy(() -> load("gone.yaml\n", Map.of()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("gone.yaml");
    }

    @Test
    void oneWithSeveralDocumentsStopsStartup() {
      var profiles =
          "demo:\n  value: a\n---\nspring.config.activate.on-profile: prod\ndemo:\n  value: b\n";
      assertThatThrownBy(() -> load("a.yaml\n", Map.of("a.yaml", profiles)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("a.yaml")
          .hasMessageContaining("one YAML document");
    }
  }
}
