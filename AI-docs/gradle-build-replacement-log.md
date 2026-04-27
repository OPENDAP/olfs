# Gradle Build Replacement Log

Date: 2026-04-26  
Repository: `/Users/jhrg/src/hyrax/olfs`

## Request

Create a plan toward a Gradle build file that can replace Ant `build.xml`, using `README.md` and `AI-docs/codex-deep-dive.md`. The first phase needed to compare `build.gradle`, `ant_convert_build.gradle`, and `build.xml` and decide whether to start from one of them or create a third file.

## Files Inspected

- `README.md`
- `AI-docs/codex-deep-dive.md`
- `build.xml`
- `build.gradle`
- `ant_convert_build.gradle`
- `.travis.yml`
- `build-for-snyk.gradle`
- `lib/`
- `resources/ngap/lib/`
- active source/resource token usages under `src/` and `resources/`

I did not inspect or modify `retired/`.

## Commands Run

```bash
rg --files -g '!*retired*'
rg -n '<target|<property|<war|<javac|<copy|<filter' build.xml
rg -n 'plugins|dependencies|sourceSets|war|tasks.register' build.gradle ant_convert_build.gradle
rg -n '@HyraxVersion@|@OlfsVersion@|@SERVICE_CONTEXT@|@WCS_SOFTWARE_VERSION@|@WcsSoftwareVersion@|HyraxVersion|OlfsVersion|SERVICE_CONTEXT|WCS_SOFTWARE_VERSION|WcsSoftwareVersion' src resources -g '!retired/**'
rg --files lib resources/ngap/lib
sed -n '90,210p' .travis.yml
ant -p
git status --short
```

`git status --short` showed an existing modified `AGENTS.md`; I left it untouched.

## Reasoning Notes

Ant remains the production authority because it owns the release targets used by CI and snapshot packaging: `server`, `DISTRO`, `ngap-dist`, `robots`, and `check`.

The current `build.gradle` is materially closer to a real replacement than `ant_convert_build.gradle` because it has current Sonar integration, Gradle wrapper-era assumptions, newer dependency work, local Saxon handling, and early multi-WAR tasks. It still needs serious correction before production use.

`ant_convert_build.gradle` is useful mainly as a record of an older conversion approach. Its generated source/webapp staging idea is valuable, but the file itself has older repository usage, older Gradle idioms, stale dependencies, and incomplete Ant exclusions.

I recommended against a new long-lived third build file because the final project should converge on `build.gradle`. A temporary implementation branch or scratch file could still be useful, but it should not become another build path to keep synchronized.

The largest technical risks are version token filtering, exact WAR composition, dependency parity with checked-in jars, Java `1.8` compatibility, and NGAP library layout.

One discrepancy needs explicit validation: Ant defines a `WCS_SOFTWARE_VERSION` filter token, while active WCS 2.0 XSL files contain `@WcsSoftwareVersion@`. The current `build.gradle` uses `WcsSoftwareVersion`. This may be an existing Ant gap or historical drift.

## Output

Created `AI-docs/gradle-build-replacement-plan.md` with the candidate comparison, starting-point recommendation, phased migration plan, and verification matrix.

## Phase 3 Implementation

Date: 2026-04-26

### Goal

Implement the Phase 3 bootstrap and staging foundation:

- make the Gradle wrapper/build script configure on a current Gradle/JDK stack
- move `showInfo` output to task execution time
- remove source-mutation behavior from Gradle cleanup/preprocessing
- stage filtered Java and resource trees under `build/`
- explicitly handle the live WCS token form `@WcsSoftwareVersion@`

### Files Changed

- `build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`

### Commands Run

```bash
ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build PreProcessSourceCode
sed -n '1,120p' src/opendap/bes/Version.java
sed -n '1,120p' resources/WCS/2.0/xsl/capabilities.xsl
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew help --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew tasks --all --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew showInfo --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew showInfo -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew PreProcessSourceCode -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build --no-daemon
sed -n '35,55p' build/src/opendap/bes/Version.java
sed -n '40,55p' build/resources/xsl/capabilities.xsl
find build/src/opendap -maxdepth 3 \( -path 'build/src/opendap/cmr' -o -path 'build/src/opendap/wcs/v1_1_2' -o -path 'build/src/opendap/semantics' -o -path 'build/src/opendap/aws' -o -path 'build/src/opendap/async' \)
find build/ngap -maxdepth 2 -type f | sort
```

### Reasoning Notes

The original `build.gradle` was too entangled to safely patch in place for this phase. It mixed obsolete preprocessing (`*.java.in`), destructive cleanup of checked-in source, Java 1.9 compilation settings, configuration-time logging, and older task experiments that were not a reliable base for parity work.

I replaced it with a smaller Phase 3 foundation that keeps `build.gradle` as the canonical file while focusing only on bootstrap and staging:

- Java compile semantics are now `1.8`.
- `showInfo` now prints only during task execution.
- `clean` no longer deletes checked-in source.
- `PreProcessSourceCode` stages filtered source and resources into `build/src`, `build/resources`, `build/robots`, and `build/ngap`.
- Source staging matches Ant's include/exclude rules for `com/**`, `opendap/**`, and `org/opendap/**`.
- WCS staging now replaces both `WCS_SOFTWARE_VERSION` and `WcsSoftwareVersion`.

I also updated the wrapper distribution from Gradle `6.5` to `8.14.3`. That resolves the repo-level incompatibility between the old wrapper line and the current Sonar plugin/JDK expectations.

One local environment issue surfaced during validation: this machine's `JAVA_HOME` is set to an old JDK 8 while `java` on `PATH` is a newer JDK. The successful validation commands used:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home
```

That is an environment issue, not a repo-tracked build script issue, so I did not change `gradlew` itself in Phase 3.

### Validation Results

- `./gradlew help` succeeded with the updated wrapper/runtime.
- `./gradlew tasks --all` succeeded and listed the new staging tasks plus `showInfo`.
- `./gradlew showInfo` succeeded and no longer prints during configuration.
- `./gradlew showInfo -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build` confirmed Ant-style property overrides.
- `./gradlew PreProcessSourceCode -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build` succeeded.
- `build/src/opendap/bes/Version.java` contains substituted `CI-Build` values.
- `build/resources/xsl/capabilities.xsl` contains substituted `CI-Build` for `WcsSoftwareVersion`.
- excluded source trees such as `opendap/cmr`, `opendap/wcs/v1_1_2`, `opendap/semantics`, `opendap/aws`, and `opendap/async` were not staged.
- `build/ngap` was staged without `lib/**`, while landing-page assets under `landing/` remained present.

### Known Remaining Gaps

This phase intentionally did not implement:

- Ant-parity dependency file collections
- Ant-parity WAR assembly tasks
- distribution bundles
- Ant-parity `check`/`test` behavior
- README updates

Those belong to Phases 4 through 7.

## Phase 4 Implementation

Date: 2026-04-26

### Goal

Implement explicit Ant-parity dependency and classpath sets for:

- Hyrax production WAR/runtime jars
- Tomcat-provided compile-only jars
- NGAP WAR library assembly inputs

### Files Changed

- `build.gradle`

### Commands Run

```bash
rg -n "<property name=\".*\\.lib\"|<fileset id=\"hyrax-libs\"|olfs.compile.classpath|catalina.lib|servlet-api.lib|xalan|xerces|cloudwatch|commons-httpclient|gson|jackson|redisson|memcached|elasticache" build.xml
sed -n '240,390p' build.xml
ls lib | sort
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew showInfo --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew showDependencySets verifyDependencyLayout --no-daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew compileJava -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build --no-daemon
```

### Reasoning Notes

Phase 3 still used a broad `fileTree(dir: 'lib', include: ['*.jar'])` minus Tomcat jars. That was too permissive and would have produced the wrong library surface for WAR packaging in Phase 5.

I replaced that with explicit Ant-aligned dependency lists:

- `hyraxLibNames`: the exact jars declared by Ant's `hyrax-libs` fileset
- `providedTomcatLibNames`: `catalina-6.0.53.jar` and `servlet-api-3.0.jar`
- `ngapContainerExcludedLibNames`: the NGAP jars Ant intentionally keeps out of the WAR because the containers place them in Tomcat's shared `lib`

I then mapped those into three explicit Gradle configurations:

- `hyraxLibs`
- `providedTomcatLibs`
- `ngapWarLibs`

Compilation now uses:

- `implementation(hyraxLibFiles)`
- `compileOnly(providedTomcatLibFiles)`

This is closer to Ant's `olfs.compile.classpath`, which is `build.classes + providedTomcatLibs + hyrax-libs`.

For NGAP, `ngapWarLibs` is defined as:

- all Hyrax jars
- plus `resources/ngap/lib/*.jar`
- minus the container-managed exclusions

That preserves Ant behavior even where it includes duplicate or older jars in the NGAP library directory. I did not “clean up” those duplicates because this phase is about parity, not modernization.

I also added:

- `showDependencySets` for a readable dump of the resolved file collections
- `verifyDependencyLayout` to fail fast if the sets drift away from Ant parity

### Validation Results

- `showInfo` reported:
  - `hyrax-libs: 45 jars`
  - `providedTomcatLibs: 2 jars`
  - `ngapWarLibs: 56 jars`
- `showDependencySets` listed the exact resolved file names for each set.
- `verifyDependencyLayout` passed.
- `compileJava -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build` passed.

The compile succeeded with only deprecation and Java 8 obsolescence warnings under the newer JDK, which is expected for this codebase and toolchain.

### Known Remaining Gaps

This phase intentionally did not implement:

- Ant-parity WAR assembly and naming
- robots/NGAP/web descriptor packaging behavior
- distribution bundles
- Ant-style `check` / `test`
- Sonar classpath/source-set reconciliation
