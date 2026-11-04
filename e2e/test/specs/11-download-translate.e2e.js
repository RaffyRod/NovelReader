const allure = require('@wdio/allure-reporter').default;
const resultPage = require('../pageobjects/result.page');
const searchPage = require('../pageobjects/search.page');
const settingsPage = require('../pageobjects/settings.page');

/**
 * Opens novel detail via Popular covers, then one live search.
 * @returns {Promise<boolean>}
 */
async function openNovelDetailForDownload() {
  await resultPage.recoverMainApp();
  await resultPage.ensureMainChrome(15000);
  await settingsPage.enableAllSearchProviders().catch(() => {});

  await searchPage.open();
  await searchPage.expectLoaded();

  // Popular covers on Buscar home (fast path when network populates the row).
  const covers = await $$('android.widget.ImageView');
  for (const el of covers.slice(0, 8)) {
    if (!(await el.isDisplayed().catch(() => false))) continue;
    await el.click().catch(() => {});
    await driver.pause(2000);
    if (await resultPage.isDetailVisible()) return true;
    await driver.back().catch(() => {});
    await driver.pause(400);
    await searchPage.open().catch(() => {});
  }

  // One live search against modern SearchRow UI (not legacy chips).
  try {
    return await resultPage.openFirstFromSearch('amor');
  } catch (err) {
    console.warn(`openFirstFromSearch: ${err.message || err}`);
    return false;
  }
}

describe('Download language translation', function () {
  this.timeout(180000);

  before(async () => {
    allure.addFeature('Download translate');
    allure.addEpic('QuickNovel Ultimate');
    await resultPage.waitReady();
  });

  after(async () => {
    await resultPage.dismissDownloadDialog().catch(() => {});
    await resultPage.ensureMainChrome().catch(() => {});
  });

  it('download dialog exposes language picker and batch chips for EPUB', async () => {
    allure.addSeverity('critical');

    const openedNovel = await openNovelDetailForDownload();
    expect(openedNovel).toBe(true);

    const dialog = await resultPage.openDownloadDialog();
    expect(dialog).toBe(true);

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
