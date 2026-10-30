const BasePage = require('./base.page');

class LibraryPage extends BasePage {
  get tabLibrary() {
    return this.byA11y('Biblioteca');
  }

  get searchField() {
    return this.byTextContains('Buscar mis novelas');
  }

  get importDropZone() {
    return this.byText('Importar un libro');
  }

  get importHint() {
    return this.byTextContains('EPUB o PDF');
  }

  get continueReading() {
    return this.byTextContains('Continuar leyendo');
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
    }).catch(async () => {
      await driver.touchAction([
        { action: 'press', x: 500, y: 1600 },
        { action: 'wait', ms: 200 },
        { action: 'moveTo', x: 500, y: 700 },
        { action: 'release' },
      ]);
    });
    await driver.pause(500);
    await expect(this.searchField).toBeDisplayed();
  }

  async openFirstNovelIfPresent() {
    const novel = this.byTextContains('Capítulo');
    if (await novel.isExisting().catch(() => false)) {
      await novel.click();
      await driver.pause(1500);
      return true;
    }
    const anyTitle = await $$('android.widget.TextView');
    for (const el of anyTitle) {
      const t = await el.getText().catch(() => '');
      if (
        t &&
        t.length > 8 &&
        !t.includes('Importar') &&
        !t.includes('Buscar') &&
        !t.includes('Biblioteca') &&
        !t.includes('Todas')
      ) {
        await el.click();
        await driver.pause(1500);
        return true;
      }
    }
    return false;
  }
}

module.exports = new LibraryPage();
