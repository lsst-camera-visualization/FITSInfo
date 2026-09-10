# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

FITSInfo is a web application for browsing and viewing LSST camera images. It is a JAX-RS (Jersey) REST backend packaged as a WAR (Java 17, Tomcat), plus a static JavaScript frontend built on OpenSeadragon. The backend queries per-site image-metadata databases and relays new-image notifications; the frontend renders deep-zoomable images served by external Cantaloupe IIIF servers.

## Build & test

```bash
mvn package        # compiles, runs tests, builds target/FITSInfo-<version>.war
mvn test           # run all tests (JUnit 4)
mvn test -Dtest=FilterTest#testUnary   # single test method
```

The build inherits from the `org.lsst.ccs.parent` POM and resolves dependencies from the LSST Nexus repository (`repo-nexus.lsst.org`), so building requires network access to that repo. The WAR deploys under context path `/FITSInfo` (see `src/main/webapp/META-INF/context.xml`) to a servlet container (NetBeans project targets Tomcat).

There is no JS build step — frontend files under `src/main/webapp/` are served as-is. `d3.min.js` and `plot.min.js` are vendored libraries; `openseadragon/` is a vendored copy of OpenSeadragon plus plugins.

## Architecture

### Multi-site data sources
The app serves several independent LSST installations (sites). Every REST path is prefixed with `{site}` (e.g. `/rest/comcam/images`). `SessionUtil.getSession(site)` lazily builds and caches one Hibernate `SessionFactory` per site by loading `<site>.cfg.xml` from `src/main/resources/`. To add a site, add a `<site>.cfg.xml`. Known sites: `comcam`, `auxtel`, `maincamera`/`lsstcam`, `lsstcam-bts`, `tucson` (H2), plus site aliases mapped in `EventSender`. All Hibernate sessions are opened **read-only**.

Note: config XMLs contain plaintext DB credentials for production databases — these are committed and long-standing, not something to "fix" unprompted.

### REST layer (`org.lsst.fits.fitsinfo`)
`MyConfiguration` (`@ApplicationPath("/rest")`) registers all resources:
- `FitsDataSource` — `/{site}/images` (paged/grouped/filtered/sorted list), `/{site}/image/{id}`, `/{site}/imageInfo/{id}` (image + next/previous, `id` may be `latest`), `/{site}/nodeMap/{id}` (raft→IIIF-server layout).
- `FitsHeaderDataSource` (`org.lsst.ccs.fits.headers.restdatasource`) — `/{site}/headers/{id}[/{component}]`, reads per-image FITS-header JSON from a hardcoded local path (`/home/tonyj/Data/`) via `JsonReader`.
- `WebHook` — `POST /webhook` receives new-image notifications from external systems.
- `EventSender` — `GET /{site}/notify` is a Server-Sent Events stream; the webhook fans out to connected SSE clients.
- `CORSResponseFilter` — adds permissive CORS headers.

### Notification flow
External system → `POST /rest/webhook` → `WebHook` → `NotificationsManager` (singleton, one `SseBroadcaster` per `Source`) → broadcasts `newImage` SSE event → browser `EventSource` in `view.html` reloads. `NotificationsManager` has a header comment documenting curl test commands and the nginx config SSE requires (`proxy_buffering off`, etc.) — consult it when notifications appear broken.

### DevExtreme query protocol (`Filter`, `Sort`, `Group`, `GroupSummary`)
The frontend uses the DevExtreme DataGrid, which sends `filter`/`sort`/`group` as JSON query params. Each is a JAX-RS `@QueryParam` type with a `fromString`/`fromObjects` factory that parses the DevExtreme JSON into Hibernate JPA Criteria predicates against the `Image` entity. `Filter` is recursive (`SimpleFilter`/`UnaryFilter`/`ComplexFilter`); `FilterTest` covers the parsing shapes. Changing the query contract means keeping these parsers and the `index.html` grid config in sync.

### Domain model
- `Image` (`org.lsst.fits.dao`) — the `ccs_image` JPA entity; composite key (`telCode`, `controller`, `dayobs`, `seqnum`). Several fields (`obsId`, `run`, `runMode`) are `@Formula`-computed SQL, not stored columns. `Image_` is the JPA static metamodel — regenerate/update it alongside `Image` field changes.
- `ImageName` (`org.lsst.ccs.imagenaming`) — parses the canonical image-name format `SS_C_YYYYMMDD_NNNNNN` (source code, controller code, dayobs, seqnum). `Source`/`Controller` enums map two-letter codes. IDs in REST paths are these strings.

### Frontend pages (`src/main/webapp/`)
- `index.html` — image-browser landing (DevExtreme DataGrid); picks default site/data-sources from `window.location.hostname` (chile / BTS / SLAC).
- `view.html` — the OpenSeadragon deep-zoom viewer. Chooses IIIF (Cantaloupe) server URLs by site, builds the full-focal-plane raft mosaic (`R00`..`R44` → per-node IIIF servers, hardcoded in a `MyOpenSeadragon` subclass), wires toolbar buttons, next/prev navigation, and the SSE live-update `EventSource`.
- `colorstretch.js` (`ColorStretch` global) — client-side colormap/stretch engine: decoders unpack 18-bit (or 24-bit) pixel values encoded in RGB channels, histogram accumulation drives auto-stretch, and colormaps (SAO-style) map scalar→RGB. This is the substance of the current `feature/colorstretch` work.
- `openseadragon-filtering-lsst.js` — a locally patched fork of the NIST OpenSeadragon filtering plugin (adds zoom level to the canvas passed to processors); `colorstretch` plugs in here as an OSD filter processor.
- `test-colorstretch.html`, `test-manual.html` — standalone manual test/harness pages for the stretch pipeline.

## Conventions
- `javax.*` (Java EE 8 / Jakarta pre-namespace-change), not `jakarta.*` — Jersey 2.31.
- Hibernate queries are built with the JPA Criteria API throughout `ImageDAO`, never HQL/SQL strings.
- `Main` in `org.lsst.fits.dao` is a scratch/manual DB-connectivity check (`main` method), not part of the deployed app.
