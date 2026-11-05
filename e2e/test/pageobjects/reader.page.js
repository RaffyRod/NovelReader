const BasePage = require('./base.page');

/** @returns {string} */
function pkg() {
  return process.env.APP_PACKAGE || 'com.ultimateSoft.quicknovel.ultimate';
}

class ReaderPage extends BasePage {
  get ttsButton() {
    return this.byId(`${pkg()}:id/read_action_tts`);
  }

  get ttsButtonA11y() {
    return this.byA11y('Lectura en voz alta');
  }

  get ttsButtonA11yEn() {
    return this.byA11y('Text To Speech');
  }

  get chaptersButton() {
    return this.byId(`${pkg()}:id/read_action_chapters`);
  }

  get settingsButton() {
    return this.byId(`${pkg()}:id/read_action_settings`);
  }

  get ttsPausePlay() {
    return this.byId(`${pkg()}:id/tts_action_pause_play`);
  }

  get ttsStop() {
    return this.byId(`${pkg()}:id/tts_action_stop`);
  }

  get loadingOverlay() {
    return this.byId(`${pkg()}:id/read_loading`);
  }

  get loadingGlyph() {
    return this.byId(`${pkg()}:id/loading_glyph`);
  }

  get loadingTitle() {
    return this.byId(`${pkg()}:id/loading_title`);
  }

  get loadingSubtitle() {
    return this.byId(`${pkg()}:id/loading_text`);
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
    const id = pkg();
    // Click a content TextView — clicking real_text (RecyclerView) hits empty center
    // and never fires TextAdapter switchVisibility.
    const para = await $(
      `android=new UiSelector().resourceId("${id}:id/real_text").childSelector(new UiSelector().className("android.widget.TextView").instance(2))`,
    );
    if (await para.isDisplayed().catch(() => false)) {
      await para.click().catch(() => {});
    } else {
      const anyLine = await $(
        `android=new UiSelector().resourceId("${id}:id/real_text").childSelector(new UiSelector().className("android.widget.TextView"))`,
      );
      if (await anyLine.isDisplayed().catch(() => false)) {
        await anyLine.click().catch(() => {});
      } else {
        const { width, height } = await driver.getWindowSize();
        // Short EPUB text sits in the upper third; mid-screen is empty RV space.
        await driver.execute('mobile: clickGesture', {
          x: Math.floor(width / 2),
          y: Math.floor(height * 0.28),
        });
      }
    }
    await driver.pause(700);
  }

  /**
   * @returns {Promise<boolean>}
   */
  async hasTtsChrome() {
    if (await this.ttsButton.isDisplayed().catch(() => false)) return true;
    if (await this.chaptersButton.isDisplayed().catch(() => false)) return true;
    if (await this.ttsButtonA11y.isDisplayed().catch(() => false)) return true;
    if (await this.ttsButtonA11yEn.isDisplayed().catch(() => false)) return true;
    if (await this.byId(`${pkg()}:id/reader_bottom_view`).isDisplayed().catch(() => false)) {
      return true;
    }
    return false;
  }

  /**
   * Taps the reading area a few times until the reader bottom bar appears.
   * @returns {Promise<boolean>}
   */
  async isReaderVisible() {
    const id = pkg();
    if (await this.byId(`${id}:id/read_fail`).isDisplayed().catch(() => false)) return false;
    const hasLayout = await this.byId(`${id}:id/read_normal_layout`).isDisplayed().catch(() => false);
    const rounds = hasLayout ? 6 : 3;
    for (let i = 0; i < rounds; i++) {
      if (await this.hasTtsChrome()) return true;
      await this.revealControls();
      if (await this.hasTtsChrome()) return true;
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
   * True only when pause or stop controls are visible (TTS actually started).
   * @returns {Promise<boolean>}
   */
  async hasActiveTtsBar() {
    if (await this.ttsPausePlay.isDisplayed().catch(() => false)) return true;
    if (await this.ttsStop.isDisplayed().catch(() => false)) return true;
    return false;
  }

  /**
   * Stops TTS if the active bar is up, so a later start can be asserted for real.
   */
  async stopTtsIfRunning() {
    if (!(await this.hasActiveTtsBar())) return;
    if (await this.ttsStop.isDisplayed().catch(() => false)) {
      await this.ttsStop.click();
      await driver.pause(1200);
    }
  }

  /**
   * Taps TTS and waits until pause/stop appears. Fails if TTS never starts.
   */
  async startTts() {
    await this.stopTtsIfRunning();

    for (let attempt = 0; attempt < 6; attempt++) {
      if (!(await this.hasTtsChrome())) {
        await this.revealControls();
      }

      if (await this.ttsButton.isDisplayed().catch(() => false)) {
        await this.ttsButton.click();
      } else if (await this.ttsButtonA11y.isDisplayed().catch(() => false)) {
        await this.ttsButtonA11y.click();
      } else if (await this.ttsButtonA11yEn.isDisplayed().catch(() => false)) {
        await this.ttsButtonA11yEn.click();
      } else {
        await driver.pause(400);
        continue;
      }

      // Do not re-tap content (that toggles chrome off). Wait for pause/stop.
      const deadline = Date.now() + 10000;
      while (Date.now() < deadline) {
        if (await this.hasActiveTtsBar()) return;
        await driver.pause(400);
      }
    }
    throw new Error('TTS did not start: pause/stop controls never appeared');
  }
}

module.exports = new ReaderPage();
