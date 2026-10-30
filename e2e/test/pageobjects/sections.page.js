const BasePage = require('./base.page');

class SectionsPage extends BasePage {
  get tabSections() {
    return this.byA11y('Secciones');
  }

  async open() {
    await this.openTab('Secciones');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSections.waitForDisplayed({ timeout: 15000 });
    await expect(this.tabSections).toBeDisplayed();
  }
}

module.exports = new SectionsPage();
