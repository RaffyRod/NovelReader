const allure = require('@wdio/allure-reporter').default;
const sectionsPage = require('../pageobjects/sections.page');
const libraryPage = require('../pageobjects/library.page');

describe('Sections translation history', () => {
  before(async () => {
    allure.addFeature('Sections');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  after(async () => {
    await driver.back().catch(() => {});
    await libraryPage.ensureMainChrome().catch(() => {});
  });

  it('opens Traducciones from Secciones and shows history dialog', async () => {
    allure.addSeverity('critical');
    await libraryPage.open();
    await libraryPage.expectLoaded();

    await sectionsPage.openTranslationHistory();

    const titled = await sectionsPage.translationHistoryTitle.isDisplayed().catch(() => false);
    const empty = await sectionsPage.translationHistoryEmpty.isDisplayed().catch(() => false);
    expect(titled || empty).toBe(true);

    await driver.back().catch(() => {});
    await driver.pause(400);
  });
});
