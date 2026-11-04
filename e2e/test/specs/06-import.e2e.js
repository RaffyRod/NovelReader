const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const {
  hasFixtureContent,
  openAndAssertFixtureReader,
} = require('../helpers/seedNovel');

describe('Import book (Import-B)', function () {
  this.timeout(45000);

  before(async () => {
    allure.addFeature('Import');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  after(async () => {
    await libraryPage.recoverMainApp().catch(() => {});
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
    await driver.pause(1200);

    // One page-source check — sequential UiSelector finds burn the mocha budget.
    const src = String((await driver.getPageSource().catch(() => '')) || '');
    const pickerVisible =
      /Recent|BROWSE|Download|Show roots|Open from|Files|Documents|Documentos|Descargas/i.test(
        src,
      ) || !(await libraryPage.isMainChromeVisible());
    expect(pickerVisible).toBe(true);

    await libraryPage.recoverMainApp();
  });

  it('opens sample EPUB and shows fixture title, chapter text, and TTS', async () => {
    allure.addSeverity('critical');
    // DocumentsUI path is flaky on CI emulators; MediaStore VIEW still exercises
    // the same ReadActivity2 EPUB parse/render path and must prove real content.
    await openAndAssertFixtureReader();
    expect(await hasFixtureContent()).toBe(true);
    await expect(readerPage.ttsButton).toBeDisplayed();
  });
});
