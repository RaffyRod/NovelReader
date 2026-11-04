const BasePage = require('./base.page');
const searchPage = require('./search.page');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';

/**
 * Novel detail / result screen and the chapter download dialog.
 */
class ResultPage extends BasePage {
  get stickyDownload() {
    return this.byId(`${APP_ID}:id/sticky_download`);
  }

  get downloadButton() {
    return this.byId(`${APP_ID}:id/result_download_btt`);
  }

  get downloadByText() {
    return this.byText('Descargar');
  }

  get downloadDialogTitle() {
    return this.byTextContains('Descargar');
  }

  get translateLangLabel() {
    return this.byTextContains('Idioma de descarga');
  }

  get translateNone() {
    return this.byTextContains('Original (sin traducir)');
  }

  get chaptersPerFile() {
    return this.byTextContains('Capítulos por archivo');
  }

  get formatEpub() {
    return this.byText('EPUB');
  }

  get cancelButton() {
    const cancel = this.byText('Cancelar');
    return cancel;
  }

  /**
   * Opens the first usable search hit and waits for the detail screen.
   * @returns {Promise<boolean>}
   */
  async openFirstFromSearch(query = 'amor') {
    await searchPage.ensureMainChrome(15000).catch(() => {});
    await searchPage.open();
    try {
      await searchPage.expectLoaded();
    } catch (e) {
      console.warn(`Search chrome missing: ${e.message || e}`);
      await searchPage.recoverMainApp();
      await searchPage.open();
      await searchPage.expectLoaded();
    }
    await searchPage.search(query, 70000);
    // Coil covers use novel title as contentDescription (Compose — not always ImageView).
    const covers = await searchPage.findNovelCoverElements();
    for (const el of covers) {
      await el.click().catch(() => {});
      await driver.pause(2500);
      if (await this.isDetailVisible()) return true;
    }
    // Fallback: open a SearchRow (provider header) then first cover in the dialog/list.
    const row = await $(
      'android=new UiSelector().className("android.widget.TextView").textMatches("(?i).*(Wattpad|Royal|Novel|Wuxia|Sky|Devil).*")',
    );
    if (await row.isDisplayed().catch(() => false)) {
      await row.click().catch(() => {});
      await driver.pause(1500);
      const more = await searchPage.findNovelCoverElements();
      for (const el of more) {
        await el.click().catch(() => {});
        await driver.pause(2500);
        if (await this.isDetailVisible()) return true;
      }
    }
    for (let i = 0; i < 12; i++) {
      if (await this.isDetailVisible()) return true;
      await driver.pause(500);
    }
    return false;
  }

  /**
   * True when download / read actions for a novel detail are on screen.
   */
  async isDetailVisible() {
    if (await this.stickyDownload.isDisplayed().catch(() => false)) return true;
    if (await this.downloadButton.isDisplayed().catch(() => false)) return true;
    if (await this.byTextContains('Leer').isDisplayed().catch(() => false)) return true;
    if (await this.byTextContains('Capítulos').isDisplayed().catch(() => false)) return true;
    return false;
  }

  /**
   * Opens the chapter download dialog (format / range / language).
   * @returns {Promise<boolean>}
   */
  async openDownloadDialog() {
    const candidates = [
      this.stickyDownload,
      this.downloadButton,
      this.downloadByText,
      this.byTextContains('Descargar'),
    ];
    for (const el of candidates) {
      if (await el.isDisplayed().catch(() => false)) {
        await el.click();
        await driver.pause(1200);
        if (await this.isDownloadDialogVisible()) return true;
      }
    }
    // Chapters tab may expose download via selection FAB flow — try sticky again after scroll.
    await this.scrollToText('Descargar');
    if (await this.downloadByText.isDisplayed().catch(() => false)) {
      await this.downloadByText.click();
      await driver.pause(1200);
    }
    return this.isDownloadDialogVisible();
  }

  async isDownloadDialogVisible() {
    return (
      (await this.formatEpub.isDisplayed().catch(() => false)) ||
      (await this.chaptersPerFile.isDisplayed().catch(() => false)) ||
      (await this.translateLangLabel.isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Capítulos (1-').isDisplayed().catch(() => false))
    );
  }

  /**
   * Scrolls inside the download dialog until the language picker is visible.
   * @returns {Promise<boolean>}
   */
  async revealTranslateLanguage() {
    for (let i = 0; i < 6; i++) {
      if (await this.translateLangLabel.isDisplayed().catch(() => false)) return true;
      if (await this.translateNone.isDisplayed().catch(() => false)) return true;
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 600,
        width: 600,
        height: 900,
        direction: 'down',
        percent: 0.55,
      }).catch(() => {});
      await driver.pause(300);
    }
    return (
      (await this.translateLangLabel.isDisplayed().catch(() => false)) ||
      (await this.translateNone.isDisplayed().catch(() => false))
    );
  }

  async dismissDownloadDialog() {
    if (await this.cancelButton.isDisplayed().catch(() => false)) {
      await this.cancelButton.click();
    } else {
      await driver.back().catch(() => {});
    }
    await driver.pause(500);
  }
}

module.exports = new ResultPage();
