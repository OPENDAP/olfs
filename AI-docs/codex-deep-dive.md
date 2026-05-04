# Codex Deep Dive: Hyrax OLFS Repository

Date: 2026-05-04  
Repository root: `/Users/jimg/src/opendap/hyrax/olfs`

## Executive Summary

This repository is still the Java servlet front end for Hyrax, but the active runtime surface is narrower than the previous deep dive described. The current live tree centers on:

- the main Hyrax/OLFS servlet application
- BES-backed DAP request dispatch
- NGAP-specific dispatch and packaging
- aggregation, viewers, docs, and sitemap/robots services
- authentication, authorization, and a standalone PDP service

The old deep dive treated WCS 2.0 and W10n as active first-class services in the current tree. That is no longer correct. In the current repository state:

- there is no active `opendap.wcs` source package
- there is no active `opendap.w10n` source package
- the main `web.xml` files do not map WCS or W10n servlets
- there is no live `resources/WCS` tree in `resources/`

That matches your note that many formerly active `src` files have since been moved into `retired/`. For this refresh, `retired/` was treated as historical only and was not inspected.

The build story is also more nuanced than before. Ant remains the production authority and the release packaging path, but Gradle is no longer just a loose sidecar: it now mirrors the Ant task names and Java 8 settings closely enough that Travis runs both Ant and Gradle build paths. There is still some historical residue in the build scripts, especially around removed WCS-era paths.

## Current Repository Shape

Current active inventory, excluding `retired/`:

- `288` Java source files under `src/`
- `185` files under `resources/`

Largest active source areas:

- `opendap/bes`: 73 files
- `opendap/dap4`: 29 files
- `opendap/http`: 27 files
- `opendap/coreServlet`: 27 files
- `opendap/auth`: 25 files
- `opendap/threddsHandler`: 21 files

Active top-level resource areas:

- `resources/hyrax`: main Hyrax webapp resources
- `resources/ngap`: NGAP-specific webapp resources
- `resources/robots`: standalone sitemap/robots webapp
- `resources/pdpService`: standalone PDP webapp
- `resources/aggregation`: aggregation examples and tests

Still present but not part of the main Hyrax WAR staging path:

- `resources/experiments`
- `resources/gsoc`
- `resources/sonar`

Important top-level build files:

- `build.xml`
- `build.gradle`
- `pdpservice-build.xml`
- `aggregation-build.xml`
- `.travis.yml`

## Build System

### Ant Is Still The Release Authority

`build.xml` remains the authoritative production build file. It defines the expected release and packaging targets, including:

- `server`
- `check`
- `DISTRO`
- `ngap`
- `ngap-dist`
- `robots`
- `hyrax-robots`

It still preprocesses source and resources by copying them into `build/` and filtering tokens before compilation. That matters because `src/opendap/bes/Version.java` is intentionally tokenized in source and compiled from the filtered staged copy in `build/src`.

Important Ant facts verified from the current tree:

- Java source/target is `1.8`
- `check` runs a focused five-test JUnit suite
- `PreProcessSourceCode` stages filtered source and resource trees before `compile`

From the `ant check` run on 2026-05-04:

- Ant copied `290` files into `build/src`
- Ant copied `54` Hyrax resource files into `build/resources`
- Ant copied `23` NGAP resource files into `build/ngap`
- Ant copied `3` robots files into `build/robots`
- Ant compiled `288` Java source files

### Gradle Has Moved Closer To Ant Parity

The previous deep dive overstated Gradle drift. The current `build.gradle` now explicitly mirrors Ant in several important ways:

- Java source/target compatibility is `1.8`
- `options.release = 8`
- Ant-style task names exist: `server`, `DISTRO`, `ngap-dist`, `robots`, `hyrax-robots`, `check`
- source and resource staging/filtering is modeled explicitly
- the `check` task is wired to the same focused JUnit suite as Ant

Travis confirms this is not theoretical. The build stage runs both:

- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server`
- `gradle -PHYRAX_VERSION=CI-Build -POLFS_VERSION=CI-Build server DISTRO ngap-dist check`

So the right current description is:

- Ant is still the release authority
- Gradle is an active parity effort, not just a placeholder

### Build Script Drift Still Exists

There is still historical build residue worth calling out:

- `build.gradle` has `mergeWcsResources`, which references `resources/WCS/2.0`, but that directory is absent in the live tree.
- `stageSourceTree` excludes packages such as `opendap/wcs/v1_1_2/**` that no longer exist in the active source tree.
- `build.xml` source-distribution packaging still references older top-level resource paths such as `resources/WebStart/**`, `resources/gateway/**`, and `resources/nciso/**`, while the current live layout places these under `resources/hyrax/...`.

Those are real drift signals. I am inferring from the current tree that these are historical leftovers, not current product-defining structure.

## CI

`.travis.yml` currently shows:

- Ant build coverage
- Gradle build coverage
- Snyk scanning
- Gradle Sonar scanning
- Ant-based snapshot packaging for OLFS and NGAP

That reinforces the build conclusion above: Ant is still the release/package authority, but Gradle is part of current CI, not abandoned.

## Runtime Deployment Descriptors And Routing

### Main Hyrax Webapp

Primary descriptor:

- `resources/hyrax/WEB-INF/web.xml`

Current active filters:

- `ClickJackFilter` mapped to `/*`
- `BotFilter` mapped to `/*`
- `UrlRewriteFilter` mapped to `/*`

Filters present but disabled by default in the main Hyrax descriptor:

- `IdFilter`
- `PEPFilter`

Current active servlet mappings in the main Hyrax descriptor:

- `opendap.coreServlet.DispatchServlet` at `/hyrax/*`
- `opendap.aggregation.AggregationServlet` at `/aggregation/*`
- `opendap.viewers.ViewersServlet` at `/viewers/*`
- `opendap.coreServlet.DocServlet` at `/docs/*`
- `opendap.bes.BESSiteMapService` at `/siteMap/*`

Important routing behavior from `resources/hyrax/WEB-INF/urlrewrite.xml`:

- `/` rewrites to `/siteMap/`
- explicitly named prefixes such as `docs`, `viewers`, `hyrax`, `aggregation`, and `siteMap` are preserved
- everything else is rewritten into `/hyrax/...`

That makes the dispatch servlet the default target for dataset-style requests.

### NGAP Webapp

Primary descriptor:

- `resources/ngap/web.xml`

Compared with the main Hyrax descriptor, NGAP differs in important ways:

- `IdFilter` is enabled
- `PEPFilter` is enabled
- `BotFilter` is not present
- `<distributable />` is enabled
- NGAP has its own `olfs.xml`
- NGAP has its own `urlrewrite.xml`
- NGAP has its own logging config

Important routing behavior from `resources/ngap/urlrewrite.xml`:

- `/` and `/index.html` route to `/docs/ngap/ngap.html`
- some explicit prefixes are preserved
- ordinary unmatched paths are rewritten to `/hyrax/ngap/...`

So NGAP is not a separate top-level servlet implementation. It is the same main dispatch servlet plus NGAP-specific routing, config, and auth/logging behavior.

### Standalone Robots/Sitemap Webapp

Primary descriptor:

- `resources/robots/WEB-INF/web.xml`

This standalone app maps:

- `opendap.bes.BESSiteMapService` at `/siteMap/*`
- JSP handling for `/robots.txt` and `*.jsp`

The Ant and Gradle robots builds both turn `robots.jsp` into `robots.txt`.

### Standalone PDP Webapp

Primary descriptor:

- `resources/pdpService/WEB-INF/web.xml`

This app maps:

- `opendap.auth.PDPService` at `/*`

It is a separate deployment path from the main Hyrax and NGAP webapps.

## Core Configuration Behavior

### Persistent Config Lookup

`opendap.coreServlet.ServletUtil` currently checks configuration locations in this order:

1. `/etc/olfs/`
2. `/usr/share/olfs/`
3. bundled `WEB-INF/conf`

That is still the live code path. If docs or old notes imply different environment-based lookup, that should be treated as documentation drift until proven in code.

`PersistentConfigurationHandler` still installs bundled default configuration into the persistent config directory when the expected semaphore file is missing.

### Main Hyrax `olfs.xml`

Primary default config:

- `resources/hyrax/WEB-INF/conf/olfs.xml`

Important active defaults:

- BES prefix `/`
- BES host `localhost`
- BES port `10022`
- BES `ClientPool maximum="200" maxCmds="2000"`
- `NodeCache maxEntries="20000" refreshInterval="600"`
- `SiteMapCache refreshInterval="600"`
- `ThreddsService prefix="thredds" useMemoryCache="true" allowRemote="false"`
- `GatewayService prefix="gateway"`
- `DatasetUrlResponse type="download"`
- `DataRequestForm type="dap4"`
- `HttpPost enabled="true" max="2000000"`

Optional or commented-out controls include:

- `AllowDirectDataSourceAccess`
- `ForceLinksToHttps`
- `ShowDmrppLink`
- `RequireUserSelection`
- `AddFileoutTypeSuffixToDownloadFilename`
- `EnableCombinedLog`
- `NoDynamicNavigation`
- `BotFilter`
- `Timer`
- `PreloadNcmlIntoBes`

### NGAP `olfs.xml`

Primary NGAP config:

- `resources/ngap/olfs.xml`

This file explicitly says it is a stand-in that will be replaced by an injected version in deployed NGAP environments, so treat the checked-in file as representative but not authoritative for production NGAP.

Important differences from the default Hyrax config:

- `ClientPool maximum="25"`
- `DatasetUrlResponse type="requestForm"`
- `UseDualCloudWatchLogs` enabled
- `ShowDmrppLink` enabled
- `EnableCombinedLog` enabled

### Auth Configuration

Primary auth config:

- `resources/hyrax/WEB-INF/conf/user-access.xml`

Important current behavior:

- URS is the configured `IdProvider`
- a local `SimplePDP` is defined
- guest access is limited
- `users` role gets broad access

But the key deployment nuance is this:

- the main Hyrax `web.xml` does not enable `IdFilter` or `PEPFilter` by default
- the NGAP `web.xml` does enable them

So `user-access.xml` matters more to NGAP and to deployments that explicitly turn on auth in the main Hyrax app.

## Core Request Flow

The main request entry point is:

- `src/opendap/coreServlet/DispatchServlet.java`

During init it does all of the following:

- debug/logging setup
- persistent config installation
- config load from `olfs.xml`
- combined log and CloudWatch toggles
- POST enablement detection
- timer setup
- `BESManager` initialization
- `AuthenticationControls` initialization
- dispatch handler chain construction

Current GET handler order:

1. `opendap.bes.VersionDispatchHandler`
2. `opendap.ncml.NcmlDatasetDispatcher`
3. `opendap.threddsHandler.StaticCatalogDispatch`
4. `opendap.gateway.DispatchHandler`
5. `opendap.ngap.NgapDapDispatcher`
6. `opendap.bes.BesDapDispatcher`
7. `opendap.bes.DirectoryDispatchHandler` unless `NoDynamicNavigation` is set
8. `opendap.bes.BESThreddsDispatchHandler` unless `NoDynamicNavigation` is set
9. `opendap.bes.FileDispatchHandler`

Current POST handler order:

1. `opendap.ngap.NgapDapDispatcher`
2. `opendap.bes.BesDapDispatcher`

`doHead()` is intentionally restricted. Outside the service root it returns `405` with `Allow: GET, POST`.

## BES And DAP Integration

### BESManager

`opendap.bes.BESManager` remains the central BES bootstrap and routing manager.

Important current behavior:

- requires at least one BES entry
- requires a root `/` BES prefix
- groups BES instances by prefix
- uses longest-prefix matching
- initializes node cache and sitemap cache from config

### DAP Dispatch

`opendap.bes.BesDapDispatcher` is still the main DAP dispatcher. It consumes settings such as:

- `AllowDirectDataSourceAccess`
- `DatasetUrlResponse`
- `DataRequestForm`
- `RequireUserSelection`
- `ForceLinksToHttps`
- `AddFileoutTypeSuffixToDownloadFilename`

The current tree still supports a broad responder set across DAP2 and DAP4, including:

- metadata responses
- data responses
- request forms
- file access handling
- ISO and rubric-style alternate handlers through `nciso`
- DMR++ support in NGAP through `NgapDmrppResponder`

I did not re-enumerate every suffix from the responder classes this time because the bigger correctness issue was whether the surrounding services were still wired at all. The DAP responder surface is still large and BES-centered.

## Other Active Services

### Gateway

The active gateway path is the dispatch handler:

- `opendap.gateway.DispatchHandler`

It is in the main dispatch chain and is driven by the configured gateway prefix.

There is also:

- `opendap.gateway.DispatchServlet`

but that class is explicitly marked `@Deprecated` and is not mapped by the live main web descriptors.

### NGAP

NGAP logic is implemented by:

- `opendap.ngap.NgapDapDispatcher`
- `opendap.ngap.NgapBesApi`
- `opendap.ngap.NgapDmrppResponder`

NGAP is not a separate servlet mapping. It is a prefix-aware dispatch extension inside the main dispatch chain, plus separate NGAP packaging and rewrite rules.

### Aggregation

Primary servlet:

- `opendap.aggregation.AggregationServlet`

Mapped in the main app:

- `/aggregation/*`

Also built separately by:

- `aggregation-build.xml`

### Viewers

Primary servlet:

- `opendap.viewers.ViewersServlet`

Current live config:

- `resources/hyrax/WEB-INF/conf/viewers.xml`

Currently active viewer handlers in config:

- `opendap.webstart.IdvViewerRequestHandler`
- `opendap.webstart.NetCdfToolsViewerRequestHandler`
- `opendap.webstart.AutoplotRequestHandler`

The ncWMS and Godiva service handler examples are present but commented out, so they should not be described as active defaults.

`ViewersServlet` prefers localized `WebStart` resources from the persistent config directory and falls back to bundled `WebStart` resources in the webapp.

### Docs

Primary servlet:

- `opendap.coreServlet.DocServlet`

It serves docs from persistent config `docs/` when present, otherwise from bundled webapp docs, and performs simple token replacement for `<CONTEXT_PATH />` and `<SERVLET_NAME />` in text content.

### Sitemap/Robots

Primary servlet:

- `opendap.bes.BESSiteMapService`

It is still mapped in the main app and in the standalone robots app, even though the main `web.xml` comments say it was moved to the robots service.

### PDP

Primary servlet:

- `opendap.auth.PDPService`

It supports both GET and POST evaluation and enforces secure transport by default unless the config explicitly disables that behavior.

## What Is No Longer Active In The Current Tree

These are the biggest corrections to the previous deep dive:

- There is no active `opendap.wcs` package in `src/`.
- There is no active `opendap.w10n` package in `src/`.
- The main `resources/hyrax/WEB-INF/web.xml` does not map WCS or W10n servlets.
- The NGAP `web.xml` does not map WCS or W10n servlets either.
- There is no live `resources/WCS` tree in `resources/`.

That means the old sections that described WCS 2.0 and W10n as current mapped services are now wrong for this repository state.

The repository still contains historical clues in build logic and comments that those services once mattered. Those clues should now be treated as historical residue unless a future active tree reintroduces them outside `retired/`.

## Testing And Verification

Commands run during this refresh:

1. `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show`
2. `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build check`

Notes from verification:

- Both commands required an explicit `JAVA_HOME` in this shell because Ant otherwise tried to use `/System/Library/Frameworks/JavaVM.framework/Home`.
- `ant check` succeeded.
- The focused JUnit suite passed.
- Compilation under JDK 23 produced 36 warnings, mostly deprecation and Java 8 bootstrap-path warnings rather than immediate build breaks.

## Current Risk Areas

- Build scripts still contain historical path references and exclusions related to removed WCS-era content.
- Main Hyrax and NGAP differ materially in auth behavior; do not assume a config change affects both the same way.
- Persistent config precedence still matters for docs, viewers, and `olfs.xml`.
- `DatasetUrlResponse` and `AllowDirectDataSourceAccess` remain security- and behavior-sensitive.
- The checked-in NGAP `olfs.xml` is explicitly a stand-in, not necessarily the deployed reality.
- Reflection-heavy code in auth/viewers and deprecated JDK APIs compile today but generate many warnings on newer JDKs.

## Practical Guidance

For future work in this repository:

1. Treat `retired/` as historical unless you explicitly need archaeology.
2. Verify servlet mappings in the live `web.xml` files before describing a service as active.
3. Use Ant as the release/build authority.
4. Treat Gradle as important and increasingly accurate, but still verify historical-path assumptions.
5. Check `urlrewrite.xml` whenever you change routing or service exposure.
6. Distinguish main Hyrax behavior from NGAP behavior before changing auth, logging, or URL semantics.
