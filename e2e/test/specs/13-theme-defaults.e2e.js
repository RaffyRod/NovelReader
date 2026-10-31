const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Theme defaults', () => {
  before(async () => {
    allure.addFeature('Settings / Theme');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('keeps dark mode on and Ultimate Blue as default theme', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    await settingsPage.scrollToText('Modo oscuro');
    await driver.pause(400);
    const darkRow = settingsPage.byText('Modo oscuro');
    await darkRow.waitForDisplayed({ timeout: 15000 });

    const darkOn = await settingsPage.isPreferenceSwitchOnNear(darkRow);
    expect(darkOn).toBe(true);

    await settingsPage.scrollToText('Tema Ultimate');
    await driver.pause(400);
    const themeRow = settingsPage.byTextContains('Tema Ultimate');
    await themeRow.waitForDisplayed({ timeout: 15000 });

    const blue = settingsPage.byTextContains('Ultimate Blue');
    const blueVisible = await blue.isDisplayed().catch(() => false);
    // ListPreference shows current value as subtitle next to the row.
    expect(blueVisible).toBe(true);
  });
});
