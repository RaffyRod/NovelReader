const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', '..');
const apisPath = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/util/Apis.kt');
const providersDir = path.join(root, 'app/src/main/java/com/lagradost/quicknovel/providers');
const apis = fs.readFileSync(apisPath, 'utf8');
const classes = [...apis.matchAll(/^\s+([A-Za-z0-9_]+)\(\),/gm)].map((m) => m[1]);
const files = fs.readdirSync(providersDir).filter((f) => f.endsWith('.kt'));
const by = {};
const list = [];

for (const className of classes) {
  let file = files.find((f) => f.replace('.kt', '') === className);
  if (!file) {
    file = files.find((f) =>
      fs.readFileSync(path.join(providersDir, f), 'utf8').includes(`class ${className}`),
    );
  }
  const src = fs.readFileSync(path.join(providersDir, file), 'utf8');
  const nameMatch =
    src.match(new RegExp(`class ${className}[\\s\\S]*?override val name\\s*[:=]\\s*"([^"]+)"`)) ||
    src.match(/override val name\s*[:=]\s*"([^"]+)"/);
  const langMatch =
    src.match(new RegExp(`class ${className}[\\s\\S]*?override val lang\\s*=\\s*"([^"]+)"`)) ||
    src.match(/override val lang\s*=\s*"([^"]+)"/);
  const name = nameMatch ? nameMatch[1] : className;
  const lang = langMatch ? langMatch[1] : 'en';
  by[lang] = by[lang] || [];
  by[lang].push(name);
  list.push({ name, lang, className });
}

console.log(JSON.stringify({ byLang: by, list }, null, 2));
