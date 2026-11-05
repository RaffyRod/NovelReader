const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Settings', function () {
  this.timeout(90000);

  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('opens settings and shows dark mode preference', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    let darkRow = null;
    for (const label of ['Modo oscuro', 'Dark mode']) {
      await settingsPage.scrollToText(label).catch(() => {});
      const row = settingsPage.byTextContains(label);
      if (await row.isDisplayed().catch(() => false)) {
        darkRow = row;
        break;
      }
    }
    expect(darkRow).not.toBeNull();
    await expect(darkRow).toBeDisplayed();
  });

  it('shows tutorial TTS entry and opens its dialog', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    const opened = await settingsPage.openTutorialTts();
    expect(opened).toBe(true);
    await driver.pause(500);

    const dialog =
      (await settingsPage.byTextContains('Abre un capítulo').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Open a chapter').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('altavoz').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('speaker').isDisplayed().catch(() => false)) ||
      (await settingsPage.byText('Cerrar').isDisplayed().catch(() => false)) ||
      (await settingsPage.byText('Close').isDisplayed().catch(() => false));
    expect(dialog).toBe(true);

    const closeEs = await settingsPage.byText('Cerrar');
    const closeEn = await settingsPage.byText('Close');
    if (await closeEs.isDisplayed().catch(() => false)) await closeEs.click();
    else if (await closeEn.isDisplayed().catch(() => false)) await closeEn.click();
    else await driver.back();
  });
});
