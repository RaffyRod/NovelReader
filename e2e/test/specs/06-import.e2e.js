const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const {
  hasFixtureContent,
  openAndAssertFixtureReader,
} = require('../helpers/seedNovel');

describe('Import book (Import-B)', function () {
  this.timeout(90000);

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

  it('opens sample EPUB and shows fixture title, chapter text, and TTS', async () => {
    allure.addSeverity('critical');
    // DocumentsUI path is flaky on CI emulators; FileProvider VIEW still exercises
    // the same ReadActivity2 EPUB parse/render path and must prove real content.
    await openAndAssertFixtureReader();
    expect(await hasFixtureContent()).toBe(true);
    await expect(readerPage.ttsButton).toBeDisplayed();
    await expect(
      $(`android=new UiSelector().textContains("Prueba QuickNovel E2E")`),
    ).toBeDisplayed();
  });
});
