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
    await expect(darkRow).toBeDisplayed();
  });

  it('shows tutorial TTS entry and opens its dialog', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    await $(
      'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Tutorial"))',
    ).catch(() => {});
    await $(
      'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("(TTS)"))',
    ).catch(() => {});

    const ttsRow = settingsPage.tutorialTts;
    await ttsRow.waitForDisplayed({ timeout: 10000 });
    await ttsRow.click();
    await driver.pause(700);

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
