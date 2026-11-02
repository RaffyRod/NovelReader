const BasePage = require('./base.page');

class LibraryPage extends BasePage {
  get tabLibrary() {
    const pkg = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
    return this.byId(`${pkg}:id/navigation_download`);
  }

  get searchField() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Buscar mis novelas|Search my novels).*")',
    );
  }

  get importDropZone() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Importar un libro|Import a book).*")',
    );
  }

  get importHint() {
    return this.byTextContains('EPUB');
  }

  get continueReading() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Continuar leyendo|Continue reading).*")',
    );
  }

  get sortFab() {
    return this.byA11y('Ordenar');
  }

  async open() {
    await this.openTab('Biblioteca');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabLibrary.waitForDisplayed({ timeout: 20000 });
    await expect(this.tabLibrary).toBeDisplayed();
    await expect(this.searchField).toBeDisplayed();
  }

  async expectImportZoneVisible() {
    await this.importDropZone.waitForDisplayed({ timeout: 15000 });
    await expect(this.importDropZone).toBeDisplayed();
    await expect(this.importHint).toBeDisplayed();
  }

  /**
   * Search stays pinned: after a swipe, the library search field must still be visible.
   */
  async expectSearchStaysPinnedAfterScroll() {
    await this.searchField.waitForDisplayed({ timeout: 10000 });
    await driver.execute('mobile: scrollGesture', {
      left: 100,
      top: 800,
      width: 400,
      height: 1000,
      direction: 'down',
      percent: 0.7,
    });
    await driver.pause(500);
    await expect(this.searchField).toBeDisplayed();
  }

  /**
   * @param {string} label
   */
  isChromeNoise(label) {
    const noise = [
      'Importar',
      'Buscar',
      'Biblioteca',
      'Ordenar',
      'Secciones',
      'Todas',
      'Continuar',
      'EPUB',
      'PDF',
      'Ajustes',
      'Historial',
      'Grid',
      'Lista',
      'Filtrar',
    ];
    return !label || label.length < 3 || noise.some((n) => label.includes(n));
  }

  /**
   * True when Biblioteca shows at least one novel card (not just chrome).
   */
  async hasNovelCard() {
    if (await this.continueReading.isDisplayed().catch(() => false)) return true;
    if (await this.byTextContains('Capítulo').isDisplayed().catch(() => false)) return true;
    const titles = await $$('android.widget.TextView');
    for (const el of titles) {
      const t = (await el.getText().catch(() => '')) || '';
      if (this.isChromeNoise(t)) continue;
      if (t.length >= 5 && t.length <= 120) return true;
    }
    return false;
  }

  /**
   * Opens a downloaded novel into the reader when possible.
   * @returns {Promise<boolean>}
   */
  async openFirstNovelIfPresent() {
    if (await this.continueReading.isDisplayed().catch(() => false)) {
      await this.continueReading.click();
      await driver.pause(2500);
      return true;
    }

    const chapterProgress = this.byTextContains('Capítulo');
    if (await chapterProgress.isDisplayed().catch(() => false)) {
      await chapterProgress.click();
      await driver.pause(2500);
      return true;
    }

    const titles = await $$('android.widget.TextView');
    for (const el of titles) {
      const t = (await el.getText().catch(() => '')) || '';
      if (this.isChromeNoise(t)) continue;
      if (t.length < 5 || t.length > 120) continue;
      await el.click();
      await driver.pause(2500);
      // Must leave library: TTS button or at least leave search field
      const stillLibrary = await this.searchField.isDisplayed().catch(() => false);
      if (!stillLibrary) return true;
    }
    return false;
  }
}

module.exports = new LibraryPage();
