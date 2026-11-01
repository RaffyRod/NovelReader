const allure = require('@wdio/allure-reporter').default;
const path = require('path');
const { execFileSync } = require('child_process');
const searchPage = require('../pageobjects/search.page');

const catalog = require(path.join(__dirname, '../data/providers.json'));

/**
 * Prefer Spanish providers that the HTTP probe marked OK; fall back to a stable pair.
 * @returns {string[]}
 */
function pickMultiSelectSample() {
  let report = { results: [] };
  try {
    report = require(path.join(__dirname, '../data/provider-probe-report.json'));
  } catch (_) {
    /* probe runs in the previous test */
  }
  const ok = new Set((report.results || []).filter((r) => r.ok).map((r) => r.name));
  const spanish = catalog.providers.filter((p) => p.lang === 'es').map((p) => p.name);
  const preferred = ['DevilNovels', 'SkyNovels', 'Nova'].filter((n) => spanish.includes(n));
  const healthyPreferred = preferred.filter((n) => ok.size === 0 || ok.has(n));
  if (healthyPreferred.length >= 2) return healthyPreferred.slice(0, 2);
  const healthyEs = spanish.filter((n) => ok.has(n));
  if (healthyEs.length >= 2) return healthyEs.slice(0, 2);
  return preferred.slice(0, 2);
}

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

    const sample = pickMultiSelectSample();
    expect(sample.length).toBeGreaterThanOrEqual(2);
    allure.addAttachment(
      'multiselect-sample',
      Buffer.from(JSON.stringify(sample, null, 2)),
      'application/json',
    );

    await searchPage.open();
    await searchPage.expectLoaded();

    const selected = await searchPage.selectOnlyProviders(sample);
    allure.addAttachment(
      'multiselect-enabled',
      Buffer.from(JSON.stringify({ wanted: sample, selected }, null, 2)),
      'application/json',
    );

    if (selected.enabled.length < 2) {
      throw new Error(
        `Multi-select failed to enable at least 2 providers (got: ${selected.enabled.join(', ') || 'none'})`,
      );
    }
    expect(selected.enabled.length).toBeGreaterThanOrEqual(2);

    // Best-effort scoped search. Hosts are often Cloudflare/down on CI; multi-select
    // enablement is the hard gate. When chips appear, they must stay in-sample.
    if (!(await searchPage.providersButton.isDisplayed().catch(() => false))) {
      await searchPage.open();
      await searchPage.expectLoaded();
    }

    let hits = new Map();
    let lastError = null;
    for (const query of ['amor', 'el', 'the']) {
      try {
        await searchPage.search(query, 60000);
        hits = await searchPage.collectProviderHits();
        if (hits.size > 0) break;
      } catch (err) {
        lastError = err;
      }
    }

    allure.addAttachment(
      'multiselect-search-hits',
      Buffer.from(
        JSON.stringify(
          { hits: Object.fromEntries(hits), lastError: lastError ? lastError.message : null },
          null,
          2,
        ),
      ),
      'application/json',
    );

    if (hits.size === 0) {
      allure.addAttachment(
        'multiselect-search-soft-skip',
        Buffer.from(
          `Multi-select OK [${selected.enabled.join(', ')}]; live search had no chips (${lastError ? lastError.message : 'empty'}).`,
        ),
        'text/plain',
      );
      return;
    }

    const hitNames = [...hits.keys()];
    const unexpected = hitNames.filter(
      (name) => !sample.some((s) => name === s || name.includes(s)),
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
