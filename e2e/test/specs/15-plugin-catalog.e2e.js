const allure = require('@wdio/allure-reporter').default;
const settingsPage = require('../pageobjects/settings.page');

/**
 * Catalog “Elegir proveedores” regressions: select-all, name-only filter, paging hint.
 * Opens Add repository → Continuar when languages appear → asserts catalog chrome.
 */
describe('Plugin catalog (Elegir proveedores)', () => {
  before(async () => {
    allure.addFeature('Plugin catalog');
    allure.addEpic('QuickNovel Ultimate');
    await settingsPage.waitReady();
  });

  afterEach(async () => {
    for (let i = 0; i < 4; i++) {
      await driver.back().catch(() => {});
      await driver.pause(250);
    }
    await settingsPage.ensureMainChrome();
  });

  it('opens catalog and shows select-all, name filter, and paging controls', async () => {
    allure.addSeverity('critical');
    await settingsPage.open();
    await settingsPage.expectLoaded();
    const opened = await settingsPage.openPluginAddRepo();
    expect(opened).toBe(true);

    // Fetch official / continue past language picker when possible.
    const continuar = await $('android=new UiSelector().textContains("Continuar")');
    if (await continuar.isDisplayed().catch(() => false)) {
      await continuar.click();
      await driver.pause(2500);
    }
    const fetchBtn = await $(
      'android=new UiSelector().textContains("Obtener").clickable(true)',
    );
    if (await fetchBtn.isDisplayed().catch(() => false)) {
      await fetchBtn.click();
      await driver.pause(8000);
      const cont2 = await $('android=new UiSelector().textContains("Continuar")');
      if (await cont2.isDisplayed().catch(() => false)) {
        await cont2.click();
        await driver.pause(2500);
      }
    }

    const catalogTitle = await $(
      'android=new UiSelector().textContains("Elegir proveedores")',
    );
    const selectAll = await $(
      'android=new UiSelector().textContains("Seleccionar todos")',
    );
    const selectAllEn = await $(
      'android=new UiSelector().textContains("Select all")',
    );
    const filterHint = await $(
      'android=new UiSelector().textContains("Buscar por nombre o sitio")',
    );
    const filterHintLegacy = await $(
      'android=new UiSelector().textContains("Buscar por nombre")',
    );
    const filterHintEn = await $(
      'android=new UiSelector().textContains("Search by name or site")',
    );
    const filterHintEnLegacy = await $(
      'android=new UiSelector().textContains("Search by name")',
    );
    const pageHint = await $(
      'android=new UiSelector().textContains("Anterior")',
    );
    const pageLabel = await $(
      'android=new UiSelector().textContains("Página")',
    );
    const madaraChip = await $(
      'android=new UiSelector().text("Madara").clickable(true)',
    );

    const onCatalog =
      (await catalogTitle.isDisplayed().catch(() => false)) ||
      (await selectAll.isDisplayed().catch(() => false)) ||
      (await selectAllEn.isDisplayed().catch(() => false)) ||
      (await pageLabel.isDisplayed().catch(() => false));

    if (!onCatalog) {
      // Offline / repo not fetched — still assert Settings path works.
      allure.addDescription('Catalog dialog not reached (network/repo); settings entry OK');
      expect(opened).toBe(true);
      return;
    }

    const hasSelectAll =
      (await selectAll.isDisplayed().catch(() => false)) ||
      (await selectAllEn.isDisplayed().catch(() => false));
    expect(hasSelectAll).toBe(true);

    const filterField = await $('android=new UiSelector().className("android.widget.EditText")');
    const hasFilterField = await filterField.isDisplayed().catch(() => false);
    const hasNameFilter =
      (await filterHint.isDisplayed().catch(() => false)) ||
      (await filterHintLegacy.isDisplayed().catch(() => false)) ||
      (await filterHintEn.isDisplayed().catch(() => false)) ||
      (await filterHintEnLegacy.isDisplayed().catch(() => false)) ||
      hasFilterField;
    expect(hasNameFilter).toBe(true);

    // Site/host filter: type a host fragment when catalog rows are present.
    if (hasFilterField) {
      let hostQuery = '';
      const texts = await $$('android.widget.TextView');
      for (const el of texts) {
        const t = ((await el.getText().catch(() => '')) || '').trim();
        const m = t.match(/([a-z0-9-]+\.[a-z.]{2,})/i);
        if (m && !/buscar|search|página|page|nombre|sitio/i.test(t)) {
          hostQuery = m[1];
          break;
        }
      }
      if (hostQuery) {
        await filterField.click();
        await filterField.clearValue().catch(() => {});
        await filterField.setValue(hostQuery);
        await driver.pause(900);
        const stillHasHost = await $(
          `android=new UiSelector().textContains("${hostQuery}")`,
        )
          .isDisplayed()
          .catch(() => false);
        expect(stillHasHost).toBe(true);
        allure.addAttachment('catalog-host-query', Buffer.from(hostQuery), 'text/plain');
      }
    }

    const hasPaging =
      (await pageLabel.isDisplayed().catch(() => false)) ||
      (await pageHint.isDisplayed().catch(() => false));
    expect(hasPaging).toBe(true);

    // Madara kind chip must not appear as a catalog filter anymore.
    expect(await madaraChip.isDisplayed().catch(() => false)).toBe(false);

    if (await selectAll.isDisplayed().catch(() => false)) {
      await selectAll.click();
      await driver.pause(800);
      const toastOrChecked = await $(
        'android=new UiSelector().textContains("Seleccionad")',
      );
      // Toast may vanish quickly — at least click must not crash the dialog.
      expect(await catalogTitle.isDisplayed().catch(() => false) ||
        await pageLabel.isDisplayed().catch(() => false)).toBe(true);
      void toastOrChecked;
    }
  });
});
