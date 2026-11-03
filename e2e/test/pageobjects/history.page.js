const BasePage = require('./base.page');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';

class HistoryPage extends BasePage {
  get tabHistory() {
    return this.byId(`${APP_ID}:id/navigation_history`);
  }

  get emptyState() {
    return this.byTextContains('Aún no hay novelas recientes');
  }

  get emptyStateEn() {
    return this.byTextContains('No recent');
  }

  get readingSection() {
    return this.byTextContains('Leyendo');
  }

  get readingSectionEn() {
    return this.byTextContains('Reading');
  }

  get visitedSection() {
    return this.byTextContains('Visitadas');
  }

  get visitedSectionEn() {
    return this.byTextContains('Visited');
  }

  async open() {
    await this.openTab('Historial');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.ensureMainChrome(20000);
    const byId = this.tabHistory;
    const byEs = this.byA11y('Historial');
    const byEn = this.byA11y('History');
    const ok =
      (await byId.waitForDisplayed({ timeout: 15000 }).then(() => true).catch(() => false)) ||
      (await byEs.isDisplayed().catch(() => false)) ||
      (await byEn.isDisplayed().catch(() => false));
    expect(ok).toBe(true);
  }
}

module.exports = new HistoryPage();
