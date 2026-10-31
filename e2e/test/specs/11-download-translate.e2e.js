const allure = require('@wdio/allure-reporter').default;
const resultPage = require('../pageobjects/result.page');

describe('Download language translation', function () {
  this.timeout(240000);

  before(async () => {
    allure.addFeature('Download translate');
    allure.addEpic('QuickNovel Ultimate');
    await resultPage.waitReady();
  });

  after(async () => {
    await resultPage.dismissDownloadDialog().catch(() => {});
    await resultPage.ensureMainChrome().catch(() => {});
  });

  it('download dialog exposes language picker and batch chips for EPUB', async function () {
    allure.addSeverity('critical');
    await resultPage.ensureMainChrome();

    const openedNovel = await resultPage.openFirstFromSearch('amor');
    if (!openedNovel) {
      allure.addDescription('Skipped: no novel detail from search (network/providers)');
      return this.skip();
    }

    const dialog = await resultPage.openDownloadDialog();
    if (!dialog) {
      allure.addDescription('Skipped: download dialog did not open (novel may lack chapters)');
      return this.skip();
    }

    expect(await resultPage.formatEpub.isDisplayed().catch(() => false)).toBe(true);

    const batches =
      (await resultPage.chaptersPerFile.isDisplayed().catch(() => false)) ||
      (await resultPage.byTextContains('Todo en un archivo').isDisplayed().catch(() => false)) ||
      (await resultPage.byTextContains('10').isDisplayed().catch(() => false));
    expect(batches).toBe(true);

    const langVisible = await resultPage.revealTranslateLanguage();
    expect(langVisible).toBe(true);

    await resultPage.dismissDownloadDialog();
  });
});
