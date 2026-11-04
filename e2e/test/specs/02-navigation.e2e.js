const allure = require('@wdio/allure-reporter').default;
const libraryPage = require('../pageobjects/library.page');
const searchPage = require('../pageobjects/search.page');
const historyPage = require('../pageobjects/history.page');
const settingsPage = require('../pageobjects/settings.page');
const sectionsPage = require('../pageobjects/sections.page');

describe('Bottom navigation', function () {
  this.timeout(90000);

  before(async () => {
    allure.addFeature('Navigation');
    allure.addEpic('QuickNovel Ultimate');
    await libraryPage.waitReady();
  });

  it('opens Biblioteca, Buscar, Historial, Ajustes and Secciones', async () => {
    allure.addSeverity('blocker');

    await libraryPage.open();
    await libraryPage.expectLoaded();

    // Secciones only appears while Biblioteca is selected.
    await sectionsPage.expectLoaded();
    await sectionsPage.open();

    await searchPage.open();
    await searchPage.expectLoaded();

    await historyPage.open();
    await historyPage.expectLoaded();

    await settingsPage.open();
    await settingsPage.expectLoaded();

    await libraryPage.open();
    await libraryPage.expectLoaded();
    await sectionsPage.expectLoaded();
  });
});
