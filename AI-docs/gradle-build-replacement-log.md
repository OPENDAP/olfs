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
