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

## Resolved decisions (from author, 2026-09-10)

The original open questions have been answered:

1. **Site resolution is `telCode`-driven, not environment-driven.** The image-name
   prefix determines the site: `TS_` → `maincamera` (TS8), `AT_` → `auxtel`,
   `CC_` → `comcam`/`tucson`, `MC_` → `lsstcam`/`lsstcam-bts`. The `view` query param
   overrides. The environment supplies only (a) the host `suffix` and (b) which site
   keys populate the dropdown catalog. **Add an explicit `TS_` branch** — the current
   `view.html` has none (TS8 works only by falling through to the `site="maincamera"`
   default at line 155); make it intentional.
2. **`maincamera` (TS8) is currently inactive.** Its images are only accessible via S3,
   and that path is **not yet configured or supported** — planned future work. Keep
   `maincamera` in `sites.json` as a faithful entry (`label: "TS8"`, `telCode: "TS"`),
   but mark it `active: false` and do **not** invent a working IIIF URL for it. The
   `slac` environment must NOT list it as a live default. `MC_` on any non-BTS host
   resolves to `lsstcam` (preserving current behavior); TS8 is reached only via `TS_`.
3. **`sources` lists confirmed.** auxtel/tucson/`lsstcam-bts` inherit the default
   `["raw","RubinTV"]` (BTS confirmed as `["raw","RubinTV"]`, not the earlier draft
   guess of `["raw"]`); comcam=`["raw","postISR","calexp"]`; lsstcam=`["raw","postISR"]`.

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
    // NOTE: the slac environment lists `maincamera` only so the TS8 dropdown entry
    // still appears there (matching current index.html host==='slac' → ["TS8"]).
    // maincamera is `active: false` (S3-only, unsupported) so the viewer will not
    // route a working image to it yet. Per-image MC_ still resolves to lsstcam.
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
                     "defaultRaft": "all", "sources": ["raw", "RubinTV"],
                     "fields": { "run": false, "source": false } },
    "maincamera":  { "label": "TS8",           "telCode": "TS", "active": false,
                     "iiif": null,
                     "defaultRaft": "R22", "sources": ["raw", "RubinTV"],
                     "fields": { "run": true, "source": false } }
    // maincamera/TS8: images are S3-only; that access path is not yet configured or
    // supported. `active: false` + `iiif: null` until the S3 path is added (future
    // work). Keep the entry so the TS8 dropdown label survives; a TS_ image routing
    // here should surface a clear "not yet supported" state rather than a broken URL.
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
   image-name prefix → environment default (first in `sites`). `telCode` mapping:
   `TS_`→`maincamera`, `AT_`→`auxtel`, `CC_`→`comcam` (or `tucson` when `view==="Tucson"`),
   `MC_`→`lsstcam-bts` if the resolved environment is BTS else `lsstcam`. Add the `TS_`
   branch explicitly (current code lacks it and relies on the default fall-through).
3. Fetch `/rest/{site}/config`. Set:
   - `iif2` = site `iiif` (server already substituted `{suffix}`).
   - `raftName ||= defaultRaft`.
   - populate `#source` dropdown from `sources`.
   - toggle `#runField` / `#sourceField` visibility from `fields`.
   - **If the resolved site has `active: false` (e.g. `maincamera`/TS8, S3-only):** do
     not attempt to build an IIIF URL from `iiif: null`. Surface a clear "TS8 images are
     not yet supported (S3 access pending)" message in the viewer instead of a broken
     request. This is the one intentional behavior change vs. current code, which would
     silently produce a non-working `lsstcam-vs01` URL for a `TS_` image.
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
- `MC_` image with no `view` param resolves to `lsstcam` on chile/slac/other and
  `lsstcam-bts` on BTS (matches current code).
- `TS_` image resolves to `maincamera` and shows the "not yet supported (S3 pending)"
  state — no broken IIIF request. (Intentional change; see below.)

Regression bar: any observable difference from current behavior is a bug in the literal
extraction, **except** the one intentional change — a `TS_`/`maincamera` image now shows
an explicit unsupported-state message instead of a silently broken `lsstcam-vs01` URL.

---

## Files touched

| File | Change |
|---|---|
| `src/main/resources/sites.json` | **new** — config source of truth |
| `src/main/java/org/lsst/fits/fitsinfo/SiteConfigDataSource.java` | **new** — serves config |
| `src/main/java/org/lsst/fits/fitsinfo/MyConfiguration.java` | +1 `register(...)` line |
| `src/main/webapp/view.html` | replace site-routing block with config fetch |
| `src/main/webapp/index.html` | replace host detection + `dataSources` with config fetch |

## Follow-up items

- **DONE** — `index.html` `setView()` column/group config moved into a `grid` key per
  site in `sites.json`. Each site's `grid.columns` lists `{name, visible, groupIndex}`
  (JSON `null` groupIndex = ungroup → DevExtreme `undefined`), extracted verbatim from
  the old per-view branches. `index.html` fetches all visible sites' `/{site}/config`
  up front, caches them by label, and `setView()` applies `filter(telCode)` + column
  options generically from the cached config. The six near-identical branches are gone.
- **DONE — Raft mosaic** (the old "just use the `nodeMap` endpoint" note was too simple —
  that endpoint hardcoded SLAC URLs and ignored `source`, `suffix`, and the `dc02dc10swap`
  date hack). Implemented approach: **geometry → config, host logic stays client-side.**
  The dead `nodeMap` endpoint has since been **removed** from `FitsDataSource.java` (no
  frontend referenced it, and the client-side config-driven mosaic is now authoritative).

  1. **Geometry** is identical across both former `multiGeom` tables and is a pure function
     of the raft key: `R{r}{c}` → `x = c*0.2`, `width = 0.2`, `y = (4-r)*0.2`,
     `xpixel = c*xRaft`, `ypixel = r*yRaft`. `xRaft`/`yRaft` live in a per-site `mosaic`
     block in `sites.json` (only `lsstcam`/`lsstcam-bts`); the 25 rows are **derived** in
     `openIdentifier()`, not listed.
  2. **Per-raft host mapping** stays client-side (runtime-dependent):
     - `source === "raw"` → per-raft node from `mosaic.raw` (`R00`→`dc10`, etc.).
     - otherwise (postISR) → uniform `mosaic.default` (`vs01`).
  3. **Node names stored canonically padded; formatted per environment (fixes a latent
     bug).** The old code hardcoded `legacy = false`, so BTS was wrongly served *short*
     names (`dc2`/`vs1`); BTS actually runs *padded* names (`dc02`/`vs01`), the summit uses
     short. `sites.json` stores node names in padded form; an explicit **`paddedNodeNames`**
     boolean on each `environments[]` entry (BTS `true`, others `false`) is returned by
     `rest/config` and read in `resolveSiteConfig()`. When false, the client maps padded →
     short by stripping the leading zero (`dc02`→`dc2`, `vs01`→`vs1`) **except** the
     irregular `dc10`→`dc100`. Summit output is byte-identical to before; BTS is corrected.
  4. **`dc02dc10swap` date hack stays inline** in `openIdentifier()` (raw-only, keyed on the
     image name/date). Corners (the four rafts whose raw node is `dc10`) swap to
     `mosaic.swapNode` (`dc02`) for the 2025-05-02..07 MC_ window at the summit. **Decision:**
     the swap target now follows the same `paddedNodeNames` rule (`dc2` at summit, `dc02` at
     BTS), removing the old hardcoded-padded exception — changing the summit corner URL for
     that window (`dc02` → `dc2`), a deliberate correction alongside the BTS naming fix.
  5. Collapsed the two duplicated 25-row tables (~80 lines) into one derived builder + config.

  **Verification:** a Node harness reconstructed the old table logic and the new builder and
  diffed all 8 combinations (raw/postISR × padded/short × swap/normal) — every raft matched
  the bug-fixed expectation (old logic with `legacy = padded`), and summit short-name output
  reproduced the historic literals exactly. `mvn -o package` clean; `sites.json` valid;
  `view.html` JS passes `node --check`.
