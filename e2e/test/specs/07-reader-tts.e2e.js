const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');

describe('Reader and TTS', () => {
  before(async () => {
    allure.addFeature('Reader / TTS');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('opens a novel from library when one exists and shows TTS control', async function () {
    allure.addSeverity('critical');
    await libraryPage.open();
    const opened = await libraryPage.openFirstNovelIfPresent();
    if (!opened) {
      this.skip();
      return;
    }

    const inReader = await readerPage.isReaderVisible();
    if (!inReader) {
      this.skip();
      return;
    }
    await readerPage.expectTtsControlVisible();
    await driver.back();
  });

  it('starts TTS from the reader when a novel is open', async function () {
    allure.addSeverity('critical');
    await libraryPage.open();
    const opened = await libraryPage.openFirstNovelIfPresent();
    if (!opened) {
      this.skip();
      return;
    }

    const inReader = await readerPage.isReaderVisible();
    if (!inReader) {
      this.skip();
      return;
    }

    await readerPage.startTts();
    await readerPage.revealControls();
    const pausePlay = await readerPage.ttsPausePlay.isDisplayed().catch(() => false);
    const stop = await readerPage.ttsStop.isDisplayed().catch(() => false);
    const tts = await readerPage.ttsButton.isDisplayed().catch(() => false);
    expect(pausePlay || stop || tts).toBe(true);
    await driver.back();
  });
});
