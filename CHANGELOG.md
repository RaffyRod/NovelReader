# Changelog

All notable changes to **QuickNovel Ultimate** are listed here.
The Telegram release bot reads the section matching `versionName` from `app/build.gradle`.

## 2.0.0

### Added
- LNReader plugin hub: language cards with **Continuar**, Arabic shown as “Arabic”
- Catalog paging **in-place** (next/prev without closing) + empty **Ir a página** field
- Sections → **Traducciones** with Material Translate icon
- Search: new providers show real icon / favicon / initials + **NEW** pill (corner)
- CI: Release → Telegram (APK + release notes)

### Fixed
- Catalog “go to page” no longer jumps to page 13 when typing `3`
- Translation history no longer uses the misleading globe icon
- New providers no longer use the full-tile `fiber_new` glyph as a fake logo

### Notes
- Defaults: dark mode + Ultimate Blue · English providers ON
- Package: `com.lagradost.quicknovel.ultimate` · versionCode 200

## 1.2.9

### Added
- Plugin hub improvements (counts, catalog return after install)
- Cover placeholder (option J) in Search
- Translation history + plugin repo URL history

### Fixed
- Catalog bounce / edit flows for plugins
