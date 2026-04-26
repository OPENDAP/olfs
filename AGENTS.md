# AGENTS.md

Guidance for automated coding agents working in this repository.

## Scope

This repository is the Java servlet front end for Hyrax, commonly called the OLFS. It also contains related services and webapps, including WCS, W10n, aggregation, viewers, docs, sitemap/robots, gateway, NGAP, authentication/authorization, and PDP service code.

Do not inspect or modify `retired/` unless the user explicitly asks. Some code and resources outside `retired/`, especially under `resources/experiments` and older WCS resource folders, may still be unused or historical. Verify that a class/resource is wired into the active build or deployment descriptor before treating it as production behavior.

## Hyrax Server Documentation

The OLFS is the frontend to the Hyrax data server. The configuration guide for  Hyrax can be found online at [The Hyrax Data Server Installation and Configuration Guide](https://opendap.github.io/hyrax_guide/Master_Hyrax_Guide.html). 

Other guides that describe how the server is used for data access can be found at:
- [User Guide](https://opendap.github.io/documentation/UserGuideComprehensive.html)
- [Quick start Guide](https://opendap.github.io/documentation/QuickStart.html)

## Build Authority

Use Ant as the production build authority.

Common commands:

- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server`
- `ant check`
- `ant DISTRO`
- `ant ngap`
- `ant ngap-dist`
- `ant robots`
- `ant hyrax-robots`
- `ant -f pdpservice-build.xml server`
- `ant -f aggregation-build.xml server`

Gradle files are present and useful for ongoing Gradle support, Sonar, Snyk, and some CI checks, but Gradle is not the release authority unless maintainers say so. Cross-check Gradle behavior against `build.xml` and `.travis.yml`.

Important build caveat: Ant copies `src/` to `build/src` and filters version/context tokens before compiling. Do not edit generated `build/` files. Be careful with `src/opendap/bes/Version.java`, which contains Ant-filtered tokens in source.

## Java And Style

- Ant compiles with Java source/target 1.8.
- CI may run on newer JDKs.
- Follow existing Java package style and formatting.
- Keep LGPL/license headers consistent when adding source files.
- Prefer existing helpers in `opendap.coreServlet`, `opendap.http`, `opendap.bes`, and related packages.
- Avoid broad refactors unless explicitly requested.
- Use SLF4J/logback patterns already present in the codebase.
- Do not upgrade or remove jars in `lib/` casually; Ant depends on checked-in jars.

## Main Runtime Files

Primary Hyrax webapp:

- `resources/hyrax/WEB-INF/web.xml`
- `resources/hyrax/WEB-INF/urlrewrite.xml`
- `resources/hyrax/WEB-INF/conf/olfs.xml`
- `resources/hyrax/WEB-INF/conf/user-access.xml`
- `resources/hyrax/WEB-INF/conf/viewers.xml`
- `resources/hyrax/WEB-INF/conf/wcs_service.xml`

Other deployments:

- NGAP: `resources/ngap/web.xml`, `resources/ngap/urlrewrite.xml`, `resources/ngap/olfs.xml`
- WCS 2.0 standalone resources: `resources/WCS/2.0`
- PDP service: `resources/pdpService/WEB-INF/web.xml`
- Robots/sitemap app: `resources/robots/WEB-INF/web.xml`
- Aggregation resources: `resources/aggregation`

## Major Servlets And Services

When changing servlet behavior, consider all mapped services:

- `opendap.coreServlet.DispatchServlet`: main OLFS dispatcher, mapped to `/hyrax/*`.
- `opendap.wcs.v2_0.http.Servlet`: WCS 2.0, mapped to `/wcs/*`.
- `opendap.w10n.W10nServlet`: W10n, mapped to `/w10n/*`.
- `opendap.aggregation.AggregationServlet`: aggregation ZIP service, mapped to `/aggregation/*`.
- `opendap.viewers.ViewersServlet`: viewer and WebStart launcher support, mapped to `/viewers/*`.
- `opendap.coreServlet.DocServlet`: documentation servlet, mapped to `/docs/*`.
- `opendap.bes.BESSiteMapService`: sitemap and robots service, mapped to `/siteMap/*`.
- `opendap.auth.PDPService`: standalone PDP service.

The main dispatcher uses an ordered handler chain. Before changing request routing, inspect `opendap.coreServlet.DispatchServlet` and the relevant handler classes.

## Configuration Notes

Default bundled config lives under `resources/hyrax/WEB-INF/conf`. Runtime config lookup in code checks persistent locations such as `/etc/olfs/` and `/usr/share/olfs/` before falling back to bundled config. If changing config lookup or documented config environment behavior, update both code and docs.

`PersistentConfigurationHandler` can install bundled default configuration into a persistent config directory. Be careful changing semaphore config names such as `olfs.xml`, `viewers.xml`, and `wcs_service.xml`.

Security-sensitive config areas include:

- `AllowDirectDataSourceAccess`
- `DatasetUrlResponse`
- `user-access.xml`
- `IdFilter`
- `PEPFilter`
- `PDPService`
- NGAP/Earthdata Login behavior
- CloudWatch/combined logging settings
- bot filtering rules

## Testing Guidance

For small Java changes, start with:

- `ant check`

For WAR/build changes, use:

- `ant -DHYRAX_VERSION=CI-Build -DOLFS_VERSION=CI-Build show server`

For NGAP changes, use:

- `ant ngap`

For standalone service changes, use the matching Ant build file. Some integration behavior requires Tomcat and a BES instance; unit tests do not cover the whole servlet/BES surface.

## Dependency Guidance

Ant uses checked-in jars from `lib/` and related resource folders. Gradle uses Maven dependencies plus `flatDir` access to local jars. Keep these worlds synchronized when dependency changes are intentional.

Do not treat Gradle dependency declarations as proof that the Ant production WAR includes the same dependency.

## Git And File Hygiene

- Do not modify `retired/` unless explicitly asked.
- Do not modify generated `build/` output.
- Do not revert user changes.
- Keep docs updated when changing build, deployment descriptors, configuration, or servlet mappings.
- Prefer focused changes over broad cleanup.
- Before finishing, report which build/test commands were run or why none were run.

