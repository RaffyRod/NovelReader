const { dismissBlockingDialogs } = require('../helpers/dialogs');

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
   * Scrolls until a node with the given text is visible (UiScrollable).
   * @param {string} text
   */
  async scrollToText(text) {
    await $(
      `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("${text}"))`,
    );
  }

  /**
   * Opens a bottom-nav tab. Prefer clickable nodes so we do not hit duplicate
   * content-desc labels in the page chrome (e.g. library search also says "Buscar").
   * Selected tabs often become non-clickable — treat that as already open.
   * @param {string} tabName accessibility / content-desc of bottom nav
   */
  async openTab(tabName) {
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
