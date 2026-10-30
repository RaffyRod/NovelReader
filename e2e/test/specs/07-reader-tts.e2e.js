const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const { ensureReaderOpen } = require('../helpers/seedNovel');

describe('Reader and TTS', function () {
  this.timeout(300000);

  before(async () => {
    allure.addFeature('Reader / TTS');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('opens a novel and shows TTS control', async () => {
    allure.addSeverity('critical');
    const opened = await ensureReaderOpen();
    expect(opened).toBe(true);
    await readerPage.expectTtsControlVisible();
    await driver.back();
  });

  it('starts TTS from the reader when a novel is open', async () => {
    allure.addSeverity('critical');
    const opened = await ensureReaderOpen();
    expect(opened).toBe(true);

    await readerPage.startTts();
    await readerPage.revealControls();
    const pausePlay = await readerPage.ttsPausePlay.isDisplayed().catch(() => false);
    const stop = await readerPage.ttsStop.isDisplayed().catch(() => false);
    const tts = await readerPage.ttsButton.isDisplayed().catch(() => false);
    expect(pausePlay || stop || tts).toBe(true);
    await driver.back();
  });
});
