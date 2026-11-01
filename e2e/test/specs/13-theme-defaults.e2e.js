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

    // Previous specs may leave Settings scrolled far down — rewind first.
    for (let i = 0; i < 6; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 400,
        width: 600,
        height: 1200,
        direction: 'up',
        percent: 0.9,
      }).catch(() => {});
      await driver.pause(150);
    }

    const themeLabels = ['Tema Ultimate', 'Ultimate theme', 'Ultimate Theme'];
    let themeVisible = false;
    for (const label of themeLabels) {
      await settingsPage.scrollToText(label);
      await driver.pause(350);
      if (await settingsPage.byTextContains(label).isDisplayed().catch(() => false)) {
        themeVisible = true;
        break;
      }
    }
    expect(themeVisible).toBe(true);

    const blue = settingsPage.byTextContains('Ultimate Blue');
    const blueVisible = await blue.isDisplayed().catch(() => false);
    // ListPreference shows current value as subtitle next to the row.
    expect(blueVisible).toBe(true);

    const darkLabels = ['Modo oscuro', 'Dark mode'];
    let darkRow = null;
    for (const label of darkLabels) {
      await settingsPage.scrollToText(label);
      await driver.pause(400);
      const row = settingsPage.byText(label);
      if (await row.isDisplayed().catch(() => false)) {
        darkRow = row;
        break;
      }
    }
    expect(darkRow).not.toBeNull();
    await darkRow.waitForDisplayed({ timeout: 15000 });

    const darkOn = await settingsPage.isPreferenceSwitchOnNear(darkRow);
    expect(darkOn).toBe(true);
  });
});
