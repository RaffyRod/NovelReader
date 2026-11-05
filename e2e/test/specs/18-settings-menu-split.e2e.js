const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('Settings menu split (Config / Admin)', function () {
  this.timeout(60000);

  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  it('shows Configuración and Administración tabs', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

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
    await settingsPage.open();
    await settingsPage.expectLoaded();
    await settingsPage.selectMenuTab('config');

    let darkRow = null;
    for (const label of ['Modo oscuro', 'Dark mode']) {
      await settingsPage.scrollToText(label).catch(() => {});
      const row = settingsPage.byTextContains(label);
      if (await row.isDisplayed().catch(() => false)) {
        darkRow = row;
        break;
      }
    }
    expect(darkRow).not.toBeNull();

    const fuentesVisible =
      (await settingsPage.fuentesDeBuscar.isDisplayed().catch(() => false)) ||
      (await settingsPage.searchSources.isDisplayed().catch(() => false)) ||
      (await settingsPage.pluginHub.isDisplayed().catch(() => false));
    expect(fuentesVisible).toBe(false);
  });

  it('Administration tab shows Fuentes and hides dark mode', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();
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
