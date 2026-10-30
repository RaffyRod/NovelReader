const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');

describe('Library (Biblioteca)', () => {
  before(async () => {
    allure.addFeature('Library');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('shows library tab, search and Import-B drop zone', async () => {
    allure.addSeverity('blocker');
    await libraryPage.open();
    await libraryPage.expectLoaded();
    await libraryPage.expectImportZoneVisible();
  });

  it('keeps library search pinned after scroll', async () => {
    allure.addSeverity('critical');
    await libraryPage.open();
    await libraryPage.expectSearchStaysPinnedAfterScroll();
  });
});
