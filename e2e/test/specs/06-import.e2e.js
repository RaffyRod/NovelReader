const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const { openReaderViaEpubIntent } = require('../helpers/seedNovel');

describe('Import book (Import-B)', function () {
  this.timeout(60000);

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

    for (let i = 0; i < 3; i++) {
      await driver.back().catch(() => {});
      await driver.pause(250);
    }
    await libraryPage.ensureMainChrome(12000);
  });

  it('opens sample EPUB into the reader via FileProvider VIEW intent', async () => {
    allure.addSeverity('critical');
    const opened = await openReaderViaEpubIntent();
    expect(opened).toBe(true);
  });
});
