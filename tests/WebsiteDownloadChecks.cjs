const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../web/download.js'), 'utf8');
const apk = 'https://github.com/fangfar/Morrow/releases/download/v99.0/Morrow-99.0-vc99.apk';
const config = { schemaVersion: 1, android: { apkUrl: apk } };

function setup(fetch) {
  const buttons = Array.from({ length: 3 }, () => ({
    attrs: {},
    getAttribute(key) { return this.attrs[key] ?? null; },
    addEventListener(_, handler) { this.click = handler; },
    setAttribute(key, value) { this.attrs[key] = value; },
    removeAttribute(key) { delete this.attrs[key]; },
  }));
  const close = { addEventListener(_, handler) { this.click = handler; } };
  const feedback = { hidden: true, querySelector: () => close };
  const status = {};
  const navigations = [];
  vm.runInNewContext(source, {
    document: {
      querySelectorAll: () => buttons,
      querySelector: selector => selector === '.download-feedback' ? feedback : status,
    },
    window: { location: { assign: url => navigations.push(url) } },
    fetch, URL, AbortController, setTimeout, clearTimeout,
  });
  return { buttons, feedback, status, navigations, close };
}
const click = () => ({ preventDefault() { this.prevented = true; }, button: 0 });

(async () => {
  let calls = 0;
  const site = setup(async (url, options) => {
    calls++;
    assert.equal(url, 'remote/config.json');
    assert.equal(options.credentials, 'omit');
    return { ok: true, json: async () => config };
  });
  for (const button of site.buttons) {
    await button.click(click());
    assert.deepEqual(button.attrs, {});
  }
  assert.equal(calls, 3);
  assert.deepEqual(site.navigations, [apk, apk, apk]);
  site.close.click();
  assert.equal(site.feedback.hidden, true);
  const fullApk = apk.replace('.apk', '-full.apk');
  const choices = setup(async () => ({ ok: true, json: async () => ({ schemaVersion: 1, android: { liteApkUrl: apk, fullApkUrl: fullApk } }) }));
  choices.buttons[0].attrs['data-download'] = 'lite';
  choices.buttons[1].attrs['data-download'] = 'full';
  await choices.buttons[0].click(click());
  await choices.buttons[1].click(click());
  assert.deepEqual(choices.navigations, [apk, fullApk]);
  const unpublished = setup(async () => ({ ok: true, json: async () => config }));
  unpublished.buttons[0].attrs['data-download'] = 'lite';
  await unpublished.buttons[0].click(click());
  assert.equal(unpublished.navigations.length, 0);
  const modified = { ...click(), ctrlKey: true };
  await site.buttons[0].click(modified);
  assert.equal(modified.prevented, undefined);
  assert.equal(calls, 3);

  for (const result of [
    { ok: false },
    { ok: true, json: async () => ({ schemaVersion: 1 }) },
    { ok: true, json: async () => ({ ...config, schemaVersion: 2 }) },
    { ok: true, json: async () => ({ ...config, android: { apkUrl: '' } }) },
    { ok: true, json: async () => ({ ...config, android: { apkUrl: 'https://example.com/app.apk' } }) },
  ]) {
    const failure = setup(async () => result);
    await failure.buttons[0].click(click());
    assert.equal(failure.navigations.length, 0);
    assert.match(failure.status.textContent, /手动下载/);
    assert.deepEqual(failure.buttons[0].attrs, {});
  }
  const network = setup(async () => { throw new Error('Network unavailable'); });
  await network.buttons[0].click(click());
  assert.equal(network.navigations.length, 0);

  let resolve;
  const waiting = setup(() => new Promise(done => { resolve = done; }));
  const pending = waiting.buttons[0].click(click());
  assert.equal(waiting.buttons[0].attrs['aria-busy'], 'true');
  await waiting.buttons[1].click(click());
  resolve({ ok: true, json: async () => config });
  await pending;
  assert.equal(waiting.navigations.length, 1);
  console.log('Website download checks passed');
})().catch(error => { console.error(error); process.exitCode = 1; });
