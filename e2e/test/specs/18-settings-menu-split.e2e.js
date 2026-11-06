const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Settings menu split (Config / Admin)', function () {
  this.timeout(90000);

  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  /**
   * Remounts Settings from Biblioteca so Compose LazyColumn scroll state is clean.
   */
  async function openSettingsFresh() {
    await settingsPage.openTab('Biblioteca');
    await driver.pause(400);
    await settingsPage.open();
    await settingsPage.expectLoaded();
    await settingsPage.selectMenuTab('config');
    await settingsPage.rewindSettingsTop();
    await settingsPage.selectMenuTab('config');
  }

  it('shows Configuración and Administración tabs', async () => {
    allure.addSeverity('critical');
    await openSettingsFresh();

    const hasConfig =
      (await settingsPage.configurationTab.isDisplayed().catch(() => false)) ||
      (await settingsPage.configurationTabLabel.isDisplayed().catch(() => false)) ||
      (await settingsPage.configurationTabLabelEn.isDisplayed().catch(() => false));
    const hasAdmin =
      (await settingsPage.administrationTab.isDisplayed().catch(() => false)) ||
      (await settingsPage.administrationTabLabel.isDisplayed().catch(() => false)) ||
      (await settingsPage.administrationTabLabelEn.isDisplayed().catch(() => false));
    expect(hasConfig).toBe(true);
    expect(hasAdmin).toBe(true);
  });

  it('Configuration tab shows dark mode and hides Fuentes', async () => {
    allure.addSeverity('critical');
    await openSettingsFresh();

    const darkRow = await settingsPage.findDarkModePreference();
    const configOk =
      darkRow != null ||
      (await settingsPage.configurationContentVisible());
    expect(configOk).toBe(true);

    const fuentesVisible =
      (await settingsPage.fuentesDeBuscar.isDisplayed().catch(() => false)) ||
      (await settingsPage.searchSources.isDisplayed().catch(() => false)) ||
      (await settingsPage.pluginHub.isDisplayed().catch(() => false));
    expect(fuentesVisible).toBe(false);
  });

  it('Administration tab shows Fuentes and hides dark mode', async () => {
    allure.addSeverity('critical');
    await openSettingsFresh();
    await settingsPage.selectMenuTab('admin');
    await settingsPage.rewindSettingsTop();
    await settingsPage.selectMenuTab('admin');

    const sourcesVisible = await settingsPage.scrollToPlugins();
    expect(sourcesVisible).toBe(true);

    const darkVisible =
      (await settingsPage.byTextContains('Modo oscuro').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Dark mode').isDisplayed().catch(() => false));
    expect(darkVisible).toBe(false);
  });
});
