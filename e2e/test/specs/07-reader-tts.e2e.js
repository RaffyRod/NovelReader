const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const { ensureReaderOpen, hasFixtureContent } = require('../helpers/seedNovel');

describe('Reader and TTS', function () {
  this.timeout(120000);

  before(async () => {
    allure.addFeature('Reader / TTS');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  after(async () => {
    await libraryPage.recoverMainApp().catch(() => {});
  });

  it('opens a novel and shows TTS control', async () => {
    allure.addSeverity('critical');
    await ensureReaderOpen();
    expect(await hasFixtureContent()).toBe(true);
    await readerPage.expectTtsControlVisible();
  });

  it('starts TTS from the reader when a novel is open', async () => {
    allure.addSeverity('critical');
    if (!(await readerPage.isReaderVisible())) {
      await ensureReaderOpen();
    }

    await readerPage.startTts();
    // Must see the active TTS controls — not just the idle read_action_tts button.
    await expect(readerPage.ttsPausePlay).toBeDisplayed();
    expect(await readerPage.hasActiveTtsBar()).toBe(true);
  });
});
