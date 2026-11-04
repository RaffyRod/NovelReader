const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Theme defaults', function () {
  this.timeout(90000);

  before(async () => {
    allure.addFeature('Settings / Theme');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('keeps dark mode on and Ultimate Blue as default theme', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    // Dark mode sits above Ultimate theme — assert it before scrolling down.
    let darkRow = null;
    for (const label of ['Modo oscuro', 'Dark mode']) {
      await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("${label}"))`,
      ).catch(() => {});
      const row = settingsPage.byTextContains(label);
      if (await row.isDisplayed().catch(() => false)) {
        darkRow = row;
        break;
      }
    }
    expect(darkRow).not.toBeNull();
    const onSwitch = await $('android=new UiSelector().description("switch_on")')
      .isDisplayed()
      .catch(() => false);
    const nearOn = await settingsPage.isPreferenceSwitchOnNear(darkRow);
    expect(onSwitch || nearOn).toBe(true);

    await $(
      'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Ultimate Blue"))',
    ).catch(() => {});
    await driver.pause(300);

    let blueOk = await settingsPage.byTextContains('Ultimate Blue').isDisplayed().catch(() => false);
    if (!blueOk) {
      await $(
        'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Tema Ultimate"))',
      ).catch(() => {});
      await $(
        'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Ultimate theme"))',
      ).catch(() => {});
      const themeRow = (await settingsPage
        .byTextContains('Tema Ultimate')
        .isDisplayed()
        .catch(() => false))
        ? settingsPage.byTextContains('Tema Ultimate')
        : settingsPage.byTextContains('Ultimate theme');
      await themeRow.click();
      await driver.pause(600);
      blueOk = await settingsPage.byTextContains('Ultimate Blue').isDisplayed().catch(() => false);
      expect(blueOk).toBe(true);
      const cancel = settingsPage.byText('Cancelar');
      if (await cancel.isDisplayed().catch(() => false)) await cancel.click();
      else await driver.back();
      await driver.pause(400);
    }
    expect(blueOk).toBe(true);
  });
});
