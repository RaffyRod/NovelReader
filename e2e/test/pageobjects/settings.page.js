const BasePage = require('./base.page');

class SettingsPage extends BasePage {
  get tabSettings() {
    const pkg = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
    return this.byId(`${pkg}:id/navigation_settings`);
  }

  get tutorialGroup() {
    return this.byText('Tutorial');
  }

  /** Preference title includes "(TTS)" — avoids matching other "Lectura en voz alta" strings. */
  get tutorialTts() {
    return this.byTextContains('(TTS)');
  }

  get tutorialDownload() {
    // Subtitle of the download tutorial row (unique vs generic "Descargas" elsewhere).
    return this.byTextContains('Cómo descargar novelas');
  }

  get tutorialProviders() {
    return this.byTextContains('Cómo agregar un Blog');
  }

  get pluginGroup() {
    return this.byTextContains('Plugins (LNReader)');
  }

  get pluginHub() {
    return this.byTextContains('Administrar plugins');
  }

  get pluginAddRepo() {
    return this.byTextContains('Añadir repositorio');
  }

  get pluginHubTitle() {
    return this.byTextContains('Plugins instalados');
  }

  get pluginAddRepoTitle() {
    return this.byTextContains('Añadir repositorio');
  }

  /** Unified Search sources preference (Fuentes de Buscar / Search sources). */
  get fuentesDeBuscar() {
    return this.byTextContains('Fuentes de Buscar');
  }

  get searchSources() {
    return this.byTextContains('Search sources');
  }

  get uiTheme() {
    return this.byTextContains('Fire');
  }

  async open() {
    await this.openTab('Ajustes');
  }

  async expectLoaded() {
    await this.waitReady();
    await this.tabSettings.waitForDisplayed({ timeout: 15000 });
    await expect(this.tabSettings).toBeDisplayed();
  }

  /**
   * True when the Tutorial preference group (under Información) is on screen.
   * Do not treat bare "Descargas" as success — that label appears earlier in Settings.
   */
  async tutorialsVisible() {
    return (
      (await this.tutorialGroup.isDisplayed().catch(() => false)) ||
      (await this.tutorialTts.isDisplayed().catch(() => false)) ||
      (await this.tutorialDownload.isDisplayed().catch(() => false)) ||
      (await this.tutorialProviders.isDisplayed().catch(() => false))
    );
  }

  async scrollToTutorials() {
    if (await this.tutorialsVisible()) return true;
    await driver.setTimeout({ implicit: 0 }).catch(() => {});
    for (let i = 0; i < 8; i++) {
      await driver
        .execute('mobile: scrollGesture', {
          left: 80,
          top: 400,
          width: 600,
          height: 1200,
          direction: 'down',
          percent: 0.85,
        })
        .catch(() => {});
      await driver.pause(150);
      if (await this.tutorialsVisible()) return true;
    }
    return this.tutorialsVisible();
  }

  async openTutorialTts() {
    if (!(await this.scrollToTutorials())) return false;

    const candidates = [
      this.byText('Lectura en voz alta (TTS)'),
      this.byText('Text to speech (TTS)'),
      this.tutorialTts,
    ];
    for (const item of candidates) {
      if (await item.isDisplayed().catch(() => false)) {
        await item.click();
        await driver.pause(800);
        return true;
      }
    }
    return false;
  }

  get allowSpecialCharsLabels() {
    return ['Permitir caracteres especiales', 'Allow special characters'];
  }

  /**
   * Finds the special-chars preference row (ES/EN), scrolling from the top of Settings.
   * Previous suites may leave the settings list scrolled past this item.
   */
  async findAllowSpecialSearchCharsTitle() {
    for (const label of this.allowSpecialCharsLabels) {
      const el = this.byTextContains(label);
      if (await el.isDisplayed().catch(() => false)) return el;
    }

    // Preference sits near the top of General — short rewind (long loops hung special-chars 3m+).
    for (let i = 0; i < 3; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 400,
        width: 600,
        height: 1200,
        direction: 'up',
        percent: 0.9,
      }).catch(() => {});
      await driver.pause(120);
    }

    for (const label of this.allowSpecialCharsLabels) {
      await this.scrollToText(label).catch(() => {});
      const el = this.byTextContains(label);
      if (await el.isDisplayed().catch(() => false)) return el;
    }

    return this.byTextContains('Permitir caracteres especiales');
  }

  /**
   * Scrolls to the special-chars search preference and toggles it via row click
   * (Compose Switch uses onPreferenceClick, not a native Switch click).
   * @param {boolean} enabled desired state after the call
   */
  async setAllowSpecialSearchChars(enabled) {
    await this.open();
    await this.expectLoaded();
    const title = await this.findAllowSpecialSearchCharsTitle();
    await title.waitForDisplayed({ timeout: 8000 });

    const isOn = await this.isAllowSpecialSearchCharsOn(title);
    if (isOn !== enabled) {
      await title.click();
      await driver.pause(500);
    }
  }

  /**
   * True when an accessibility checked attribute means ON.
   * @param {unknown} value
   */
  isCheckedValue(value) {
    return value === true || value === 'true' || value === 1 || value === '1';
  }

  /**
   * Finds the Switch / checkable control nearest to a preference title row.
   * Compose Material3 Switch is often exposed as a checkable View, not android.widget.Switch.
   * @param {WebdriverIO.Element} titleEl
   * @returns {Promise<boolean>}
   */
  async isPreferenceSwitchOnNear(titleEl) {
    const titleBounds = await titleEl.getLocation().catch(() => null);
    const titleText = ((await titleEl.getText().catch(() => '')) || '').trim();
    const pools = await Promise.all([
      $$('android.widget.Switch'),
      $$('android=new UiSelector().checkable(true)'),
      $$('android=new UiSelector().className("android.view.View").checkable(true)'),
    ]);
    const seen = new Set();
    const candidates = [];
    for (const pool of pools) {
      for (const el of pool) {
        const id = el.elementId || el.ELEMENT;
        if (!id || seen.has(id)) continue;
        seen.add(id);
        candidates.push(el);
      }
    }

    let best = null;
    let bestDist = Number.POSITIVE_INFINITY;
    for (const sw of candidates) {
      if (!(await sw.isDisplayed().catch(() => false))) continue;
      const checkable = await sw.getAttribute('checkable').catch(() => null);
      if (checkable === 'false' || checkable === false) continue;
      if (!titleBounds) {
        best = sw;
        continue;
      }
      const loc = await sw.getLocation().catch(() => null);
      if (!loc) continue;
      const dist = Math.abs(loc.y - titleBounds.y);
      // Prefer controls on the same preference row (density-tolerant).
      if (dist < bestDist) {
        bestDist = dist;
        best = sw;
      }
    }
    if (best && bestDist <= 280) {
      const checked = await best.getAttribute('checked').catch(() => null);
      if (checked != null && String(checked).length > 0) {
        return this.isCheckedValue(checked);
      }
      const selected = await best.getAttribute('selected').catch(() => null);
      if (selected != null && String(selected).length > 0) {
        return this.isCheckedValue(selected);
      }
      const stateDesc = await best.getAttribute('stateDescription').catch(() => null);
      if (stateDesc && /on|activado|encendido|marcado/i.test(String(stateDesc))) return true;
      if (stateDesc && /off|desactivado|apagado|desmarcado/i.test(String(stateDesc))) return false;
    }

    // Hierarchy fallback: Compose sometimes omits Switch class — parse page source near title.
    if (!titleText) return false;
    const source = await driver.getPageSource().catch(() => '');
    if (!source) return false;
    const idx = source.indexOf(titleText);
    if (idx < 0) return false;
    const window = source.slice(Math.max(0, idx - 200), idx + 3500);
    const attrs = [...window.matchAll(/\bchecked="(true|false)"/gi)];
    if (attrs.length === 0) return false;
    // First checked attr after the title text is the trailing Switch on that row.
    return attrs[0][1].toLowerCase() === 'true';
  }

  /**
   * Reads the checked state of the switch associated with the special-chars preference.
   * Falls back to false when the control is not exposed as a native Switch.
   * @param {WebdriverIO.Element} [titleEl]
   */
  async isAllowSpecialSearchCharsOn(titleEl) {
    const title = titleEl || (await this.findAllowSpecialSearchCharsTitle());
    return this.isPreferenceSwitchOnNear(title);
  }

  /**
   * Enables English providers and selects every non-English language.
   * Does not touch the providers multi-select (defaults to all).
   */
  async enableAllSearchProviders() {
    await this.open();
    await this.expectLoaded();

    await this.scrollToText('Mostrar proveedores en inglés');
    await driver.pause(500);
    const englishRow = this.byText('Mostrar proveedores en inglés');
    await englishRow.waitForDisplayed({ timeout: 15000 });

    // Click the switch that belongs to the English preference (last visible switch near row).
    const switches = await $$('android.widget.Switch');
    for (const sw of switches) {
      const checked = await sw.getAttribute('checked');
      if (checked !== 'true') {
        await sw.click();
        await driver.pause(400);
        break;
      }
    }
  }

  /**
   * @returns {Promise<boolean>}
   */
  async pluginsSectionVisible() {
    return (
      (await this.pluginGroup.isDisplayed().catch(() => false)) ||
      (await this.pluginHub.isDisplayed().catch(() => false)) ||
      (await this.pluginAddRepo.isDisplayed().catch(() => false)) ||
      (await this.fuentesDeBuscar.isDisplayed().catch(() => false)) ||
      (await this.searchSources.isDisplayed().catch(() => false))
    );
  }

  /**
   * Scrolls Settings until the Plugins (LNReader) / Fuentes group is visible.
   * One UiScrollable pass per marker (no scrollToText gesture storm — that hung CI locally).
   * @returns {Promise<boolean>}
   */
  async scrollToPlugins() {
    if (await this.pluginsSectionVisible()) return true;

    const markers = [
      'Fuentes de Buscar',
      'Administrar plugins',
      'Plugins (LNReader)',
      'Search sources',
      'Manage plugins',
    ];
    for (const marker of markers) {
      await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("${marker}"))`,
      ).catch(() => {});
      await driver.pause(200);
      if (await this.pluginsSectionVisible()) return true;
    }

    for (let i = 0; i < 6; i++) {
      await driver
        .execute('mobile: scrollGesture', {
          left: 80,
          top: 500,
          width: 600,
          height: 1200,
          direction: 'down',
          percent: 0.75,
        })
        .catch(() => {});
      await driver.pause(200);
      if (await this.pluginsSectionVisible()) return true;
    }
    return this.pluginsSectionVisible();
  }

  /**
   * Opens the installed-plugins hub dialog from Settings.
   * @returns {Promise<boolean>}
   */
  async openPluginHub() {
    await this.open();
    await this.expectLoaded();
    if (!(await this.scrollToPlugins())) return false;
    const hub = this.pluginHub;
    if (!(await hub.isDisplayed().catch(() => false))) return false;
    await hub.click();
    await driver.pause(900);
    return (
      (await this.pluginHubTitle.isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Aún no hay plugins').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('No plugins yet').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Añadir repositorio').isDisplayed().catch(() => false))
    );
  }

  get fuentesEditBtn() {
    // Footer enters bulk-select mode (was labeled Editar; now Seleccionar).
    return this.byText('Seleccionar');
  }

  get fuentesEditBtnEn() {
    return this.byText('Select');
  }

  get fuentesDeleteSelected() {
    return this.byTextContains('Eliminar seleccionados');
  }

  get fuentesDeleteSelectedEn() {
    return this.byTextContains('Delete selected');
  }

  get fuentesDoneBtn() {
    return this.byText('Hecho');
  }

  get fuentesDoneBtnEn() {
    return this.byText('Done');
  }

  /**
   * Opens unified Fuentes de Buscar / Search sources manager (mock A: switch-only).
   * @returns {Promise<boolean>}
   */
  async openFuentesDeBuscar() {
    await this.open();
    await this.expectLoaded();
    if (!(await this.scrollToPlugins())) return false;
    const rowEs = this.fuentesDeBuscar;
    const rowEn = this.searchSources;
    if (await rowEs.isDisplayed().catch(() => false)) {
      await rowEs.click();
    } else if (await rowEn.isDisplayed().catch(() => false)) {
      await rowEn.click();
    } else {
      return false;
    }
    await driver.pause(900);
    return (
      (await this.byTextContains('Activas').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Active').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Instaladas').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Installed').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Todas').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('All').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Fuentes').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Sources').isDisplayed().catch(() => false))
    );
  }

  /**
   * True when Fuentes is in normal mode (Seleccionar visible, not Eliminar seleccionados).
   * @returns {Promise<boolean>}
   */
  async fuentesIsNormalMode() {
    const edit =
      (await this.fuentesEditBtn.isDisplayed().catch(() => false)) ||
      (await this.fuentesEditBtnEn.isDisplayed().catch(() => false));
    const deleting =
      (await this.fuentesDeleteSelected.isDisplayed().catch(() => false)) ||
      (await this.fuentesDeleteSelectedEn.isDisplayed().catch(() => false));
    return edit && !deleting;
  }

  /**
   * Enters Fuentes edit mode (checkboxes + delete selected).
   * @returns {Promise<boolean>}
   */
  async enterFuentesEditMode() {
    const editEs = this.fuentesEditBtn;
    const editEn = this.fuentesEditBtnEn;
    if (await editEs.isDisplayed().catch(() => false)) {
      await editEs.click();
    } else if (await editEn.isDisplayed().catch(() => false)) {
      await editEn.click();
    } else {
      return false;
    }
    await driver.pause(700);
    return (
      (await this.fuentesDeleteSelected.isDisplayed().catch(() => false)) ||
      (await this.fuentesDeleteSelectedEn.isDisplayed().catch(() => false)) ||
      (await this.fuentesDoneBtn.isDisplayed().catch(() => false)) ||
      (await this.fuentesDoneBtnEn.isDisplayed().catch(() => false))
    );
  }

  /**
   * Fuentes / catalog filter field (hint: name or site).
   */
  get fuentesFilterField() {
    return $('android=new UiSelector().className("android.widget.EditText")');
  }

  /**
   * Types into the Fuentes filter box.
   * @param {string} query
   */
  async typeFuentesFilter(query) {
    const field = this.fuentesFilterField;
    await field.waitForDisplayed({ timeout: 10000 });
    await field.click();
    await field.clearValue().catch(() => {});
    await field.setValue(query);
    await driver.pause(700);
  }

  /**
   * True when any list row TextView contains [needle] (case-insensitive).
   * @param {string} needle
   * @returns {Promise<boolean>}
   */
  async fuentesListContains(needle) {
    const q = needle.trim().toLowerCase();
    if (!q) return false;
    const texts = await $$('android.widget.TextView');
    for (const el of texts) {
      const t = ((await el.getText().catch(() => '')) || '').trim().toLowerCase();
      if (t.includes(q)) return true;
    }
    return false;
  }

  /**
   * Leaves Fuentes edit mode via Hecho / Done.
   * @returns {Promise<boolean>}
   */
  async exitFuentesEditMode() {
    const doneEs = this.fuentesDoneBtn;
    const doneEn = this.fuentesDoneBtnEn;
    if (await doneEs.isDisplayed().catch(() => false)) {
      await doneEs.click();
    } else if (await doneEn.isDisplayed().catch(() => false)) {
      await doneEn.click();
    } else {
      return false;
    }
    await driver.pause(700);
    return this.fuentesIsNormalMode();
  }

  get hostHealthSheet() {
    return this.byA11y('host_health_sheet');
  }

  get hostHealthProgress() {
    return this.byA11y('host_health_progress');
  }

  get hostHealthResult() {
    return this.byA11y('host_health_result');
  }

  get hostHealthConfigured() {
    return this.byA11y('host_health_configured');
  }

  get hostHealthStatus() {
    return this.byA11y('host_health_status');
  }

  get hostHealthLatency() {
    return this.byA11y('host_health_latency');
  }

  get hostHealthRecheck() {
    return this.byA11y('host_health_recheck');
  }

  get hostHealthRecheckEn() {
    return this.byTextContains('Check again');
  }

  get hostHealthRecheckEs() {
    return this.byTextContains('Comprobar de nuevo');
  }

  get hostHealthClose() {
    return this.byA11y('host_health_close');
  }

  get hostHealthCloseEn() {
    return this.byText('Close');
  }

  get hostHealthCloseEs() {
    return this.byText('Cerrar');
  }

  /**
   * Selects the Todas / All tab inside Fuentes when visible.
   */
  async selectFuentesTabAll() {
    const tabAll = this.byTextContains('Todas');
    const tabAllEn = this.byTextContains('All');
    if (await tabAll.isDisplayed().catch(() => false)) {
      await tabAll.click();
      await driver.pause(500);
      return true;
    }
    if (await tabAllEn.isDisplayed().catch(() => false)) {
      await tabAllEn.click();
      await driver.pause(500);
      return true;
    }
    return false;
  }

  /**
   * Finds a provider title TextView in Fuentes suitable for long-press (not tabs/filter).
   * Prefers a known built-in name so CI has a stable target.
   * @returns {Promise<WebdriverIO.Element|null>}
   */
  async findFuentesLongPressTarget() {
    const preferred = ['Wattpad', 'SkyLight', 'Royal', 'Wuxia', 'Novel'];
    for (const name of preferred) {
      const el = this.byTextContains(name);
      if (await el.isDisplayed().catch(() => false)) return el;
    }
    const skip =
      /activas|active|instaladas|installed|todas|all|filtrar|filter|seleccionar|select|cerrar|close|fuentes|sources|buscar|search/i;
    const texts = await $$('android.widget.TextView');
    for (const el of texts) {
      if (!(await el.isDisplayed().catch(() => false))) continue;
      const t = ((await el.getText().catch(() => '')) || '').trim();
      if (t.length < 3 || t.length > 48) continue;
      if (skip.test(t)) continue;
      if (/built-in|proveedor|plugin|http/i.test(t)) continue;
      return el;
    }
    return null;
  }

  /**
   * Long-presses an element via UiAutomator2 gesture.
   * @param {WebdriverIO.Element} el
   * @param {number} [durationMs]
   */
  async longPressElement(el, durationMs = 1100) {
    const loc = await el.getLocation();
    const size = await el.getSize();
    const x = Math.round(loc.x + size.width / 2);
    const y = Math.round(loc.y + size.height / 2);
    await driver.execute('mobile: longClickGesture', { x, y, duration: durationMs });
    await driver.pause(600);
  }

  /**
   * Opens Fuentes, long-presses a row, waits for host-health sheet chrome.
   * @returns {Promise<boolean>}
   */
  async openHostHealthSheetFromFuentes() {
    const opened = await this.openFuentesDeBuscar();
    if (!opened) return false;
    await this.selectFuentesTabAll();
    const target = await this.findFuentesLongPressTarget();
    if (!target) return false;
    await this.longPressElement(target);
    return this.isHostHealthSheetOpen();
  }

  /**
   * True when the host-health sheet (progress or result) is on screen.
   * @returns {Promise<boolean>}
   */
  async isHostHealthSheetOpen() {
    if (await this.hostHealthSheet.isDisplayed().catch(() => false)) return true;
    if (await this.hostHealthProgress.isDisplayed().catch(() => false)) return true;
    if (await this.hostHealthResult.isDisplayed().catch(() => false)) return true;
    const checking =
      (await this.byTextContains('Comprobando que el sitio').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Checking that the site').isDisplayed().catch(() => false));
    const configured =
      (await this.byTextContains('URL configurada').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Configured URL').isDisplayed().catch(() => false));
    const recheck =
      (await this.hostHealthRecheck.isDisplayed().catch(() => false)) ||
      (await this.hostHealthRecheckEn.isDisplayed().catch(() => false));
    return checking || configured || recheck;
  }

  /**
   * Waits until probe finishes (result / latency / status) or timeout.
   * Soft: progress alone still counts as success mid-flight if network is slow.
   * @param {number} [timeoutMs]
   * @returns {Promise<{done: boolean, sawProgress: boolean, statusText: string}>}
   */
  async waitHostHealthResult(timeoutMs = 25000) {
    const deadline = Date.now() + timeoutMs;
    let sawProgress = false;
    let statusText = '';
    while (Date.now() < deadline) {
      if (await this.hostHealthProgress.isDisplayed().catch(() => false)) sawProgress = true;
      if (
        (await this.byTextContains('Comprobando que el sitio').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Checking that the site').isDisplayed().catch(() => false))
      ) {
        sawProgress = true;
      }
      const status = this.hostHealthStatus;
      if (await status.isDisplayed().catch(() => false)) {
        statusText = ((await status.getText().catch(() => '')) || '').trim();
        if (statusText) {
          return { done: true, sawProgress, statusText };
        }
      }
      const latencyVisible =
        (await this.hostHealthLatency.isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Latencia').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Latency').isDisplayed().catch(() => false));
      const resultVisible =
        (await this.hostHealthResult.isDisplayed().catch(() => false)) ||
        (await this.byTextContains('URL final').isDisplayed().catch(() => false)) ||
        (await this.byTextContains('Final URL').isDisplayed().catch(() => false));
      if (latencyVisible || resultVisible) {
        const ok =
          (await this.byTextContains('OK').isDisplayed().catch(() => false)) ||
          (await this.byTextContains('Caído').isDisplayed().catch(() => false)) ||
          (await this.byTextContains('Down').isDisplayed().catch(() => false)) ||
          (await this.byTextContains('Movido').isDisplayed().catch(() => false)) ||
          (await this.byTextContains('Moved').isDisplayed().catch(() => false));
        return { done: ok || latencyVisible, sawProgress, statusText };
      }
      await driver.pause(400);
    }
    return { done: false, sawProgress, statusText };
  }

  /**
   * Taps Recheck / Comprobar de nuevo when visible.
   * @returns {Promise<boolean>}
   */
  async tapHostHealthRecheck() {
    const candidates = [
      this.hostHealthRecheck,
      this.hostHealthRecheckEs,
      this.hostHealthRecheckEn,
    ];
    for (const btn of candidates) {
      if (await btn.isDisplayed().catch(() => false)) {
        await btn.click();
        await driver.pause(400);
        return true;
      }
    }
    return false;
  }

  /**
   * Dismisses the host-health bottom sheet via Cerrar / Close / back.
   */
  async dismissHostHealthSheet() {
    const candidates = [
      this.hostHealthClose,
      this.hostHealthCloseEs,
      this.hostHealthCloseEn,
    ];
    for (const btn of candidates) {
      if (await btn.isDisplayed().catch(() => false)) {
        await btn.click();
        await driver.pause(500);
        return;
      }
    }
    await driver.back().catch(() => {});
    await driver.pause(500);
  }

  /**
   * Opens the add-repository dialog (URL / paste JSON).
   * @returns {Promise<boolean>}
   */
  async openPluginAddRepo() {
    await this.open();
    await this.expectLoaded();
    if (!(await this.scrollToPlugins())) return false;
    // Prefer the dedicated preference row; hub positive button is also "Añadir repositorio".
    const row = this.pluginAddRepo;
    if (await row.isDisplayed().catch(() => false)) {
      await row.click();
      await driver.pause(900);
    } else if (await this.openPluginHub()) {
      const add = this.byTextContains('Añadir repositorio');
      if (await add.isDisplayed().catch(() => false)) {
        await add.click();
        await driver.pause(900);
      }
    }
    return (
      (await this.byTextContains('plugins.min.json').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('pegar JSON').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('paste JSON').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Obtener').isDisplayed().catch(() => false)) ||
      (await this.byTextContains('Fetch').isDisplayed().catch(() => false))
    );
  }

  /**
   * Checks every unchecked box in a multi-select dialog, then confirms.
   */
  async selectAllInDialog() {
    for (let pass = 0; pass < 10; pass++) {
      const boxes = await $$('android.widget.CheckBox');
      let toggled = false;
      for (const box of boxes) {
        if ((await box.getAttribute('checked')) !== 'true') {
          await box.click();
          toggled = true;
          await driver.pause(80);
        }
      }
      await driver.execute('mobile: scrollGesture', {
        left: 100,
        top: 600,
        width: 500,
        height: 1000,
        direction: 'down',
        percent: 0.75,
      });
      await driver.pause(250);
      if (!toggled && pass > 1) break;
    }

    const ok = this.byText('OK');
    const aceptar = this.byText('Aceptar');
    if (await ok.isExisting().catch(() => false)) {
      await ok.click();
    } else if (await aceptar.isExisting().catch(() => false)) {
      await aceptar.click();
    } else {
      await driver.back();
    }
    await driver.pause(600);
  }
}

module.exports = new SettingsPage();
