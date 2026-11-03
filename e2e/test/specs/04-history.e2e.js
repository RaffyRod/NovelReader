const allure = require('@wdio/allure-reporter').default;
const historyPage = require('../pageobjects/history.page');

describe('History', () => {
  before(async () => {
    allure.addFeature('History');
    allure.addEpic('QuickNovel Ultimate');
    await historyPage.waitReady();
  });

  it('shows history tab with empty state or reading/visited sections', async () => {
    allure.addSeverity('normal');
    // Prior smoke specs (Wattpad MainPage) can leave chrome hidden — recover first.
    await historyPage.ensureMainChrome(25000);
    await historyPage.open();
    await historyPage.expectLoaded();

    const empty =
      (await historyPage.emptyState.isDisplayed().catch(() => false)) ||
      (await historyPage.emptyStateEn.isDisplayed().catch(() => false));
    const reading =
      (await historyPage.readingSection.isDisplayed().catch(() => false)) ||
      (await historyPage.readingSectionEn.isDisplayed().catch(() => false));
    const visited =
      (await historyPage.visitedSection.isDisplayed().catch(() => false)) ||
      (await historyPage.visitedSectionEn.isDisplayed().catch(() => false));
    expect(empty || reading || visited).toBe(true);
  });
});
