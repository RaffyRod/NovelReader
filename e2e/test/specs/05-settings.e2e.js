const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Settings', function () {
  this.timeout(90000);

  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  /**
   * Remount Settings so prior suite scroll (Config/Admin) does not clip Tutorial rows.
   */
  async function openSettingsFresh() {
    await settingsPage.openTab('Biblioteca');
    await driver.pause(400);
    await settingsPage.open();
    await settingsPage.expectLoaded();
    await settingsPage.selectMenuTab('config');
    await settingsPage.rewindSettingsTop();
  }

  it('opens settings and shows dark mode preference', async () => {
    allure.addSeverity('critical');
    await openSettingsFresh();

    const darkRow = await settingsPage.findDarkModePreference();
    expect(darkRow).not.toBeNull();
    await expect(darkRow).toBeDisplayed();
  });

  it('shows tutorial TTS entry and opens its dialog', async () => {
    allure.addSeverity('critical');
    await openSettingsFresh();

    const opened = await settingsPage.openTutorialTts();
    expect(opened).toBe(true);
    expect(await settingsPage.isTutorialTtsDialogOpen()).toBe(true);

    const closeEs = settingsPage.byText('Cerrar');
    const closeEn = settingsPage.byText('Close');
    if (await closeEs.isDisplayed().catch(() => false)) await closeEs.click();
    else if (await closeEn.isDisplayed().catch(() => false)) await closeEn.click();
    else await driver.back();
    await driver.pause(400);
  });
});
