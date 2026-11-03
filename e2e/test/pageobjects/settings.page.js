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
    // Tutorial is the last group — rewind then scroll past Información.
    for (let i = 0; i < 6; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 400,
        width: 600,
        height: 1200,
        direction: 'up',
        percent: 0.9,
      }).catch(() => {});
      await driver.pause(150);
    }

    const anchors = ['Información', 'Tutorial', '(TTS)', 'Cómo descargar novelas', 'Cómo agregar un Blog'];
    for (const marker of anchors) {
      await this.scrollToText(marker);
      await driver.pause(250);
      if (await this.tutorialsVisible()) return true;
    }

    for (let i = 0; i < 16; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 600,
        height: 1400,
        direction: 'down',
        percent: 0.9,
      });
      await driver.pause(280);
      if (await this.tutorialsVisible()) return true;
    }
    return false;
  }

  async openTutorialTts() {
    if (!(await this.scrollToTutorials())) return false;

    // Prefer exact ES/EN titles; fall back to "(TTS)" marker.
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

    await this.scrollToText('(TTS)');
    await driver.pause(300);
    const tts = this.tutorialTts;
    if (await tts.isDisplayed().catch(() => false)) {
      await tts.click();
      await driver.pause(800);
      return true;
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

    // Preference sits near the top of General — rewind list first.
    for (let i = 0; i < 8; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 400,
        width: 600,
        height: 1200,
        direction: 'up',
        percent: 0.9,
      }).catch(() => {});
      await driver.pause(200);
    }

    for (const label of this.allowSpecialCharsLabels) {
      await this.scrollToText(label);
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
    await title.waitForDisplayed({ timeout: 20000 });

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
    return this.byText('Editar');
  }

  get fuentesEditBtnEn() {
    return this.byText('Edit');
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
   * True when Fuentes is in normal mode (Editar visible, not Eliminar seleccionados).
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
