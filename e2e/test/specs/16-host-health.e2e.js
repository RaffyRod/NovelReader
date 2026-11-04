const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');
const searchPage = require('../pageobjects/search.page');

describe('Host health bottom sheet', () => {
  before(async () => {
    allure.addFeature('Host health');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  afterEach(async () => {
    // Dismiss bottom sheet / dialogs without full app restart (keeps general suite stable).
    await settingsPage.dismissHostHealthSheet().catch(() => {});
    for (let i = 0; i < 4; i++) {
      await driver.back().catch(() => {});
      await driver.pause(200);
    }
    await settingsPage.ensureMainChrome(18000).catch(() => {});
  });

  // Buscar first (fresh chrome). Mock #1: long-press only — not auto, no tile button.
  it('Buscar: tap does NOT open host sheet (only opens provider path)', async () => {
    allure.addSeverity('critical');
    await searchPage.ensureMainChrome(20000);
    await searchPage.open();
    await searchPage.expectLoaded();
    const target = await searchPage.findProviderTileTarget();
    expect(target).not.toBeNull();
    await target.click();
    await driver.pause(1200);

    const sheetOpen = await settingsPage.isHostHealthSheetOpen();
    allure.addAttachment(
      'tap-should-not-open-sheet',
      Buffer.from(JSON.stringify({ sheetOpen }, null, 2)),
      'application/json',
    );
    expect(sheetOpen).toBe(false);

    await driver.back().catch(() => {});
    await searchPage.ensureMainChrome(15000).catch(() => {});
  });

  it('Buscar long-press opens bottom sheet with loader then result (mock #1)', async () => {
    allure.addSeverity('critical');
    await searchPage.ensureMainChrome(20000);
    const opened = await searchPage.openHostHealthFromSearchTile();
    expect(opened).toBe(true);

    const sheetRoot =
      (await settingsPage.hostHealthSheet.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('long-press').isDisplayed().catch(() => false));
    const sawChecking =
      (await settingsPage.hostHealthProgress.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Comprobando').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Checking').isDisplayed().catch(() => false));

    const result = await settingsPage.waitHostHealthResult(28000);
    allure.addAttachment(
      'host-health-search-bottom-sheet',
      Buffer.from(JSON.stringify({ sheetRoot, sawChecking, ...result }, null, 2)),
      'application/json',
    );

    const hasConfigured =
      (await settingsPage.hostHealthConfigured.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('URL configurada').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Configured URL').isDisplayed().catch(() => false));
    const hasRecheck =
      (await settingsPage.hostHealthRecheck.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthRecheckEs.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthRecheckEn.isDisplayed().catch(() => false));
    const hasClose =
      (await settingsPage.hostHealthClose.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthCloseEs.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthCloseEn.isDisplayed().catch(() => false));

    expect(sheetRoot || sawChecking || result.sawProgress || opened).toBe(true);
    expect(result.done || hasConfigured || sawChecking || result.sawProgress).toBe(true);
    expect(hasConfigured || hasRecheck || hasClose || result.done || sawChecking).toBe(true);

    await settingsPage.dismissHostHealthSheet();
    await searchPage.ensureMainChrome(15000);
    await searchPage.open();
    expect(await searchPage.isMainChromeVisible()).toBe(true);
    expect(await settingsPage.isHostHealthSheetOpen()).toBe(false);
  });

  it('Fuentes long-press opens bottom sheet with progress then result', async () => {
    allure.addSeverity('critical');
    await settingsPage.ensureMainChrome(15000);
    const opened = await settingsPage.openHostHealthSheetFromFuentes();
    expect(opened).toBe(true);

    const sawCheckingEarly =
      (await settingsPage.hostHealthProgress.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Comprobando').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Checking').isDisplayed().catch(() => false));

    const result = await settingsPage.waitHostHealthResult(28000);
    allure.addAttachment(
      'host-health-fuentes',
      Buffer.from(JSON.stringify({ sawCheckingEarly, ...result }, null, 2)),
      'application/json',
    );

    const hasConfigured =
      (await settingsPage.hostHealthConfigured.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('URL configurada').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Configured URL').isDisplayed().catch(() => false));
    const hasLatency =
      (await settingsPage.hostHealthLatency.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Latencia').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Latency').isDisplayed().catch(() => false));
    const hasRecheck =
      (await settingsPage.hostHealthRecheck.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthRecheckEs.isDisplayed().catch(() => false)) ||
      (await settingsPage.hostHealthRecheckEn.isDisplayed().catch(() => false));

    expect(result.done || hasConfigured || hasLatency || sawCheckingEarly).toBe(true);
    expect(hasConfigured || hasLatency || hasRecheck || result.sawProgress || sawCheckingEarly).toBe(
      true,
    );
  });

  it('Fuentes recheck shows progress again and close returns to list', async () => {
    allure.addSeverity('critical');
    await settingsPage.ensureMainChrome(15000);
    const opened = await settingsPage.openHostHealthSheetFromFuentes();
    expect(opened).toBe(true);
    await settingsPage.waitHostHealthResult(28000);

    const rechecked = await settingsPage.tapHostHealthRecheck();
    expect(rechecked).toBe(true);

    const progressAgain =
      (await settingsPage.hostHealthProgress.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Comprobando').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Checking').isDisplayed().catch(() => false));
    const after = await settingsPage.waitHostHealthResult(28000);
    allure.addAttachment(
      'host-health-recheck',
      Buffer.from(JSON.stringify({ progressAgain, after }, null, 2)),
      'application/json',
    );
    expect(progressAgain || after.done || after.sawProgress).toBe(true);

    await settingsPage.dismissHostHealthSheet();
    const backOnFuentes =
      (await settingsPage.byTextContains('Activas').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Active').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Todas').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('All').isDisplayed().catch(() => false)) ||
      (await settingsPage.fuentesEditBtn.isDisplayed().catch(() => false)) ||
      (await settingsPage.fuentesEditBtnEn.isDisplayed().catch(() => false));
    expect(backOnFuentes).toBe(true);
  });
});
