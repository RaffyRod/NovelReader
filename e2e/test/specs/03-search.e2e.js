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

  it('live grid filter by page name shrinks tiles then clears', async () => {
    allure.addSeverity('critical');
    await searchPage.open();
    await searchPage.expectLoaded();
    // Previous specs may leave a novel query that live-filters the provider grid empty.
    await searchPage.typeQuery('');
    await driver.pause(500);
    await searchPage.revealProvidersGrid();

    const tileName = await searchPage.firstVisibleProviderTileName();
    expect(tileName).toBeTruthy();
    const others = searchPage
      .preferredProviderNames()
      .filter((n) => n !== tileName)
      .slice(0, 4);
    // Need another known tile on screen before filtering so we can prove it disappears.
    let otherVisible = null;
    for (const name of others) {
      if (await searchPage.byTextContains(name).isDisplayed().catch(() => false)) {
        otherVisible = name;
        break;
      }
    }
    expect(otherVisible).toBeTruthy();

    await searchPage.typeQuery(tileName);
    await driver.pause(900);
    const matchVisible = await searchPage.byTextContains(tileName).isDisplayed().catch(() => false);
    const otherGone = !(await searchPage
      .byTextContains(otherVisible)
      .isDisplayed()
      .catch(() => false));
    allure.addAttachment(
      'grid-filter-state',
      Buffer.from(JSON.stringify({ tileName, otherVisible, matchVisible, otherGone }, null, 2)),
      'application/json',
    );
    expect(matchVisible).toBe(true);
    expect(otherGone).toBe(true);

    await searchPage.typeQuery('');
    await driver.pause(700);
    await searchPage.revealProvidersGrid();
    const restored = await searchPage.byTextContains(otherVisible).isDisplayed().catch(() => false);
    expect(restored).toBe(true);
  });

  it('provider-name submit opens provider page or stays stable', async () => {
    allure.addSeverity('critical');
    await searchPage.open();
    await searchPage.expectLoaded();
    await searchPage.typeQuery('');
    await driver.pause(400);
    await searchPage.revealProvidersGrid();

    const tileName = await searchPage.firstVisibleProviderTileName();
    expect(tileName).toBeTruthy();

    await searchPage.typeQuery(tileName);
    const field = await $('android.widget.EditText');
    await field.addValue('\uE007').catch(() => {});
    await driver.execute('mobile: performEditorAction', { action: 'search' }).catch(() => {});
    await driver.execute('mobile: shell', { command: 'input', args: ['keyevent', '66'] });

    // Cap network wait — provider MainPage can hang on a dead host.
    const deadline = Date.now() + 15000;
    let onMain = false;
    let stillSearch = false;
    while (Date.now() < deadline) {
      stillSearch = await searchPage.providersButton.isDisplayed().catch(() => false);
      onMain = await searchPage.isProviderMainPageOpen();
      if (onMain || stillSearch) break;
      await driver.pause(500);
    }
    allure.addAttachment(
      'provider-submit-state',
      Buffer.from(JSON.stringify({ onMain, stillSearch, tileName }, null, 2)),
      'application/json',
    );
    // Must land on provider MainPage (or loading) OR remain on Buscar home — not a blank limbo.
    expect(onMain || stillSearch).toBe(true);

    // Always force chrome back so Historial / later smoke specs are not stranded.
    await driver.back().catch(() => {});
    await driver.pause(400);
    await searchPage.recoverMainApp();
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
