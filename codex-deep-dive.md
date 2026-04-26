# Codex Deep Dive: Hyrax OLFS Repository

Date: 2026-04-26  
Repository root: `/Users/jhrg/src/hyrax/olfs`

## Executive Summary

This repository is the Java servlet front end for Hyrax, commonly referred to as the OLFS. It is not only the OLFS request dispatcher, however. The active source and resource trees also contain several related web services, filters, helper servlets, and standalone build outputs:

- the main Hyrax/OLFS servlet application, rooted at the `opendap` deployment context by default
- DAP2 and DAP4 request dispatching backed by BES
- WCS 2.0 service support
- W10n service support
- aggregation ZIP service support
- viewer and Java WebStart launch support
- documentation, sitemap, and robots services
- gateway and NGAP-specific dispatch paths
- authentication, authorization, PDP, bot filtering, and clickjacking filters
- standalone helper applications and tools such as the PDP WAR, robots WAR, XML validator, XSL transformer, and hex encoder

The production build story is Ant-first. The Gradle files are real and used in some CI/security/analysis paths, but they are still secondary and appear to have drift from the production Ant build. `.travis.yml` confirms this: production-like build and release packaging use Ant targets such as `server`, `DISTRO`, and `ngap-dist`, while Gradle has a separate build stage and scan-related usage.

The `retired/` directory was intentionally ignored for this analysis, per request. Some files under active `src/` and `resources/` also appear stale, experimental, or only conditionally used. The most important examples are older WCS 1.1.2 resources and several `resources/experiments` descriptors that reference classes not present in the active source tree.

## Repository Shape

Top-level files and directories of interest:

- `build.xml`: primary production Ant build for OLFS/Hyrax, NGAP, robots, and helper jars.
- `pdpservice-build.xml`: Ant build for the standalone PDP service WAR.
- `aggregation-build.xml`: Ant build for a standalone aggregation service WAR.
- `.travis.yml`: CI definition; shows Ant production build and release packaging paths.
- `build.gradle`: evolving Gradle build for WAR creation, Sonar, and related work.
- `build-for-snyk.gradle`: older Gradle/Snyk-oriented dependency scan build file.
- `src/`: Java source tree.
- `resources/`: webapp resources, configuration, XSL, JSP, web descriptors, static assets, and test/demo inputs.
- `doc/`: static user and admin documentation copied into the webapp.
- `lib/`: checked-in jar dependencies used heavily by the Ant build.
- `ant_targets`: notes about active Ant targets and historical build files.
- `README.md`, `INSTALL`, `install.html`: user-facing build/install guidance.

The active source tree contains 356 Java files under `src/`. The largest packages are:

- `opendap.bes`: BES API, dispatchers, responders, node cache, sitemap, DAP response handling.
- `opendap.wcs`: WCS service implementation.
- `opendap.dap4`: DAP4 utilities and response support.
- `opendap.http`: HTTP request/response helpers.
- `opendap.coreServlet`: core servlet framework, main dispatcher, filters, docs servlet, persistent config.
- `opendap.auth`: authentication, authorization, IdP, PEP, PDP, user profile support.
- `opendap.threddsHandler`: static and dynamic THREDDS catalog support.
- `opendap.gateway`, `opendap.aggregation`, `opendap.viewers`, `opendap.w10n`, `opendap.ngap`: service-specific implementations.

The active resource tree contains about 330 files. The heaviest resource areas are:

- `resources/hyrax`: main webapp resources and default configuration.
- `resources/WCS`: WCS resources, especially WCS 2.0 schemas, examples, XSL, and config.
- `resources/ngap`: NGAP-specific deployment descriptor, config, logging, landing page, errors, and supporting jars.
- `resources/aggregation`: aggregation test/demo inputs and baselines.
- `resources/robots`: standalone robots/sitemap webapp.
- `resources/pdpService`: standalone PDP service webapp.
- `resources/experiments`: experimental descriptors and pages, likely not part of production.

## Build System

### Ant Is The Production Authority

`build.xml` is the primary build file. It declares the project as `Hyrax-OLFS` and provides the main build path for:

- `server`: builds the main `opendap.war`.
- `DISTRO`: builds source, server, and robots distributions.
- `robots` / `hyrax-robots`: builds a standalone `ROOT.war` robots/sitemap app.
- `ngap`: builds `ngap.war`.
- `ngap-dist`: builds NGAP distribution packaging.
- `check`: runs a focused JUnit suite.
- helper jar targets including hex encoder, XML validator, and XSL transformer.

The Ant build copies source to `build/src` and filters version/service tokens before compilation. Important tokens include:

- `@HyraxVersion@`
- `@OlfsVersion@`
- `@SERVICE_CONTEXT@`
- `@WCS_SOFTWARE_VERSION@`

This matters because `src/opendap/bes/Version.java` contains unresolved version tokens in source. That is expected for the Ant path, where filtered copies are compiled from `build/src`.

The Ant compiler settings use Java source/target 1.8. CI currently runs the Ant build under JDK 17.

### Main Ant Build Outputs

Important Ant targets include:

- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server`
  - CI-style main WAR build.
- `ant check`
  - Runs a small JUnit suite.
- `ant DISTRO`
  - Builds source/server/robots distribution artifacts.
- `ant ngap`
  - Builds NGAP WAR.
- `ant ngap-dist`
  - Builds NGAP distribution artifacts.
- `ant robots`
  - Builds standalone `ROOT.war` for robots/sitemap behavior.

`ant_targets` reinforces that the active build file is `build.xml`, covering OLFS/Hyrax and NGAP production rules.

### Standalone Ant Builds

`pdpservice-build.xml` builds a standalone PDP service:

- deployment context: `pdp`
- servlet: `opendap.auth.PDPService`
- resources: `resources/pdpService`
- output: WAR under `build/dist`

`aggregation-build.xml` builds a standalone aggregation service:

- deployment context: `opendap`
- servlet: `opendap.aggregation.AggregationServlet`
- output: WAR named like the main app

The aggregation servlet is also present in the main Hyrax web descriptor, so the standalone build is not the only way to expose it.

### Gradle Is Present But Secondary

`build.gradle` defines Java/WAR/Eclipse/IDEA/Sonar support and custom WAR tasks:

- `war`
- `buildOpendapWar`
- `buildNgapWar`
- `buildRobotsWar`
- `sonar`

It uses both Maven Central dependencies and `lib/` via `flatDir`. It also has custom version substitution logic.

There are signs of drift:

- Gradle source compatibility is set to 1.9, while Ant compiles for 1.8.
- Gradle `clean` deletes `src/opendap/bes/Version.java`.
- Gradle `substituteVersionInfo` processes `**/*.java.in`, but no active `Version.java.in` was found.
- The checked-in `Version.java` still contains Ant-style tokens.
- Some dependencies appear duplicated or version-divergent between checked-in jars and Gradle declarations.

CI has a Gradle build stage, but the release/package stages are Ant based. Treat Gradle support as useful but still in-progress unless the project maintainers explicitly say otherwise.

### CI

`.travis.yml` contains several relevant stages:

- build stage:
  - installs Ant
  - runs `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server`
  - also has a Gradle stage downloading Gradle 8.14.3 and running `gradle tasks` and `gradle war`
- scan stage:
  - runs Snyk via `run-snyk.sh`
  - runs `gradle sonar`
- snapshot stage:
  - uses Ant `DISTRO`
  - packages OLFS/Hyrax and NGAP artifacts

This supports the conclusion that Ant is the production build authority and Gradle is supplemental.

## Runtime Deployment Descriptors

### Main Hyrax Webapp

Primary descriptor:

- `resources/hyrax/WEB-INF/web.xml`

Important filters:

- `ClickJackFilter`: `opendap.coreServlet.ClickjackFilter`, mapped to `/*`.
- `BotFilter`: `opendap.coreServlet.BotFilter`, mapped to `/*`.
- `UrlRewriteFilter`: Tuckey URL rewrite filter, mapped to `/*`.
- `IdFilter`: present but commented out by default.
- `PEPFilter`: present but commented out by default.

Important servlets:

- `hyrax`: `opendap.coreServlet.DispatchServlet`, mapped to `/hyrax/*`.
- `wcs`: `opendap.wcs.v2_0.http.Servlet`, mapped to `/wcs/*`.
- `w10n`: `opendap.w10n.W10nServlet`, mapped to `/w10n/*`.
- `aggregation`: `opendap.aggregation.AggregationServlet`, mapped to `/aggregation/*`.
- `viewers`: `opendap.viewers.ViewersServlet`, mapped to `/viewers/*`.
- `docs_servlet`: `opendap.coreServlet.DocServlet`, mapped to `/docs/*`.
- `site_map_service`: `opendap.bes.BESSiteMapService`, mapped to `/siteMap/*`.

The URL rewrite rules in `resources/hyrax/WEB-INF/urlrewrite.xml` preserve explicit service prefixes such as `wcs`, `docs`, `viewers`, `hyrax`, `w10n`, `aggregation`, and `siteMap`. Requests outside those prefixes are rewritten into `/hyrax/...`, making the main dispatcher the default route for dataset-style paths.

### NGAP Webapp

Primary descriptor:

- `resources/ngap/web.xml`

NGAP exposes many of the same services as the main Hyrax webapp, but with NGAP-specific config:

- deployment context defaults to `ngap`
- `IdFilter` is enabled
- `PEPFilter` is enabled
- NGAP-specific `urlrewrite.xml`
- NGAP-specific `olfs.xml`
- NGAP-specific `logback.xml`
- `<distributable/>` is enabled

The NGAP rewrite rules send ordinary paths to `/hyrax/ngap/...` and route `/` or `/index.html` to the NGAP landing documentation page.

### Robots/Sitemap Webapp

Primary descriptor:

- `resources/robots/WEB-INF/web.xml`

This builds as a standalone ROOT context application. It maps:

- `BESSiteMapService` to `/siteMap/*`
- JSP handling to `/robots.txt` and `*.jsp`

It is designed to expose robot and sitemap behavior separately from the full OLFS webapp.

### PDP Service Webapp

Primary descriptor:

- `resources/pdpService/WEB-INF/web.xml`

This standalone service maps:

- `opendap.auth.PDPService` to `/*`

The servlet evaluates authorization decisions from request parameters such as user id, auth context, resource id, query, and action. By default it expects secure transport unless configured otherwise.

### WCS Standalone Descriptor

Primary descriptor:

- `resources/WCS/2.0/WEB-INF/web.xml`

This descriptor maps:

- `opendap.wcs.v2_0.http.Servlet`
- `opendap.coreServlet.DocServlet`

The main Hyrax WAR also exposes WCS 2.0 under `/wcs/*`, so this descriptor is best understood as standalone WCS packaging support.

### Legacy Or Experimental Descriptors

Some descriptors in active `resources/` do not appear production-active:

- `resources/WCS/1.1.2/WEB-INF/web.xml` references `opendap.wcs.v1_1_2.http.Servlet`, but no active `src/opendap/wcs/v1_1_2` package was found.
- `resources/experiments` contains descriptors referencing packages such as `opendap.noaa_s3`, `opendap.aws.glacier`, `opendap.experiments`, and `opendap.hai`; these classes were not found in active `src/`.

These files may be historical, prototype, or externally coupled. Do not assume resource presence means production support.

## Main Configuration

Primary default configuration:

- `resources/hyrax/WEB-INF/conf/olfs.xml`

Important defaults:

- BES root prefix `/`
- BES host `localhost`
- BES port `10022`
- BES client pool maximum `200`
- BES max commands `2000`
- node cache max entries `20000`
- node cache refresh interval `600`
- site map cache refresh interval `600`
- THREDDS prefix `thredds`
- THREDDS remote catalogs disabled by default
- gateway prefix `gateway`
- dataset URL response type `download`
- DAP request form type `dap4`
- HTTP POST enabled with max size `2000000`

Commented or optional controls include:

- direct data source access
- forced HTTPS links
- required user selection before file access
- download filename suffix handling
- combined logging to BES
- dynamic navigation disabling
- bot filtering rules
- timers
- NCML preload

The code path for configuration localization is in `opendap.coreServlet.ServletUtil`. It checks `/etc/olfs/`, then `/usr/share/olfs/`, then bundled `WEB-INF/conf`. The README/install files mention `OLFS_CONFIG_DIR`, but that variable was not found in the active config lookup code. Treat that as documentation/code drift to verify before relying on the environment variable.

`PersistentConfigurationHandler` copies bundled default configuration into a persistent config directory when the expected semaphore file, such as `olfs.xml`, is missing.

## Core OLFS Request Flow

The central servlet is:

- `opendap.coreServlet.DispatchServlet`

It initializes:

- logging
- persistent configuration
- `olfs.xml`
- combined logging settings
- HTTP POST controls
- request timers
- `BESManager`
- authentication controls
- GET and POST dispatch handler chains

The GET handler chain is ordered as follows:

1. `opendap.bes.VersionDispatchHandler`
2. `opendap.ncml.NcmlDatasetDispatcher`
3. `opendap.threddsHandler.StaticCatalogDispatch`
4. `opendap.gateway.DispatchHandler`
5. `opendap.ngap.NgapDapDispatcher`
6. `opendap.bes.BesDapDispatcher`
7. `opendap.bes.DirectoryDispatchHandler`, unless dynamic navigation is disabled
8. `opendap.bes.BESThreddsDispatchHandler`, unless dynamic navigation is disabled
9. `opendap.bes.FileDispatchHandler`

The POST handler chain includes:

1. `opendap.ngap.NgapDapDispatcher`
2. `opendap.bes.BesDapDispatcher`

`DispatchServlet` opens a request cache, assigns a request id, logs access details, and dispatches to the first handler that claims the request. If no handler matches, it returns a not-found response. HEAD handling is intentionally narrow and only accepts root-ish HEAD requests.

The handler interface is:

- `opendap.coreServlet.DispatchHandler`

Handlers implement:

- `init`
- `requestCanBeHandled`
- `handleRequest`
- `getLastModified`
- `destroy`

## BES Integration

The BES integration center is:

- `opendap.bes.BESManager`
- `opendap.bes.BesApi`

`BESManager` is a singleton that reads BES configuration from `olfs.xml`, groups BES instances by path prefix, requires a root `/` prefix, and chooses the longest matching prefix for a request. Within a group, it round-robins among configured BES instances.

`BesApi` builds BES XML requests and transmits them using the PPT/BES client code. It handles:

- BES version requests
- catalog and node requests
- DAP2 and DAP4 metadata/data responses
- file response support
- request id propagation
- max response and variable size settings
- XML base handling
- DAP4 checksum flags
- request context fields used by BES modules

Supporting packages include:

- `opendap.ppt`: PPT client and protocol support.
- `opendap.io`: chunked transfer and Hyrax encoding support.
- `opendap.dap4`: DAP4 request/response metadata and helpers.

## DAP And Dataset Responses

The main DAP dispatcher is:

- `opendap.bes.BesDapDispatcher`

It reads configuration flags such as:

- `AllowDirectDataSourceAccess`
- `RequireUserSelection`
- `ShowDmrppLink`
- `DataRequestForm`
- `ForceLinksToHttps`
- `DatasetUrlResponse`
- `AddFileoutTypeSuffixToDownloadFilename`
- `HttpPost max`

It builds a large ordered set of DAP responders.

DAP4 support includes:

- Dataset Services Response
- `.dap`
- `.dmr`
- `.dmr.html`
- `.dmr.rdf`
- `.dmr.json`
- `.dmr.ijsn`
- `.dmr.iso`
- `.dmr.rubric`
- output alternatives such as CSV, XML, NetCDF, TIFF, JPEG2000, JSON, I-JSON, and CoverageJSON

DAP2 support includes:

- `.dods`
- `.ascii`
- `.csv`
- `.nc`
- `.nc4`
- `.xdap`
- `.tiff`
- `.jp2`
- `.json`
- `.ijsn`
- `.covjson`
- `.ddx`
- `.dds`
- `.das`
- `.rdf`
- `.info`
- `.iso`
- `.rubric`

`opendap.bes.dap4Responders.Dap4Responder` handles suffix matching, alternative representations, and DAP4 HTTP Accept negotiation. It returns `406 Not Acceptable` when no acceptable representation can be produced.

`opendap.bes.dapResponders.FileAccess` controls direct file/source access. Without `AllowDirectDataSourceAccess`, direct source access is denied. Dataset URL behavior is controlled by `DatasetUrlResponse`, which can produce a DSR-style response, redirect to a request form, or attempt download behavior.

## Other Services And Servlets

### WCS 2.0

Primary class:

- `opendap.wcs.v2_0.http.Servlet`

Primary mapping:

- `/wcs/*`

This servlet initializes WCS service configuration, request handlers, and a `WcsServiceManager`. It also attempts to initialize `BESManager` from `olfs.xml` if needed. GET requests go through the HTTP GET handler. POST requests try XML, SOAP, and form handling.

Default WCS service configuration:

- `resources/hyrax/WEB-INF/conf/wcs_service.xml`

This loads:

- service identification XML
- service provider XML
- operations metadata XML
- WCS catalog implementations

The default catalog configuration uses dynamic catalog support, including a COADS dynamic service example.

### W10n

Primary class:

- `opendap.w10n.W10nServlet`

Primary mapping:

- `/w10n/*`

W10n initializes `W10nResponder` and registers a W10n service. It asks BES for W10n path information, distinguishes metadata requests by trailing slash, and supports JSON/HTML metadata plus data outputs such as W10n JSON, DAP2, NetCDF-3, and NetCDF-4.

Common query parameters include:

- `output`
- `callback`
- `reCache`
- `flatten`
- `traverse`

W10n rendering uses XSL resources such as:

- `w10nDataset.xsl`
- `showNodeToW10nCatalog.xsl`

### Aggregation

Primary class:

- `opendap.aggregation.AggregationServlet`

Primary mapping:

- `/aggregation/*`

The aggregation servlet was developed for archive-style workflows that build ZIP archives by iteratively calling BES. It supports GET, POST, and HEAD.

Operations include:

- `version`
- `file`
- `netcdf3`
- `netcdf4`
- `ascii`
- `csv`

Inputs include file parameters, variable selections or constraint expressions, and optional bounding boxes. Test and example resources live under `resources/aggregation`.

### Viewers

Primary class:

- `opendap.viewers.ViewersServlet`

Primary mapping:

- `/viewers/*`

The viewers service loads `viewers.xml`, locates WebStart resources, and creates handlers for configured viewers and web services. Built-in viewer/service handlers include:

- IDV
- NetCDF Tools
- Autoplot
- ncWMS/Godiva
- WCS

Requests generally need:

- `dapService`
- `datasetID`

The root viewers endpoint produces a dataset viewer page. Other viewer ids can return JNLP launch content.

### Documentation

Primary class:

- `opendap.coreServlet.DocServlet`

Primary mapping:

- `/docs/*`

`DocServlet` serves documentation from the persistent config docs directory when present, otherwise from bundled webapp documentation. It performs token replacement for context and servlet values in text-like files.

### Sitemap And Robots

Primary class:

- `opendap.bes.BESSiteMapService`

Primary mapping in main app:

- `/siteMap/*`

Standalone mapping:

- ROOT app via `resources/robots/WEB-INF/web.xml`

This service initializes BES support if necessary and emits robots/sitemap content using BES site map data and configured cache behavior.

### Gateway

Primary class in active dispatcher chain:

- `opendap.gateway.DispatchHandler`

The gateway handler extends the BES DAP dispatcher with a gateway-oriented BES API. It handles a configured prefix, usually `gateway`, serves a gateway form, and proxies remote data source requests through BES gateway support.

There is also a deprecated standalone `opendap.gateway.DispatchServlet`. It is not mapped by the main Hyrax web descriptor.

### NGAP

Primary class:

- `opendap.ngap.NgapDapDispatcher`

NGAP is integrated into the main dispatcher chain and into the NGAP-specific WAR. It extends the BES DAP dispatcher with `NgapBesApi`, Earthdata Login/URS context, NGAP BES module fields, and DMR++ response support.

The NGAP WAR enables authentication and authorization filters and uses CloudWatch-oriented logging configuration.

### Authentication, Authorization, And PDP

Important classes:

- `opendap.auth.IdFilter`
- `opendap.auth.PEPFilter`
- `opendap.auth.PDPService`
- `opendap.auth.UrsIdP`
- `opendap.auth.UserProfile`
- `opendap.auth.PolicyDecisionPoint`

`IdFilter` manages authentication sessions and user identity providers using `user-access.xml`.

`PEPFilter` enforces configured policy decisions. It can redirect unauthenticated users to an IdP or return 401/403 responses.

`PDPService` exposes policy decisions as a standalone servlet. It evaluates request fields such as:

- user id
- authentication context
- resource id
- query string
- action

The default user-access configuration is permissive in development-oriented ways and should be reviewed carefully before production changes.

### Bot And Clickjacking Filters

`opendap.coreServlet.BotFilter` blocks or handles requests based on bot filtering rules configured in `olfs.xml`. It can match by IP, user agent, path, and response behavior.

`opendap.coreServlet.ClickjackFilter` is installed over all requests and is intended to set clickjacking-related response protection.

## Static And Dynamic Navigation

Static THREDDS support is handled by:

- `opendap.threddsHandler.StaticCatalogDispatch`

Dynamic directory and catalog behavior includes:

- `opendap.bes.DirectoryDispatchHandler`
- `opendap.bes.BESThreddsDispatchHandler`

Default THREDDS configuration uses prefix `thredds` and disables remote catalog traversal. XSL resources transform static catalogs and BES node listings into browsable HTML and catalog responses.

Dynamic navigation can be disabled through `NoDynamicNavigation` in `olfs.xml`.

## Resource Areas

### `resources/hyrax`

This is the main webapp resource tree. It contains:

- main `WEB-INF/web.xml`
- main `urlrewrite.xml`
- default `olfs.xml`
- `user-access.xml`
- `viewers.xml`
- `wcs_service.xml`
- XSL transforms
- JSP pages
- error pages
- JavaScript and static assets
- WebStart resources
- gateway form resources

### `resources/WCS`

This tree contains WCS resources. WCS 2.0 is active in the main webapp. WCS 1.1.2 resources appear legacy or incomplete relative to active source.

### `resources/ngap`

This tree contains the NGAP web descriptor, config, URL rewrite rules, logging config, landing documentation, session manager support, NGAP-specific errors, and jars.

### `resources/robots`

This tree contains resources for the standalone ROOT robots/sitemap app.

### `resources/pdpService`

This tree contains resources for the standalone PDP service app.

### `resources/aggregation`

This tree contains examples, tests, and baselines for aggregation behavior.

### `resources/experiments`

This tree appears experimental and references classes not present in active `src/`. Treat it as non-production unless maintainers say otherwise.

## Documentation

`doc/` contains static documentation, CSS, images, and install/user guide material. The Ant build copies it into the webapp documentation location.

`DocServlet` may also serve localized documentation from a persistent config directory if installed.

## Tests

Tests are mixed into active source packages. The Ant `check` target currently runs a focused suite including:

- `opendap.coreServlet.Scrub`
- `opendap.aggregation.AggregationParamsTest`
- `opendap.auth.UrsIdPTest`
- `opendap.bes.dap4Responders.Dap4ResponderTest`
- `opendap.coreServlet.RequestIdTest`

Additional tests exist but are not necessarily part of Ant `check`, including WCS and DAP4 tests.

Because much of this repository is servlet and BES-integrated code, unit tests cover only part of the behavior. Changes to dispatching, BES calls, config parsing, or web descriptors often require WAR-level testing with Tomcat and a BES instance.

## Risks, Drift, And Things To Watch

- Ant is the production build authority; Gradle is present but not equivalent.
- Version token filtering is Ant-oriented. Be careful changing `src/opendap/bes/Version.java` or Gradle clean/substitution behavior.
- Some active resources reference missing source packages. Verify class existence and build inclusion before treating a descriptor as active.
- `resources/experiments` appears non-production despite being outside `retired/`.
- Documentation mentions `OLFS_CONFIG_DIR`, but active config lookup code checked `/etc/olfs`, `/usr/share/olfs`, and bundled config.
- Dependency versions differ between checked-in `lib/` jars and Gradle declarations.
- Main web.xml is broad and includes several services; servlet changes should consider rewrite rules and descriptor mappings.
- Authentication and authorization behavior differs between default Hyrax and NGAP descriptors.
- Direct source file access is deliberately gated by `AllowDirectDataSourceAccess`.
- NGAP behavior depends on Earthdata Login/URS context, auth filters, and CloudWatch logging configuration.
- WCS 2.0 is active; WCS 1.1.2 appears legacy in this active tree.
- Dynamic navigation and THREDDS behavior depend on config, XSL transforms, and BES node/catalog responses.

## Practical Change Guidance

For production-facing changes:

1. Start by checking the relevant web descriptor and Ant target.
2. Prefer Ant commands for build verification.
3. Keep Gradle changes synchronized with Ant only when intentionally improving Gradle support.
4. Do not edit generated `build/` outputs.
5. Do not assume resources are active unless they are copied by Ant, referenced from a descriptor, or instantiated by active code.
6. For servlet changes, inspect URL rewrite behavior as well as Java code.
7. For request handling changes, check the `DispatchServlet` handler order.
8. For DAP changes, check both DAP2 and DAP4 responders.
9. For config changes, check persistent config installation behavior.
10. For security changes, check both default Hyrax and NGAP deployments.

