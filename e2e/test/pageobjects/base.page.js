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

  async waitReady(timeout = 20000) {
    await dismissBlockingDialogs();
    await driver.pause(400);
  }

  /**
   * @param {string} tabName accessibility / content-desc of bottom nav
   */
  async openTab(tabName) {
    await dismissBlockingDialogs();
    const tab = this.byA11y(tabName);
    await tab.waitForDisplayed({ timeout: 15000 });
    await tab.click();
    await driver.pause(800);
    await dismissBlockingDialogs();
  }
}

module.exports = BasePage;
