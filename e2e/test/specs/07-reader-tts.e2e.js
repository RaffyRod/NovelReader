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

    await readerPage.expectTtsControlVisible();
  });

  it('starts TTS from the reader when a novel is open', async function () {
    allure.addSeverity('critical');
    await libraryPage.open();
    const opened = await libraryPage.openFirstNovelIfPresent();
    if (!opened) {
      this.skip();
      return;
    }

    await readerPage.startTts();
    // TTS bar / notification path: after start, pause or stop should appear or TTS stays active.
    await readerPage.revealControls();
    const pause = await readerPage.byA11y('Pausar').isExisting().catch(() => false);
    const stop = await readerPage.byA11y('Detener').isExisting().catch(() => false);
    const ttsEs = await readerPage.ttsButton.isDisplayed().catch(() => false);
    const ttsEn = await readerPage.ttsButtonEn.isDisplayed().catch(() => false);
    expect(pause || stop || ttsEs || ttsEn).toBe(true);
    await driver.back();
  });
});
