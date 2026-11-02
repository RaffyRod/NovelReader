const BasePage = require('./base.page');

class SectionsPage extends BasePage {
  get tabSections() {
    return this.byA11y('Secciones');
  }

  /** Content-desc for LibraryRail translation history (ES default UI). */
  get translationHistory() {
    return this.byA11y('Traducciones');
  }

  get translationHistoryTitle() {
    return this.byTextContains('Historial de traducciones');
  }

  get translationHistoryEmpty() {
    return this.byTextContains('Aún no hay traducciones');
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

  /**
   * Opens the left Sections rail and asserts the translation-history entry is present
   * (Translate icon; content-desc Traducciones — not a globe-only language affordance).
   */
  async openTranslationHistory() {
    await this.open();
    await this.translationHistory.waitForDisplayed({ timeout: 10000 });
    await this.translationHistory.click();
    await driver.pause(600);
  }
}

module.exports = new SectionsPage();
