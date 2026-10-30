const BasePage = require('./base.page');

class SettingsPage extends BasePage {
  get tabSettings() {
    return this.byA11y('Ajustes');
  }

  get tutorialGroup() {
    return this.byText('Tutorial');
  }

  get tutorialTts() {
    return this.byTextContains('Lectura en voz alta');
  }

  get tutorialDownload() {
    return this.byText('Descargas');
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

  async scrollToTutorials() {
    const markers = ['Tutorial', 'Descargas', 'Lectura en voz alta', 'Proveedores'];
    for (const marker of markers) {
      await this.scrollToText(marker);
      await driver.pause(300);
      if (await this.tutorialGroup.isDisplayed().catch(() => false)) return true;
      if (await this.tutorialDownload.isDisplayed().catch(() => false)) return true;
      if (await this.tutorialTts.isDisplayed().catch(() => false)) return true;
    }
    for (let i = 0; i < 6; i++) {
      await driver.execute('mobile: scrollGesture', {
        left: 80,
        top: 500,
        width: 600,
        height: 1400,
        direction: 'down',
        percent: 0.85,
      });
      await driver.pause(400);
      if (
        (await this.tutorialGroup.isDisplayed().catch(() => false)) ||
        (await this.tutorialDownload.isDisplayed().catch(() => false)) ||
        (await this.tutorialTts.isDisplayed().catch(() => false))
      ) {
        return true;
      }
    }
    return false;
  }

  async openTutorialTts() {
    await this.scrollToTutorials();
    const item = this.tutorialTts;
    if (await item.isExisting().catch(() => false)) {
      await item.click();
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
   * Reads the checked state of the switch associated with the special-chars preference.
   * Falls back to false when the control is not exposed as a native Switch.
   * @param {WebdriverIO.Element} [titleEl]
   */
  async isAllowSpecialSearchCharsOn(titleEl) {
    const title = titleEl || (await this.findAllowSpecialSearchCharsTitle());
    const titleBounds = await title.getLocation().catch(() => null);
    const switches = await $$('android.widget.Switch');
    let best = null;
    let bestDist = Number.POSITIVE_INFINITY;
    for (const sw of switches) {
      if (!(await sw.isDisplayed().catch(() => false))) continue;
      if (!titleBounds) {
        best = sw;
        continue;
      }
      const loc = await sw.getLocation().catch(() => null);
      if (!loc) continue;
      const dist = Math.abs(loc.y - titleBounds.y);
      if (dist < bestDist) {
        bestDist = dist;
        best = sw;
      }
    }
    if (!best) return false;
    return (await best.getAttribute('checked')) === 'true';
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
