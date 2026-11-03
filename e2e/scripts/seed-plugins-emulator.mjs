#!/usr/bin/env node
/**
 * Seeds 200+ LNReader plugins (+ custom providers) into the debug Ultimate APK
 * on a running emulator via `adb` + `run-as` (release builds cannot run-as).
 *
 * Usage: node e2e/scripts/seed-plugins-emulator.mjs [count=220]
 */
import { execFileSync, execSync } from 'node:child_process';
import { writeFileSync, mkdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { createHash } from 'node:crypto';

const PKG = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate.debug';
const COUNT = Math.max(1, Number(process.argv[2] || process.env.SEED_COUNT || 220));
const MANIFEST_URL =
  process.env.PLUGIN_MANIFEST_URL ||
  'https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json';
const REPO_URL = MANIFEST_URL;
const WORK = join(tmpdir(), `qn-seed-plugins-${Date.now()}`);

function adb(args, opts = {}) {
  return execFileSync('adb', args, {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
    ...opts,
  });
}

function xmlEscape(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function storeKey(name) {
  const normalized = name.trim().toLowerCase();
  if (!normalized) return 'provider_empty';
  return Buffer.from(normalized, 'utf8').toString('hex');
}

function safeId(id) {
  return id.replace(/[^a-zA-Z0-9._-]/g, '_');
}

console.log(`Package: ${PKG}`);
console.log(`Target installs: ${COUNT}`);
console.log(`Fetching catalog: ${MANIFEST_URL}`);

const catalogRes = await fetch(MANIFEST_URL);
if (!catalogRes.ok) {
  throw new Error(`Manifest HTTP ${catalogRes.status}`);
}
const catalog = await catalogRes.json();
if (!Array.isArray(catalog) || catalog.length === 0) {
  throw new Error('Catalog empty or invalid JSON array');
}
console.log(`Catalog size: ${catalog.length}`);

const picks = catalog.slice(0, Math.min(COUNT, catalog.length));
const now = Date.now();

mkdirSync(join(WORK, 'plugins'), { recursive: true });
mkdirSync(join(WORK, 'prefs'), { recursive: true });

const prefEntries = [];

// Official repo entry
prefEntries.push([
  'plugin_repo/lnreader_official',
  {
    id: 'lnreader_official',
    url: REPO_URL,
    label: 'LNReader official',
    lastSyncedAt: now,
    pluginCount: catalog.length,
  },
]);

for (let i = 0; i < picks.length; i++) {
  const item = picks[i];
  const id = String(item.id || `plugin_${i}`).trim() || `plugin_${i}`;
  const name = String(item.name || id).trim() || id;
  const site = String(item.site || '').trim();
  const lang = String(item.lang || 'English').trim();
  const version = String(item.version || '1.0.0').trim();
  const jsUrl = String(item.url || '').trim();
  const iconUrl = String(item.iconUrl || '').trim();
  const fileName = `${safeId(id)}.js`;
  const jsPath = `/data/data/${PKG}/files/plugins/${fileName}`;
  writeFileSync(
    join(WORK, 'plugins', fileName),
    `// seeded stub for ${name}\nexport default {};\n`,
  );

  prefEntries.push([
    `plugin_installed/${id}`,
    {
      id,
      name,
      localName: name,
      site,
      lang,
      langCode: 'en',
      version,
      latestVersion: version,
      jsUrl,
      iconUrl,
      repoUrl: REPO_URL,
      providerName: '',
      installedAt: now - i * 1000,
      jsPath,
      cssMapped: false,
      templateKind: 'js',
    },
  ]);
}

// Extra custom CSS providers (proveedores propios) on top of plugins.
const PROVIDER_EXTRA = 40;
for (let i = 1; i <= PROVIDER_EXTRA; i++) {
  const name = `Seed Provider ${String(i).padStart(3, '0')}`;
  const baseUrl = `https://example${i}.test`;
  const cfg = {
    name,
    baseUrl,
    lang: 'en',
    searchUrl: '/?s={query}&post_type=wp-manga',
    resultItem: 'div.c-tabs-item__content',
    resultLink: 'div.post-title a',
    resultImage: 'div.tab-thumb img',
    novelTitle: 'div.post-title h1',
    novelAuthor: '.author-content a',
    novelCover: 'div.summary_image img',
    novelSynopsis: 'div.summary__content',
    chapterLink: '.wp-manga-chapter > a[href]',
    chaptersNewestFirst: true,
    chaptersViaAjax: true,
    content: 'div.text-left',
    kind: 'css',
    iconUrl: '',
  };
  prefEntries.push([`custom_provider/${storeKey(name)}`, cfg]);
}

let xml = `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n`;
for (const [key, value] of prefEntries) {
  xml += `    <string name="${xmlEscape(key)}">${xmlEscape(JSON.stringify(value))}</string>\n`;
}
xml += `</map>\n`;

const prefsFile = join(WORK, 'prefs', 'rebuild_preference.xml');
writeFileSync(prefsFile, xml, 'utf8');
console.log(`Wrote prefs with ${prefEntries.length} keys`);

adb(['shell', 'am', 'force-stop', PKG]);
adb(['shell', `run-as ${PKG} mkdir -p files/plugins shared_prefs`]);

// One archive + one push (hundreds of adb push calls kill the daemon on Windows).
const tarPath = join(WORK, 'seed.tar');
execSync(`tar -cf "${tarPath}" -C "${WORK}" prefs plugins`, { stdio: 'inherit' });
const remoteTar = `/data/local/tmp/qn-seed-${createHash('md5').update(String(now)).digest('hex').slice(0, 8)}.tar`;
adb(['push', tarPath, remoteTar]);
adb(['shell', `rm -rf /data/local/tmp/qn-seed-extract && mkdir -p /data/local/tmp/qn-seed-extract`]);
adb(['shell', `tar -xf ${remoteTar} -C /data/local/tmp/qn-seed-extract`]);
adb([
  'shell',
  `run-as ${PKG} sh -c 'cp /data/local/tmp/qn-seed-extract/prefs/rebuild_preference.xml shared_prefs/rebuild_preference.xml && cp -r /data/local/tmp/qn-seed-extract/plugins/. files/plugins/ && chmod 660 shared_prefs/rebuild_preference.xml'`,
]);
adb(['shell', `rm -rf ${remoteTar} /data/local/tmp/qn-seed-extract`]);
rmSync(WORK, { recursive: true, force: true });

const installedCount = adb([
  'shell',
  `run-as ${PKG} sh -c 'ls files/plugins | wc -l'`,
]).trim();
const prefsCheck = adb([
  'shell',
  `run-as ${PKG} sh -c 'grep -c plugin_installed shared_prefs/rebuild_preference.xml'`,
]).trim();

console.log(`JS files in app: ${installedCount}`);
console.log(`plugin_installed keys in prefs: ${prefsCheck}`);
console.log(`custom providers seeded: ${PROVIDER_EXTRA}`);

adb([
  'shell',
  'am',
  'start',
  '-n',
  `${PKG}/com.lagradost.quicknovel.MainActivity`,
]);

console.log('Done. Open Settings → Plugins / Proveedores to validate.');
