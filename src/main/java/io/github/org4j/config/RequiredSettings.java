package io.github.org4j.config;

import java.util.LinkedHashSet;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationListener;

/**
 * Stops startup when a {@code ${VAR}} in any configuration file, the app's or a library's, is left
 * unresolved, naming every one missing. Spring's binder would otherwise bind the placeholder's
 * literal text. A value set any other way, including a test's {@code @DynamicPropertySource},
 * counts; an empty value is a value.
 */
public class RequiredSettings implements ApplicationListener<ApplicationPreparedEvent> {

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent event) {
    var environment = event.getApplicationContext().getEnvironment();
    var missing = new LinkedHashSet<String>();
    for (var source : environment.getPropertySources()) {
      if (source instanceof OriginTrackedMapPropertySource file) {
        for (var name : file.getPropertyNames()) {
          try {
            environment.getProperty(name);
          } catch (IllegalArgumentException unresolved) {
            missing.add(name + ": " + unresolved.getMessage());
          }
        }
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Required settings are missing; set them as environment variables:\n  "
              + String.join("\n  ", missing));
    }
  }
}
