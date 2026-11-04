const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');

describe('Import book (Import-B)', () => {
  before(async () => {
    allure.addFeature('Import');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('shows Fire drop-zone with EPUB/PDF hint under search', async () => {
    allure.addSeverity('critical');
    await libraryPage.open();
    await libraryPage.expectImportZoneVisible();
  });

  it('opens system document picker when tapping import', async () => {
    allure.addSeverity('critical');
    await libraryPage.open();
    await libraryPage.importDropZone.click();
    await driver.pause(2000);

    const markers = ['Recent', 'BROWSE', 'Download', 'Show roots', 'Open from', 'Files', 'Documents'];
    let pickerVisible = false;
    for (const marker of markers) {
      if (await libraryPage.byTextContains(marker).isExisting().catch(() => false)) {
        pickerVisible = true;
        break;
      }
    }
    expect(pickerVisible).toBe(true);

    // Leave DocumentsUI so later general-suite specs keep bottom nav.
    for (let i = 0; i < 3; i++) {
      await driver.back().catch(() => {});
      await driver.pause(300);
    }
    await libraryPage.ensureMainChrome(15000);
  });

  it('imports sample EPUB into Biblioteca', async () => {
    allure.addSeverity('critical');
    const { importFixtureEpub } = require('../helpers/seedNovel');
    const imported = await importFixtureEpub();
    expect(imported).toBe(true);
    await libraryPage.open();
    expect(await libraryPage.hasNovelCard()).toBe(true);
  });
});
