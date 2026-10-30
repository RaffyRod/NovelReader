const allure = require('@wdio/allure-reporter').default;
const searchPage = require('../pageobjects/search.page');

describe('Search novels', () => {
  before(async () => {
    allure.addFeature('Search');
    allure.addEpic('QuickNovel Ultimate');
    await searchPage.waitReady();
  });

  it('opens search tab and accepts a query', async () => {
    allure.addSeverity('critical');
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('Mother of Learning');
    const field = await $('android.widget.EditText');
    const value = await field.getText();
    expect(value.toLowerCase()).toContain('mother');
  });
});
