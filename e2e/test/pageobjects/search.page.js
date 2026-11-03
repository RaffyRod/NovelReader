const BasePage = require('./base.page');

const APP_ID = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';

class SearchPage extends BasePage {
  get tabSearch() {
    return this.byId(`${APP_ID}:id/navigation_search`);
  }

  get providersButton() {
    // ES: Proveedores · EN: Search Providers (content-desc from R.string.search_providers)
    return $(
      'android=new UiSelector().descriptionMatches("(?i).*(Proveedores|Search Providers|Providers).*")',
    );
  }

  /** Fallback when content-desc is missing on Compose IconButton. */
  get providersButtonByDescContains() {
    return $('android=new UiSelector().descriptionContains("Proveedor")');
  }

  get providersButtonEn() {
    return $('android=new UiSelector().descriptionContains("Provider")');
  }

  get providersTitle() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Proveedores|Providers).*")',
    );
  }

  get searchField() {
    return $(
      'android=new UiSelector().className("android.widget.EditText")',
    );
  }

  get noResults() {
    return $(
      'android=new UiSelector().textMatches("(?i).*(Sin resultados|No results).*")',
    );
  }

  get dialogOk() {
    return this.byText('OK');
  }

  get dialogAceptar() {
    return this.byText('Aceptar');
  }

  async open() {
    await this.openTab('Buscar');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSearch.waitForDisplayed({ timeout: 20000 });
    // Prefer providers tune control; fall back to search field (locale-safe).
    const providersVisible = await this.providersButton
      .waitForDisplayed({ timeout: 8000 })
      .then(() => true)
      .catch(() => false);
    if (!providersVisible) {
      await this.searchField.waitForDisplayed({ timeout: 15000 });
    }
    await expect(this.tabSearch).toBeDisplayed();
  }

  /**
   * Resolves the Search tune / providers IconButton with locale fallbacks.
   * @returns {Promise<WebdriverIO.Element|null>}
   */
  async findProvidersButton() {
    const candidates = [
      this.providersButton,
      this.providersButtonByDescContains,
      this.providersButtonEn,
      $(`~Proveedores`),
      $(`~Search Providers`),
      $(`~Providers`),
    ];
    for (const el of candidates) {
      if (await el.isDisplayed().catch(() => false)) return el;
    }
    for (const el of candidates) {
      const ok = await el
        .waitForDisplayed({ timeout: 4000 })
        .then(() => true)
        .catch(() => false);
      if (ok) return el;
    }
    return null;
  }

  async openProvidersDialog() {
    // Ensure home Search (not results) so the tune IconButton is visible.
    await this.open();
    await this.expectLoaded();
    const btn = await this.findProvidersButton();
    if (!btn) {
      throw new Error('Providers multi-select button not found on Buscar');
    }
    await btn.click();
    await driver.pause(1000);
    const ok = await this.dialogOk.isExisting().catch(() => false);
    const aceptar = await this.dialogAceptar.isExisting().catch(() => false);
    if (!ok && !aceptar) {
      throw new Error('Providers multi-select dialog did not open');
    }
  }

  async confirmProvidersDialog() {
    if (await this.dialogOk.isExisting().catch(() => false)) {
      await this.dialogOk.click();
    } else if (await this.dialogAceptar.isExisting().catch(() => false)) {
      await this.dialogAceptar.click();
    } else {
      await driver.back();
    }
    await driver.pause(800);
  }

  /**
   * Compose MultiSelectDialog rows are clickable TextViews (check icon has no a11y name).
   * @returns {Promise<Array<{name: string, label: string, el: WebdriverIO.Element}>>}
   */
  async readVisibleProviderRows() {
    const rows = [];
    const skip = new Set([
      'OK',
      'Aceptar',
      'Cancelar',
      'Proveedores',
      'Providers',
      'Search Providers',
      'Cancel',
      'Seleccionar todo',
      'Select all',
      'Seleccionar ninguno',
      'Select none',
      'Ninguno',
      'Todos',
    ]);
    const texts = await $$('android.widget.TextView');
    for (const el of texts) {
      const label = ((await el.getText().catch(() => '')) || '').trim();
      if (!label || skip.has(label)) continue;
      const name = label
        .replace(/^[\p{Emoji_Presentation}\p{Extended_Pictographic}\uFE0F\s]+/u, '')
        .trim();
      if (name.length < 2 || name.length > 60) continue;
      rows.push({ name, label, el });
    }
    return rows;
  }

  async scrollDialog(direction) {
    await driver.execute('mobile: scrollGesture', {
      left: 80,
      top: 520,
      width: 520,
      height: 1100,
      direction,
      percent: 0.85,
    });
    await driver.pause(280);
  }

  /**
   * Clicks a provider row by exact normalized name (never textContains — "Nova" ≠ "Novel*").
   * @param {string} name
   */
  async clickProviderRowByName(name) {
    const rows = await this.readVisibleProviderRows();
    const row = rows.find((r) => r.name === name);
    if (!row) return false;
    await row.el.click();
    return true;
  }

  /**
   * Scrolls the multi-select LazyColumn until a provider name is visible.
   * @param {string} name
   */
  async scrollProviderIntoViewExact(name) {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const sel =
      `android=new UiScrollable(new UiSelector().scrollable(true))` +
      `.scrollIntoView(new UiSelector().textMatches("^.*${escaped}$"))`;
    const el = $(sel);
    if (await el.isExisting().catch(() => false)) return true;

    for (let i = 0; i < 24; i++) {
      const rows = await this.readVisibleProviderRows();
      if (rows.some((r) => r.name === name)) return true;
      await this.scrollDialog(i < 12 ? 'down' : 'up');
    }
    return false;
  }

  /**
   * Clicks Select none / Select all when the multi-select bulk actions are visible.
   * @param {'none'|'all'} mode
   */
  async clickProvidersBulkAction(mode) {
    const labels =
      mode === 'none'
        ? ['Seleccionar ninguno', 'Select none', 'Ninguno']
        : ['Seleccionar todo', 'Select all', 'Todos'];
    for (const label of labels) {
      const btn = this.byText(label);
      if (await btn.isDisplayed().catch(() => false)) {
        await btn.click();
        await driver.pause(350);
        return true;
      }
    }
    return false;
  }

  /**
   * Leaves only the given provider names selected.
   * Prefers bulk "Select none" then enables wanted (fast). Falls back to targeted toggles.
   * @param {string[]} names
   */
  async selectOnlyProviders(names) {
    await this.openProvidersDialog();
    const wanted = [...new Set(names)];
    const enabled = new Set();

    const cleared = await this.clickProvidersBulkAction('none');
    if (!cleared) {
      const clearedNames = new Set();
      for (let i = 0; i < 6; i++) {
        const rows = await this.readVisibleProviderRows();
        for (const row of rows) {
          if (wanted.includes(row.name) || clearedNames.has(row.name)) continue;
          if (await this.clickProviderRowByName(row.name)) {
            clearedNames.add(row.name);
            await driver.pause(60);
          }
        }
        await this.scrollDialog('down');
      }
    }

    for (const name of wanted) {
      await this.scrollProviderIntoViewExact(name);
      if (await this.clickProviderRowByName(name)) {
        enabled.add(name);
        await driver.pause(150);
      }
    }

    await this.confirmProvidersDialog();
    return { enabled: [...enabled], clearedCount: cleared ? -1 : 0 };
  }

  /**
   * Re-opens dialog and returns whether each wanted name is still findable/toggleable.
   * @param {string[]} names
   */
  async assertProvidersVisibleInDialog(names) {
    await this.openProvidersDialog();
    const found = new Set();
    for (const direction of ['down', 'up', 'down']) {
      for (let i = 0; i < 12; i++) {
        const rows = await this.readVisibleProviderRows();
        for (const row of rows) {
          const match = names.find(
            (n) => row.name === n || row.name.endsWith(n) || row.name.includes(n),
          );
          if (match) found.add(match);
        }
        await this.scrollDialog(direction);
      }
    }
    await this.confirmProvidersDialog();
    return {
      found: [...found],
      missing: names.filter((n) => !found.has(n)),
    };
  }

  /**
   * Counts TextViews that look like provider tiles on the Buscar grid (excludes chrome).
   * @returns {Promise<number>}
   */
  async countVisibleProviderTiles() {
    const skip = /^(buscar|search|proveedores|providers|recientes|recent|popular|todos|all|sin resultados|no results)$/i;
    const texts = await $$('android.widget.TextView');
    let count = 0;
    for (const el of texts) {
      const label = ((await el.getText().catch(() => '')) || '').trim();
      if (!label || label.length < 2 || label.length > 48) continue;
      if (skip.test(label)) continue;
      if (/^\d+$/.test(label)) continue;
      if (/\(\d+\)$/.test(label)) continue;
      count += 1;
    }
    return count;
  }

  /**
   * Opens a provider tile from the Buscar grid by visible name (scrolls a few times).
   * @param {string} name
   * @returns {Promise<boolean>}
   */
  async openProviderTileByName(name) {
    const needle = name.trim().toLowerCase();
    for (let pass = 0; pass < 8; pass++) {
      const texts = await $$('android.widget.TextView');
      for (const el of texts) {
        const label = ((await el.getText().catch(() => '')) || '').trim();
        if (!label) continue;
        const plain = label.toLowerCase();
        if (plain === needle || plain.includes(needle)) {
          await el.click();
          await driver.pause(1500);
          return true;
        }
      }
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 500,
        height: 1100,
        direction: 'down',
        percent: 0.7,
      });
      await driver.pause(400);
    }
    return false;
  }

  /**
   * True when MainPage / provider browse UI is showing (back affordance or result grid).
   */
  async isProviderMainPageOpen() {
    const back = await $(
      'android=new UiSelector().descriptionMatches("(?i)(Navigate up|Atrás|Back|Volver)")',
    );
    if (await back.isDisplayed().catch(() => false)) return true;
    // Main page often keeps a toolbar title matching the provider; search field may be gone.
    const field = await this.searchField.isDisplayed().catch(() => false);
    const providers = await this.providersButton.isDisplayed().catch(() => false);
    return !field && !providers;
  }

  /**
   * Types into the search field without submitting.
   * @param {string} query
   */
  async typeQuery(query) {
    const field = await $('android.widget.EditText');
    await field.waitForDisplayed({ timeout: 10000 });
    await field.click();
    await driver.pause(300);
    await field.clearValue().catch(() => {});
    await field.setValue(query);
    await driver.pause(300);
  }

  /**
   * @param {string} query
   * @param {number} timeoutMs
   */
  async search(query, timeoutMs = 180000) {
    await this.typeQuery(query);
    const field = await $('android.widget.EditText');
    await field.addValue('\uE007').catch(() => {});
    await driver.pause(400);
    await driver.execute('mobile: performEditorAction', { action: 'search' }).catch(() => {});
    await driver.pause(400);
    await driver.execute('mobile: shell', { command: 'input', args: ['keyevent', '66'] });
    await driver.pause(1500);
    await this.waitSearchIdle(timeoutMs);
  }

  async countResultChipLabels() {
    let count = 0;
    const texts = await $$('android.widget.TextView');
    for (const el of texts) {
      const t = ((await el.getText().catch(() => '')) || '').trim();
      if (/^.+\s*\((\d+)\)$/.test(t) && !t.startsWith('Todos') && !t.startsWith('All')) count += 1;
    }
    return count;
  }

  async waitSearchIdle(timeoutMs = 180000) {
    const started = Date.now();
    let lastCount = -1;
    let stableRounds = 0;
    let loadingSeen = false;
    let maxChips = 0;

    while (Date.now() - started < timeoutMs) {
      const chipCount = await this.countResultChipLabels();
      const noResults = await this.noResults.isDisplayed().catch(() => false);
      const providersVisible = await this.providersButton.isDisplayed().catch(() => false);
      maxChips = Math.max(maxChips, chipCount);
      if (!providersVisible) loadingSeen = true;

      if (chipCount !== lastCount) {
        stableRounds = 0;
        lastCount = chipCount;
      } else {
        stableRounds += 1;
      }

      if (loadingSeen && providersVisible && stableRounds >= 3) return;
      if (providersVisible && maxChips > 0 && stableRounds >= 4) return;
      if (noResults && providersVisible && stableRounds >= 2) return;
      if (!loadingSeen && maxChips === 0 && providersVisible && Date.now() - started > 25000) {
        if (noResults) return;
        throw new Error('Search did not start (no loading / no chips after 25s)');
      }
      await driver.pause(1000);
    }

    if (maxChips > 0 || (await this.noResults.isDisplayed().catch(() => false))) return;
    throw new Error(`Search did not finish within ${timeoutMs}ms (chips=${lastCount})`);
  }

  async collectProviderHits() {
    const hits = new Map();
    const { width, height } = await driver.getWindowSize();
    const y = Math.floor(height * 0.22);

    for (let pass = 0; pass < 12; pass++) {
      const texts = await $$('android.widget.TextView');
      for (const el of texts) {
        const t = ((await el.getText().catch(() => '')) || '').trim();
        const m = t.match(/^(.+?)\s*\((\d+)\)$/);
        if (!m) continue;
        const name = m[1].trim();
        const count = Number(m[2]);
        if (name === 'Todos' || name === 'All' || Number.isNaN(count)) continue;
        hits.set(name, Math.max(hits.get(name) || 0, count));
      }
      await driver.execute('mobile: swipeGesture', {
        left: Math.floor(width * 0.15),
        top: y - 40,
        width: Math.floor(width * 0.7),
        height: 80,
        direction: 'left',
        percent: 0.85,
      });
      await driver.pause(250);
    }
    return hits;
  }
}

module.exports = new SearchPage();
