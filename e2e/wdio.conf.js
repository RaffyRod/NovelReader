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
      'QuickNovel-Ultimate-1.1.4-universal.apk',
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
    features: [
      [
        './test/specs/05-settings.e2e.js',
        './test/specs/06-import.e2e.js',
        './test/specs/09-search-special-chars.e2e.js',
        './test/specs/10-plugins.e2e.js',
        './test/specs/11-download-translate.e2e.js',
      ],
    ],
    reader: [['./test/specs/07-reader-tts.e2e.js']],
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
      'appium:newCommandTimeout': 240,
      'appium:adbExecTimeout': 120000,
      'appium:uiautomator2ServerInstallTimeout': 120000,
      'appium:disableWindowAnimation': true,
    },
  ],
  logLevel: process.env.WDIO_LOG_LEVEL || 'info',
  bail: 0,
  waitforTimeout: 15000,
  connectionRetryTimeout: 180000,
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
  afterTest: async function (test, context, { error }) {
    if (error) {
      await driver.takeScreenshot();
    }
  },
};
