const { dismissBlockingDialogs } = require('../helpers/dialogs');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
const APP_ACTIVITY = 'com.lagradost.quicknovel.MainActivity';
const MAIN_TABS = ['Biblioteca', 'Buscar', 'Historial', 'Ajustes'];

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
   * True when any main bottom-nav tab is visible.
   */
  async isMainChromeVisible() {
    for (const tab of MAIN_TABS) {
      const el = $(`android=new UiSelector().description("${tab}")`);
      if (await el.isDisplayed().catch(() => false)) return true;
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
   * Opens a bottom-nav tab. Prefer clickable nodes so we do not hit duplicate
   * content-desc labels in the page chrome (e.g. library search also says "Buscar").
   * Selected tabs often become non-clickable — treat that as already open.
   * @param {string} tabName accessibility / content-desc of bottom nav
   */
  async openTab(tabName) {
    await this.ensureMainChrome();
    await dismissBlockingDialogs();
    const selected = $(`android=new UiSelector().description("${tabName}").selected(true)`);
    if (await selected.isExisting().catch(() => false)) {
      await driver.pause(300);
      return;
    }
    const clickable = $(`android=new UiSelector().description("${tabName}").clickable(true)`);
    if (await clickable.isDisplayed().catch(() => false)) {
      await clickable.click();
    } else {
      // Selected tabs can be non-clickable; other labels may still be tappable without clickable=true.
      const any = $(`android=new UiSelector().description("${tabName}")`);
      await any.waitForDisplayed({ timeout: 15000 });
      await any.click();
    }
    await driver.pause(1000);
    await dismissBlockingDialogs();
  }
}

module.exports = BasePage;
