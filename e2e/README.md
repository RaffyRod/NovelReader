# QuickNovel Ultimate — E2E (WebdriverIO + Appium)

Android automation with **WebdriverIO**, **Appium UiAutomator2**, **Mocha**, and **Allure**.

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
| `08-providers` | HTTP probe + search multi-select (varios proveedores) + search scoped results |
| `09-search-special-chars` | Blocks special chars by default; accepts them when Settings toggle is on |

### Reader seed

`07-reader-tts` uses `test/helpers/seedNovel.js` with `fixtures/sample-novel.epub`:

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
# or set APP_PATH to your Desktop APK
```

4. Install and run:

```bash
cd e2e
npm install
npx appium driver install uiautomator2
npm run providers:generate   # refresh list from Apis.kt
npm test                     # full suite
npm run test:smoke           # 01–04
npm run test:features        # 05, 06, 09
npm run test:reader          # 07
npm run test:providers       # 08 (long / network)
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
| `unit` | Gradle `:app:testDebugUnitTest` |
| `build-apk` | Assemble + sign APK → artifact |
| `e2e` (matrix) | Parallel emulator jobs: **smoke**, **features**, **reader**, **providers** |
| `allure-report` | Merges Allure shards → HTML artifact |

`providers` uses `continue-on-error` so Cloudflare / site flakes do not fail the whole gate.

Download **allure-report** from the Actions run, or merge shards locally:

```bash
cd e2e
npx allure generate ./allure-results --clean -o ./allure-report
npx allure open ./allure-report
```
