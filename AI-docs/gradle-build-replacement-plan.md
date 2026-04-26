# Gradle Build Replacement Plan

Date: 2026-04-26  
Repository: `/Users/jhrg/src/hyrax/olfs`

## Goal

Replace the production Ant `build.xml` path with a Gradle build that can serve as the release authority for OLFS/Hyrax, NGAP, robots/sitemap, distributions, focused checks, and helper artifacts.

The replacement Gradle build must preserve Ant behavior before it modernizes it. The initial success criterion is parity with `build.xml`, not a cleaner dependency graph or a broader test strategy.

## Phase 1: Candidate Analysis And Starting Point

### Ant Build Authority

`build.xml` is still the authoritative production build. It provides these main behaviors:

- `server` / `opendap`: builds `build/dist/opendap.war`.
- `check` / `test`: runs the focused JUnit 4 suite used by Ant.
- `DISTRO`: builds source, server, and robots distribution bundles.
- `robots`: builds standalone `build/dist/ROOT.war`.
- `hyrax-robots` and `hyrax-robots-dist`: builds and packages both Hyrax and robots WARs.
- `ngap` and `ngap-dist`: builds and packages `ngap.war`.
- `hexEncoderApp`, `validatorApp`, and `XSLTransformer`: builds helper jar artifacts.
- `show`: prints build settings.

Important Ant build mechanics that Gradle must match:

- Java source and target are `1.8`.
- Source is copied to `build/src` and filtered before compilation.
- `src/opendap/bes/Version.java` intentionally contains Ant-style tokens in the checked-in source.
- Main Hyrax resources are copied and filtered to `build/resources`.
- WCS 2.0 resources are merged into the Hyrax resource tree, excluding WCS `web.xml`, `urlrewrite.xml`, and logback files.
- Hyrax WAR libraries come from the checked-in `lib/` jar set, while Tomcat `catalina` and `servlet-api` jars are compile-time only.
- NGAP copies Hyrax libs plus selected `resources/ngap/lib` jars, excluding container-level session/cache jars from `WEB-INF/lib`.
- Distribution names and version properties are driven by `HYRAX_VERSION`, `OLFS_VERSION`, `NGAP_VERSION`, `WCS_VERSION`, `OLFS_DIST_BASE`, `NGAP_DIST_BASE`, and deployment context properties.

### `build.gradle`

The current `build.gradle` is the best starting point, but it is not yet a drop-in Ant replacement.

Strengths:

- Uses the active Gradle plugins: `java`, `war`, IDE plugins, and Sonar.
- Has current Sonar integration used by CI.
- Knows about the Gradle wrapper-era workflow documented in `README.md`.
- Carries many newer dependency versions reflected in current checked-in jars.
- Uses local `lib/` jars where repository artifacts are not reliable, especially Saxon.
- Has some custom resource filtering tasks and early multi-WAR task names.
- Has version properties and a `showInfo` task concept.

Problems to fix before it can replace Ant:

- It targets Java `1.9`, while Ant production compiles `1.8`.
- It compiles directly from `src`, so `Version.java` token replacement is not equivalent to Ant.
- `clean` deletes `src/opendap/bes/Version.java`, which is source mutation and must be removed.
- `substituteVersionInfo` expects `*.java.in` templates, but no active `*.java.in` files were found.
- Dependency declarations diverge from Ant and checked-in jars, including duplicate or mismatched `gson`, `commons-codec`, `commons-io`, `httpclient`, `jackson-databind`, and cloudwatch appender entries.
- `showInfo` prints during task configuration instead of task execution.
- WAR tasks do not yet match Ant output names, web descriptors, library sets, resource exclusions, or NGAP/robots layout.
- The Gradle `test` source set appears unlikely to include tests as written because `srcDirs = ['src']` is paired with `include 'src/**/*Test.java'`.
- NGAP dependency handling is present but not equivalent to Ant's checked-in `resources/ngap/lib` behavior.

### `ant_convert_build.gradle`

`ant_convert_build.gradle` should be treated as historical conversion scaffolding, not the implementation base.

Useful references:

- It models an Ant-like `PreProcessSourceCode` task.
- It copies sources into a generated tree rather than compiling source in place.
- It shows an early attempt at resource staging and Ant-style `server`.

Reasons not to use it as the starting point:

- It is smaller and older than `build.gradle`.
- It depends on `src_gradle` as a generated source/webapp tree.
- It uses obsolete or removed Gradle idioms such as `archiveName` and `compile`.
- It depends on repositories such as `jcenter()`.
- Its dependency versions are farther from the current Ant checked-in jars.
- It lacks current Sonar work and most later Gradle task exploration.
- Several Ant exclusions are commented out or incomplete.

### Recommendation

Use the current `build.gradle` as the migration base, but rewrite the production build sections around Ant parity.

Do not start a new long-lived third build file. A temporary comparison file or branch is reasonable during implementation, but the final replacement should be `build.gradle` so CI, README guidance, Gradle wrapper usage, Sonar, and developer muscle memory converge on one Gradle entry point.

Keep `ant_convert_build.gradle` only as a reference until its useful ideas are absorbed. Once Gradle reaches parity, either remove it or move its historical notes into `AI-docs/`.

## Phase 2: Define Gradle Parity Invariants

Before editing task behavior, encode the invariants the Gradle build must preserve:

- Compile with Java source and target `1.8`.
- Never edit or delete checked-in source during Gradle tasks.
- Stage filtered Java source under `build/src` or another generated directory and compile from that staged tree.
- Keep Ant-compatible property names and command-line overrides, including `-PHYRAX_VERSION=...` and `-POLFS_VERSION=...`.
- Use checked-in `lib/` jars as the production WAR authority until maintainers intentionally move the release build to Maven-resolved dependencies.
- Keep Tomcat jars on the compile classpath but out of deployed WAR libraries.
- Keep build outputs in Ant-compatible locations, especially `build/dist`.
- Preserve Ant task names or provide exact aliases where users and CI expect them.

## Phase 3: Rebuild Source And Resource Staging

Implement Gradle tasks equivalent to Ant preprocessing:

- Copy active Java sources from `src/` to generated source, applying token replacement.
- Include `com/**`, `opendap/**`, and `org/opendap/**`.
- Exclude `opendap/cmr/**`, `opendap/wcs/v1_1_2/**`, `opendap/semantics/**`, `opendap/aws/**`, and `opendap/async/**`.
- Copy and filter `resources/hyrax` into generated resources.
- Merge `resources/WCS/2.0` `xsl/**` and `WEB-INF/**` into generated resources with the same Ant exclusions.
- Copy and filter `resources/robots`.
- Copy and filter `resources/ngap`, excluding `*.png` and `lib/**`.

Open item: Ant defines `WCS_SOFTWARE_VERSION`, while active WCS 2.0 XSL files use `@WcsSoftwareVersion@`. Current Gradle uses `WcsSoftwareVersion`. This mismatch needs a small reproduction check before locking the replacement behavior.

## Phase 4: Dependencies And Classpaths

Create explicit Gradle file collections for Ant-equivalent dependency sets:

- `hyraxLibs`: exact jars included by the Ant `hyrax-libs` fileset.
- `providedTomcatLibs`: `catalina-6.0.53.jar` and `servlet-api-3.0.jar`.
- `ngapWarLibs`: staged Hyrax libs plus selected `resources/ngap/lib` jars with Ant's container-level exclusions.
- Optional analysis-only Maven dependencies can remain separate from production packaging if needed for Snyk or Sonar.

This phase should also remove duplicate and version-divergent production dependencies from the WAR path unless they are intentionally confirmed.

## Phase 5: WAR Tasks

Implement Ant-compatible Gradle WAR tasks:

- `server`: cleanly build `build/dist/opendap.war`.
- `opendap`: alias `server`.
- `robots`: build `build/dist/ROOT.war`, including the robots `robots.jsp` to `robots.txt` behavior.
- `hyraxRobots` and `hyrax-robots`: build both WARs.
- `ngap`: build `build/dist/ngap.war` or `${NGAP_DEPLOYMENT_CONTEXT}.war`.

Each WAR task must set the same `web.xml`, classes, libraries, docs, resource includes, and resource exclusions as Ant.

## Phase 6: Distribution And Helper Artifacts

Implement the remaining release-oriented tasks:

- `srcDist` and `src-dist`.
- `serverDist` and `server-dist`.
- `hyraxRobotsDist` and `hyrax-robots-dist`.
- `DISTRO`.
- `ngapDist` and `ngap-dist`.
- `hexEncoderApp`.
- `validatorApp`.
- `XSLTransformer`.

The `.tgz` contents and prefixes should be compared against Ant outputs with versioned build properties.

## Phase 7: Checks, CI, And Documentation

Recreate Ant `check` first, then broaden only after parity:

- Configure JUnit 4 execution for the same focused classes Ant runs.
- Keep `test` as an alias for `check` if maintainers still expect that workflow.
- Make Sonar consume the Gradle-compiled classes and the same source/library scopes.
- Update README build guidance only after parity is demonstrated.
- Update CI/release scripts after `server`, `DISTRO`, `ngap-dist`, and `check` pass in Gradle.

## Phase 8: Verification Matrix

Minimum command matrix before declaring Gradle a replacement:

```bash
ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server
ant check
ant DISTRO
ant ngap
ant ngap-dist
ant robots
ant hyrax-robots

./gradlew -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build showInfo server
./gradlew check
./gradlew DISTRO
./gradlew ngap
./gradlew ngap-dist
./gradlew robots
./gradlew hyrax-robots
```

Artifact comparisons:

- Compare WAR file lists for Ant and Gradle outputs, ignoring timestamps and generated metadata.
- Compare `WEB-INF/lib` exactly for each WAR.
- Confirm `opendap.bes.Version` reports substituted version strings.
- Confirm web descriptors and URL rewrite files come from the expected resource trees.
- Confirm docs are present and `doc/src.distribution.readme` is excluded.
- Confirm NGAP excludes container-managed session/cache jars from `WEB-INF/lib`.

## Current Decision

Proceed from `build.gradle`, not `ant_convert_build.gradle`, and avoid a new canonical third build file. The work should be implemented as an Ant-parity rewrite of the existing Gradle production tasks, with small, verifiable phases.
