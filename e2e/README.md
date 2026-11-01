# QuickNovel Ultimate — E2E (WebdriverIO + Appium)

Android automation with **WebdriverIO**, **Appium UiAutomator2**, **Mocha**, and **Allure**.

**App under test:** `1.2.6` · defaults **dark mode** + **Ultimate Blue** (`BlueFire`) · English providers ON.  
Manuals: [`docs/manual-1.2.3/`](../docs/manual-1.2.3/).

## Scenarios

| Spec | Coverage |
|------|----------|
| `01-library` | Biblioteca, search, Import-B drop zone, pinned search |
| `02-navigation` | Bottom tabs: Biblioteca / Buscar / Historial / Ajustes / Secciones |
| `03-search` | Search tab + query input |
| `04-history` | Empty state or Leyendo / Visitadas |
| `05-settings` | Settings + Tutorial TTS |
| `06-import` | Import-B UI + system document picker |
| `07-reader-tts` | Seeds a novel (import fixture / search stream), opens reader, shows TTS, starts TTS — **no skips** |
| `08-providers` | HTTP probe + multi-select (Select none → enable 2+) + best-effort scoped search |
| `09-search-special-chars` | Blocks special chars by default; accepts them when Settings toggle is on |
| `10-plugins` | Settings → Plugins (LNReader) hub: open manage plugins / add repository entry points |
| `11-download-translate` | Novel detail → Download dialog: language picker + Gemini / Google online engine |
| `12-reader-loading` | Reader **Glyph Pulse (F)**: 本 overlay, no raw URL as title (cache may skip flash) |
| `13-theme-defaults` | Fresh install: **Modo oscuro** ON + **Ultimate Blue** |

### Reader seed

`07-reader-tts` / `12-reader-loading` use `test/helpers/seedNovel.js` with `fixtures/sample-novel.epub`:

1. Open an existing library card if present  
2. Else import the fixture via Import-B + DocumentsUI  
3. Else open a search hit and tap **Leer en línea**

## Local setup

1. Node 18+
2. Android SDK + running emulator (`adb devices`)
3. Build or copy a signed APK:

```bash
# from repo root
./gradlew :app:assembleRelease
# copy signed APK to e2e/apps/QuickNovel-Ultimate.apk
# or Desktop: QuickNovel-Ultimate-1.2.6-universal.apk
```

4. Install and run:

```bash
cd e2e
npm install
npx appium driver install uiautomator2
npm run providers:generate   # refresh list from Apis.kt
npm test                     # full suite
npm run test:smoke           # 01–04
npm run test:features        # 05, 13, 06, 09–11
npm run test:reader          # 07 + 12
npm run test:theme           # 13 only
npm run test:reader-loading  # 12 only
npm run test:providers       # 08 (long / network)
npm run test:plugins         # 10
npm run test:download-translate  # 11
npm run allure:report
```

### Env vars

| Variable | Meaning |
|----------|---------|
| `APP_PATH` | Absolute path to the APK |
| `ANDROID_DEVICE_NAME` | Emulator / device name |
| `ANDROID_PLATFORM_VERSION` | e.g. `14` |
| `CI` | Enables one mocha retry |

## CI

GitHub Actions workflow `.github/workflows/e2e-android.yml` runs on **every push** and PRs with **separate jobs**:

| Job | What it does |
|-----|----------------|
| `unit` | Gradle `:app:testDebugUnitTest` (incl. Glyph Pulse + theme defaults) |
| `build-apk` | Assemble + sign APK → artifact |
| `e2e` (matrix) | Parallel emulator jobs: **smoke**, **features**, **reader**, **providers** |
| `allure-report` | Merges Allure shards → HTML artifact |

`providers` uses `continue-on-error` so Cloudflare / site flakes do not fail the whole gate.

**Smoke “canceled” after green:** WDIO can finish (`5 passing`) and then `android-emulator-runner` hits **Terminate Emulator** while a newer push cancels the workflow (`cancel-in-progress`). Concurrency now cancels **only on pull_request**, not on push, so a passed smoke is not flipped by teardown cancel.

Download **allure-report** from the Actions run, or merge shards locally:

```bash
cd e2e
npx allure generate ./allure-results --clean -o ./allure-report
npx allure open ./allure-report
```
