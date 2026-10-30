const BasePage = require('./base.page');

class SearchPage extends BasePage {
  get tabSearch() {
    // Selected bottom-nav items often become non-clickable; do not require clickable here.
    return this.byA11y('Buscar');
  }

  get providersButton() {
    return this.byA11y('Proveedores');
  }

  get providersTitle() {
    return this.byTextContains('Proveedores');
  }

  get noResults() {
    return this.byTextContains('Sin resultados');
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
    // Proveedores tune icon is unique to the Search screen.
    await this.providersButton.waitForDisplayed({ timeout: 20000 });
    await expect(this.providersButton).toBeDisplayed();
  }

  async openProvidersDialog() {
    await this.providersButton.waitForDisplayed({ timeout: 15000 });
    await this.providersButton.click();
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
    const skip = new Set(['OK', 'Aceptar', 'Cancelar', 'Proveedores', 'Cancel']);
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
   * Clicks a provider row by visible text (fresh lookup avoids stale Compose nodes).
   * @param {string} name
   */
  async clickProviderRowByName(name) {
    const exact = $(`android=new UiSelector().text("${name}")`);
    if (await exact.isExisting().catch(() => false)) {
      await exact.click();
      return true;
    }
    const contains = $(`android=new UiSelector().textContains("${name}")`);
    if (await contains.isExisting().catch(() => false)) {
      await contains.click();
      return true;
    }
    return false;
  }

  /**
   * Leaves only the given provider names selected.
   * Default dialog starts with all selected → clear each once, then re-enable wanted.
   * @param {string[]} names
   */
  async selectOnlyProviders(names) {
    await this.openProvidersDialog();
    const wanted = [...new Set(names)];
    const cleared = new Set();

    // Pass 1: clear default selection (click each distinct visible name once)
    for (const direction of ['down', 'up']) {
      for (let i = 0; i < 10; i++) {
        const rows = await this.readVisibleProviderRows();
        for (const row of rows) {
          if (cleared.has(row.name)) continue;
          if (await this.clickProviderRowByName(row.name)) {
            cleared.add(row.name);
            await driver.pause(120);
          }
        }
        await this.scrollDialog(direction);
      }
    }

    // Pass 2: enable only wanted (fresh find each time)
    const enabled = new Set();
    for (const name of wanted) {
      for (let attempt = 0; attempt < 12; attempt++) {
        if (await this.clickProviderRowByName(name)) {
          enabled.add(name);
          await driver.pause(150);
          break;
        }
        await this.scrollDialog(attempt % 2 === 0 ? 'down' : 'up');
      }
    }

    await this.confirmProvidersDialog();
    return { enabled: [...enabled], clearedCount: cleared.size };
  }

  /**
   * Re-opens dialog and returns whether each wanted name is still findable/toggleable.
   * Verifies persistence by confirming OK without changes after a second open.
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
      if (/^.+\s*\((\d+)\)$/.test(t) && !t.startsWith('Todos')) count += 1;
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
      if (noResults && providersVisible && stableRounds >= 3) return;
      if (!loadingSeen && maxChips === 0 && Date.now() - started > 45000) {
        throw new Error('Search did not start (no loading / no chips after 45s)');
      }
      await driver.pause(1200);
    }

    if (maxChips > 0) return;
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
        if (name === 'Todos' || Number.isNaN(count)) continue;
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
