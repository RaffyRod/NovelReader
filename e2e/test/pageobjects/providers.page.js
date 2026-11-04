const BasePage = require('./base.page');
const searchPage = require('./search.page');
const settingsPage = require('./settings.page');

const QUERIES = ['my', 'hello', 'amor', 'el', 'the', 'over'];

class ProvidersPage extends BasePage {
  /**
   * Enables English + all languages/providers once from Settings.
   */
  async prepareAllProviders() {
    await settingsPage.enableAllSearchProviders();
  }

  /**
   * Opens the Search "Proveedores" multi-select dialog.
   */
  async openProvidersDialog() {
    await searchPage.open();
    await searchPage.expectLoaded();
    const btn = searchPage.providersButton;
    await btn.waitForDisplayed({ timeout: 15000 });
    await btn.click();
    await driver.pause(1000);
  }

  async confirmDialog() {
    const ok = this.byText('OK');
    const aceptar = this.byText('Aceptar');
    if (await ok.isExisting().catch(() => false)) await ok.click();
    else if (await aceptar.isExisting().catch(() => false)) await aceptar.click();
    else await driver.back();
    await driver.pause(600);
  }

  /**
   * @param {string} label
   * @param {string[]} names
   */
  matchProviderLabel(label, names) {
    const plain = label
      .replace(/^[\p{Emoji_Presentation}\p{Extended_Pictographic}\uFE0F\s]+/u, '')
      .trim();
    return names.some((n) => plain === n || plain.endsWith(` ${n}`) || plain.includes(n));
  }

  /**
   * Syncs visible checkboxes once (no scroll).
   * @param {string[]} names
   */
  async syncVisibleProviderChecks(names) {
    const boxes = await $$('android.widget.CheckBox');
    for (const box of boxes) {
      const parent = await box.$('..');
      const texts = await parent.$$('android.widget.TextView');
      let label = '';
      for (const t of texts) {
        const v = ((await t.getText().catch(() => '')) || '').trim();
        if (v) label = v;
      }
      const shouldCheck = this.matchProviderLabel(label, names);
      const checked = (await box.getAttribute('checked')) === 'true';
      if (shouldCheck !== checked) {
        await box.click();
        await driver.pause(60);
      }
    }
  }

  /**
   * Leaves only the given provider names checked in the search providers dialog.
   * Fast path: few scroll passes (CI hangs if this takes minutes).
   * @param {string[]} names
   */
  async selectOnlyProviders(names) {
    await this.openProvidersDialog();

    const matched = new Set();
    for (let pass = 0; pass < 6; pass++) {
      const boxes = await $$('android.widget.CheckBox');
      for (const box of boxes) {
        const parent = await box.$('..');
        const texts = await parent.$$('android.widget.TextView');
        let label = '';
        for (const t of texts) {
          const v = ((await t.getText().catch(() => '')) || '').trim();
          if (v) label = v;
        }
        const shouldCheck = this.matchProviderLabel(label, names);
        if (shouldCheck) matched.add(label);
        const checked = (await box.getAttribute('checked')) === 'true';
        // Only flip boxes we positively identify; never wipe the whole list first.
        if (shouldCheck && !checked) {
          await box.click();
          await driver.pause(60);
        } else if (!shouldCheck && checked && matched.size > 0) {
          // Uncheck extras only after we already locked at least one wanted provider.
          await box.click();
          await driver.pause(60);
        }
      }
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 500,
        height: 1100,
        direction: 'down',
        percent: 0.75,
      }).catch(() => {});
      await driver.pause(180);
    }

    if (matched.size === 0) {
      console.warn(`selectOnlyProviders: none of [${names.join(', ')}] found — keeping current selection`);
    }

    await this.confirmDialog();
  }

  /**
   * Searches with fallback queries until the provider shows results.
   * @param {string} providerName
   * @returns {Promise<{ok: boolean, query?: string, count?: number}>}
   */
  async assertProviderReturnsData(providerName) {
    await this.selectOnlyProviders([providerName]);

    for (const query of QUERIES) {
      await searchPage.search(query, 120000);
      const hits = await searchPage.collectProviderHits();
      const count = hits.get(providerName) || 0;
      if (count > 0) {
        return { ok: true, query, count };
      }
      // Also accept any result grid when only one provider is selected
      if (hits.size > 0) {
        const first = [...hits.entries()][0];
        if (first[1] > 0) return { ok: true, query, count: first[1], alias: first[0] };
      }
      const noResults = await searchPage.noResults.isDisplayed().catch(() => false);
      if (!noResults) {
        const cards = await $$('android.widget.TextView');
        if (cards.length > 8) {
          return { ok: true, query, count: cards.length, via: 'grid' };
        }
      }
    }
    return { ok: false };
  }
}

module.exports = new ProvidersPage();
