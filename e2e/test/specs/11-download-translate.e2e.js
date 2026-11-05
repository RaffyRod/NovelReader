const allure = require('@wdio/allure-reporter').default;
const resultPage = require('../pageobjects/result.page');
const searchPage = require('../pageobjects/search.page');

/**
 * Opens novel detail for the download dialog.
 * Prefer unified search first — home-cover loops + Settings toggles burned the
 * previous 90s mocha budget before openFirstFromSearch could finish (CI general).
 * @returns {Promise<boolean>}
 */
async function openNovelDetailForDownload() {
  await resultPage.recoverMainApp();
  await resultPage.ensureMainChrome(15000);

  // 1) Unified search → Coil cover (English providers default on).
  try {
    if (await resultPage.openFirstFromSearch('amor')) return true;
  } catch (err) {
    console.warn(`openFirstFromSearch(amor): ${err.message || err}`);
  }

  // 2) Popular / home covers (short pass only).
  await searchPage.recoverMainApp().catch(() => {});
  await searchPage.open().catch(() => {});
  await searchPage.expectLoaded().catch(() => {});
  const homeCovers = await searchPage.findNovelCoverElements();
  for (const el of homeCovers.slice(0, 3)) {
    await el.click().catch(() => {});
    await driver.pause(2000);
    if (await resultPage.isDetailVisible()) return true;
    await driver.back().catch(() => {});
    await driver.pause(300);
  }

  // 3) Provider MainPage → first novel cover.
  await searchPage.recoverMainApp();
  await searchPage.open();
  await searchPage.expectLoaded();
  const tile = await searchPage.findProviderTileTarget();
  if (!tile) return false;
  await tile.click();
  await driver.pause(1500);
  const settled = await searchPage.waitProviderMainPageSettled(30000);
  if (!settled.hasCards && !settled.ok) return false;

  const pageCovers = await searchPage.findNovelCoverElements();
  for (const el of pageCovers.slice(0, 6)) {
    await el.click().catch(() => {});
    await driver.pause(2000);
    if (await resultPage.isDetailVisible()) return true;
  }

  // Text titles on MainPage as last resort.
  const texts = await $$('android.widget.TextView');
  for (const el of texts.slice(0, 30)) {
    const t = ((await el.getText().catch(() => '')) || '').trim();
    if (!t || t.length < 10 || t.length > 90) continue;
    if (/Buscar|Search|Fiction|English|Spanish|Filter|Z-Library|Wattpad|Proveedor/i.test(t)) {
      continue;
    }
    await el.click().catch(() => {});
    await driver.pause(2000);
    if (await resultPage.isDetailVisible()) return true;
  }
  return false;
}

describe('Download language translation', function () {
  // Search alone can take ~70s; keep headroom for dialog asserts after open.
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
