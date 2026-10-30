const BasePage = require('./base.page');

class SettingsPage extends BasePage {
  get tabSettings() {
    return this.byA11y('Ajustes');
  }

  get tutorialGroup() {
    return this.byText('Tutorial');
  }

  get tutorialTts() {
    return this.byTextContains('Lectura en voz alta');
  }

  get tutorialDownload() {
    return this.byText('Descargas');
  }

  get uiTheme() {
    return this.byTextContains('Fire');
  }

  async open() {
    await this.openTab('Ajustes');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSettings.waitForDisplayed({ timeout: 15000 });
    await expect(this.tabSettings).toBeDisplayed();
  }

  async openTutorialTts() {
    const item = this.tutorialTts;
    if (await item.isExisting().catch(() => false)) {
      await item.click();
      await driver.pause(800);
      return true;
    }
    return false;
  }
}

module.exports = new SettingsPage();
