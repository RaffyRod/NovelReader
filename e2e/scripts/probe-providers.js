/**
 * HTTP smoke probe for every built-in provider mainUrl.
 * Complements UI search: verifies each source host responds with content.
 */
const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');

const root = path.join(__dirname, '..', '..');
const apisPath = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/util/Apis.kt');
const providersDir = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/providers');
const outPath = path.join(__dirname, '..', 'test/data/provider-probe-report.json');

const UA =
  'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36';

function loadCatalog() {
  const apis = fs.readFileSync(apisPath, 'utf8');
  const classes = [...apis.matchAll(/^\s+([A-Za-z0-9_]+)\(\),/gm)].map((m) => m[1]);
  const files = fs.readdirSync(providersDir).filter((f) => f.endsWith('.kt'));
  const entries = [];

  for (const className of classes) {
    let file = files.find((f) => f.replace('.kt', '') === className);
    if (!file) {
      file = files.find((f) =>
        fs.readFileSync(path.join(providersDir, f), 'utf8').includes(`class ${className}`),
      );
    }
    const src = fs.readFileSync(path.join(providersDir, file), 'utf8');
    const name =
      (src.match(new RegExp(`class ${className}[\\s\\S]*?override val name\\s*[:=]\\s*"([^"]+)"`)) ||
        src.match(/override val name\s*[:=]\s*"([^"]+)"/) || [, className])[1];
    const mainUrl =
      (src.match(new RegExp(`class ${className}[\\s\\S]*?override val mainUrl\\s*=\\s*"([^"]+)"`)) ||
        src.match(/override val mainUrl\s*=\s*"([^"]+)"/) || [, null])[1];
    entries.push({ className, name, mainUrl });
  }
  return entries;
}

function normalizeHost(host) {
  if (!host) return '';
  const h = String(host).toLowerCase();
  return h.startsWith('www.') ? h.slice(4) : h;
}

function fetchOnce(url, redirects = 0, startUrl = url) {
  return new Promise((resolve) => {
    if (!url || redirects > 5) {
      resolve({
        ok: false,
        status: 0,
        error: 'no-url',
        bytes: 0,
        finalUrl: url || startUrl,
        domainChanged: false,
      });
      return;
    }
    const lib = url.startsWith('https') ? https : http;
    const req = lib.get(
      url,
      {
        headers: { 'User-Agent': UA, Accept: 'text/html,*/*' },
        timeout: 20000,
        rejectUnauthorized: false,
      },
      (res) => {
        if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location) {
          const next = new URL(res.headers.location, url).toString();
          res.resume();
          fetchOnce(next, redirects + 1, startUrl).then(resolve);
          return;
        }
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => {
          const buf = Buffer.concat(chunks);
          let startHost = '';
          let finalHost = '';
          try {
            startHost = normalizeHost(new URL(startUrl).host);
            finalHost = normalizeHost(new URL(url).host);
          } catch (_) {
            /* ignore */
          }
          const domainChanged = Boolean(startHost && finalHost && startHost !== finalHost);
          const softAlive = [401, 403, 429].includes(res.statusCode);
          resolve({
            ok:
              softAlive ||
              (res.statusCode >= 200 && res.statusCode < 400 && buf.length > 200),
            status: res.statusCode,
            bytes: buf.length,
            error: null,
            finalUrl: url,
            domainChanged,
          });
        });
      },
    );
    req.on('error', (e) =>
      resolve({
        ok: false,
        status: 0,
        error: e.message,
        bytes: 0,
        finalUrl: url,
        domainChanged: false,
      }),
    );
    req.on('timeout', () => {
      req.destroy();
      resolve({
        ok: false,
        status: 0,
        error: 'timeout',
        bytes: 0,
        finalUrl: url,
        domainChanged: false,
      });
    });
  });
}

async function main() {
  const catalog = loadCatalog();
  const results = [];
  for (const entry of catalog) {
    const probe = await fetchOnce(entry.mainUrl);
    results.push({ ...entry, ...probe });
    const mark = probe.ok ? (probe.domainChanged ? 'MOVE' : 'OK') : 'FAIL';
    const move = probe.domainChanged ? ` → ${probe.finalUrl}` : '';
    console.log(
      `${mark.padEnd(4)} ${entry.name} → ${entry.mainUrl}${move} (${probe.status}, ${probe.bytes}b${probe.error ? `, ${probe.error}` : ''})`,
    );
  }
  const failed = results.filter((r) => !r.ok);
  const moved = results.filter((r) => r.ok && r.domainChanged);
  const report = {
    total: results.length,
    ok: results.length - failed.length,
    failed: failed.map((f) => f.name),
    domainChanged: moved.map((m) => ({ name: m.name, from: m.mainUrl, to: m.finalUrl })),
    results,
  };
  fs.writeFileSync(outPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`\n${report.ok}/${report.total} providers reachable`);
  if (moved.length) {
    console.log(`Domain changed: ${moved.map((m) => m.name).join(', ')}`);
  }
  if (failed.length) {
    console.error(`Down: ${failed.map((f) => f.name).join(', ')}`);
    process.exitCode = 1;
  }
}

main();
