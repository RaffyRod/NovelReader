const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const readerPage = require('../pageobjects/reader.page');
const { ensureReaderOpen, hasFixtureContent } = require('../helpers/seedNovel');

describe('Reader Glyph Pulse loading', function () {
  this.timeout(120000);

  before(async () => {
    allure.addFeature('Reader / Glyph Pulse');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  after(async () => {
    await libraryPage.recoverMainApp().catch(() => {});
  });

  it('shows Glyph Pulse (本) without raw URL, or opens reader from cache', async () => {
    allure.addSeverity('critical');

    const pulsePromise = readerPage.waitForGlyphPulse(45000);
    await ensureReaderOpen();

    const pulse = await pulsePromise;
    if (pulse.seen) {
      expect(pulse.glyph).toContain('本');
      expect(pulse.title).not.toMatch(/^https?:\/\//i);
      expect(pulse.subtitle).not.toMatch(/^https?:\/\//i);
      expect(pulse.title.length + pulse.subtitle.length).toBeGreaterThan(0);
    } else {
      // Local EPUB / cache path: glyph may never paint — still require a real open
      // with fixture text and TTS chrome (no empty shell / fail screen).
      expect(await hasFixtureContent()).toBe(true);
      const failShown = await readerPage
        .byId(
          `${process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate'}:id/read_fail`,
        )
        .isDisplayed()
        .catch(() => false);
      expect(failShown).toBe(false);
    }

    await readerPage.expectInReader();
  });
});
