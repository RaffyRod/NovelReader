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
  ];

  for (let attempt = 0; attempt < 4; attempt++) {
    let dismissed = false;
    for (const label of labels) {
      const btn = await $(`android=new UiSelector().textContains("${label}")`);
      if (await btn.isExisting().catch(() => false)) {
        await btn.click().catch(() => {});
        dismissed = true;
        await driver.pause(600);
      }
    }
    if (!dismissed) {
      break;
    }
  }
}

module.exports = { dismissBlockingDialogs };
