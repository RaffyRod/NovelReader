const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const { ensureReaderOpen } = require('../helpers/seedNovel');

describe('Reader and TTS', function () {
  this.timeout(120000);

  before(async () => {
    allure.addFeature('Reader / TTS');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('opens a novel and shows TTS control', async () => {
    allure.addSeverity('critical');
    await ensureReaderOpen();
    await readerPage.expectTtsControlVisible();
  });

  it('starts TTS from the reader when a novel is open', async () => {
    allure.addSeverity('critical');
    if (!(await readerPage.isReaderVisible())) {
      await ensureReaderOpen();
    }

    await readerPage.startTts();
    // Starting TTS swaps bottom chrome for the TTS bar — do not re-tap content
    // (switchVisibility would hide it).
    expect(await readerPage.hasActiveTtsBar()).toBe(true);
  });
});
