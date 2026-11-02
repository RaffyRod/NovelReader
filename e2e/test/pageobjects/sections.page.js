const BasePage = require('./base.page');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';

class SectionsPage extends BasePage {
  get tabSections() {
    return this.byId(`${APP_ID}:id/navigation_sections`);
  }

  /** Content-desc for LibraryRail translation history (ES/EN). */
  get translationHistory() {
    return $(
      'android=new UiSelector().descriptionMatches("(?i)(Traducciones|Translations)")',
    );
  }

  get translationHistoryTitle() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Historial de traducciones|Translation history).*")',
    );
  }

  get translationHistoryEmpty() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Aún no hay traducciones|No translations yet).*")',
    );
  }

  /**
   * Secciones is only visible while Biblioteca is the selected destination.
   */
  async open() {
    await this.openTab('Biblioteca');
    await this.tabSections.waitForDisplayed({ timeout: 15000 });
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
