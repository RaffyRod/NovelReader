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

  get loadingOverlay() {
    return this.byId(`${APP_ID}:id/read_loading`);
  }

  get loadingGlyph() {
    return this.byId(`${APP_ID}:id/loading_glyph`);
  }

  get loadingTitle() {
    return this.byId(`${APP_ID}:id/loading_title`);
  }

  get loadingSubtitle() {
    return this.byId(`${APP_ID}:id/loading_text`);
  }

  /**
   * Polls for Glyph Pulse (本). Cached chapters may skip the overlay.
   * @param {number} timeoutMs
   * @returns {Promise<{seen: boolean, title: string}>}
   */
  async waitForGlyphPulse(timeoutMs = 10000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      const shown = await this.loadingGlyph.isDisplayed().catch(() => false);
      if (shown) {
        const glyph = (await this.loadingGlyph.getText().catch(() => '')) || '';
        const title = (await this.loadingTitle.getText().catch(() => '')) || '';
        const subtitle = (await this.loadingSubtitle.getText().catch(() => '')) || '';
        return { seen: true, glyph, title, subtitle };
      }
      await driver.pause(200);
    }
    return { seen: false, glyph: '', title: '', subtitle: '' };
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

  /**
   * Starts TTS from the reader chrome. Retries reveal because the bottom bar auto-hides,
   * and falls back to content-description when the resource-id node is momentarily gone.
   */
  async startTts() {
    for (let attempt = 0; attempt < 6; attempt++) {
      await this.revealControls();

      // Already on the TTS bottom bar from a prior tap / retry.
      if (await this.ttsPausePlay.isDisplayed().catch(() => false)) return;
      if (await this.ttsStop.isDisplayed().catch(() => false)) return;

      if (await this.ttsButton.isDisplayed().catch(() => false)) {
        await this.ttsButton.click();
        await driver.pause(2500);
        return;
      }
      if (await this.ttsButtonA11y.isDisplayed().catch(() => false)) {
        await this.ttsButtonA11y.click();
        await driver.pause(2500);
        return;
      }
      await driver.pause(400);
    }
    throw new Error('TTS control not found after reveal retries');
  }
}

module.exports = new ReaderPage();
