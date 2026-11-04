const allure = require('@wdio/allure-reporter').default;
const searchPage = require('../pageobjects/search.page');

describe('Search novels', function () {
  this.timeout(180000);

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

  it('live grid filter by page name shrinks tiles then clears', async function () {
    allure.addSeverity('critical');
    this.timeout(90000);
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('');
    await driver.pause(400);

    // Prefer two known tiles without long scroll storms.
    await $(
      'android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("AllNovel"))',
    ).catch(() => {});
    const tileName = (await searchPage.byTextContains('AllNovel').isDisplayed().catch(() => false))
      ? 'AllNovel'
      : await searchPage.firstVisibleProviderTileName();
    expect(tileName).toBeTruthy();

    const otherCandidates = searchPage.preferredProviderNames().filter((n) => n !== tileName);
    let otherVisible = null;
    for (const name of otherCandidates) {
      if (await searchPage.byTextContains(name).isDisplayed().catch(() => false)) {
        otherVisible = name;
        break;
      }
    }
    expect(otherVisible).toBeTruthy();

    await searchPage.typeQuery(tileName);
    await driver.pause(900);
    expect(await searchPage.byTextContains(tileName).isDisplayed().catch(() => false)).toBe(true);
    expect(await searchPage.byTextContains(otherVisible).isDisplayed().catch(() => false)).toBe(
      false,
    );

    await searchPage.typeQuery('');
    await driver.pause(700);
    expect(await searchPage.byTextContains(otherVisible).isDisplayed().catch(() => false)).toBe(
      true,
    );
  });

  it('provider-name submit opens provider page or stays stable', async function () {
    allure.addSeverity('critical');
    this.timeout(60000);
    const { execFileSync } = require('child_process');
    const pkg = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';

    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('');
    await driver.pause(300);

    const tileName = 'AllNovel';
    await searchPage.typeQuery(tileName);
    const field = await $('android.widget.EditText');
    await field.addValue('\uE007').catch(() => {});
    await driver.execute('mobile: performEditorAction', { action: 'search' }).catch(() => {});
    await driver.execute('mobile: shell', { command: 'input', args: ['keyevent', '66'] });
    await driver.pause(2500);

    // Do not run `adb uiautomator dump` while Appium UiAutomator2 owns the device (CI fails).
    // dumpsys does not conflict; providers tune vanishing also signals left Buscar home.
    let dumpsys = '';
    try {
      dumpsys = execFileSync('adb', ['shell', 'dumpsys', 'activity', 'activities'], {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
      });
    } catch (_) {
      dumpsys = '';
    }
    const topResumed =
      (dumpsys.match(/topResumedActivity=ActivityRecord\{[^}]+\s+(\S+)/) || [])[1] || '';
    const providersStill = await searchPage.providersButton.isDisplayed().catch(() => false);
    const providerPage = await searchPage.isProviderMainPageOpen().catch(() => false);
    const leftHome =
      providerPage ||
      !providersStill ||
      /MainPage|ReadActivity/i.test(topResumed);
    allure.addAttachment(
      'provider-submit-state',
      Buffer.from(
        JSON.stringify(
          { leftHome, providersStill, providerPage, topResumed, tileName },
          null,
          2,
        ),
      ),
      'application/json',
    );
    expect(leftHome).toBe(true);

    execFileSync('adb', ['shell', 'am', 'force-stop', pkg], { stdio: 'ignore' });
    execFileSync(
      'adb',
      ['shell', 'am', 'start', '-n', `${pkg}/com.lagradost.quicknovel.MainActivity`],
      { stdio: 'ignore' },
    );
    await driver.pause(2000);
    await searchPage.open();
    await searchPage.expectLoaded();
    expect(await searchPage.isMainChromeVisible()).toBe(true);
  });

  it('novel-name search runs without crashing', async () => {
    allure.addSeverity('critical');
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('');
    await searchPage.search('Mother of Learning', 45000);

    const noResults = await searchPage.noResults.isDisplayed().catch(() => false);
    const queryOpen = await searchPage.isSearchQueryOpen();
    const hasRows = await searchPage.hasSearchResultRows();
    const fieldOk = await searchPage.searchField.isDisplayed().catch(() => false);
    const tabOk = await searchPage.tabSearch.isDisplayed().catch(() => false);

    allure.addAttachment(
      'novel-search-state',
      Buffer.from(
        JSON.stringify({ noResults, queryOpen, hasRows, fieldOk, tabOk }, null, 2),
      ),
      'application/json',
    );

    expect(noResults || queryOpen || hasRows).toBe(true);
    expect(fieldOk || tabOk || queryOpen).toBe(true);
  });
});
