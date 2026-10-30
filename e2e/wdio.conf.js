const path = require('path');
const fs = require('fs');

const APP_ID = 'com.lagradost.quicknovel.ultimate';
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
      'QuickNovel-Ultimate-1.1.0-universal.apk',
    ),
  ];
  return candidates.find((p) => p && fs.existsSync(p)) || candidates[0];
}

const appPath = resolveAppPath();

exports.config = {
  runner: 'local',
  specs: ['./test/specs/**/*.e2e.js'],
  exclude: [],
  maxInstances: 1,
  capabilities: [
    {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': process.env.ANDROID_DEVICE_NAME || 'Android Emulator',
      'appium:platformVersion': process.env.ANDROID_PLATFORM_VERSION || undefined,
      'appium:app': appPath,
      'appium:appPackage': APP_ID,
      'appium:appActivity': APP_ACTIVITY,
      'appium:noReset': false,
      'appium:fullReset': false,
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
  services: [
    [
      'appium',
      {
        args: {
          relaxedSecurity: true,
          allowInsecure: ['adb_shell'],
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
  },
  afterTest: async function (test, context, { error }) {
    if (error) {
      await driver.takeScreenshot();
    }
  },
};
