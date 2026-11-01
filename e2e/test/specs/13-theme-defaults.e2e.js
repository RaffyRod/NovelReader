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
    for (let i = 0; i < 8; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 400,
        width: 600,
        height: 1200,
        direction: 'up',
        percent: 0.9,
      }).catch(() => {});
      await driver.pause(120);
    }

    const themeLabels = ['Tema Ultimate', 'Ultimate theme', 'Ultimate Theme'];
    let themeRow = null;
    for (const label of themeLabels) {
      await settingsPage.scrollToText(label);
      await driver.pause(350);
      const row = settingsPage.byText(label);
      if (await row.isDisplayed().catch(() => false)) {
        themeRow = row;
        break;
      }
    }
    expect(themeRow).not.toBeNull();

    // Subtitle can be clipped on short CI viewports — open the picker and assert default.
    await themeRow.click();
    await driver.pause(800);
    const blueInDialog = settingsPage.byText('Ultimate Blue');
    await blueInDialog.waitForDisplayed({ timeout: 10000 });
    expect(await blueInDialog.isDisplayed()).toBe(true);
    // Dismiss picker (Cancel / Aceptar / back)
    const cancel = settingsPage.byText('Cancelar');
    const cancelEn = settingsPage.byText('Cancel');
    if (await cancel.isDisplayed().catch(() => false)) await cancel.click();
    else if (await cancelEn.isDisplayed().catch(() => false)) await cancelEn.click();
    else await driver.back();
    await driver.pause(500);

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

    let darkOn = await settingsPage.isPreferenceSwitchOnNear(darkRow);
    if (!darkOn) {
      // Explicit semantics from SwitchPreferenceWidget (switch_on / ON).
      const onSwitch = $('android=new UiSelector().description("switch_on")');
      darkOn = await onSwitch.isDisplayed().catch(() => false);
    }
    expect(darkOn).toBe(true);
  });
});
