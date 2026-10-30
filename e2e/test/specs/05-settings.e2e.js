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

    const tutorial = await settingsPage.tutorialGroup.isDisplayed().catch(() => false);
    const download = await settingsPage.tutorialDownload.isDisplayed().catch(() => false);
    const tts = await settingsPage.tutorialTts.isDisplayed().catch(() => false);
    expect(tutorial || download || tts).toBe(true);
  });

  it('opens TTS tutorial dialog when available', async () => {
    allure.addSeverity('normal');
    await settingsPage.open();
    const opened = await settingsPage.openTutorialTts();
    if (opened) {
      const close = await settingsPage.byText('Cerrar');
      if (await close.isExisting().catch(() => false)) {
        await close.click();
      } else {
        await driver.back();
      }
    }
    expect(opened).toBe(true);
  });
});
