const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Settings', () => {
  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('opens settings and shows tutorial entries', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    const found = await settingsPage.scrollToTutorials();
    expect(found).toBe(true);
    expect(await settingsPage.tutorialsVisible()).toBe(true);
  });

  it('opens TTS tutorial dialog when available', async () => {
    allure.addSeverity('normal');
    await settingsPage.open();
    await settingsPage.expectLoaded();
    const opened = await settingsPage.openTutorialTts();
    expect(opened).toBe(true);
    const closeEs = await settingsPage.byText('Cerrar');
    const closeEn = await settingsPage.byText('Close');
    if (await closeEs.isDisplayed().catch(() => false)) {
      await closeEs.click();
    } else if (await closeEn.isDisplayed().catch(() => false)) {
      await closeEn.click();
    } else {
      await driver.back();
    }
  });
});
