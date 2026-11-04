const path = require('path');
const { execFileSync } = require('child_process');
const libraryPage = require('../pageobjects/library.page');
const searchPage = require('../pageobjects/search.page');
const readerPage = require('../pageobjects/reader.page');

const FIXTURE_NAME = 'e2e-sample-novel.epub';
const FIXTURE_PATH = path.join(__dirname, '../../fixtures/sample-novel.epub');
const DEVICE_PATH = `/sdcard/Download/${FIXTURE_NAME}`;

/**
 * Pushes the sample EPUB onto the emulator Downloads folder.
 */
function pushFixtureEpub() {
  execFileSync('adb', ['push', FIXTURE_PATH, DEVICE_PATH], { stdio: 'pipe' });
}

/**
 * Tries to pick the fixture file inside the system document picker.
 * @returns {Promise<boolean>}
 */
async function pickFixtureInDocumentUi() {
  const labels = [
    FIXTURE_NAME,
    'e2e-sample-novel',
    'Prueba-QuickNovel',
    'Download',
    'Downloads',
    'Descargas',
    'Show roots',
    'Mostrar raíces',
    'Recent',
    'Recientes',
  ];

  for (let pass = 0; pass < 8; pass++) {
    for (const label of labels) {
      const el = await $(`android=new UiSelector().textContains("${label}")`);
      if (!(await el.isExisting().catch(() => false))) continue;
      if (!(await el.isDisplayed().catch(() => false))) continue;
      await el.click().catch(() => {});
      await driver.pause(900);
      if (label.includes('sample') || label.includes('Prueba') || label.endsWith('.epub')) {
        return true;
      }
    }
    await driver.execute('mobile: scrollGesture', {
      left: 80,
      top: 500,
      width: 600,
      height: 1400,
      direction: 'down',
      percent: 0.6,
    }).catch(() => {});
    await driver.pause(400);
  }
  return false;
}

/**
 * Imports the fixture EPUB through Biblioteca → Import-B → DocumentsUI.
 * @returns {Promise<boolean>}
 */
async function importFixtureEpub() {
  pushFixtureEpub();
  await libraryPage.open();
  await libraryPage.importDropZone.waitForDisplayed({ timeout: 15000 });
  await libraryPage.importDropZone.click();
  await driver.pause(2000);
  const picked = await pickFixtureInDocumentUi();
  if (!picked) {
    await driver.back().catch(() => {});
    return false;
  }
  // Wait for import to land a card in the library
  for (let i = 0; i < 20; i++) {
    await libraryPage.open();
    if (await libraryPage.hasNovelCard()) return true;
    await driver.pause(1000);
  }
  return false;
}

/**
 * Opens a search result and starts online reading to reach the reader.
 * @returns {Promise<boolean>}
 */
async function openReaderViaSearch() {
  await searchPage.ensureMainChrome(15000).catch(() => {});
  await searchPage.open();
  try {
    await searchPage.expectLoaded();
  } catch (e) {
    console.warn(`openReaderViaSearch chrome: ${e.message || e}`);
    await searchPage.recoverMainApp();
    await searchPage.open();
    try {
      await searchPage.expectLoaded();
    } catch (e2) {
      console.warn(`openReaderViaSearch unavailable: ${e2.message || e2}`);
      return false;
    }
  }
  try {
    await searchPage.search('amor', 90000);
  } catch (e) {
    console.warn(`openReaderViaSearch search: ${e.message || e}`);
    return false;
  }
  const hits = await searchPage.collectProviderHits();
  if (hits.size === 0) return false;

  // Tap first novel-looking title under the chips area
  const texts = await $$('android.widget.TextView');
  for (const el of texts) {
    const t = ((await el.getText().catch(() => '')) || '').trim();
    if (!t || t.length < 8 || t.length > 100) continue;
    if (/^\(.+\)$/.test(t)) continue;
    if (/Todos|\(\d+\)|Proveedores|Buscar|Biblioteca|Historial|Ajustes|Secciones/.test(t)) {
      continue;
    }
    await el.click();
    await driver.pause(2500);
    break;
  }

  const stream = await $(
    'android=new UiSelector().textContains("Leer en línea")',
  );
  const leer = await $('android=new UiSelector().text("Leer")');
  if (await stream.isDisplayed().catch(() => false)) {
    await stream.click();
  } else if (await leer.isDisplayed().catch(() => false)) {
    await leer.click();
  } else {
    // Chapter row fallback
    const chapter = await $('android=new UiSelector().textContains("Capítulo")');
    if (await chapter.isDisplayed().catch(() => false)) {
      await chapter.click();
    }
  }
  await driver.pause(3000);
  return readerPage.isReaderVisible();
}

/**
 * Ensures the reader can be opened: library novel, import fixture, or search stream.
 * @returns {Promise<boolean>} true when the reader UI is visible
 */
async function ensureReaderOpen() {
  await libraryPage.ensureMainChrome();
  await libraryPage.open();
  if (await libraryPage.openFirstNovelIfPresent()) {
    if (await readerPage.isReaderVisible()) return true;
    const leer = await $('android=new UiSelector().textContains("Leer")');
    if (await leer.isDisplayed().catch(() => false)) {
      await leer.click();
      await driver.pause(2500);
      if (await readerPage.isReaderVisible()) return true;
    }
  }

  if (await importFixtureEpub()) {
    if (await libraryPage.openFirstNovelIfPresent()) {
      if (await readerPage.isReaderVisible()) return true;
      const leer = await $('android=new UiSelector().textContains("Leer")');
      if (await leer.isDisplayed().catch(() => false)) {
        await leer.click();
        await driver.pause(2500);
      }
      if (await readerPage.isReaderVisible()) return true;
    }
  }

  return openReaderViaSearch();
}

module.exports = {
  ensureReaderOpen,
  importFixtureEpub,
  openReaderViaSearch,
  pushFixtureEpub,
};
