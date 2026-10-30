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

    const recent = await libraryPage.byTextContains('Recent');
    const browse = await libraryPage.byTextContains('BROWSE');
    const downloads = await libraryPage.byTextContains('Download');
    const showRoots = await libraryPage.byTextContains('Show roots');

    const pickerVisible =
      (await recent.isExisting().catch(() => false)) ||
      (await browse.isExisting().catch(() => false)) ||
      (await downloads.isExisting().catch(() => false)) ||
      (await showRoots.isExisting().catch(() => false));

    expect(pickerVisible).toBe(true);
    await driver.back();
    await driver.pause(800);
    await libraryPage.expectLoaded();
  });
});
