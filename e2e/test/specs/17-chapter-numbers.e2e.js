const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const historyPage = require('../pageobjects/history.page');
const resultPage = require('../pageobjects/result.page');
const BasePage = require('../pageobjects/base.page');
const {
  listTitle,
  looksNumberedChapterLabel,
  extractChapterNumber,
} = require('../helpers/chapterTitles');

/**
 * Clicks the Capítulos / Chapters tab on novel detail (exact label only).
 * @returns {Promise<boolean>}
 */
async function clickChaptersTab() {
  for (const label of ['Capítulos', 'Chapters']) {
    const tab = await $(`android=new UiSelector().text("${label}")`);
    if (await tab.isDisplayed().catch(() => false)) {
      await tab.click();
      await driver.pause(1200);
      return true;
    }
  }
  return false;
}

/**
 * Opens Capítulos on a novel detail. Search first (history is often empty mid-suite).
 * @returns {Promise<boolean>}
 */
async function openChaptersTab() {
  await libraryPage.recoverMainApp().catch(() => {});
  await driver.pause(600);

  // 1) Unified search — most reliable after download/settings specs.
  if (!(await resultPage.isDetailVisible())) {
    const opened = await resultPage.openFirstFromSearch('amor').catch((err) => {
      console.warn(`openFirstFromSearch(amor): ${err.message || err}`);
      return false;
    });
    if (!opened && !(await resultPage.isDetailVisible())) {
      // 2) History fallback when search hosts flake.
      await historyPage.open().catch(() => {});
      await driver.pause(800);
      const titles = await $$(
        'android=new UiSelector().className("android.widget.TextView").textMatches("(?i)^(?!Visitad|Hoy|Ayer|Biblioteca|Buscar|Historial|Ajustes).{5,100}$")',
      );
      for (const t of titles.slice(0, 6)) {
        const text = await t.getText().catch(() => '');
        if (!text || text.length < 5) continue;
        await t.click().catch(() => {});
        await driver.pause(2200);
        if (await resultPage.isDetailVisible()) break;
      }
    }
  }

  if (!(await resultPage.isDetailVisible())) return false;
  if (!(await clickChaptersTab())) return false;
  return true;
}

/**
 * Collects visible chapter row texts from the Capítulos list.
 * @returns {Promise<string[]>}
 */
async function visibleChapterLabels() {
  const pkg = BasePage.appId();
  const rows = await $$(
    `android=new UiSelector().resourceId("${pkg}:id/name")`,
  );
  const out = [];
  for (const row of rows) {
    const text = await row.getText().catch(() => '');
    if (text && text.trim().length > 0) out.push(text.trim());
  }
  if (out.length > 0) return out;

  // Fallback wildcard id (debug builds / older APKs).
  const anyName = await $$('android=new UiSelector().resourceIdMatches(".*:id/name")');
  for (const row of anyName) {
    const text = await row.getText().catch(() => '');
    if (text && text.trim().length > 0) out.push(text.trim());
  }
  if (out.length > 0) return out;

  // Fallback: any TextView that looks like a chapter label.
  const tvs = await $$('android.widget.TextView');
  for (const tv of tvs) {
    const text = await tv.getText().catch(() => '');
    if (looksNumberedChapterLabel(text)) out.push(text.trim());
  }
  return out;
}

/**
 * Polls until chapter rows appear (provider TOC load is async on CI).
 * @param {number} timeoutMs
 * @returns {Promise<string[]>}
 */
async function waitForChapterLabels(timeoutMs = 45000) {
  const deadline = Date.now() + timeoutMs;
  let labels = [];
  while (Date.now() < deadline) {
    labels = await visibleChapterLabels();
    if (labels.length > 0) return labels;
    // Re-tap Capítulos in case we landed on Novela / loading shell.
    await clickChaptersTab().catch(() => {});
    await driver
      .execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 600,
        height: 1000,
        direction: 'down',
        percent: 0.35,
      })
      .catch(() => {});
    await driver.pause(1500);
  }
  return labels;
}

describe('Chapter numbering (integers + decimals)', function () {
  this.timeout(180000);

  before(async () => {
    allure.addFeature('Result / Chapter numbers');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  after(async () => {
    await libraryPage.recoverMainApp().catch(() => {});
  });

  it('listTitle preserves decimal chapter numbers (2.5, 3.3) over list index', async () => {
    allure.addSeverity('critical');
    // Contract with app ChapterSpec.listTitle — decimals must survive list position.
    expect(listTitle(7, 'Chapter 2.5', 'Capítulo')).toBe('Capítulo 2.5');
    expect(listTitle(9, 'Capítulo 3.3', 'Capítulo')).toBe('Capítulo 3.3');
    expect(listTitle(7, 'Chapter 2.5: Extra scene', 'Capítulo')).toBe(
      'Capítulo 2.5. Extra scene',
    );
    expect(listTitle(3, '1.5', 'Capítulo')).toBe('Capítulo 1.5');
    expect(listTitle(12, '4.25 Side story', 'Capítulo')).toBe(
      'Capítulo 4.25. Side story',
    );
    expect(listTitle(2, 'Chapter 2', 'Capítulo')).toBe('Capítulo 2');
    expect(extractChapterNumber('Capítulo 2.5')).toBe('2.5');
    expect(extractChapterNumber('Capítulo 3.3. Side')).toBe('3.3');
  });

  it('shows numbered chapter labels on Capítulos (emulator)', async () => {
    allure.addSeverity('critical');
    const opened = await openChaptersTab();
    expect(opened).toBe(true);

    const labels = await waitForChapterLabels(45000);
    expect(labels.length).toBeGreaterThan(0);
    const numbered = labels.filter(looksNumberedChapterLabel);
    // Majority of visible chapter rows must carry an integer or decimal number.
    expect(numbered.length).toBeGreaterThan(0);
    expect(numbered.length / labels.length).toBeGreaterThanOrEqual(0.5);

    for (const label of numbered.slice(0, 8)) {
      const num = extractChapterNumber(label);
      expect(num).not.toBeNull();
      expect(num).toMatch(/^\d+(\.\d+)?$/);
    }
  });
});
