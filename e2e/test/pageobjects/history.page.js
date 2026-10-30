const BasePage = require('./base.page');

class HistoryPage extends BasePage {
  get tabHistory() {
    return this.byA11y('Historial');
  }

  get emptyState() {
    return this.byTextContains('Aún no hay novelas recientes');
  }

  get readingSection() {
    return this.byTextContains('Leyendo');
  }

  get visitedSection() {
    return this.byTextContains('Visitadas');
  }

  async open() {
    await this.openTab('Historial');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabHistory.waitForDisplayed({ timeout: 15000 });
    await expect(this.tabHistory).toBeDisplayed();
  }
}

module.exports = new HistoryPage();
