const { dismissBlockingDialogs } = require('../helpers/dialogs');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
const APP_ACTIVITY = 'com.lagradost.quicknovel.MainActivity';

/** Bottom-nav resource-ids (locale-independent). */
const TAB_IDS = {
  Biblioteca: `${APP_ID}:id/navigation_download`,
  Library: `${APP_ID}:id/navigation_download`,
  Buscar: `${APP_ID}:id/navigation_search`,
  Search: `${APP_ID}:id/navigation_search`,
  Historial: `${APP_ID}:id/navigation_history`,
  History: `${APP_ID}:id/navigation_history`,
  Ajustes: `${APP_ID}:id/navigation_settings`,
  Settings: `${APP_ID}:id/navigation_settings`,
  Secciones: `${APP_ID}:id/navigation_sections`,
  Sections: `${APP_ID}:id/navigation_sections`,
};

/** Labels that may appear depending on app / system locale. */
const TAB_ALIASES = {
  Biblioteca: ['Biblioteca', 'Library'],
  Buscar: ['Buscar', 'Search'],
  Historial: ['Historial', 'History'],
  Ajustes: ['Ajustes', 'Settings'],
  Secciones: ['Secciones', 'Sections'],
};

const MAIN_TAB_IDS = [
  TAB_IDS.Biblioteca,
  TAB_IDS.Buscar,
  TAB_IDS.Historial,
  TAB_IDS.Ajustes,
];

class BasePage {
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
    for (const id of MAIN_TAB_IDS) {
      const el = this.byId(id);
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
   * Restarts MainActivity if back presses are not enough.
   */
  async ensureMainChrome(timeoutMs = 25000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      await dismissBlockingDialogs();
      if (await this.isMainChromeVisible()) return;
      await driver.back().catch(() => {});
      await driver.pause(450);
    }
    await driver.execute('mobile: startActivity', {
      component: `${APP_ID}/${APP_ACTIVITY}`,
    });
    await driver.pause(2000);
    await dismissBlockingDialogs();
  }

  /**
   * Scrolls until a node with the given text is visible (UiScrollable + gestures).
   * @param {string} text
   */
  async scrollToText(text) {
    const target = this.byTextContains(text);
    if (await target.isDisplayed().catch(() => false)) return;

    await $(
      `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("${text}"))`,
    ).catch(() => {});
    if (await target.isDisplayed().catch(() => false)) return;

    for (const direction of ['up', 'down', 'up', 'down']) {
      for (let i = 0; i < 5; i++) {
        if (await target.isDisplayed().catch(() => false)) return;
        await driver.execute('mobile: scrollGesture', {
          left: 80,
          top: 400,
          width: 600,
          height: 1200,
          direction,
          percent: 0.75,
        }).catch(() => {});
        await driver.pause(250);
      }
    }
  }

  /**
   * Opens a bottom-nav tab via resource-id (locale-safe), with description fallbacks.
   * @param {string} tabName Spanish label used by page objects (Biblioteca, Buscar, …)
   */
  async openTab(tabName) {
    await this.ensureMainChrome();
    await dismissBlockingDialogs();

    const resId = TAB_IDS[tabName];
    if (resId) {
      const byRes = this.byId(resId);
      if (await byRes.isDisplayed().catch(() => false)) {
        const selected = await byRes.getAttribute('selected').catch(() => 'false');
        if (selected === 'true') {
          await driver.pause(300);
          return;
        }
        await byRes.click();
        await driver.pause(1000);
        await dismissBlockingDialogs();
        return;
      }
    }

    const aliases = TAB_ALIASES[tabName] || [tabName];
    for (const label of aliases) {
      const selected = $(`android=new UiSelector().description("${label}").selected(true)`);
      if (await selected.isExisting().catch(() => false)) {
        await driver.pause(300);
        return;
      }
    }
    for (const label of aliases) {
      const clickable = $(`android=new UiSelector().description("${label}").clickable(true)`);
      if (await clickable.isDisplayed().catch(() => false)) {
        await clickable.click();
        await driver.pause(1000);
        await dismissBlockingDialogs();
        return;
      }
    }
    for (const label of aliases) {
      const any = $(`android=new UiSelector().description("${label}")`);
      if (await any.isDisplayed().catch(() => false)) {
        await any.click();
        await driver.pause(1000);
        await dismissBlockingDialogs();
        return;
      }
    }

    // Last resort: wait on resource-id (locale-independent), then description aliases.
    if (resId) {
      const byRes = this.byId(resId);
      await byRes.waitForDisplayed({ timeout: 20000 });
      await byRes.click();
      await driver.pause(1000);
      await dismissBlockingDialogs();
      return;
    }
    for (const label of aliases) {
      const any = $(`android=new UiSelector().description("${label}")`);
      if (await any.waitForDisplayed({ timeout: 5000 }).then(() => true).catch(() => false)) {
        await any.click();
        await driver.pause(1000);
        await dismissBlockingDialogs();
        return;
      }
    }
    throw new Error(`Bottom nav tab not found: ${tabName}`);
  }
}

module.exports = BasePage;
