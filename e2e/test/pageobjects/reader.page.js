const BasePage = require('./base.page');

class ReaderPage extends BasePage {
  get ttsButton() {
    return this.byA11y('Lectura en voz alta');
  }

  get ttsButtonEn() {
    return this.byA11y('Text To Speech');
  }

  get settingsButton() {
    return this.byA11y('Ajustes');
  }

  get chaptersButton() {
    return this.byA11y('Capítulos');
  }

  async revealControls() {
    const { width, height } = await driver.getWindowSize();
    await driver.action('pointer')
      .move({ x: Math.floor(width / 2), y: Math.floor(height / 2) })
      .down()
      .up()
      .perform()
      .catch(async () => {
        await driver.touchAction({
          action: 'tap',
          x: Math.floor(width / 2),
          y: Math.floor(height / 2),
        });
      });
    await driver.pause(600);
  }

  async expectTtsControlVisible() {
    await this.revealControls();
    const es = await this.ttsButton.isDisplayed().catch(() => false);
    const en = await this.ttsButtonEn.isDisplayed().catch(() => false);
    expect(es || en).toBe(true);
  }

  async startTts() {
    await this.revealControls();
    if (await this.ttsButton.isDisplayed().catch(() => false)) {
      await this.ttsButton.click();
    } else {
      await this.ttsButtonEn.click();
    }
    await driver.pause(2000);
  }
}

module.exports = new ReaderPage();
