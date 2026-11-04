/**
 * Dismisses blocking update / download / ANR dialogs that appear on cold start.
 * Keep finds at implicit 0 — callers (ensureMainChrome) invoke this in tight loops.
 */
async function dismissBlockingDialogs() {
  const labels = [
    'Skip update',
    'Wait',
    'Esperar',
    'Close app',
    'Cerrar app',
    'Don’t allow',
    "Don't allow",
    'Cancelar',
    'Cancel',
    'Cerrar',
    'Close',
    'OK',
    'Entendido',
  ];

  await driver.setTimeout({ implicit: 0 }).catch(() => {});

  for (let attempt = 0; attempt < 2; attempt++) {
    let dismissed = false;
    for (const label of labels) {
      const btn = await $(`android=new UiSelector().textContains("${label}")`);
      if (await btn.isExisting().catch(() => false)) {
        await btn.click().catch(() => {});
        dismissed = true;
        await driver.pause(200);
      }
    }
    const pkg = process.env.APP_PACKAGE || 'com.lagradost.quicknovel.ultimate';
    const hasNav = await $(
      `android=new UiSelector().resourceId("${pkg}:id/navigation_download")`,
    )
      .isDisplayed()
      .catch(() => false);
    if (hasNav || !dismissed) break;
  }
}

module.exports = { dismissBlockingDialogs };
