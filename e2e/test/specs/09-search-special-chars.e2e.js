const allure = require('@wdio/allure-reporter').default;
const searchPage = require('../pageobjects/search.page');
const settingsPage = require('../pageobjects/settings.page');

describe('Search special characters', function () {
  this.timeout(60000);

  before(async () => {
    allure.addFeature('Search');
    allure.addEpic('QuickNovel Ultimate');
    await searchPage.waitReady();
  });

  it('blocks special characters in search by default', async () => {
    allure.addSeverity('critical');
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('hola@#mundo!');
    const field = await $('android.widget.EditText');
    const value = await field.getText();
    expect(value).not.toContain('@');
    expect(value).not.toContain('#');
    expect(value).not.toContain('!');
    expect(value.toLowerCase()).toContain('hola');
    expect(value.toLowerCase()).toContain('mundo');
  });

  it('accepts special characters when the settings toggle is enabled', async () => {
    allure.addSeverity('critical');
    await settingsPage.setAllowSpecialSearchChars(true);
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('hola@mundo');
    const field = await $('android.widget.EditText');
    const value = await field.getText();
    expect(value).toContain('@');
    expect(value.toLowerCase()).toContain('hola');
    expect(value.toLowerCase()).toContain('mundo');
    await settingsPage.setAllowSpecialSearchChars(false);
  });
});
