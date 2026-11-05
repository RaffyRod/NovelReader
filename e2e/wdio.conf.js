const path = require('path');
const fs = require('fs');
const { execFileSync } = require('child_process');

const APP_ACTIVITY = 'com.lagradost.quicknovel.MainActivity';

function resolveAppPath() {
  const envPath = process.env.APP_PATH;
  if (envPath && fs.existsSync(envPath)) {
    return path.resolve(envPath);
  }
  const candidates = [
    path.join(__dirname, 'apps', 'QuickNovel-Ultimate.apk'),
    path.join(__dirname, '..', 'app', 'build', 'outputs', 'apk', 'release', 'app-universal-release-unsigned.apk'),
    path.join(
      process.env.USERPROFILE || process.env.HOME || '',
      'Desktop',
      'QuickNovel-Ultimate-2.0.0-universal.apk',
    ),
    path.join(
      process.env.USERPROFILE || process.env.HOME || '',
      'Desktop',
      'QuickNovel-Ultimate-1.2.3-universal.apk',
    ),
  ];
  return candidates.find((p) => p && fs.existsSync(p)) || candidates[0];
}

/**
 * Reads applicationId from the APK so debug builds (*.ultimate.debug) match locators.
 * @param {string} apkPath
 * @returns {string|null}
 */
function detectPackageFromApk(apkPath) {
  if (!apkPath || !fs.existsSync(apkPath)) return null;
  try {
    const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || '';
    const btRoot = path.join(sdk, 'build-tools');
    if (!fs.existsSync(btRoot)) return null;
    const versions = fs.readdirSync(btRoot).sort().reverse();
    for (const ver of versions) {
      const aapt = path.join(btRoot, ver, process.platform === 'win32' ? 'aapt.exe' : 'aapt');
      if (!fs.existsSync(aapt)) continue;
      const out = execFileSync(aapt, ['dump', 'badging', apkPath], {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
      });
      const m = out.match(/package: name='([^']+)'/);
      return m ? m[1] : null;
    }
  } catch (_) {
    /* keep default */
  }
  return null;
}

const appPath = resolveAppPath();
const APP_ID =
  process.env.APP_PACKAGE ||
  detectPackageFromApk(appPath) ||
  'com.ultimateSoft.quicknovel.ultimate';
process.env.APP_PACKAGE = APP_ID;
const appiumHost = process.env.APPIUM_HOST || '127.0.0.1';
const appiumPort = Number(process.env.APPIUM_PORT || 4723);
const useExternalAppium = process.env.APPIUM_EXTERNAL === '1';
console.log(`[wdio] app=${appPath}`);
console.log(`[wdio] package=${APP_ID}`);

exports.config = {
  runner: 'local',
  // Nested arrays = one worker runs the group sequentially (avoids CI IPC storms)
  specs: ['./test/specs/**/*.e2e.js'],
  suites: {
    smoke: [
      [
        './test/specs/01-library.e2e.js',
        './test/specs/02-navigation.e2e.js',
        './test/specs/03-search.e2e.js',
        './test/specs/04-history.e2e.js',
      ],
    ],
    // One CI job / one emulator boot for everything after smoke (saves ~4× cold-boot tax).
    // Order: chrome-light + reader/download first (fresh nav), then plugins/host-health (heavy),
    // then providers probe last so a dead host cannot starve reader seed.
    general: [
      [
        // Reader/import first (stable FileProvider seed) — theme last so a
        // launcher-alias flake cannot starve the critical path under bail=1.
        './test/specs/06-import.e2e.js',
        './test/specs/07-reader-tts.e2e.js',
        './test/specs/12-reader-loading.e2e.js',
        './test/specs/05-settings.e2e.js',
        './test/specs/18-settings-menu-split.e2e.js',
        './test/specs/19-about-privacy.e2e.js',
        './test/specs/11-download-translate.e2e.js',
        './test/specs/17-chapter-numbers.e2e.js',
        './test/specs/14-sections-translations.e2e.js',
        './test/specs/09-search-special-chars.e2e.js',
        './test/specs/10-plugins.e2e.js',
        './test/specs/16-host-health.e2e.js',
        './test/specs/15-plugin-catalog.e2e.js',
        './test/specs/08-providers.e2e.js',
        './test/specs/13-theme-defaults.e2e.js',
      ],
    ],
    // Kept for local/debug runs (`npm run test:features`, etc.).
    features: [
      [
        './test/specs/13-theme-defaults.e2e.js',
        './test/specs/05-settings.e2e.js',
        './test/specs/18-settings-menu-split.e2e.js',
        './test/specs/19-about-privacy.e2e.js',
        './test/specs/06-import.e2e.js',
        './test/specs/09-search-special-chars.e2e.js',
      ],
    ],
    'features-plugins': [
      [
        './test/specs/10-plugins.e2e.js',
        './test/specs/16-host-health.e2e.js',
        './test/specs/15-plugin-catalog.e2e.js',
      ],
    ],
    'features-flows': [
      [
        './test/specs/11-download-translate.e2e.js',
        './test/specs/14-sections-translations.e2e.js',
      ],
    ],
    plugins: [
      [
        './test/specs/10-plugins.e2e.js',
        './test/specs/16-host-health.e2e.js',
        './test/specs/15-plugin-catalog.e2e.js',
      ],
    ],
    sections: [['./test/specs/14-sections-translations.e2e.js']],
    reader: [
      [
        './test/specs/07-reader-tts.e2e.js',
        './test/specs/12-reader-loading.e2e.js',
      ],
    ],
    providers: [['./test/specs/08-providers.e2e.js']],
  },
  exclude: [],
  maxInstances: 1,
  hostname: appiumHost,
  port: appiumPort,
  path: '/',
  capabilities: [
    {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': process.env.ANDROID_DEVICE_NAME || 'Android Emulator',
      'appium:platformVersion': process.env.ANDROID_PLATFORM_VERSION || undefined,
      'appium:app': appPath,
      'appium:appPackage': APP_ID,
      'appium:appActivity': APP_ACTIVITY,
      'appium:appWaitActivity': '*',
      'appium:noReset': false,
      'appium:fullReset': false,
      'appium:forceAppLaunch': true,
      'appium:shouldTerminateApp': true,
      'appium:autoGrantPermissions': true,
      'appium:newCommandTimeout': 300,
      'appium:adbExecTimeout': 180000,
      'appium:androidInstallTimeout': 180000,
      'appium:uiautomator2ServerInstallTimeout': 180000,
      'appium:uiautomator2ServerLaunchTimeout': 120000,
      'appium:disableWindowAnimation': true,
      'appium:ignoreHiddenApiPolicyError': true,
    },
  ],
  logLevel: process.env.WDIO_LOG_LEVEL || 'info',
  // Fail-fast always: continuing after chrome/nav death burns 60–80m on timeout walls.
  // Override with WDIO_BAIL=0 only when debugging a full matrix.
  bail: process.env.WDIO_BAIL === '0' ? 0 : 1,
  waitforTimeout: 10000,
  // Session create installs APK + UiAutomator2; CI_FAST used to set 90s and caused
  // UND_ERR_HEADERS_TIMEOUT (~95s) on GH runners.
  connectionRetryTimeout: 240000,
  connectionRetryCount: 2,
  specFileRetries: 0,
  services: useExternalAppium
    ? []
    : [
        [
          'appium',
          {
            args: {
              relaxedSecurity: true,
              allowInsecure: ['adb_shell'],
              address: appiumHost,
              port: appiumPort,
            },
            command: 'appium',
          },
        ],
      ],
  framework: 'mocha',
  reporters: [
    'spec',
    [
      'allure',
      {
        outputDir: './allure-results',
        disableWebdriverStepsReporting: false,
        disableWebdriverScreenshotsReporting: false,
        addConsoleLogs: true,
      },
    ],
  ],
  mochaOpts: {
    ui: 'bdd',
    // Root before/session needs headroom; per-describe this.timeout(90000) caps individual tests.
    timeout: Number(process.env.WDIO_TIMEOUT_MS || 120000),
    retries: 0,
    bail: process.env.WDIO_BAIL === '0' ? false : true,
  },
  before: async () => {
    await driver.updateSettings({ waitForIdleTimeout: 100 });
    await driver.setTimeout({ implicit: 0 }).catch(() => {});
    // Clear leftover ANR / frozen process from prior runs (local emulator gets stuck often).
    const { execFileSync } = require('child_process');
    try {
      execFileSync('adb', ['shell', 'am', 'force-stop', APP_ID], { stdio: 'ignore' });
    } catch (_) {
      await driver.terminateApp(APP_ID).catch(() => {});
    }
    await driver.pause(400);
    await driver.execute('mobile: startActivity', {
      component: `${APP_ID}/${APP_ACTIVITY}`,
    });
    await driver.pause(1200);
    const { dismissBlockingDialogs } = require('./test/helpers/dialogs');
    await dismissBlockingDialogs().catch(() => {});
  },
  /**
   * Between describe blocks in a nested worker: clear stuck dialogs from prior specs.
   */
  beforeSuite: async () => {
    const { dismissBlockingDialogs } = require('./test/helpers/dialogs');
    const chromeVisible = async () => {
      for (const id of [
        `${APP_ID}:id/navigation_download`,
        `${APP_ID}:id/navigation_search`,
        `${APP_ID}:id/navigation_settings`,
      ]) {
        if (
          await $(`android=new UiSelector().resourceId("${id}")`)
            .isDisplayed()
            .catch(() => false)
        ) {
          return true;
        }
      }
      return false;
    };
    if (await chromeVisible()) {
      await dismissBlockingDialogs().catch(() => {});
      return;
    }
    await dismissBlockingDialogs().catch(() => {});
    if (await chromeVisible()) return;
    await driver.terminateApp(APP_ID).catch(() => {});
    await driver.pause(400);
    // Prefer explicit MainActivity — activateApp can resolve Ultimate*Launcher aliases.
    await driver
      .execute('mobile: startActivity', {
        component: `${APP_ID}/${APP_ACTIVITY}`,
      })
      .catch(() => {});
    await driver.pause(1000);
    await dismissBlockingDialogs().catch(() => {});
  },
  afterTest: async function (test, context, { error }) {
    if (error) {
      await driver.takeScreenshot().catch(() => {});
    }
    const { dismissBlockingDialogs } = require('./test/helpers/dialogs');
    const BasePage = require('./test/pageobjects/base.page');
    await dismissBlockingDialogs().catch(() => {});
    // Reader / picker / sheets leave MainActivity — restore chrome for the next spec.
    for (let i = 0; i < 2; i++) {
      const hasNav = await $(
        `android=new UiSelector().resourceId("${APP_ID}:id/navigation_search")`,
      )
        .isDisplayed()
        .catch(() => false);
      if (hasNav) return;
      await driver.back().catch(() => {});
      await driver.pause(200);
    }
    const page = new BasePage();
    // Hard relaunch — soft back loops after a dead chrome cost minutes per failed test.
    await page.recoverMainApp().catch(() => {});
  },
};
