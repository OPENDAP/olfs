# Hyrax OLFS (OPeNDAP Lightweight Front-end Server)

Status badges
- CI (Travis CI): https://travis-ci.org/OPENDAP/olfs (pipeline uses OpenJDK 17; Gradle and Ant)
- SonarCloud project: https://sonarcloud.io/project/overview?id=opendap-olfs

Overview
Hyrax is OPeNDAP’s modular data server. OLFS is the Java web front-end that handles HTTP requests, URL rewriting, user-facing endpoints, and proxies requests to the Hyrax back-end BES (Back-End Server). This repository builds a Java Servlet WAR (opendap.war) deployable to Apache Tomcat under the context /opendap.

Current versions (as of 2025-10-01)
- Hyrax: 1.17.1
- OLFS: 1.18.15
- Required back-end components:
  - BES 3.21.1 — https://github.com/OPENDAP/bes/releases/tag/3.21.1
  - libdap4 3.21.1 — https://github.com/OPENDAP/libdap4/releases/tag/3.21.1

Notes
- See install.html in this repository for additional installation details.
- Some legacy components under src/opendap/... are deprecated but retained for reference.

Technology stack
- Language: Java
- Packaging: WAR (Servlet)
- Application server: Apache Tomcat (recommended Tomcat 9+)
  - Web descriptor: resources/hyrax/WEB-INF/web.xml (Servlet 4.0)
- Build systems:
  - Gradle (preferred; wrapper included)
  - Apache Ant (legacy build maintained; still used for release packaging)
- CI/CD: Travis CI; SonarCloud analysis; Snyk security scan
- Logging: Logback (logback-classic)

Requirements
- Java Development Kit: OpenJDK 17 (CI builds with JDK 17). The Gradle build targets Java 9 bytecode (source/targetCompatibility 1.9) and is compatible with newer JDKs.
- Apache Tomcat: 9.x or newer recommended. Older servlet containers may work, but web.xml declares Servlet 4.0.
- BES and libdap4 installed and running (BES listener reachable from OLFS). You can use besctl to start BES and confirm beslistener is running.
- Tools (pick one build path):
  - Gradle 8.x (wrapper provided via ./gradlew)
  - Ant 1.9+ (for legacy build/release targets)
- Optional: Node.js/npm for Snyk tooling if running security scans locally.

Quick start
1) Get the code
   git clone https://github.com/OPENDAP/olfs.git
   cd olfs

2) Ensure BES is running
   - Build/install libdap4 and BES, start beslistener (e.g., using besctl).
   - Default OLFS config assumes BES at localhost:10022 (configurable).

3a) Build with Gradle (recommended)
   ./gradlew clean war
   # Artifact: build/libs/opendap.war

3b) Build with Ant (legacy)
   ant server
   # Distribution WAR: build/dist/opendap.war
   # To stamp a release build:
   ant server -DHYRAX_VERSION=<hyrax_version> -DOLFS_VERSION=<olfs_version>

4) Deploy to Tomcat
   export CATALINA_HOME=/path/to/tomcat
   rm -rf "$CATALINA_HOME"/webapps/opendap*
   # Using Gradle build output:
   cp build/libs/opendap.war "$CATALINA_HOME"/webapps/
   # Or Ant build output:
   # cp build/dist/opendap.war "$CATALINA_HOME"/webapps/

5) Start Tomcat
   "$CATALINA_HOME"/bin/startup.sh
   # Application will be available at: http://localhost:8080/opendap/

Configuration
- Default config location (after first deploy):
  $CATALINA_HOME/webapps/opendap/WEB-INF/conf

- Persistent configuration: choose one
  - Set environment variable OLFS_CONFIG_DIR to an existing directory writable by the Tomcat user
  - Or create /etc/olfs with read/write access for the Tomcat user
  Note: If both exist, OLFS_CONFIG_DIR takes precedence.

- Apply changes
  - Edit configuration files in the chosen location (copy is installed on first startup).
  - If your BES is not running on localhost:10022, edit olfs.xml and update <host> and <port>.
  - Restart Tomcat to apply changes:
    "$CATALINA_HOME"/bin/shutdown.sh; "$CATALINA_HOME"/bin/startup.sh

Entry points and web context
- Deployed context: /opendap (configurable via build/deploy)
- Primary deployment descriptor: resources/hyrax/WEB-INF/web.xml
- The WAR bundles JSPs, filters (e.g., urlrewritefilter), and the Hyrax/OLFS servlets.

Build, tasks, and scripts
Gradle (preferred)
- Common tasks:
  - ./gradlew clean war — build the opendap.war
  - ./gradlew test — run unit tests (JUnit 5 engine is configured)
  - ./gradlew showInfo — print build/version properties (HYRAX_VERSION, OLFS_VERSION, etc.)
  - ./gradlew sonar -Dsonar.token=... — run SonarCloud analysis (see CI example)
- Properties (overrides):
  - -POLFS_VERSION=x.y.z -PHYRAX_VERSION=x.y.z to set release numbers

Ant (legacy and release packaging)
- Notable targets (see build.xml for the full list):
  - ant server — build the OLFS WAR
  - ant DISTRO — build release distribution bundles (tar.gz)
  - ant ngap, ngap-dist — build NGAP variant
  - ant show — show configuration
  - ant check — run checks/tests (if available for your environment)
  - Variables: -DHYRAX_VERSION, -DOLFS_VERSION, -DOLFS_DIST_BASE, -DNGAP_DIST_BASE

Local scripts
- run-snyk.sh — runs Snyk security scan against dependencies
  Prereqs: npm install -g snyk snyk-gradle-plugin; env SNYK_TOKEN set
- releaseMe, mkDistro, mkReleaseTags — release helper scripts (internal use; see each script)
- travis/* — CI helper scripts used by Travis pipeline (e.g., compute_build_tags.sh)

Environment variables
- CATALINA_HOME — Apache Tomcat installation directory (required to deploy/run)
- OLFS_CONFIG_DIR — Directory for persistent OLFS configuration (optional; overrides /etc/olfs)
- HYRAX_VERSION — Version string used for release builds (Ant/Gradle)
- OLFS_VERSION — Version string used for release builds (Ant/Gradle)
- ANT_OPTS — JVM options for Ant builds (optional)
- JAVA_HOME — JDK location used by your build tools
- CI/analysis (used in CI): SONAR_LOGIN (token), SNYK_TOKEN, AWS_* for uploading snapshot artifacts

Tests
- Unit tests: Gradle uses JUnit 5; run with:
  ./gradlew test
- Ant-based checks: if available in your environment:
  ant check
- Additional integration and legacy tests may exist under retired/ and load_tests/ (not run by default).

Project structure (selected)
- build.gradle — Gradle build (Java 9 bytecode, WAR, Sonar integration)
- build.xml — Ant build with many targets (server, DISTRO, ngap, robots, etc.)
- resources/ — web resources, including hyrax/WEB-INF/web.xml and configuration templates
- src/ — Java source code for OLFS and related components
- gradle/ and gradlew — Gradle wrapper
- install.html — supplemental installation notes
- .travis.yml — CI definition (multi-stage; JDK 17; Gradle/Ant builds; Sonar; Snyk; snapshot packaging)
- COPYRIGHT — licensing information

License
This project is licensed under the GNU Lesser General Public License (LGPL) v2.1 or later. See the COPYRIGHT file for details.

Additional references
- Hyrax documentation and news: http://docs.opendap.org/index.php/Hyrax

Deprecated/legacy components
- Aggregation servlet in src/opendap/aggregation is likely no longer used but remains for reference; see its README and curl examples.

TODOs / Open items
- Document the exact Tomcat versions officially supported and any container-specific settings.
- Provide a Docker-based quickstart if available (hyrax-docker is referenced in CI, but container details belong in that repo).
- Expand testing section with integration test instructions if/when maintained tests are added back to active build.

This is gonna hurt

