package io.github.org4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

class RequiredSettingsTest {

  @Configuration(proxyBeanMethods = false)
  static class App {}

  private static final String FILE = "--spring.config.location=classpath:/required.yaml";

  @Test
  void aMissingSettingStopsStartup() {
    assertThatThrownBy(
            () -> new SpringApplicationBuilder(App.class).web(WebApplicationType.NONE).run(FILE))
        .hasStackTraceContaining("demo.required")
        .hasStackTraceContaining("DEMO_REQUIRED");
  }

  @Test
  void aGivenSettingStarts() {
    try (var context =
        new SpringApplicationBuilder(App.class)
            .web(WebApplicationType.NONE)
            .run(FILE, "--DEMO_REQUIRED=set")) {
      assertThat(context.getEnvironment().getProperty("demo.required")).isEqualTo("set");
    }
  }

  @Test
  void everyMissingSettingIsNamedOnce() {
    assertThatThrownBy(
            () ->
                new SpringApplicationBuilder(App.class)
                    .web(WebApplicationType.NONE)
                    .run(FILE + ",classpath:/required-too.yaml"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEMO_REQUIRED")
        .hasMessageContaining("DEMO_ALSO")
        .satisfies(
            failure -> assertThat(failure.getMessage().split("demo.required:", -1)).hasSize(2));
  }

  @Test
  void anEmptyValueIsAValue() {
    try (var context =
        new SpringApplicationBuilder(App.class)
            .web(WebApplicationType.NONE)
            .run(FILE, "--DEMO_REQUIRED=")) {
      assertThat(context.getEnvironment().getProperty("demo.required")).isEmpty();
    }
  }
}
