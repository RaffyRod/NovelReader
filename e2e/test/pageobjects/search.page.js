const BasePage = require('./base.page');

class SearchPage extends BasePage {
  get tabSearch() {
    return this.byA11y('Buscar');
  }

  get searchHint() {
    return this.byTextContains('Buscar novelas');
  }

  get providersTitle() {
    return this.byTextContains('Proveedores');
  }

  async open() {
    await this.openTab('Buscar');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSearch.waitForDisplayed({ timeout: 15000 });
    await expect(this.tabSearch).toBeDisplayed();
  }

  async typeQuery(query) {
    const field = await $('android.widget.EditText');
    await field.waitForDisplayed({ timeout: 10000 });
    await field.click();
    await field.setValue(query);
    await driver.pause(1000);
  }
}

module.exports = new SearchPage();
