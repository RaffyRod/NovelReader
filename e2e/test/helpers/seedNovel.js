const path = require('path');
const { execFileSync } = require('child_process');
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');

const FIXTURE_NAME = 'e2e-sample-novel.epub';
const FIXTURE_PATH = path.join(__dirname, '../../fixtures/sample-novel.epub');
const READ_ACTIVITY = 'com.lagradost.quicknovel.ReadActivity2';

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
 * Seeds EPUB into the app's external files dir and returns a FileProvider content URI.
 * Works for release CI APKs (no run-as / DocumentsUI).
 * @returns {string}
 */
function seedEpubFileProviderUri() {
  const pkg = appId();
  const dir = `/sdcard/Android/data/${pkg}/files`;
  const dest = `${dir}/${FIXTURE_NAME}`;
  adb(['shell', 'mkdir', '-p', dir]);
  execFileSync('adb', ['push', FIXTURE_PATH, dest], { stdio: 'pipe' });
  return `content://${pkg}.provider/external_files/${FIXTURE_NAME}`;
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
 * One cheap pass for reader chrome.
 * @returns {Promise<boolean>}
 */
async function hasReaderChromeOnce() {
  await readerPage.revealControls().catch(() => {});
  if (await readerPage.hasTtsChrome()) return true;
  return false;
}

/**
 * Opens the fixture EPUB in ReadActivity2 via app FileProvider (CI-safe).
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
    if (layout) {
      // Book is open — reveal chrome for TTS assertions.
      if (await readerPage.isReaderVisible()) return true;
      if (await hasReaderChromeOnce()) return true;
      // Content visible is enough to proceed; caller reveals TTS again.
      return true;
    }
    await driver.pause(500);
  }
  return false;
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
 * Ensures the reader can be opened via FileProvider EPUB (fast) or library.
 * No live search — that burned ~1h on CI when DocumentsUI/search hung.
 * @returns {Promise<true>}
 */
async function ensureReaderOpen() {
  if (await openReaderViaEpubIntent()) return true;

  if (await libraryPage.hasNovelCard().catch(() => false)) {
    if (await openReaderFromLibrary()) return true;
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
  importFixtureEpub,
  openReaderFromLibrary,
  openReaderViaEpubIntent,
  pushFixtureEpub,
  seedEpubFileProviderUri,
};
