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
 * Navigates DocumentsUI toward Downloads / the fixture file.
 * @returns {Promise<boolean>}
 */
async function pickFixtureInDocumentUi() {
  const navLabels = [
    'Show roots',
    'Mostrar raíces',
    'Files',
    'Documents',
    'Downloads',
    'Download',
    'Descargas',
    'Recent',
    'Recientes',
  ];
  const fileLabels = [FIXTURE_NAME, 'e2e-sample-novel', 'sample-novel', 'Prueba-QuickNovel'];

  for (let pass = 0; pass < 10; pass++) {
    for (const label of fileLabels) {
      const el = await $(`android=new UiSelector().textContains("${label}")`);
      if (!(await el.isExisting().catch(() => false))) continue;
      if (!(await el.isDisplayed().catch(() => false))) continue;
      await el.click().catch(() => {});
      await driver.pause(1200);
      // Left DocumentsUI → app resumed / import started.
      const stillPicker =
        (await $('android=new UiSelector().textContains("Downloads")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().textContains("Open from")').isDisplayed().catch(() => false)) ||
        (await $('android=new UiSelector().packageName("com.google.android.documentsui")').isExisting().catch(() => false)) ||
        (await $('android=new UiSelector().packageName("com.android.documentsui")').isExisting().catch(() => false));
      if (!stillPicker) return true;
      // Sometimes need a second tap on the row / Open.
      const openBtn = await $('android=new UiSelector().textMatches("(?i)(Open|Abrir|Select|Seleccionar)")');
      if (await openBtn.isDisplayed().catch(() => false)) {
        await openBtn.click().catch(() => {});
        await driver.pause(1200);
        return true;
      }
      return true;
    }

    for (const label of navLabels) {
      const el = await $(`android=new UiSelector().textContains("${label}")`);
      if (await el.isDisplayed().catch(() => false)) {
        await el.click().catch(() => {});
        await driver.pause(700);
      }
    }

    // Hamburger / drawer in DocumentsUI
    const drawer = await $('android=new UiSelector().descriptionContains("Show roots")');
    if (await drawer.isDisplayed().catch(() => false)) {
      await drawer.click().catch(() => {});
      await driver.pause(500);
    }

    await driver.execute('mobile: scrollGesture', {
      left: 80,
      top: 400,
      width: 600,
      height: 1400,
      direction: 'down',
      percent: 0.55,
    }).catch(() => {});
    await driver.pause(350);
  }
  return false;
}

/**
 * Imports the fixture EPUB through Biblioteca → Import-B → DocumentsUI.
 * @returns {Promise<boolean>}
 */
async function importFixtureEpub() {
  pushFixtureEpub();
  await libraryPage.ensureMainChrome(15000).catch(() => {});
  await libraryPage.open();
  await libraryPage.importDropZone.waitForDisplayed({ timeout: 15000 });
  await libraryPage.importDropZone.click();
  await driver.pause(2000);
  const picked = await pickFixtureInDocumentUi();
  if (!picked) {
    await driver.back().catch(() => {});
    await driver.pause(400);
    await driver.back().catch(() => {});
    return false;
  }
  for (let i = 0; i < 25; i++) {
    await libraryPage.ensureMainChrome(8000).catch(() => {});
    await libraryPage.open();
    if (await libraryPage.hasNovelCard()) return true;
    await driver.pause(800);
  }
  return false;
}

/**
 * Opens first library novel into the reader (detail → Leer if needed).
 * @returns {Promise<boolean>}
 */
async function openReaderFromLibrary() {
  await libraryPage.ensureMainChrome(15000).catch(() => {});
  await libraryPage.open();
  if (!(await libraryPage.openFirstNovelIfPresent())) return false;
  if (await readerPage.isReaderVisible()) return true;
  const leer = await $('android=new UiSelector().textMatches("(?i).*(Leer|Read).*")');
  if (await leer.isDisplayed().catch(() => false)) {
    await leer.click();
    await driver.pause(2500);
  }
  return readerPage.isReaderVisible();
}

/**
 * Opens a search result and starts online reading to reach the reader.
 * Scopes to a small set of providers so CI does not hang on dead hosts.
 * @returns {Promise<boolean>}
 */
async function openReaderViaSearch() {
  await searchPage.ensureMainChrome(15000).catch(() => {});
  await searchPage.open();
  await searchPage.expectLoaded();

  // Prefer import path for reader; search is last resort with short queries.
  let searched = false;
  for (const query of ['amor', 'the', 'el']) {
    try {
      await searchPage.search(query, 60000);
      searched = true;
      break;
    } catch (e) {
      console.warn(`openReaderViaSearch query "${query}": ${e.message || e}`);
      await searchPage.recoverMainApp();
      await searchPage.open();
      await searchPage.expectLoaded();
    }
  }
  if (!searched) return false;
  if (!(await searchPage.hasSearchResultRows()) && (await searchPage.collectProviderHits()).size === 0) {
    return false;
  }

  const covers = await searchPage.findNovelCoverElements();
  for (const el of covers.slice(0, 5)) {
    await el.click().catch(() => {});
    await driver.pause(2500);
    if (await readerPage.isReaderVisible()) break;
  }

  const stream = await $('android=new UiSelector().textContains("Leer en línea")');
  const leer = await $('android=new UiSelector().textMatches("(?i)^Leer$|^Read$")');
  if (await stream.isDisplayed().catch(() => false)) {
    await stream.click();
  } else if (await leer.isDisplayed().catch(() => false)) {
    await leer.click();
  } else {
    const chapter = await $('android=new UiSelector().textContains("Capítulo")');
    if (await chapter.isDisplayed().catch(() => false)) {
      await chapter.click();
    }
  }
  await driver.pause(3000);
  return readerPage.isReaderVisible();
}

/**
 * Ensures the reader can be opened: library → import fixture → scoped search.
 * Throws when none of the paths reach the reader (no soft skip).
 * @returns {Promise<true>}
 */
async function ensureReaderOpen() {
  if (await openReaderFromLibrary()) return true;

  if (await importFixtureEpub()) {
    if (await openReaderFromLibrary()) return true;
  }

  await searchPage.recoverMainApp();
  if (await openReaderViaSearch()) return true;

  throw new Error(
    'Could not open reader: library empty, EPUB import failed, and scoped search did not reach reader UI',
  );
}

module.exports = {
  ensureReaderOpen,
  importFixtureEpub,
  openReaderFromLibrary,
  openReaderViaSearch,
  pushFixtureEpub,
};
