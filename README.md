# 本 QuickNovel Ultimate

> Search, read, listen, and download novels — free, ad-free, open source.

| 🌐 Docs language | 🔗 Link |
|------------------|---------|
| 🇪🇸 Español | [Full manual](docs/manual-1.2.3/es/) |
| 🇬🇧 English | [Full manual](docs/manual-1.2.3/en/) |
| 🇷🇺 Русский | [README_RU](README_RU.md) |

Fork of [QuickNovel](https://github.com/LagradOst/QuickNovel) with Ultimate branding, themes, Import-B, Sections, custom providers, plugins, and stronger QA.

---

## ⚡ At a glance

| | |
|---|---|
| 🏷️ **Version** | **1.2.6** (`versionCode` 126) |
| 📦 **Package** | `com.lagradost.quicknovel.ultimate` |
| 🌙 **Defaults** | Dark mode **ON** · **Ultimate Blue** · show English providers **ON** |
| 📥 **Download** | [Releases](https://github.com/RaffyRod/NovelReader/releases) · universal APK |
| 🧑‍💻 **Repo** | https://github.com/RaffyRod/NovelReader |
| ⬆️ **Upstream** | https://github.com/LagradOst/QuickNovel |

### ✅ First steps (2 minutes)

| Step | What to do |
|------|------------|
| 1️⃣ | Uninstall older Ultimate / stock QuickNovel if present |
| 2️⃣ | Install `QuickNovel-Ultimate-1.2.6-universal.apk` |
| 3️⃣ | If prompted to update to LagradOst 3.x → **Cancel / Skip** |
| 4️⃣ | Open **QuickNovel Ultimate** (orb + 本) |
| 5️⃣ | App language defaults to **Español** (change in Settings) |

> 💡 Tip: if you see two “QuickNovel” icons, always use **Ultimate**.

---

## ✨ What you can do

| Area | Highlights |
|------|------------|
| 🎨 **Look** | Ultimate Blue + dark mode; 11 accent packs; icon styles A–J |
| 📚 **Library** | Pinned local search, follow, Import-B (EPUB/PDF) |
| 🧭 **Sections** | Library shelves (Ultimate-only) |
| 🕘 **History** | **Reading** / **Visited** with date groups |
| 🔎 **Search** | Multi-provider, chips, special chars blocked by default; English sources on by default |
| 🔌 **Sources** | Custom providers (Blog/Madara) + **LNReader plugins** soft-card hub |
| 🖼️ **Covers** | Title-as-cover placeholder (theme + language) when no art |
| 📖 **Reader** | **Glyph Pulse** (本) while loading — themed, UI language, no raw URL |
| ⬇️ **Download** | Ranges, chapters/file, language via Gemini / Google / Original |
| 🔊 **TTS** | Piper/Sherpa voices (Dave ES / Lessac EN) + notification |
| 🧪 **QA** | Unit + Appium E2E (specs 01–13) + CI suites (smoke / features / providers / reader) |

---

## 📱 Screenshots

<p>
<img src="./.github/ultimate/library.png" height="360" alt="Library"/>
<img src="./.github/ultimate/search.png" height="360" alt="Search"/>
<img src="./.github/ultimate/search-results.png" height="360" alt="Search results"/>
<img src="./.github/ultimate/history.png" height="360" alt="History"/>
</p>
<p>
<img src="./.github/ultimate/sections.png" height="360" alt="Sections"/>
<img src="./.github/ultimate/settings-providers.png" height="360" alt="Settings"/>
<img src="./.github/ultimate/add-provider.png" height="360" alt="Add provider"/>
<img src="./.github/ultimate/provider-multiselect.png" height="360" alt="Multi-select"/>
</p>

More shots → [`docs/manual-1.2.3/es/imagenes/`](docs/manual-1.2.3/es/imagenes/)

---

## 📖 Manuals & tutorials

| 🌍 | Format | Path |
|----|--------|------|
| 🇪🇸 | HTML + Word + bug template | [`docs/manual-1.2.3/es/`](docs/manual-1.2.3/es/) |
| 🇬🇧 | HTML + Word + bug template | [`docs/manual-1.2.3/en/`](docs/manual-1.2.3/en/) |
| 🗂️ | Docs index | [`docs/README.md`](docs/README.md) |
| 🎨 | HTML mocks (themes, Glyph Pulse, plugins…) | [`docs/previews/`](docs/previews/) |

In-app: **Settings → Tutorial** (Providers · Downloads · TTS).

Typical Desktop pack: `QuickNovel-Ultimate-1.2.6-Docs/` (APK + manuals + previews). Manuals folder remains `docs/manual-1.2.3/` until the next doc regen.

---

## 🛠️ Quick build

```bash
./gradlew :app:assembleRelease
```

| Command | Purpose |
|---------|---------|
| `./gradlew :app:assembleRelease` | Release APK |
| `./gradlew :app:testDebugUnitTest` | Unit tests |
| `cd e2e && npm test` | Full E2E suite |
| `npm run test:smoke` (in `e2e/`) | Smoke 01–04 |
| `python docs/manual-1.2.3/_generar_docs_en.py` | Regenerate EN manual |

Sign the universal APK under `app/build/outputs/apk/release/` or use the Desktop build.  
E2E details → [`e2e/README.md`](e2e/README.md) · CI → [`.github/workflows/e2e-android.yml`](.github/workflows/e2e-android.yml)

---

## 🧰 Stack (summary)

| Layer | Tech |
|-------|------|
| 💻 Language | Kotlin |
| 🖼️ UI | Jetpack Compose (M3) + legacy Views |
| 🌐 Network | OkHttp / NiceHttp, Jsoup |
| 🖼️ Images | Coil 3 |
| 📖 Reader | EPUB + PDFBox |
| 🔊 TTS | Android TTS + Sherpa/Piper |
| 🌐 Translation | ML Kit + optional Gemini |
| 🧪 Tests | JUnit · WebdriverIO 9 · Appium · Allure |

---

## 🌐 Providers

Includes sources such as SkyNovels, Nova, DevilNovels, Royal Road, Scribblehub, NovelFull, and more (`Apis.kt`).  
Also: **custom providers** (Blog / Website) and **LNReader plugins** from Settings.

---

## 👏 Credits

Based on QuickNovel by [LagradOst](https://github.com/LagradOst/QuickNovel).  
Upstream Discord: https://discord.gg/5Hus6fM

---

## ⚖️ Legal notice

Any legal issues regarding the content on this application should be taken up with the actual file hosts and providers themselves as we are not affiliated with them.

In case of copyright infringement, please directly contact the responsible parties or the streaming websites.

The app is purely for educational and personal use.

QuickNovel does not host any content on the app, and has no control over what media is put up or taken down. QuickNovel functions like any other search engine, such as Google. QuickNovel does not host, upload or manage any videos, films or content. It simply crawls, aggregates and displays links in a convenient, user-friendly interface.

It merely scrapes 3rd-party websites that are publicly accessible via any regular web browser. It is the responsibility of the user to avoid any actions that might violate the laws governing his/her locality. Use QuickNovel at your own risk.
