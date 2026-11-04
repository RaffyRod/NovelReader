const path = require('path');
const { execFileSync } = require('child_process');
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');

const FIXTURE_NAME = 'e2e-sample-novel.epub';
const FIXTURE_PATH = path.join(__dirname, '../../fixtures/sample-novel.epub');
const READ_ACTIVITY = 'com.lagradost.quicknovel.ReadActivity2';
/** Markers that must appear when the sample EPUB actually rendered. */
const FIXTURE_MARKERS = [
  'Prueba QuickNovel E2E',
  'novela de prueba',
  'Capitulo 1',
  'Capítulo 1',
];

/** @returns {string} */
function appId() {
  return process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
}

/**
 * Runs adb and returns stdout as string.
 * @param {string[]} args
 * @returns {string}
 */
function adb(args) {
  return execFileSync('adb', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
}

/**
 * Pushes the sample EPUB onto the emulator Downloads folder.
 */
function pushFixtureEpub() {
  execFileSync('adb', ['push', FIXTURE_PATH, `/sdcard/Download/${FIXTURE_NAME}`], {
    stdio: 'pipe',
  });
}

/**
 * Seeds EPUB under /sdcard/Download and returns a FileProvider content URI.
 * Avoids /sdcard/Android/data/<pkg> — shell mkdir is Permission denied on API 30+ CI emulators.
 * Uses provider_paths external-path name=external (sdcard root).
 * @returns {string}
 */
function seedEpubFileProviderUri() {
  const pkg = appId();
  pushFixtureEpub();
  return `content://${pkg}.provider/external/Download/${FIXTURE_NAME}`;
}

/**
 * True when ReadActivity2 is the resumed activity.
 * @returns {Promise<boolean>}
 */
async function isReadActivityOpen() {
  const activity = await driver.getCurrentActivity().catch(() => '');
  if (String(activity).includes('ReadActivity2')) return true;
  try {
    const dump = adb(['shell', 'dumpsys', 'activity', 'activities']);
    return /topResumedActivity=.*ReadActivity2/.test(dump);
  } catch (_) {
    return false;
  }
}

/**
 * True when the sample EPUB body/title is on screen (not just an empty reader shell).
 * @returns {Promise<boolean>}
 */
async function hasFixtureContent() {
  for (const marker of FIXTURE_MARKERS) {
    const el = await $(`android=new UiSelector().textContains("${marker}")`);
    if (await el.isDisplayed().catch(() => false)) return true;
  }
  return false;
}

/**
 * One pass: reveal chrome and check TTS bar.
 * @returns {Promise<boolean>}
 */
async function hasReaderChromeOnce() {
  await readerPage.revealControls().catch(() => {});
  return readerPage.hasTtsChrome();
}

/**
 * Opens the fixture EPUB in ReadActivity2 via app FileProvider (CI-safe).
 * Succeeds only when layout + fixture text are visible (hard content check).
 * @returns {Promise<boolean>}
 */
async function openReaderViaEpubIntent() {
  const uri = seedEpubFileProviderUri();
  const component = `${appId()}/${READ_ACTIVITY}`;
  // adb am start (Appium startActivity drops the VIEW data → "Empty intent").
  adb([
    'shell',
    'am',
    'start',
    '-a',
    'android.intent.action.VIEW',
    '-d',
    uri,
    '-t',
    'application/epub+zip',
    '-n',
    component,
    '--grant-read-uri-permission',
  ]);
  await driver.pause(2500);
  const deadline = Date.now() + 25000;
  while (Date.now() < deadline) {
    if (await readerPage.byId(`${appId()}:id/fail_text`).isDisplayed().catch(() => false)) {
      const msg = await readerPage.byId(`${appId()}:id/fail_text`).getText().catch(() => '');
      throw new Error(`ReadActivity failed: ${msg}`);
    }
    const layout = await readerPage
      .byId(`${appId()}:id/read_normal_layout`)
      .isDisplayed()
      .catch(() => false);
    if (layout && (await hasFixtureContent())) {
      return true;
    }
    await driver.pause(500);
  }
  return false;
}

/**
 * Opens fixture EPUB and asserts real content (throws on soft/empty open).
 * @returns {Promise<void>}
 */
async function openAndAssertFixtureReader() {
  const opened = await openReaderViaEpubIntent();
  expect(opened).toBe(true);
  expect(await hasFixtureContent()).toBe(true);
  const chrome = await readerPage.isReaderVisible();
  expect(chrome).toBe(true);
  await expect(readerPage.ttsButton).toBeDisplayed();
}

/**
 * Opens first library novel into the reader (detail → Leer if needed).
 * @returns {Promise<boolean>}
 */
async function openReaderFromLibrary() {
  await libraryPage.ensureMainChrome(10000).catch(() => {});
  await libraryPage.open();
  if (!(await libraryPage.openFirstNovelIfPresent())) return false;
  if (await hasReaderChromeOnce()) return true;
  const leer = await $('android=new UiSelector().textMatches("(?i).*(Leer|Read).*")');
  if (await leer.isDisplayed().catch(() => false)) {
    await leer.click();
    await driver.pause(1500);
  }
  return hasReaderChromeOnce();
}

/**
 * Ensures the reader is open with real chrome (TTS bar).
 * FileProvider EPUB first (asserts fixture text); library fallback still requires chrome.
 * @returns {Promise<true>}
 */
async function ensureReaderOpen() {
  if (await openReaderViaEpubIntent()) {
    expect(await hasFixtureContent()).toBe(true);
    const chrome = await readerPage.isReaderVisible();
    expect(chrome).toBe(true);
    return true;
  }

  if (await libraryPage.hasNovelCard().catch(() => false)) {
    if (await openReaderFromLibrary()) {
      const chrome = await readerPage.isReaderVisible();
      expect(chrome).toBe(true);
      return true;
    }
  }

  throw new Error(
    'Could not open reader: FileProvider EPUB seed failed and library has no novel',
  );
}

/** @deprecated kept for callers that still import by name */
async function importFixtureEpub() {
  pushFixtureEpub();
  return openReaderViaEpubIntent();
}

module.exports = {
  ensureReaderOpen,
  FIXTURE_MARKERS,
  hasFixtureContent,
  importFixtureEpub,
  openAndAssertFixtureReader,
  openReaderFromLibrary,
  openReaderViaEpubIntent,
  pushFixtureEpub,
  seedEpubFileProviderUri,
};
