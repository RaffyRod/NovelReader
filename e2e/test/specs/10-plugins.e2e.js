const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Plugins (LNReader)', () => {
  before(async () => {
    allure.addFeature('Plugins');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('shows the Plugins group in Settings', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();
    const found = await settingsPage.scrollToPlugins();
    expect(found).toBe(true);
    const group = await settingsPage.pluginGroup.isDisplayed().catch(() => false);
    const hub = await settingsPage.pluginHub.isDisplayed().catch(() => false);
    const add = await settingsPage.pluginAddRepo.isDisplayed().catch(() => false);
    expect(group || hub || add).toBe(true);
  });

  it('opens the plugin hub dialog', async () => {
    allure.addSeverity('critical');
    const opened = await settingsPage.openPluginHub();
    expect(opened).toBe(true);
    await driver.back().catch(() => {});
    await driver.pause(400);
  });

  it('opens add repository with JSON catalog fields', async () => {
    allure.addSeverity('normal');
    const opened = await settingsPage.openPluginAddRepo();
    expect(opened).toBe(true);
    await driver.back().catch(() => {});
    await driver.pause(400);
  });
});
