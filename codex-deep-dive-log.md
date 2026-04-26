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

