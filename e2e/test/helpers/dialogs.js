/**
 * Dismisses blocking update / download dialogs that appear on cold start.
 */
async function dismissBlockingDialogs() {
  const labels = [
    'Skip update',
    'Cancelar',
    'Cancel',
    'Cerrar',
    'Close',
    'OK',
    'Entendido',
    'Don’t allow',
    "Don't allow",
    'Allow',
  ];

  for (let attempt = 0; attempt < 6; attempt++) {
    let dismissed = false;
    for (const label of labels) {
      const btn = await $(`android=new UiSelector().textContains("${label}")`);
      if (await btn.isExisting().catch(() => false)) {
        // Prefer Skip update over Cancel when both exist
        if (label === 'Allow') {
          const skip = await $(`android=new UiSelector().textContains("Skip update")`);
          if (await skip.isExisting().catch(() => false)) continue;
        }
        await btn.click().catch(() => {});
        dismissed = true;
        await driver.pause(600);
      }
    }
    const pkg = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
    const hasNav =
      (await $(
        `android=new UiSelector().resourceId("${pkg}:id/navigation_download")`,
      ).isDisplayed().catch(() => false)) ||
      (await $(
        'android=new UiSelector().descriptionMatches("(?i)(Biblioteca|Library)")',
      ).isExisting().catch(() => false));
    if (hasNav && !dismissed) break;
    if (!dismissed && !hasNav) {
      await driver.back().catch(() => {});
      await driver.pause(400);
    }
    if (!dismissed) break;
  }
}

module.exports = { dismissBlockingDialogs };
