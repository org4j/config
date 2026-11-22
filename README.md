# config

Configuration every org4j library shares: each library's own settings and error definitions load
without the app importing them, and a setting left unset stops startup. Only Spring Boot's core,
so every library can depend on it. No properties, errors or beans of its own.

```groovy
runtimeOnly libs.config
```

## Library defaults

A library lists its YAML files, one per line, in
`src/main/resources/META-INF/spring/io.github.org4j.config.LibraryDefaults.imports`. The format is
Boot's `AutoConfiguration.imports`, `#` comments allowed. The library ships no Java for it.

```
# META-INF/spring/io.github.org4j.config.LibraryDefaults.imports
properties/otp-exception.yaml
```

`LibraryDefaults` runs right after Boot's config data and adds each file last, in the order listed.

| Case | Result |
|---|---|
| the app's files, profiles, `spring.config.import`, environment variables, arguments | override the library's file |
| a parent and a child context | the file is added once |
| a listed file that isn't on the classpath | startup stops, naming it |
| a file with more than one YAML document (`---`) | startup stops: `spring.config.activate` would not be honoured here |

## Required settings

No setting has a default. A `${VAR}` in any configuration file, the app's or a library's, that
nothing resolves stops startup once the context is prepared (`RequiredSettings`), naming every one
missing, each once. Without it, Spring's binder would bind the placeholder's literal text.

| Value | Counts |
|---|---|
| an environment variable, an argument, a test's `@DynamicPropertySource` | yes |
| empty (`DEMO_REQUIRED=`) | yes: an empty value is a value |
| nothing | no: startup stops |

## Tests

Unit tests only (`./gradlew test`).
- `LibraryDefaultsTest`: a test `.imports` file, overridden by a config file and by an argument,
  and added once to a parent and child. Files on a throwaway classpath: listing order, a missing
  file, a file with several documents.
- `RequiredSettingsTest`: a missing setting stops startup, every missing one is named once, and
  given and empty values start.

Each library's integration test boots against its loaded values.
