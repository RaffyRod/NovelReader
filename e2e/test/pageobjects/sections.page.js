const BasePage = require('./base.page');

class SectionsPage extends BasePage {
  get tabSections() {
    return this.byA11y('Secciones');
  }

  /**
   * Secciones is only visible while Biblioteca is the selected destination.
   */
  async open() {
    await this.openTab('Biblioteca');
    await this.tabSections.waitForDisplayed({ timeout: 10000 });
    await this.tabSections.click();
    await driver.pause(800);
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSections.waitForDisplayed({ timeout: 10000 });
    await expect(this.tabSections).toBeDisplayed();
  }
}

module.exports = new SectionsPage();
