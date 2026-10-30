const BasePage = require('./base.page');

const APP_ID = 'com.lagradost.quicknovel.ultimate';

class ReaderPage extends BasePage {
  get ttsButton() {
    return this.byId(`${APP_ID}:id/read_action_tts`);
  }

  get ttsButtonA11y() {
    return this.byA11y('Lectura en voz alta');
  }

  get chaptersButton() {
    return this.byId(`${APP_ID}:id/read_action_chapters`);
  }

  get settingsButton() {
    return this.byId(`${APP_ID}:id/read_action_settings`);
  }

  get ttsPausePlay() {
    return this.byId(`${APP_ID}:id/tts_action_pause_play`);
  }

  get ttsStop() {
    return this.byId(`${APP_ID}:id/tts_action_stop`);
  }

  async revealControls() {
    const { width, height } = await driver.getWindowSize();
    const x = Math.floor(width / 2);
    const y = Math.floor(height * 0.45);
    await driver.execute('mobile: clickGesture', { x, y });
    await driver.pause(700);
  }

  /**
   * Taps the reading area a few times until the reader bottom bar appears.
   * @returns {Promise<boolean>}
   */
  async isReaderVisible() {
    for (let i = 0; i < 4; i++) {
      await this.revealControls();
      if (await this.ttsButton.isDisplayed().catch(() => false)) return true;
      if (await this.chaptersButton.isDisplayed().catch(() => false)) return true;
      if (await this.ttsButtonA11y.isDisplayed().catch(() => false)) return true;
    }
    return false;
  }

  async expectInReader() {
    const visible = await this.isReaderVisible();
    expect(visible).toBe(true);
    await expect(this.ttsButton).toBeDisplayed();
  }

  async expectTtsControlVisible() {
    await this.expectInReader();
  }

  async startTts() {
    const visible = await this.isReaderVisible();
    expect(visible).toBe(true);
    await this.ttsButton.click();
    await driver.pause(2500);
  }
}

module.exports = new ReaderPage();
