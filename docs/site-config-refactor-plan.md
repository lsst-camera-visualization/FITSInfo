# Implementation Plan: Centralized Site Configuration

## Objective

Replace duplicated, code-branching site logic in `src/main/webapp/index.html` and
`src/main/webapp/view.html` with a single backend-served configuration that both
pages consume as data. Behavior-preserving refactor.

## Scope

**In scope:**
- Site catalog (the `dataSources` list).
- Host detection (`cp.lsst.org`→chile / `ls.lsst.org`→BTS / `slac.stanford.edu`→slac).
- Viewer routing in `view.html` (`iif2`, `raftName`, `site`, `sources`, `runField`/`sourceField` visibility).

**Deferred to a follow-up (schema leaves room; do not implement now):**
- `index.html` `setView()` column/group configuration.
- The two `multiGeom` raft-mosaic tables → the existing `nodeMap` endpoint.
- The transient `dc02dc10swap` date hack (stays inline in `view.html`).

## Open questions to resolve before/during implementation

These come from ambiguities in the current code. Confirm with the author; do not
silently guess when extracting literals:

1. **`MC_` image with no `view` param.** Current code picks `lsstcam` vs `lsstcam-bts`
   purely by host. Environment `sites` ordering in `sites.json` must preserve this.
2. **`TS8` label → `maincamera` site.** `index.html` maps dropdown entry `"TS8"` to
   `site="maincamera"`. Kept faithfully; confirm it is not stale.
3. **`sources` lists per site.** Extracted from current `view.html` branches
   (auxtel=`["raw","RubinTV"]`, comcam=`["raw","postISR","calexp"]`,
   lsstcam=`["raw","postISR"]`, default=`["raw","RubinTV"]`). Verify each.

## Guiding principle

Build `sites.json` by extracting the **exact current literals** — no value cleanup.
Any behavioral diff after the change is therefore a bug, not an intended change.

---

## Step 1 — Author `src/main/resources/sites.json`

Create the config file. Values below are extracted from current code; verify against
the open questions above.

```jsonc
{
  "environments": [
    { "match": "cp.lsst.org",       "name": "chile", "suffix": ".cp.lsst.org",
      "sites": ["lsstcam", "comcam", "auxtel"] },
    { "match": "ls.lsst.org",       "name": "BTS",   "suffix": ".ls.lsst.org",
      "sites": ["lsstcam-bts"] },
    { "match": "slac.stanford.edu", "name": "slac",  "suffix": ".cp.lsst.org",
      "sites": ["maincamera"] },
    { "match": "",                  "name": "other", "suffix": ".cp.lsst.org",
      "sites": ["lsstcam", "comcam", "auxtel", "maincamera", "tucson", "lsstcam-bts"] }
  ],
  "sites": {
    "lsstcam":     { "label": "Main Camera",   "telCode": "MC",
                     "iiif": "http://lsstcam-vs01{suffix}:8182/iiif/2/",
                     "defaultRaft": "all", "sources": ["raw", "postISR"],
                     "fields": { "run": false, "source": true } },
    "comcam":      { "label": "ComCam",        "telCode": "CC",
                     "iiif": "http://comcam-dc01.cp.lsst.org:8182/iiif/2/",
                     "defaultRaft": "R22", "sources": ["raw", "postISR", "calexp"],
                     "fields": { "run": false, "source": true } },
    "auxtel":      { "label": "AuxTel",        "telCode": "AT",
                     "iiif": "http://auxtel-dc01.cp.lsst.org:8182/iiif/2/",
                     "defaultRaft": "R00", "sources": ["raw", "RubinTV"],
                     "fields": { "run": false, "source": true } },
    "tucson":      { "label": "ComCam Tucson", "telCode": "CC",
                     "iiif": "http://comcam-dc01.tu.lsst.org:8182/iiif/2/",
                     "defaultRaft": "R22", "sources": ["raw", "RubinTV"],
                     "fields": { "run": false, "source": false } },
    "lsstcam-bts": { "label": "LSSTCam BTS",   "telCode": "MC",
                     "iiif": "http://lsstcam-vs01{suffix}:8182/iiif/2/",
                     "defaultRaft": "all", "sources": ["raw"],
                     "fields": { "run": false, "source": false } },
    "maincamera":  { "label": "TS8",           "telCode": "TS",
                     "iiif": "http://lsstcam-vs01{suffix}:8182/iiif/2/",
                     "defaultRaft": "R22", "sources": ["raw", "RubinTV"],
                     "fields": { "run": true, "source": false } }
  }
}
```

Notes:
- `{suffix}` token is substituted with the resolved environment `suffix`. Sites with a
  fixed host (comcam, auxtel, tucson) contain no token.
- Environment matching: first `match` whose substring is found in the hostname wins;
  the empty-string `match` is the catch-all default and must be last.

---

## Step 2 — Add `SiteConfigDataSource` JAX-RS resource

New file: `src/main/java/org/lsst/fits/fitsinfo/SiteConfigDataSource.java`

Endpoints (mirror the `@Path("/")` + `@Produces(APPLICATION_JSON)` style of
`FitsDataSource`):

- `GET /{...}/config?host={hostname}` — global: returns the resolved environment
  (`name`, `suffix`) plus the ordered list of `{ key, label }` for that environment's
  visible sites. Path chosen to not collide with `/{site}/images` etc. Recommended
  concrete paths:
  - `GET /config?host=...` → global catalog + resolved environment.
  - `GET /{site}/config` → the single-site object from `sites.json`, with `{suffix}`
    already substituted using the `host` query param (default env if `host` omitted).

Implementation notes:
- Load `sites.json` once from the classpath (`getClass().getResourceAsStream("/sites.json")`),
  parse with Jackson (`ObjectMapper` — already a dependency via
  `jersey-media-json-jackson`). Cache the parsed object in a static field.
- Resolve environment: iterate `environments`, return first whose `match` is a
  substring of `host` (empty `match` always matches → catch-all). Substitute `suffix`
  into any `{suffix}` token in the returned `iiif` string.
- Keep the resource additive: no changes to existing resources.

---

## Step 3 — Register the resource

Edit `src/main/java/org/lsst/fits/fitsinfo/MyConfiguration.java`: add one line,
`register(SiteConfigDataSource.class);`. No other server changes.

---

## Step 4 — Refactor `view.html`

Replace the `iif2`/`site`/`sources`/field-visibility block (currently ~lines 146–215).

1. Fetch `/rest/config?host={window.location.hostname}` → obtain `suffix` and
   environment. This removes the duplicated `serverName.indexOf(...)` host detection
   (current line ~161) and resolves the "can't test on a laptop" FIXME (line ~159):
   an unknown laptop host falls through to the default environment.
2. Resolve the site key in this order: `view` query param → `telCode` derived from the
   image-name prefix (`AT_`/`CC_`/`MC_`/`TS_`) → environment default (first in `sites`).
3. Fetch `/rest/{site}/config`. Set:
   - `iif2` = site `iiif` (server already substituted `{suffix}`).
   - `raftName ||= defaultRaft`.
   - populate `#source` dropdown from `sources`.
   - toggle `#runField` / `#sourceField` visibility from `fields`.
4. Leave everything downstream unchanged: `restURL`, `EventSource("rest/"+site+"/notify")`,
   `openIdentifier(...)`, colorMaps/biases/scales lists, and the `multiGeom` mosaic
   (deferred) all keep working off the derived `site` string.

Sequencing: the config fetches are async; ensure the `imageName === "latest"` fetch and
`osd.openIdentifier(...)` calls run **after** config resolves (chain the promises).

---

## Step 5 — Refactor `index.html` (catalog + host detection only)

1. Fetch `/rest/config?host={window.location.hostname}`; build `dataSources` from the
   returned `{ key, label }` list. Removes the duplicated host-detection block
   (current lines ~21–29) and the hardcoded `dataSources` array (line ~22).
2. `setView()` column/group logic **stays as-is this round** (deferred). Its trailing
   `site = "comcam"` style assignments may read the site key from the catalog entry so
   keys are not hardcoded twice, but the column config itself is out of scope.
3. Ensure the grid is constructed after the catalog fetch resolves (the default `view`
   is `dataSources[0]`, which now comes from config).

---

## Step 6 — Verification (manual; no automated frontend tests exist)

Build: `mvn package` (compiles the new resource, runs existing JUnit tests, produces the WAR).

For **each** environment (chile / BTS / slac / other), using the `?host=` server-side
override so it is testable from a laptop, confirm against pre-change behavior:

- `index.html`: data-source dropdown contents and order; default selected site.
- `view.html`: resolved `site`, `iif2` URL (including `{suffix}` substitution),
  default raft, `#source` dropdown contents, and `#runField`/`#sourceField` visibility.
- Live update still connects: `EventSource("rest/{site}/notify")`.
- Spot-check an `MC_` image with no `view` param resolves to the same site as before in
  both chile and BTS (open question #1).

Regression bar: any observable difference from current behavior is a bug in the literal
extraction, not an intended change.

---

## Files touched

| File | Change |
|---|---|
| `src/main/resources/sites.json` | **new** — config source of truth |
| `src/main/java/org/lsst/fits/fitsinfo/SiteConfigDataSource.java` | **new** — serves config |
| `src/main/java/org/lsst/fits/fitsinfo/MyConfiguration.java` | +1 `register(...)` line |
| `src/main/webapp/view.html` | replace site-routing block with config fetch |
| `src/main/webapp/index.html` | replace host detection + `dataSources` with config fetch |

## Notes for the follow-up (not this effort)

- `index.html` `setView()` column/group config → add a `grid` key per site in `sites.json`.
- Raft mosaic: generalize the existing unused `nodeMap` endpoint in `FitsDataSource.java`
  to emit the full `x/y/width/xpixel/ypixel/url` the client needs, and replace both
  25-entry `multiGeom` tables in `view.html`.
