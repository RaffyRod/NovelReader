/**
 * Mirrors app ChapterSpec.listTitle so e2e can assert decimal-safe numbering
 * without depending on a live novel that happens to ship 2.5 chapters.
 *
 * @param {number} number Absolute list position (1-based integer).
 * @param {string} title Raw provider title.
 * @param {string} [chapterWord='Capítulo'] Localized chapter word.
 * @returns {string}
 */
function listTitle(number, title, chapterWord = 'Capítulo') {
  const clean = String(title || '').trim();
  const word = String(chapterWord || 'Chapter').trim() || 'Chapter';
  if (!clean) return `${word} ${number}`;

  const labeled = /^(?:chapter|cap[ií]tulo|cap\.?)\s*#?(\d+(?:\.\d+)?)\s*[:.\-–—]?\s*(.*)$/i.exec(
    clean,
  );
  if (labeled) {
    const num = labeled[1];
    const rest = (labeled[2] || '').trim();
    return rest ? `${word} ${num}. ${rest}` : `${word} ${num}`;
  }

  const bareDecimal = /^(\d+\.\d+)\s*[:.\-–—]?\s*(.*)$/.exec(clean);
  if (bareDecimal) {
    const num = bareDecimal[1];
    const rest = (bareDecimal[2] || '').trim();
    return rest ? `${word} ${num}. ${rest}` : `${word} ${num}`;
  }

  const already = new RegExp(`^(chapter|cap[ií]tulo|cap\\.?)?\\s*#?${number}\\b`, 'i');
  if (already.test(clean)) return clean;
  return `${number}. ${clean}`;
}

/**
 * True when a visible chapter row looks numbered (integer or decimal).
 * @param {string} text
 * @returns {boolean}
 */
function looksNumberedChapterLabel(text) {
  const t = String(text || '').trim();
  return (
    /^(Capítulo|Chapter|Cap\.?)\s+\d+(\.\d+)?(\.|$|\s)/i.test(t) ||
    /^\d+(\.\d+)?\.\s+\S/.test(t)
  );
}

/**
 * Extracts the chapter number token (may be decimal) from a localized label.
 * @param {string} text
 * @returns {string|null}
 */
function extractChapterNumber(text) {
  const t = String(text || '').trim();
  const m =
    /^(?:Capítulo|Chapter|Cap\.?)\s+(\d+(?:\.\d+)?)/i.exec(t) ||
    /^(\d+(?:\.\d+)?)\.\s+/.exec(t);
  return m ? m[1] : null;
}

module.exports = {
  listTitle,
  looksNumberedChapterLabel,
  extractChapterNumber,
};
