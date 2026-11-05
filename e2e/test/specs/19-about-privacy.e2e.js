const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

describe('About and Privacy', function () {
  this.timeout(120000);

  before(async () => {
    allure.addFeature('Settings');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  afterEach(async () => {
    await settingsPage.dismissAboutOrPrivacySheet().catch(() => {});
  });

  it('shows Información preferences under Configuration', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();

    const visible = await settingsPage.scrollToAbout();
    expect(visible).toBe(true);

    const aboutVisible =
      (await settingsPage.aboutTitle.isDisplayed().catch(() => false)) ||
      (await settingsPage.aboutTitleEn.isDisplayed().catch(() => false)) ||
      (await settingsPage.aboutGroup.isDisplayed().catch(() => false)) ||
      (await settingsPage.aboutGroupEn.isDisplayed().catch(() => false));
    const privacyVisible =
      (await settingsPage.privacyTitle.isDisplayed().catch(() => false)) ||
      (await settingsPage.privacyTitleEn.isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('privacidad').isDisplayed().catch(() => false)) ||
      (await settingsPage.byTextContains('Privacy').isDisplayed().catch(() => false));
    expect(aboutVisible).toBe(true);
    expect(privacyVisible).toBe(true);
  });

  it('opens About sheet, shows version, and opens Privacy from CTA', async () => {
    allure.addSeverity('critical');
    let opened = await settingsPage.openAboutSheet();
    if (!opened) {
      await settingsPage.dismissAboutOrPrivacySheet().catch(() => {});
      opened = await settingsPage.openAboutSheet();
    }
    expect(opened).toBe(true);

    const src = String((await driver.getPageSource().catch(() => '')) || '');
    const hasVersion =
      /2\.\d+\.\d+|about_version|Versión|Version/i.test(src) ||
      /Ultimate Blue|Lector de novelas|Novel reader/i.test(src);
    const hasPrivacyCta =
      /Ver política de privacidad|View privacy policy|about_open_privacy/i.test(src);
    expect(hasVersion).toBe(true);
    expect(hasPrivacyCta).toBe(true);

    const privacyOpened = await settingsPage.tapAboutOpenPrivacy();
    expect(privacyOpened).toBe(true);

    const privacySrc = String((await driver.getPageSource().catch(() => '')) || '');
    const hasBody =
      /Datos que no recopilamos|Data we do not collect|No pedimos cuenta|We do not require an account|Última actualización|Last updated/i.test(
        privacySrc,
      );
    expect(hasBody).toBe(true);
  });

  it('opens Privacy policy sheet from hub preference row', async () => {
    allure.addSeverity('normal');
    let opened = await settingsPage.openPrivacySheetFromHub();
    if (!opened) {
      await settingsPage.dismissAboutOrPrivacySheet().catch(() => {});
      opened = await settingsPage.openPrivacySheetFromHub();
    }
    expect(opened).toBe(true);

    const src = String((await driver.getPageSource().catch(() => '')) || '');
    expect(
      /respeta tu privacidad|respects your privacy|Almacenamiento en el dispositivo|On-device storage|Datos que no recopilamos|Data we do not collect/i.test(
        src,
      ),
    ).toBe(true);
  });
});
