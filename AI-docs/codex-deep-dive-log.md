# Codex Deep Dive Log

This is a timestamped summary of actions and reasoning for the repository deep dive. It records conclusions, rationale, and work performed rather than raw private chain-of-thought.

## Task Points

### 2026-04-26 11:16:49 MDT

Started in `/Users/jhrg/src/hyrax/olfs`. Confirmed the working directory and captured the current timestamp. Checked the top-level repository layout and the git status. The worktree appeared clean at the start.

Reasoning: establish the exact repository root and avoid accidentally attributing pre-existing changes to this task.

### 2026-04-26 11:17:00 MDT

Enumerated active files while excluding `retired/`, as requested. Noted that the repository is centered on a Java servlet application, with multiple service-specific packages and several resource-backed web applications.

Reasoning: the user explicitly asked to ignore `retired/`, but also warned that some files under active `src/` and `resources/` may be unused. The file inventory needed to distinguish active wiring from mere file presence.

### 2026-04-26 11:17:20 MDT

Read the primary project documents and build/deployment anchors: `README.md`, `build.xml`, `.travis.yml`, and the main Hyrax web descriptor.

Reasoning: build and runtime conclusions should be anchored in the files that actually control CI, packaging, and servlet mappings.

### 2026-04-26 11:17:50 MDT

Read more of `resources/hyrax/WEB-INF/web.xml`, `build.gradle`, and the source package layout.

Reasoning: compare the production Ant path with the Gradle path and identify the main package responsibilities before diving into individual services.

### 2026-04-26 11:18:10 MDT

Read main configuration and alternate webapp descriptors, including `olfs.xml`, `urlrewrite.xml`, PDP, robots, NGAP, and WCS descriptors.

Reasoning: this codebase's active behavior is defined by both Java source and deployment descriptors. Servlet mapping, filter mapping, and URL rewrite behavior are essential to understanding runtime behavior.

### 2026-04-26 11:18:40 MDT

Read Ant target definitions in `build.xml`, `pdpservice-build.xml`, and `aggregation-build.xml`, plus viewer configuration.

Reasoning: the repository contains multiple WAR outputs and helper artifacts. The deep dive needed to document services beyond the main OLFS dispatcher.

### 2026-04-26 11:19:05 MDT

Read core request flow code: `DispatchServlet`, `DispatchHandler`, `BESManager`, `BesDapDispatcher`, `NgapDapDispatcher`, and `BesApi`.

Reasoning: the main OLFS behavior depends on an ordered dispatch chain and BES integration. These files explain how requests are classified and sent to BES-backed responders.

### 2026-04-26 11:19:30 MDT

Read WCS and W10n service implementations and related configuration.

Reasoning: the user specifically asked not to document only the OLFS. WCS and W10n are first-class servlet mappings in the main web descriptor.

### 2026-04-26 11:19:55 MDT

Read aggregation, viewers, sitemap/robots, gateway, authentication, PDP, and bot filter code and configuration.

Reasoning: these services and filters materially change runtime behavior and deserve explicit documentation in the repository overview.

### 2026-04-26 11:20:10 MDT

Ran inventory and cross-check searches for Java files, resource files, web descriptors, TODO/FIXME/deprecated markers, config lookup paths, and package counts.

Reasoning: quantify the repository shape and identify drift risks, such as resource descriptors that reference missing active classes.

### 2026-04-26 11:20:24 MDT

Captured a later timestamp before writing the requested documents. Consolidated findings into three outputs:

- `codex-deep-dive.md`
- `AGENTS.md`
- `codex-deep-dive-log.md`

Reasoning: produce a deep dive for humans, a practical repo guide for future agents, and a separate timestamped action/rationale log.

### 2026-04-26 11:20:24 MDT

Documented the main conclusions:

- Ant is the production build authority.
- Gradle support exists but is secondary and appears to have drift.
- The main WAR contains multiple services and servlets beyond the OLFS dispatcher.
- NGAP, PDP, robots/sitemap, WCS, W10n, aggregation, viewers, docs, gateway, and auth behavior all need to be considered in future changes.
- Some active resources appear stale or experimental despite being outside `retired/`.

Reasoning: these points are the highest-risk assumptions future maintainers or agents might otherwise get wrong.

### 2026-05-04 15:19:50 MDT

Started a second-pass deep dive refresh in `/Users/jimg/src/opendap/hyrax/olfs`, explicitly using the current repository state rather than trusting the earlier summary. Re-read `AI-docs/codex-deep-dive.md`, `AI-docs/codex-deep-dive-log.md`, `AGENTS.md`, `build.xml`, `build.gradle`, `.travis.yml`, the live web descriptors, and the key dispatch/config classes.

Reasoning: the user reported that many former `src/` files had been moved into `retired/`, so the highest-value work was to re-verify every claim that could have become stale, especially claims about active services.

### 2026-05-04 15:19:50 MDT

Confirmed that `retired/` should be treated as historical only and avoided inspecting it. Built the refreshed understanding from the live `src/`, `resources/`, build files, and deployment descriptors only.

Reasoning: this avoids polluting the deep dive with behavior that now exists only for archaeology.

### 2026-05-04 15:19:50 MDT

Verified the largest correctness changes from the prior deep dive:

- no active `opendap.wcs` package exists in `src/`
- no active `opendap.w10n` package exists in `src/`
- the main Hyrax and NGAP `web.xml` files do not map WCS or W10n servlets
- no live `resources/WCS` tree exists under `resources/`

Reasoning: the prior deep dive described WCS and W10n as active first-class services. That is no longer true in this tree and was the most important documentation error to fix.

### 2026-05-04 15:19:50 MDT

Measured the current active tree and package distribution. Found `288` Java source files under `src/` and `185` files under `resources/`, excluding `retired/`.

Reasoning: the earlier document included counts and shape statements. Those needed to be refreshed so the new deep dive anchors itself in the current repository, not the earlier snapshot.

### 2026-05-04 15:19:50 MDT

Re-verified the live servlet and rewrite surface:

- main Hyrax maps `DispatchServlet`, `AggregationServlet`, `ViewersServlet`, `DocServlet`, and `BESSiteMapService`
- main Hyrax enables `ClickJackFilter` and `BotFilter`, but leaves `IdFilter` and `PEPFilter` commented out
- NGAP enables `ClickJackFilter`, `IdFilter`, and `PEPFilter`
- Hyrax root rewrites to `/siteMap/`
- NGAP root rewrites to `/docs/ngap/ngap.html`

Reasoning: this is the authoritative runtime surface for the current tree. It is the fastest way to distinguish active services from classes that merely still exist in source.

### 2026-05-04 15:19:50 MDT

Re-verified the dispatch chain in `DispatchServlet` and confirmed that the active GET handler order is:

1. `VersionDispatchHandler`
2. `NcmlDatasetDispatcher`
3. `StaticCatalogDispatch`
4. `gateway.DispatchHandler`
5. `NgapDapDispatcher`
6. `BesDapDispatcher`
7. `DirectoryDispatchHandler` unless dynamic navigation is disabled
8. `BESThreddsDispatchHandler` unless dynamic navigation is disabled
9. `FileDispatchHandler`

Reasoning: the dispatch chain is still one of the highest-leverage facts in the repository because many behaviors are controlled by ordered handler precedence rather than separate servlet mappings.

### 2026-05-04 15:19:50 MDT

Re-assessed the build story. The prior deep dive said Gradle was secondary and drifted significantly. That now needs nuance:

- Ant is still the release/package authority
- Gradle now mirrors Ant’s Java 8 settings and task names closely
- Travis actively runs both Ant and Gradle build paths
- some stale Gradle and Ant references to removed WCS-era or older resource paths still remain

Reasoning: the old description was directionally useful but no longer precise enough. The current repo shows real Ant-parity work in Gradle, even though historical residue remains.

### 2026-05-04 15:19:50 MDT

Ran build verification commands:

- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show`
- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build check`

Both required an explicit `JAVA_HOME` because Ant otherwise tried to use `/System/Library/Frameworks/JavaVM.framework/Home`. `ant check` succeeded, compiled `288` source files, and passed the focused five-test suite. Compilation under JDK 23 produced warnings, mostly deprecations and Java-8 bootstrap-path warnings.

Reasoning: this gives the refreshed deep dive at least one real build-and-test sanity check rather than relying only on static inspection.

### 2026-05-04 15:19:50 MDT

Rewrote `AI-docs/codex-deep-dive.md` to:

- correct the repository root path
- remove stale claims that WCS and W10n are currently active services
- update the repository counts and current active package/resource layout
- describe the present Hyrax, NGAP, robots, viewers, docs, aggregation, gateway, BES, and PDP surfaces
- document the current Ant-versus-Gradle story more accurately
- explicitly call out build-script historical residue as drift risk rather than treating it as active runtime behavior

Reasoning: the deep dive should now describe the active repository as it exists today, while still flagging the places where the codebase carries older assumptions forward in comments or build logic.

### 2026-05-04 15:40:00 MDT

Query/prompt: `Based on the following statement from codex-deep-dive.md evaluate and update the build.gradle file: "Build scripts still contain historical path references and exclusions related to removed WCS-era content."`

Reviewed `build.gradle` against the refreshed deep dive note and cross-checked the active tree without inspecting `retired/`. Verified that the live repository has no `resources/WCS/` tree and no active `src/opendap/wcs/` package. Cross-checked `build.xml` and confirmed Ant no longer stages any separate WCS resource tree during `PreProcessSourceCode`.

Reasoning: the safest cleanup was to remove only the Gradle behavior that was clearly stale in the active tree and already absent from the Ant production build.

### 2026-05-04 15:40:00 MDT

Updated `build.gradle` to remove the stale Gradle-only WCS residue:

- removed the unused `WCS_VERSION` extra property
- removed the unused `WCS_SOFTWARE_VERSION` and `WcsSoftwareVersion` token filters
- removed the `showInfo` WCS version line
- removed the dead `opendap/wcs/v1_1_2/**` source exclude
- removed the `mergeWcsResources` task that still referenced `resources/WCS/2.0`
- removed the `PreProcessSourceCode` dependency on `mergeWcsResources`

Reasoning: these references described a resource/source layout that no longer exists in the live repository and were not needed for Ant parity.

### 2026-05-04 15:40:00 MDT

Attempted Gradle validation with:

- `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home GRADLE_USER_HOME=/private/tmp/olfs-gradle-home ./gradlew -q help -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build`

The first run failed because the sandbox could not write to `~/.gradle`, so it was rerun with a writable temp `GRADLE_USER_HOME`. The rerun downloaded the wrapper and then failed with `Unsupported class file major version 67`.

Reasoning: that failure points to the local Gradle/JDK runtime combination on this machine rather than the WCS cleanup itself. The machine currently exposes JDK 23 plus older JDK 8/7 installs, and Gradle 8.9 did not complete configuration under JDK 23 here.

### 2026-05-04 16:48:19 MDT

Query/prompt: `The gradle build for the 'server' target now fails on my machine using /opt/homebrew/opt/openjdk/bin/javac. Here's the error message: ... /src/opendap/coreServlet/RequestCache.java:176: error: cannot find symbol StackWalker ...`

Reviewed the reported compile failure against the live source tree and confirmed the problem is in `src/opendap/coreServlet/RequestCache.java`, not only in generated `build/src`. The file now used `StackWalker`, which is a Java 9 API, while both Ant and Gradle still compile the production sources with Java 8 source/target settings.

Reasoning: the failure is real and deterministic. `StackWalker` cannot compile when the build is intentionally pinned to Java 8 compatibility, so the right fix is to remove the Java 9 dependency rather than weaken the build target.

### 2026-05-04 16:48:19 MDT

Updated `RequestCache.getRequestId()` to build the diagnostic stack trace from `Thread.currentThread().getStackTrace()` and `Arrays.stream(...)` instead of `StackWalker`.

Reasoning: this preserves the intent of the logging path, keeps the implementation small, and stays compatible with Java 8, which matches the repository's documented build authority and current Gradle settings.

### 2026-05-04 16:52:20 MDT

Verification results:

- `JAVA_HOME=/opt/homebrew/opt/openjdk@23/libexec/openjdk.jdk/Contents/Home ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build check` succeeded
- `gradle server --no-daemon` under Homebrew `openjdk` failed before project compilation with `Unsupported class file major version 69`
- `gradle compileJava --no-daemon` under Homebrew `openjdk@21` with a fresh `GRADLE_USER_HOME` failed the same way, also before project compilation

Reasoning: the Java source change removed the reported `StackWalker` compile error, which is confirmed by a full Ant compile and test pass. The remaining Gradle failure appears to be a separate build-runtime or environment problem in this machine's Gradle script evaluation path, not a Java source-level `RequestCache` problem.
