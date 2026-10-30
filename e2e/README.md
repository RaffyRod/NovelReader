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
| `07-reader-tts` | Open novel (if present) + TTS control |
| `08-providers` | HTTP probe + search multi-select (varios proveedores) + search scoped results |

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
npm test
npm run test:providers       # providers-only (long)
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

GitHub Actions workflow `.github/workflows/e2e-android.yml` runs on **every push** (and PRs):

1. Build release APK  
2. Start Android emulator  
3. Run WebdriverIO suite  
4. Upload `allure-results` artifact  

Generate the HTML report locally from the downloaded artifact:

```bash
cd e2e
npx allure generate ./allure-results --clean -o ./allure-report
npx allure open ./allure-report
```
