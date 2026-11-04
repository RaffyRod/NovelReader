const BasePage = require('./base.page');

class SearchPage extends BasePage {
  get tabSearch() {
    return this.byId(`${BasePage.appId()}:id/navigation_search`);
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
    await this.ensureMainChrome(15000).catch(() => {});
    await this.openTab('Buscar');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.ensureMainChrome(15000).catch(() => {});
    let tabOk = await this.tabSearch
      .waitForDisplayed({ timeout: 12000 })
      .then(() => true)
      .catch(() => false);
    if (!tabOk) {
      // Dense suites (Fuentes / host-health / catalog) can leave chrome hidden.
      await this.recoverMainApp();
      await this.ensureMainChrome(20000);
      await this.openTab('Buscar');
    }
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
   * Provider-grid chrome / section labels to ignore when counting tiles.
   * @param {string} label
   */
  isProviderTileNoise(label) {
    const skip =
      /^(buscar|search|proveedores|providers|recientes|recent|popular|todos|all|sin resultados|no results|biblioteca|library|historial|history|ajustes|settings|secciones|sections)$/i;
    if (!label || label.length < 2 || label.length > 48) return true;
    if (skip.test(label)) return true;
    if (/^\d+$/.test(label)) return true;
    if (/\(\d+\)$/.test(label)) return true;
    return false;
  }

  /**
   * Visible provider tile labels on the Buscar grid.
   * @returns {Promise<string[]>}
   */
  async listVisibleProviderTiles() {
    const texts = await $$('android.widget.TextView');
    const labels = [];
    for (const el of texts) {
      if (!(await el.isDisplayed().catch(() => false))) continue;
      const label = ((await el.getText().catch(() => '')) || '').trim();
      if (this.isProviderTileNoise(label)) continue;
      labels.push(label);
    }
    return labels;
  }

  /**
   * Counts TextViews that look like provider tiles on the Buscar grid (excludes chrome).
   * @returns {Promise<number>}
   */
  async countVisibleProviderTiles() {
    return (await this.listVisibleProviderTiles()).length;
  }

  /**
   * Scrolls the Buscar home past Populares until the Proveedores grid is on screen.
   */
  async revealProvidersGrid() {
    for (let i = 0; i < 6; i++) {
      const header =
        (await this.byTextContains('Proveedores').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Providers').isDisplayed().catch(() => false));
      const known =
        (await this.byTextContains('AllNovel').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('FreeWebNovel').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Wattpad').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Annas Archive').isDisplayed().catch(() => false));
      if (header || known) return;
      await driver
        .execute('mobile: scrollGesture', {
          left: 80,
          top: 500,
          width: 600,
          height: 1100,
          direction: 'down',
          percent: 0.7,
        })
        .catch(() => {});
      await driver.pause(350);
    }
  }

  /**
   * Known catalog tile names used for live-filter assertions.
   * @returns {string[]}
   */
  preferredProviderNames() {
    return [
      'AllNovel',
      'FreeWebNovel',
      'DevilNovels',
      'Annas Archive',
      'Wattpad',
      'FanMTL',
      'Bright Novel',
      'Galaxy Novels',
    ];
  }

  /**
   * First visible provider tile name suitable for live filter tests.
   * Prefers known catalog tiles over Populares novel titles.
   * @returns {Promise<string|null>}
   */
  async firstVisibleProviderTileName() {
    await this.revealProvidersGrid();
    for (const name of this.preferredProviderNames()) {
      if (await this.byTextContains(name).isDisplayed().catch(() => false)) return name;
    }
    return null;
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
   * Finds a visible provider tile TextView on Buscar (prefers known built-ins).
   * @returns {Promise<WebdriverIO.Element|null>}
   */
  async findProviderTileTarget() {
    const preferred = ['Wattpad', 'SkyLight', 'Royal', 'Wuxia', 'Novel'];
    for (const name of preferred) {
      const el = this.byTextContains(name);
      if (await el.isDisplayed().catch(() => false)) return el;
    }
    const skip =
      /^(buscar|search|proveedores|providers|recientes|recent|popular|todos|all|sin resultados|no results)$/i;
    const texts = await $$('android.widget.TextView');
    for (const el of texts) {
      if (!(await el.isDisplayed().catch(() => false))) continue;
      const t = ((await el.getText().catch(() => '')) || '').trim();
      if (!t || t.length < 3 || t.length > 40) continue;
      if (skip.test(t)) continue;
      if (/^\d+$/.test(t) || /\(\d+\)$/.test(t)) continue;
      return el;
    }
    return null;
  }

  /**
   * Long-press a Buscar provider tile to open the host-health modal.
   * @returns {Promise<boolean>}
   */
  async openHostHealthFromSearchTile() {
    await this.ensureMainChrome(20000).catch(() => {});
    await this.open();
    await this.expectLoaded();
    // Ensure provider grid is showing (not novel results).
    const field = this.searchField;
    if (await field.isDisplayed().catch(() => false)) {
      await field.click().catch(() => {});
      await field.clearValue().catch(() => {});
      await driver.pause(400);
    }
    let target = await this.findProviderTileTarget();
    if (!target) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 500,
        height: 1100,
        direction: 'down',
        percent: 0.6,
      }).catch(() => {});
      await driver.pause(400);
      target = await this.findProviderTileTarget();
    }
    if (!target) return false;
    const loc = await target.getLocation();
    const size = await target.getSize();
    const x = Math.round(loc.x + size.width / 2);
    const y = Math.round(loc.y + size.height / 2);
    await driver.execute('mobile: longClickGesture', { x, y, duration: 1100 });
    await driver.pause(700);
    const settingsPage = require('./settings.page');
    return settingsPage.isHostHealthSheetOpen();
  }

  /**
   * True when MainPage / provider browse UI is showing (back affordance, filter row, or no home tune).
   * Annas Archive keeps an EditText ("Búsqueda …") on MainPage — do not treat that as home.
   */
  async isProviderMainPageOpen() {
    const back = await $(
      'android=new UiSelector().descriptionMatches("(?i)(Navigate up|Atrás|Back|Volver)")',
    );
    if (await back.isDisplayed().catch(() => false)) return true;
    const filterHint =
      (await $('android=new UiSelector().textContains("Z-Library")').isDisplayed().catch(() => false)) ||
      (await $('android=new UiSelector().textContains("Fiction")').isDisplayed().catch(() => false)) ||
      (await $('android=new UiSelector().textContains("English")').isDisplayed().catch(() => false));
    if (filterHint) return true;
    const providers = await this.providersButton.isDisplayed().catch(() => false);
    if (providers) return false;
    const busqueda = await $(
      'android=new UiSelector().textContains("Búsqueda")',
    ).isDisplayed().catch(() => false);
    if (busqueda) return true;
    // Loading MainPage: spinner with no Buscar home chrome.
    const progress = await $(
      'android=new UiSelector().className("android.widget.ProgressBar")',
    )
      .isDisplayed()
      .catch(() => false);
    if (progress) return true;
    const field = await this.searchField.isDisplayed().catch(() => false);
    const tab = await this.tabSearch.isDisplayed().catch(() => false);
    // Left the provider grid (no field / no tab) → treat as provider page / intermediate.
    return !field && !tab;
  }

  /**
   * After opening a provider tile: wait until spinner settles, then require cards or an error/empty label.
   * Soft-passes only when host is clearly blocked (DDoS / Cloudflare message).
   * @param {number} timeoutMs
   * @returns {Promise<{ok:boolean, blocked:boolean, hasCards:boolean, emptyOrError:boolean}>}
   */
  async waitProviderMainPageSettled(timeoutMs = 90000) {
    const started = Date.now();
    let hasCards = false;
    let emptyOrError = false;
    let blocked = false;
    while (Date.now() - started < timeoutMs) {
      const progress = await $('android=new UiSelector().className("android.widget.ProgressBar")')
        .isDisplayed()
        .catch(() => false);
      const noResults =
        (await this.noResults.isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("blocked")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("no books")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("Sin resultados")').isDisplayed().catch(() => false));
      const ddos =
        (await $('android=new UiSelector().textContains("DDoS")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("Cloudflare")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("Checking your browser")').isDisplayed().catch(() => false));
      // Rough card signal: several TextViews below the filter row once ProgressBar is gone.
      const texts = await $$('android.widget.TextView');
      let titleish = 0;
      for (const el of texts.slice(0, 40)) {
        const t = ((await el.getText().catch(() => '')) || '').trim();
        if (t.length >= 8 && !/^(English|Spanish|Fiction|Z-Library|Buscar|Search)/i.test(t)) {
          titleish += 1;
        }
      }
      hasCards = titleish >= 3;
      emptyOrError = noResults;
      blocked = ddos || /blocked|DDoS|Cloudflare/i.test(
        (await $('android=new UiSelector().textContains("Annas")').getText().catch(() => '')) || '',
      );
      if (!progress && (hasCards || emptyOrError || blocked)) {
        return { ok: hasCards || emptyOrError || blocked, blocked, hasCards, emptyOrError };
      }
      await driver.pause(1500);
    }
    return { ok: hasCards || emptyOrError || blocked, blocked, hasCards, emptyOrError };
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

  /**
   * Legacy chip labels "Name (N)" — kept for older builds / filter rows.
   */
  async countResultChipLabels() {
    const chips = await $$(
      'android=new UiSelector().className("android.widget.TextView").textMatches(".+\\\\s*\\\\([0-9]+\\\\)$")',
    );
    let count = 0;
    for (const el of chips) {
      const t = ((await el.getText().catch(() => '')) || '').trim();
      if (/^.+\s*\((\d+)\)$/.test(t) && !t.startsWith('Todos') && !t.startsWith('All')) count += 1;
    }
    return count;
  }

  /**
   * Current SearchResults UI: query mode shows back-to-search + provider SearchRow titles (no chips).
   * @returns {Promise<boolean>}
   */
  async isSearchQueryOpen() {
    const backEs = await this.byA11y('Volver a buscar').isDisplayed().catch(() => false);
    const backEn = await this.byA11y('Back to search').isDisplayed().catch(() => false);
    return backEs || backEn;
  }

  /**
   * Chrome / nav labels that are not novel cover contentDescriptions.
   * @param {string} label
   */
  isSearchChromeDescription(label) {
    if (!label || label.length < 4 || label.length > 140) return true;
    return /^(Buscar|Search|Proveedores|Providers|Biblioteca|Library|Historial|History|Ajustes|Settings|Secciones|Sections|Volver|Back|Navigate|Ordenar|Import|EPUB|PDF)$/i.test(
      label.trim(),
    );
  }

  /**
   * Coil covers expose the novel title as contentDescription (Compose).
   * @returns {Promise<WebdriverIO.Element[]>}
   */
  async findNovelCoverElements() {
    const nodes = await $$(
      'android=new UiSelector().descriptionMatches(".{5,120}")',
    );
    const covers = [];
    for (const el of nodes) {
      const desc = ((await el.getAttribute('contentDescription').catch(() => '')) || '').trim();
      if (this.isSearchChromeDescription(desc)) continue;
      if (/^\d+$/.test(desc)) continue;
      covers.push(el);
    }
    return covers;
  }

  /**
   * True when SearchRow results (covers via a11y desc / provider headers) are on screen.
   * @returns {Promise<boolean>}
   */
  async hasSearchResultRows() {
    if (await this.noResults.isDisplayed().catch(() => false)) return false;
    const covers = await this.findNovelCoverElements();
    if (covers.length >= 1) return true;
    const chipCount = await this.countResultChipLabels();
    return chipCount > 0;
  }

  async waitSearchIdle(timeoutMs = 180000) {
    const started = Date.now();
    let stableRounds = 0;
    let loadingSeen = false;
    let resultsSeen = false;
    let providersEver = false;
    let lastProgress = true;

    while (Date.now() - started < timeoutMs) {
      const progress = await $(
        'android=new UiSelector().className("android.widget.ProgressBar")',
      )
        .isDisplayed()
        .catch(() => false);
      const noResults = await this.noResults.isDisplayed().catch(() => false);
      const providersVisible = await this.providersButton.isDisplayed().catch(() => false);
      const queryOpen = await this.isSearchQueryOpen();
      const hasRows = await this.hasSearchResultRows();
      const chipCount = await this.countResultChipLabels();

      if (providersVisible) providersEver = true;
      if (progress || (!providersVisible && queryOpen)) loadingSeen = true;
      if (hasRows || chipCount > 0) resultsSeen = true;

      if (progress !== lastProgress) {
        stableRounds = 0;
        lastProgress = progress;
      } else {
        stableRounds += 1;
      }

      // Modern UI: query open, spinner gone, covers (a11y) or empty state.
      if (queryOpen && !progress && stableRounds >= 2 && (hasRows || noResults || resultsSeen)) {
        return;
      }
      // Spinner gone after loading even if a11y covers are slow to register.
      if (queryOpen && loadingSeen && !progress && stableRounds >= 4) return;
      if (chipCount > 0 && !progress && stableRounds >= 2) return;
      if (loadingSeen && providersVisible && !progress && stableRounds >= 3) return;
      if (noResults && stableRounds >= 2) return;

      if (!loadingSeen && !queryOpen && providersVisible && Date.now() - started > 25000) {
        throw new Error('Search did not start (still on provider grid after 25s)');
      }
      if (loadingSeen && !resultsSeen && !noResults && Date.now() - started > 70000) {
        throw new Error('Search loading >70s with no result covers');
      }
      if (!loadingSeen && !providersEver && !queryOpen && Date.now() - started > 20000) {
        const field = await this.searchField.isDisplayed().catch(() => false);
        if (!field) {
          await this.ensureMainChrome(8000).catch(() => {});
          throw new Error('Search left Buscar chrome without starting');
        }
      }
      await driver.pause(1000);
    }

    if (resultsSeen || (await this.noResults.isDisplayed().catch(() => false))) return;
    if (await this.hasSearchResultRows()) return;
    throw new Error(`Search did not finish within ${timeoutMs}ms`);
  }

  async collectProviderHits() {
    const hits = new Map();
    const { width, height } = await driver.getWindowSize();
    const y = Math.floor(height * 0.22);

    for (let pass = 0; pass < 4; pass++) {
      const chips = await $$(
        'android=new UiSelector().className("android.widget.TextView").textMatches(".+\\\\s*\\\\([0-9]+\\\\)$")',
      );
      for (const el of chips) {
        const t = ((await el.getText().catch(() => '')) || '').trim();
        const m = t.match(/^(.+?)\s*\((\d+)\)$/);
        if (!m) continue;
        const name = m[1].trim();
        const count = Number(m[2]);
        if (name === 'Todos' || name === 'All' || Number.isNaN(count)) continue;
        hits.set(name, Math.max(hits.get(name) || 0, count));
      }
      if (hits.size > 0 && pass >= 1) break;
      await driver.execute('mobile: swipeGesture', {
        left: Math.floor(width * 0.15),
        top: y - 40,
        width: Math.floor(width * 0.7),
        height: 80,
        direction: 'left',
        percent: 0.85,
      }).catch(() => {});
      await driver.pause(200);
    }
    return hits;
  }
}

module.exports = new SearchPage();
