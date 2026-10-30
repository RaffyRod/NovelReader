const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', '..');
const apisPath = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/util/Apis.kt');
const providersDir = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/providers');
const outPath = path.join(__dirname, '..', 'test/data/providers.json');

const apis = fs.readFileSync(apisPath, 'utf8');
const classes = [...apis.matchAll(/^\s+([A-Za-z0-9_]+)\(\),/gm)].map((m) => m[1]);
const files = fs.readdirSync(providersDir).filter((f) => f.endsWith('.kt'));

const providers = [];
for (const className of classes) {
  let file = files.find((f) => f.replace('.kt', '') === className);
  if (!file) {
    file = files.find((f) =>
      fs.readFileSync(path.join(providersDir, f), 'utf8').includes(`class ${className}`),
    );
  }
  if (!file) {
    providers.push({ name: className, lang: 'en', hasMainPage: false, className });
    continue;
  }
  const src = fs.readFileSync(path.join(providersDir, file), 'utf8');
  const block =
    src.match(new RegExp(`class ${className}[\\s\\S]*?(?=\\nclass |\\nobject |$)`))?.[0] || src;
  const name =
    (block.match(/override val name\s*[:=]\s*"([^"]+)"/) || [, className])[1];
  const lang = (block.match(/override val lang\s*=\s*"([^"]+)"/) || [, 'en'])[1];
  const hasMainPage = /override val hasMainPage\s*=\s*true/.test(block);
  providers.push({ name, lang, hasMainPage, className });
}

providers.sort((a, b) => a.name.localeCompare(b.name));
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(
  outPath,
  `${JSON.stringify(
    {
      generatedFrom: 'Apis.kt builtInApis',
      queries: ['amor', 'el', 'my', 'the'],
      providers,
    },
    null,
    2,
  )}\n`,
);
console.log(`Wrote ${providers.length} providers (${providers.filter((p) => p.hasMainPage).length} with main page)`);
