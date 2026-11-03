const path = require('path');
const fs = require('fs');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
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

const appPath = resolveAppPath();
const appiumHost = process.env.APPIUM_HOST || '127.0.0.1';
const appiumPort = Number(process.env.APPIUM_PORT || 4723);
const useExternalAppium = process.env.APPIUM_EXTERNAL === '1';

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
    // Split for CI wall-clock: long plugin dialogs must not poison download/sections.
    features: [
      [
        './test/specs/13-theme-defaults.e2e.js',
        './test/specs/05-settings.e2e.js',
        './test/specs/06-import.e2e.js',
        './test/specs/09-search-special-chars.e2e.js',
      ],
    ],
    'features-plugins': [
      [
        './test/specs/10-plugins.e2e.js',
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
    },
  ],
  logLevel: process.env.WDIO_LOG_LEVEL || 'info',
  bail: 0,
  waitforTimeout: 15000,
  // Session create installs APK + UiAutomator2; CI_FAST used to set 90s and caused
  // UND_ERR_HEADERS_TIMEOUT (~95s) on GH runners. Keep retries for that path only.
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
    timeout: 180000,
    retries: process.env.CI ? 1 : 0,
  },
  before: async () => {
    await driver.updateSettings({ waitForIdleTimeout: 100 });
    await driver.execute('mobile: startActivity', {
      component: `${APP_ID}/${APP_ACTIVITY}`,
    });
    await driver.pause(2000);
  },
  /**
   * Between describe blocks in a nested worker: clear stuck dialogs from prior specs.
   */
  beforeSuite: async () => {
    const { dismissBlockingDialogs } = require('./test/helpers/dialogs');
    await dismissBlockingDialogs();
    const chrome = await Promise.all(
      [
        `${APP_ID}:id/navigation_download`,
        `${APP_ID}:id/navigation_search`,
        `${APP_ID}:id/navigation_settings`,
      ].map(async (id) =>
        $(`android=new UiSelector().resourceId("${id}")`)
          .isDisplayed()
          .catch(() => false),
      ),
    );
    if (chrome.some(Boolean)) return;
    await driver.terminateApp(APP_ID).catch(() => {});
    await driver.pause(500);
    await driver.activateApp(APP_ID).catch(() => {});
    await driver.pause(1200);
    await driver.execute('mobile: startActivity', {
      component: `${APP_ID}/${APP_ACTIVITY}`,
    }).catch(() => {});
    await driver.pause(1200);
    await dismissBlockingDialogs();
  },
  afterTest: async function (test, context, { error }) {
    if (error) {
      await driver.takeScreenshot();
    }
    const { dismissBlockingDialogs } = require('./test/helpers/dialogs');
    await dismissBlockingDialogs();
  },
};
