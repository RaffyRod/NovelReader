const { dismissBlockingDialogs } = require('../helpers/dialogs');

const APP_ACTIVITY = 'com.lagradost.quicknovel.MainActivity';

/** @returns {string} live applicationId (must not be frozen at require-time). */
function appId() {
  return process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
}

/** Bottom-nav id suffixes (locale-independent). */
const TAB_SUFFIXES = {
  Biblioteca: 'navigation_download',
  Library: 'navigation_download',
  Buscar: 'navigation_search',
  Search: 'navigation_search',
  Historial: 'navigation_history',
  History: 'navigation_history',
  Ajustes: 'navigation_settings',
  Settings: 'navigation_settings',
  Secciones: 'navigation_sections',
  Sections: 'navigation_sections',
};

/** Labels that may appear depending on app / system locale. */
const TAB_ALIASES = {
  Biblioteca: ['Biblioteca', 'Library'],
  Buscar: ['Buscar', 'Search'],
  Historial: ['Historial', 'History'],
  Ajustes: ['Ajustes', 'Settings'],
  Secciones: ['Secciones', 'Sections'],
};

const MAIN_TAB_SUFFIXES = [
  'navigation_download',
  'navigation_search',
  'navigation_history',
  'navigation_settings',
  'navigation_sections',
];

class BasePage {
  /** @param {string} tabName */
  tabResourceId(tabName) {
    const suffix = TAB_SUFFIXES[tabName];
    return suffix ? `${appId()}:id/${suffix}` : null;
  }

  /**
   * Hard reset to MainActivity when bottom nav is gone (dialogs / stuck activities).
   * Never use activateApp: after Ultimate icon aliases, package resolve-activity
   * returns *Launcher and can crash the emulator package manager.
   */
  async recoverMainApp() {
    const pkg = appId();
    const { execFileSync } = require('child_process');
    // adb force-stop clears ANR dialogs; terminateApp alone often leaves the app frozen.
    try {
      execFileSync('adb', ['shell', 'am', 'force-stop', pkg], { stdio: 'ignore' });
    } catch (_) {
      await driver.terminateApp(pkg).catch(() => {});
    }
    await driver.pause(500);
    try {
      execFileSync('adb', ['shell', 'am', 'start', '-n', `${pkg}/${APP_ACTIVITY}`], {
        stdio: 'ignore',
      });
    } catch (_) {
      await driver
        .execute('mobile: startActivity', {
          component: `${pkg}/${APP_ACTIVITY}`,
          intentAction: 'android.intent.action.MAIN',
        })
        .catch(() => {});
    }
    await driver.pause(1200);
    await dismissBlockingDialogs();
  }
  /**
   * @param {string} accessibilityId
   */
  byA11y(accessibilityId) {
    return $(`~${accessibilityId}`);
  }

  /**
   * @param {string} text
   */
  byText(text) {
    return $(`android=new UiSelector().text("${text}")`);
  }

  /**
   * @param {string} partial
   */
  byTextContains(partial) {
    return $(`android=new UiSelector().textContains("${partial}")`);
  }

  /**
   * @param {string} resourceId full resource-id
   */
  byId(resourceId) {
    return $(`android=new UiSelector().resourceId("${resourceId}")`);
  }

  async waitReady(timeout = 20000) {
    await dismissBlockingDialogs();
    await driver.pause(400);
  }

  /**
   * True when any main bottom-nav tab is visible (resource-id first).
   */
  async isMainChromeVisible() {
    const pkg = appId();
    for (const suffix of MAIN_TAB_SUFFIXES) {
      const el = this.byId(`${pkg}:id/${suffix}`);
      if (await el.isDisplayed().catch(() => false)) return true;
    }
    for (const labels of Object.values(TAB_ALIASES)) {
      for (const label of labels) {
        const el = $(`android=new UiSelector().description("${label}")`);
        if (await el.isDisplayed().catch(() => false)) return true;
      }
    }
    return false;
  }

  /**
   * Leaves reader / pickers / dialogs until bottom navigation is back.
   * Restarts / relaunches the app if back presses are not enough.
   */
  async ensureMainChrome(timeoutMs = 10000) {
    const hardDeadline = Date.now() + timeoutMs;
    await dismissBlockingDialogs();
    while (Date.now() < hardDeadline) {
      if (await this.isMainChromeVisible()) return;
      const act = String((await driver.getCurrentActivity().catch(() => '')) || '');
      if (/Launcher$/i.test(act)) {
        await driver
          .execute('mobile: startActivity', {
            component: `${appId()}/${APP_ACTIVITY}`,
          })
          .catch(() => {});
        await driver.pause(400);
        continue;
      }
      await driver.back().catch(() => {});
      await driver.pause(250);
    }
    // Recovery must fit inside the remaining budget (never add another full timeout).
    const remaining = hardDeadline - Date.now();
    if (remaining < 1500) {
      throw new Error('Main bottom navigation not visible before recovery budget');
    }
    await this.recoverMainApp();
    while (Date.now() < hardDeadline) {
      if (await this.isMainChromeVisible()) return;
      await dismissBlockingDialogs();
      await driver.pause(300);
    }
    throw new Error('Main bottom navigation not visible after app recovery');
  }

  /**
   * Scrolls until a node with the given text is visible (UiScrollable + gestures).
   * @param {string} text
   */
  async scrollToText(text) {
    const target = this.byTextContains(text);
    if (await target.isDisplayed().catch(() => false)) return;

    // Prefer gestures — UiScrollable.scrollIntoView can block 30s+ when missing.
    await driver.setTimeout({ implicit: 0 }).catch(() => {});
    for (const direction of ['down', 'up', 'down']) {
      for (let i = 0; i < 4; i++) {
        if (await target.isDisplayed().catch(() => false)) return;
        await driver
          .execute('mobile: scrollGesture', {
            left: 80,
            top: 400,
            width: 600,
            height: 1200,
            direction,
            percent: 0.8,
          })
          .catch(() => {});
        await driver.pause(150);
      }
    }
  }

  /**
   * Opens a bottom-nav tab via resource-id (locale-safe), with description fallbacks.
   * @param {string} tabName Spanish label used by page objects (Biblioteca, Buscar, …)
   */
  async openTab(tabName) {
    const tryOpen = async () => {
      await this.ensureMainChrome(8000);
      await dismissBlockingDialogs();

      const resId = this.tabResourceId(tabName);
      if (resId) {
        const byRes = this.byId(resId);
        if (await byRes.isDisplayed().catch(() => false)) {
          const selected = await byRes.getAttribute('selected').catch(() => 'false');
          if (selected === 'true') {
            await driver.pause(300);
            return true;
          }
          await byRes.click();
          await driver.pause(800);
          await dismissBlockingDialogs();
          return true;
        }
      }

      const aliases = TAB_ALIASES[tabName] || [tabName];
      for (const label of aliases) {
        const selected = $(`android=new UiSelector().description("${label}").selected(true)`);
        if (await selected.isExisting().catch(() => false)) {
          await driver.pause(300);
          return true;
        }
      }
      for (const label of aliases) {
        const clickable = $(`android=new UiSelector().description("${label}").clickable(true)`);
        if (await clickable.isDisplayed().catch(() => false)) {
          await clickable.click();
          await driver.pause(800);
          await dismissBlockingDialogs();
          return true;
        }
      }
      for (const label of aliases) {
        const any = $(`android=new UiSelector().description("${label}")`);
        if (await any.isDisplayed().catch(() => false)) {
          await any.click();
          await driver.pause(800);
          await dismissBlockingDialogs();
          return true;
        }
      }

      if (resId) {
        const byRes = this.byId(resId);
        if (await byRes.waitForDisplayed({ timeout: 3000 }).then(() => true).catch(() => false)) {
          await byRes.click();
          await driver.pause(500);
          await dismissBlockingDialogs();
          return true;
        }
      }
      return false;
    };

    if (await tryOpen()) return;
    await this.recoverMainApp();
    if (await tryOpen()) return;
    throw new Error(`Bottom nav tab not found: ${tabName}`);
  }
}

BasePage.appId = appId;
module.exports = BasePage;
