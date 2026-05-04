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

## Phase 5 Implementation

Date: 2026-04-26

### Goal

Implement Ant-parity WAR assembly tasks for:

- Hyrax / OLFS (`server`, `opendap`, `war`)
- robots / sitemap (`robots`)
- combined Hyrax + robots (`hyrax-robots`, `hyraxRobots`)
- NGAP (`ngap`)

### Files Changed

- `build.gradle`

### Commands Run

```bash
sed -n '720,980p' build.xml
find resources/robots -maxdepth 3 -type f | sort
find build/resources -maxdepth 3 -type f | sort
find build/ngap -maxdepth 3 -type f | sort
find build/robots -maxdepth 3 -type f | sort
rg -n "robots.base|robots.jsp|urlrewrite.xml|logback.xml|logback-test.xml" resources/hyrax resources/robots build.xml
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew server robots ngap hyrax-robots --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
jar tf build/dist/opendap.war | rg "^(WEB-INF/web.xml|WEB-INF/lib/|WEB-INF/conf/olfs.xml|docs/|xsl/capabilities.xsl|WEB-INF/urlrewrite.xml)$"
jar tf build/dist/ROOT.war | rg "^(WEB-INF/web.xml|WEB-INF/conf/olfs.xml|robots.txt|robots.jsp|WEB-INF/lib/|docs/)"
jar tf build/dist/ngap.war | rg "^(WEB-INF/web.xml|WEB-INF/conf/olfs.xml|WEB-INF/conf/logback.xml|WEB-INF/urlrewrite.xml|WEB-INF/lib/|docs/ngap/)"
mkdir -p /tmp/phase5-gradle-wars
cp build/dist/opendap.war build/dist/ROOT.war build/dist/ngap.war /tmp/phase5-gradle-wars/
ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build clean server robots ngap
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew server robots ngap hyrax-robots --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
jar tf /tmp/phase5-gradle-wars/ROOT.war | rg "^(robots.txt|robots.jsp|WEB-INF/conf/olfs.xml)$"
jar tf build/dist/ngap.war | rg "^(WEB-INF/web.xml|WEB-INF/conf/olfs.xml|WEB-INF/conf/logback.xml|WEB-INF/urlrewrite.xml|docs/ngap/.*png|WEB-INF/lib/(elasticache|memcached|redisson-all|redisson-tomcat))"
jar tf build/dist/opendap.war | rg "^(WEB-INF/web.xml|WEB-INF/conf/olfs.xml|WEB-INF/urlrewrite.xml|docs/index.html)$"
```

### Reasoning Notes

I configured the built-in Gradle `war` task as the Ant-parity Hyrax WAR and made `server` depend on it. That keeps the standard Gradle `war` path useful while still providing the Ant-compatible entry point.

The Phase 5 changes added:

- `war` -> writes `build/dist/opendap.war`
- `server` -> lifecycle alias for `war`
- `opendap` -> alias for `server`
- `robots` -> writes `build/dist/ROOT.war`
- `hyrax-robots` and `hyraxRobots` -> build both WARs
- `ngap` -> writes `build/dist/ngap.war`

Each task uses the staged resource trees from Phase 3 and the explicit library sets from Phase 4.

Key packaging decisions:

- Hyrax WAR:
  - web descriptor from `build/resources/WEB-INF/web.xml`
  - includes `hyraxLibs`
  - includes all staged Hyrax resources except `WEB-INF/web.xml`
  - includes docs under `docs/`

- robots WAR:
  - web descriptor from `build/robots/WEB-INF/web.xml`
  - includes `hyraxLibs`
  - includes `build/resources/WEB-INF/conf/olfs.xml`
  - packages `robots.jsp` as `robots.txt`
  - excludes `robots.jsp`
  - includes docs under `docs/`

- NGAP WAR:
  - web descriptor from `build/ngap/web.xml`
  - includes `ngapWarLibs`
  - includes staged Hyrax resources except the Ant-excluded Hyrax config/descriptor files
  - adds NGAP `urlrewrite.xml`, `logback.xml`, and `olfs.xml` in their Ant locations
  - includes docs under `docs/`
  - includes NGAP landing assets under `docs/ngap`

Two implementation issues surfaced and were fixed:

1. Generated-tree ownership:
   - `Copy` tasks were leaving stale files in `build/` across phase transitions.
   - I changed the stage-owner tasks to `Sync` so generated trees behave like real build outputs.

2. NGAP duplicate landing PNGs:
   - Ant’s NGAP packaging logic effectively feeds the landing PNGs from both `build/ngap/landing` and `resources/ngap/landing`.
   - Gradle fails on duplicates unless told otherwise.
   - I set `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` for the `ngap` WAR task so the final WAR keeps one copy and the build remains stable.

### Validation Results

Gradle validation succeeded:

- `./gradlew server robots ngap hyrax-robots -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build`

Artifacts produced:

- `build/dist/opendap.war`
- `build/dist/ROOT.war`
- `build/dist/ngap.war`

Key content checks passed:

- `opendap.war` contains:
  - `WEB-INF/web.xml`
  - `WEB-INF/conf/olfs.xml`
  - `WEB-INF/urlrewrite.xml`
  - `docs/index.html`

- `ROOT.war` contains:
  - `WEB-INF/conf/olfs.xml`
  - `robots.txt`
  - no `robots.jsp`

- `ngap.war` contains:
  - `WEB-INF/web.xml`
  - `WEB-INF/urlrewrite.xml`
  - `WEB-INF/conf/logback.xml`
  - `WEB-INF/conf/olfs.xml`
  - landing PNGs under `docs/ngap/`

- `ngap.war` does not contain the container-managed excluded jars:
  - `elasticache-java-cluster-client-1.1.2.jar`
  - `memcached-session-manager-2.3.2.jar`
  - `memcached-session-manager-tc9-2.3.2.jar`
  - `redisson-all-3.22.0.jar`
  - `redisson-tomcat-9-3.22.0.jar`

### Ant Comparison Note

I attempted the matching Ant comparison build:

```bash
ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build clean server robots ngap
```

It failed before WAR creation in this local environment because Ant ran under the machine’s old JDK 8 and could not load the current `logback-classic-1.5.16.jar`:

- class file version `55.0`
- runtime expected `52.0`

So the Ant-vs-Gradle artifact comparison could not be completed end-to-end on this machine during Phase 5. The Gradle WAR content checks above did complete successfully.

### Known Remaining Gaps

This phase intentionally did not implement:

- distribution bundles and helper artifacts
- Ant-style `check` / `test`
- Sonar classpath/source-set reconciliation

## Phase 6 Implementation

Date: 2026-04-26

### Goal

Implement the remaining release-oriented packaging tasks:

- `srcDist` / `src-dist`
- `serverDist` / `server-dist`
- `hyraxRobotsDist` / `hyrax-robots-dist`
- `ngapDist` / `ngap-dist`
- `DISTRO`
- `hexEncoderApp`
- `validatorApp`
- `XSLTransformer`

### Files Changed

- `build.gradle`

### Commands Run

```bash
sed -n '540,725p' build.xml
find resources -maxdepth 2 -type d | sort
find resources -maxdepth 2 \( -path 'resources/hexEncoder/*' -o -path 'resources/META-INF/*' \) -type f | sort
find doc -maxdepth 2 -type f | sort
rg -n "class Encoder|class HexAsciiEncoder|class Validator|class Transformer" src
ls -1 README* NEWS* ChangeLog* COPYRIGHT*
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew tasks --all --no-daemon | rg "srcDist|src-dist|serverDist|server-dist|hyraxRobotsDist|hyrax-robots-dist|ngapDist|ngap-dist|DISTRO|hexEncoderApp|validatorApp|XSLTransformer"
JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home ./gradlew srcDist serverDist hyraxRobotsDist ngapDist DISTRO hexEncoderApp validatorApp XSLTransformer --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
ls -l build/dist
tar -tzf build/dist/olfs-CI-Build-src.tgz
tar -tzf build/dist/olfs-CI-Build-webapp.tgz
tar -tzf build/dist/robots-olfs-CI-Build-webapp.tgz
tar -tzf build/dist/ngap-CI-Build-webapp.tgz
tar -tzf build/dist/hexEncoder.tgz
find build/dist/hexEncoder -maxdepth 2 -type f | sort
jar tf build/dist/validator.jar
jar tf build/dist/xslt.jar
ls -1 build/dist/apache-commons-cli-1.2.jar build/dist/apache-commons-httpclient-3.1.jar build/dist/apache-commons-logging-1.1.3.jar build/dist/apache-commons-codec-1.8.jar build/dist/xercesImpl-2.12.2.jar build/dist/xerces-xml-apis-2.12.2.jar build/dist/jdom-1.1.1.jar
```

### Reasoning Notes

I added a release-packaging layer on top of the Phase 5 WAR tasks and the existing staged build trees.

Implemented task families:

- `srcDist` and alias `src-dist`
- `serverDist` and alias `server-dist`
- `hyraxRobotsDist` and alias `hyrax-robots-dist`
- `ngapDist` and alias `ngap-dist`
- `DISTRO`
- helper artifact tasks:
  - `hexEncoderJar`
  - `stageHexEncoderApp`
  - `hexEncoderApp`
  - `validatorApp`
  - `XSLTransformer`

Packaging behavior added:

- `srcDist`
  - creates `${SRC_DIST}.tgz`
  - uses filtered staged sources from `build/src`
  - includes `doc/`, `lib/`, `resources/hyrax/`, `build.xml`, `NEWS`, `ChangeLog`, `COPYRIGHT`

- `serverDist`
  - creates `${WEBAPP_DIST}.tgz`
  - packages `opendap.war` plus `README.md` renamed to `README`
  - deletes `opendap.war` afterward to match Ant’s post-package dist state

- `hyraxRobotsDist`
  - creates `robots-${WEBAPP_DIST}.tgz`
  - packages `opendap.war`, `ROOT.war`, and `README`
  - deletes both WARs afterward

- `ngapDist`
  - creates `${NGAP_DIST_BASE}-webapp.tgz`
  - packages `ngap.war`
  - deletes `ngap.war` afterward

- `DISTRO`
  - depends on the source dist, Hyrax webapp dist, and robots webapp dist

- `hexEncoderApp`
  - stages a `build/dist/hexEncoder` directory
  - copies `apache-commons-cli-1.2.jar`
  - copies the `resources/hexEncoder/hexEncoder` launcher file
  - builds `hexEncoder.jar`
  - packages the directory contents into `hexEncoder.tgz`

- `validatorApp`
  - builds `validator.jar`
  - copies the exact Ant support libs into `build/dist`

- `XSLTransformer`
  - builds `xslt.jar`
  - copies the same support libs into `build/dist`

One real bug surfaced during validation:

- the release-clean path and staged preprocessing were initially unordered
- that let `clean` remove `build/resources/WEB-INF/web.xml` after preprocessing but before WAR packaging
- I fixed it by making `PreProcessSourceCode` depend on `clean`, which is also much closer to Ant’s actual `PreProcessSourceCode -> clean, init` behavior

### Validation Results

Task discovery passed and listed all required Phase 6 tasks and aliases.

Combined packaging validation passed:

```bash
./gradlew srcDist serverDist hyraxRobotsDist ngapDist DISTRO hexEncoderApp validatorApp XSLTransformer \
  -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
```

Post-build `build/dist` state matched the intended Ant-style packaging flow:

- tarballs present:
  - `olfs-CI-Build-src.tgz`
  - `olfs-CI-Build-webapp.tgz`
  - `robots-olfs-CI-Build-webapp.tgz`
  - `ngap-CI-Build-webapp.tgz`
  - `hexEncoder.tgz`
- helper outputs present:
  - `build/dist/hexEncoder/`
  - `validator.jar`
  - `xslt.jar`
  - copied support jars for validator/XSLTransformer
- WAR files were absent from `build/dist` after the dist tasks, which matches the intended cleanup behavior

Archive spot checks:

- `olfs-CI-Build-src.tgz` contains:
  - `olfs-CI-Build-src/src/...`
  - `olfs-CI-Build-src/lib/...`
  - `olfs-CI-Build-src/resources/hyrax/...`
  - `olfs-CI-Build-src/build.xml`
  - `olfs-CI-Build-src/NEWS`
  - `olfs-CI-Build-src/ChangeLog`
  - `olfs-CI-Build-src/COPYRIGHT`

- `olfs-CI-Build-webapp.tgz` contains:
  - `olfs-CI-Build-webapp/opendap.war`
  - `olfs-CI-Build-webapp/README`

- `robots-olfs-CI-Build-webapp.tgz` contains:
  - `robots-olfs-CI-Build-webapp/ROOT.war`
  - `robots-olfs-CI-Build-webapp/README`

- `ngap-CI-Build-webapp.tgz` contains:
  - `ngap-CI-Build-webapp/ngap.war`

- `hexEncoder.tgz` contains:
  - `hexEncoder`
  - `apache-commons-cli-1.2.jar`
  - `hexEncoder.jar`

- `validator.jar` contains `opendap/xml/Validator.class`
- `xslt.jar` contains `opendap/xml/Transformer.class`

### Behavior Notes

The source distribution intentionally does not add a synthetic `README` file. The current repo has `README.md`, while the Ant `src-dist` target includes `README` only. In this checkout, preserving repository truth means the source dist includes the files that actually exist rather than inventing a renamed `README`.

### Known Remaining Gaps

This phase intentionally did not implement:

- Ant-style focused `check` / `test`
- Sonar reconciliation with the staged/classpath-aware Gradle build
- the final full verification matrix and closeout documentation updates

## Phase 7 Implementation

Date: 2026-05-02 16:52:08 MDT

### Goal

Implement Phase 7 of the Gradle replacement plan:

- recreate Ant's focused `check` behavior in Gradle
- keep `test` and `check` aligned for the same focused suite
- make Sonar consume Gradle-compiled classes and Ant-parity library scopes
- update README build guidance after the Gradle gate passed
- update CI coverage for the Gradle gate without moving release publishing away from Ant before Phase 8 artifact comparison

### Files Changed

- `build.gradle`
- `README.md`
- `.travis.yml`
- `AI-docs/gradle-build-replacement-log.md`

I did not modify `retired/` or generated `build/` sources. I also did not modify the already dirty `gradle/wrapper/gradle-wrapper.properties` or untracked `.vscode/` entry that were present before this phase.

### Commands Run

```bash
sed -n '400,455p' build.xml
find src -path '*/retired/*' -prune -o -name '*Test.java' -print | sort
sed -n '1,240p' src/opendap/coreServlet/Scrub.java
sed -n '1,220p' src/opendap/aggregation/AggregationParamsTest.java
sed -n '1,220p' src/opendap/auth/UrsIdPTest.java
sed -n '1,220p' src/opendap/bes/dap4Responders/Dap4ResponderTest.java
sed -n '1,220p' src/opendap/coreServlet/RequestIdTest.java
rg -n "Java 9|JUnit 5|build/libs|gradle war|Gradle uses JUnit 5|source/targetCompatibility 1\\.9" README.md .travis.yml build.gradle
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home /tmp/gradle-9.4.1/bin/gradle tasks --all --no-daemon
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home /tmp/gradle-9.4.1/bin/gradle check --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/tmp/olfs-gradle-home /tmp/gradle-9.4.1/bin/gradle server DISTRO ngap-dist check --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
tar -tzf build/dist/olfs-CI-Build-webapp.tgz
tar -tzf build/dist/robots-olfs-CI-Build-webapp.tgz | head -20
tar -tzf build/dist/ngap-CI-Build-webapp.tgz
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ant check
git status --short
```

I first tried `./gradlew tasks --all`, but this checkout's wrapper currently points at Gradle `8.9` and the only installed local JDK I found is Homebrew OpenJDK `25`. Gradle `8.9` cannot run on Java `25`, so local validation used a temporary `/tmp/gradle-9.4.1` distribution. I left the checked-in wrapper properties untouched.

### Reasoning Notes

Ant `check` runs an explicit JUnit 4 list, not general test discovery:

- `opendap.coreServlet.Scrub`
- `opendap.aggregation.AggregationParamsTest`
- `opendap.auth.UrsIdPTest`
- `opendap.bes.dap4Responders.Dap4ResponderTest`
- `opendap.coreServlet.RequestIdTest`

Those classes live in the main `src/` tree and Ant compiles them into the main `build/classes` output. I mirrored that in Gradle by pointing the `test` task at `sourceSets.main.output.classesDirs`, disabling test scanning, and including only the Ant class list.

The first Gradle `check` run failed in `UrsIdPTest` because `javax.servlet.ServletException` was missing at test runtime. That exposed a classpath difference: Ant's `olfs.compile.classpath` includes the Tomcat-provided servlet jar, while Gradle's normal `runtimeClasspath` excludes `compileOnly` dependencies. I fixed the test classpath by adding `configurations.providedTomcatLibs` to the Gradle `test` task only.

I updated Sonar properties so analysis uses:

- `sonar.sources=src`
- `sonar.java.binaries` from `sourceSets.main.output.classesDirs`
- `sonar.java.libraries` from the Ant-parity Hyrax libs plus Tomcat-provided compile libs

I also made `sonar` and deprecated `sonarqube` tasks depend on `classes` so the Gradle-compiled classes exist before analysis.

The combined Gradle gate found one Phase 6 packaging bug: `serverDist`, `hyraxRobotsDist`, and `ngapDist` deleted WAR files in `doLast`. That Ant-style cleanup side effect does not compose in a Gradle task graph. In one invocation, `hyraxRobotsDist` could delete `opendap.war` before `serverDist` packaged it, producing a tiny webapp tarball with only `README`. I removed those `doLast` deletes so the distribution tasks are composable and the generated archives contain their WARs.

I updated `.travis.yml` only for the Gradle build job, changing it from `gradle war` to the phase gate:

```bash
gradle -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build server DISTRO ngap-dist check
```

I intentionally left Ant-based snapshot/release publishing jobs in place. Switching release publishing should wait for Phase 8 artifact comparison, not just successful Gradle task execution.

### Validation Results

Gradle task discovery succeeded and showed:

- `check - Run Ant-parity focused checks.`
- `test - Run the same focused JUnit 4 checks as Ant check.`
- `sonar`
- deprecated `sonarqube`

Gradle `check` passed:

- 17 JUnit assertions executed
- all focused Ant check classes passed

The combined Gradle gate passed:

```bash
gradle server DISTRO ngap-dist check -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
```

Archive spot checks after the final Gradle gate:

- `olfs-CI-Build-webapp.tgz` contains `opendap.war` and `README`
- `robots-olfs-CI-Build-webapp.tgz` contains `opendap.war`, `ROOT.war`, and `README`
- `ngap-CI-Build-webapp.tgz` contains `ngap.war`

Ant `check` also passed under the available OpenJDK `25`, with the same focused class list.

### Known Remaining Gaps

Phase 8 still needs the full Ant-vs-Gradle verification matrix and artifact comparisons before declaring Gradle the production replacement. In particular, release publishing still uses Ant in `.travis.yml` pending those comparisons.

## Phase 8 Implementation

Date: 2026-05-04 11:32:31 MDT

### Goal

Implement the Phase 8 verification matrix so Gradle can prove Ant-parity across the release-authority tasks and artifacts.

### Files Changed

- `build.gradle`
- `settings.gradle`

### Commands Run

```bash
sed -n '1,240p' AI-docs/gradle-build-replacement-plan.md
sed -n '1,260p' AI-docs/gradle-build-replacement-log.md
sed -n '1,220p' .travis.yml
sed -n '1,240p' README.md
git diff -- build.gradle settings.gradle
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/private/tmp/olfs-gradle-home gradle phase8Verification --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build
```

### Reasoning Notes

Phase 8 in the plan is not another packaging phase. It is the proof phase: run the Ant and Gradle matrices, snapshot the resulting artifacts, and fail if the release-significant outputs diverge.

I implemented that as explicit Gradle verification tasks instead of treating it as a one-off manual checklist. The added task chain does four things:

- runs the Gradle side of the matrix for `showInfo`, `server`, `check`, `DISTRO`, `ngap`, `ngap-dist`, `robots`, and `hyrax-robots`
- verifies the Gradle artifacts before comparison, including staged `Version.java`, descriptor/config placement, docs presence, and exact `WEB-INF/lib` contents
- runs the Ant side in separate passes and snapshots its outputs outside `build/` so Ant `clean` does not destroy the Gradle comparison set
- compares the Ant and Gradle WARs by normalized entry set, exact `WEB-INF/lib`, and exact bytes for the key descriptors/config files called out in the plan

I added `settings.gradle` only to remove Ant's default exclusion of `**/.gitignore` during archive assembly. Without that, Gradle omitted `WEB-INF/conf/cache/.gitignore` and `WEB-INF/conf/logs/.gitignore`, which Ant includes. This is a small but real parity issue, so it was worth fixing centrally instead of papering over it in individual archive tasks.

The comparison logic intentionally does not require exact tarball entry parity for every `.tgz`. During implementation, Ant's distro-oriented targets exposed cleanup interactions that make combined tarball comparison brittle and not especially informative for the actual release-authority question. The plan's explicit artifact checks are WAR contents, `WEB-INF/lib`, substituted version strings, expected descriptor origins, docs inclusion, and NGAP container-jar exclusions. The automated Phase 8 verification now enforces those directly.

One more nuance: NGAP and distro-oriented Ant targets clean and rebuild overlapping output paths. To avoid false mismatches caused by Ant clobbering prior outputs during a long matrix run, the Ant snapshot step copies artifacts immediately after the target that produces them rather than assuming they all survive to the end.

### Validation Results

- `gradle phase8Verification --no-daemon -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build` succeeded.
- The Gradle matrix built all Phase 8 artifacts successfully.
- The verification step confirmed:
  - all expected WAR and distribution files were present
  - staged `build/src/opendap/bes/Version.java` contained substituted `CI-Build` values
  - Hyrax, robots, and NGAP WARs contained the expected descriptors/config files
  - `docs/` content was present and `doc/src.distribution.readme` was excluded
  - NGAP still excluded container-managed session/cache jars from `WEB-INF/lib`
- The Ant-vs-Gradle comparison passed for:
  - normalized WAR file lists
  - exact `WEB-INF/lib` contents
  - exact bytes of the key descriptors/config files
  - substituted version strings in staged `Version.java`

### Remaining Notes

- I did not change `.travis.yml` or `README.md` in this phase. The verification matrix is now implemented and passing; CI/doc promotion to declare Gradle the sole release authority is a separate decision.
- `git status --short` also showed an existing modified `.vscode/settings.json`. I left it untouched.
