const allure = require('@wdio/allure-reporter').default;
const path = require('path');
const { execFileSync } = require('child_process');
const searchPage = require('../pageobjects/search.page');

const catalog = require(path.join(__dirname, '../data/providers.json'));

/** Spanish providers used to exercise multi-select (search across several sources). */
const MULTI_SELECT_SAMPLE = ['DevilNovels', 'SkyNovels', 'Nova'].filter((name) =>
  catalog.providers.some((p) => p.name === name),
);

describe('Providers', function () {
  this.timeout(600000);

  it('HTTP probe: provider hosts respond (soft on Cloudflare)', function () {
    allure.addFeature('Providers');
    allure.addSeverity('critical');
    this.timeout(600000);

    const script = path.join(__dirname, '../../scripts/probe-providers.js');
    let output = '';
    try {
      output = execFileSync(process.execPath, [script], {
        encoding: 'utf8',
        timeout: 560000,
      });
    } catch (err) {
      output = `${err.stdout || ''}${err.stderr || ''}`;
    }

    allure.addAttachment('probe-log', Buffer.from(output), 'text/plain');
    const report = require(path.join(__dirname, '../data/provider-probe-report.json'));
    allure.addAttachment(
      'probe-report',
      Buffer.from(JSON.stringify(report, null, 2)),
      'application/json',
    );

    const hardDown = report.results.filter(
      (r) => !r.ok && (r.status === 0 || r.error === 'timeout' || (r.error || '').includes('hang up')),
    );
    const maxHard = Math.max(3, Math.ceil(report.total * 0.1));
    if (hardDown.length > maxHard) {
      throw new Error(
        `Too many hard-down providers (${hardDown.length}>${maxHard}): ${hardDown.map((r) => r.name).join(', ')}`,
      );
    }
    expect(report.ok).toBeGreaterThan(0);
    expect(hardDown.length).toBeLessThanOrEqual(maxHard);
  });

  it('search multi-select can enable several providers and scope search results', async () => {
    allure.addFeature('Providers');
    allure.addSeverity('blocker');

    expect(MULTI_SELECT_SAMPLE.length).toBeGreaterThanOrEqual(2);

    await searchPage.open();
    await searchPage.expectLoaded();

    const selected = await searchPage.selectOnlyProviders(MULTI_SELECT_SAMPLE);
    allure.addAttachment(
      'multiselect-enabled',
      Buffer.from(JSON.stringify({ wanted: MULTI_SELECT_SAMPLE, selected }, null, 2)),
      'application/json',
    );

    if (selected.enabled.length < 2) {
      throw new Error(
        `Multi-select failed to enable at least 2 providers (got: ${selected.enabled.join(', ') || 'none'})`,
      );
    }
    expect(selected.enabled.length).toBeGreaterThanOrEqual(2);

    // Ensure we are back on Search (dialog closed)
    if (!(await searchPage.providersButton.isDisplayed().catch(() => false))) {
      await driver.back();
      await driver.pause(800);
      await searchPage.open();
      await searchPage.expectLoaded();
    }

    let hits = new Map();
    let lastError = null;
    for (const query of ['amor', 'el', 'novela']) {
      try {
        await searchPage.search(query, 120000);
        hits = await searchPage.collectProviderHits();
        if (hits.size > 0) {
          lastError = null;
          break;
        }
      } catch (err) {
        lastError = err;
      }
    }

    allure.addAttachment(
      'multiselect-search-hits',
      Buffer.from(JSON.stringify(Object.fromEntries(hits), null, 2)),
      'application/json',
    );

    if (hits.size === 0) {
      const reason = lastError ? lastError.message : 'no chips';
      throw new Error(
        `Multi-select enabled [${selected.enabled.join(', ')}] but search returned no provider chips (${reason})`,
      );
    }

    const hitNames = [...hits.keys()];
    const unexpected = hitNames.filter(
      (name) => !MULTI_SELECT_SAMPLE.some((s) => name === s || name.includes(s)),
    );
    if (unexpected.length > 0) {
      throw new Error(
        `Results include providers outside multi-select: ${unexpected.join(', ')} (selected: ${selected.enabled.join(', ')})`,
      );
    }

    const matched = selected.enabled.filter((s) =>
      hitNames.some((h) => h === s || h.includes(s)),
    );
    expect(matched.length).toBeGreaterThanOrEqual(1);
  });
});
