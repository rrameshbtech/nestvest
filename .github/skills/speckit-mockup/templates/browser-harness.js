// Prefix for every runPlaywrightCode call. Append journey checks, then `return r;`.
// Each check records its own result, so one failure never aborts the journey.
page.setDefaultTimeout(2000);
const r = {};
const pageErrors = [];
page.on('pageerror', e => pageErrors.push(e.message));
const check = async (name, fn) => {
  try { r[name] = await fn(); }
  catch (e) { r[name] = 'ERR: ' + e.message.split('\n')[0]; }
};
// Scope every locator to a container; re-call these after any re-render.
const inBox = (container, role, name) =>
  page.locator(container).getByRole(role, { name, exact: typeof name === 'string' });
const visibleScreens = () => page.evaluate(() =>
  [...document.querySelectorAll('.screen')].filter(s => !s.hidden).map(s => s.id));
const text = sel => page.locator(sel).innerText().then(t => t.replace(/\s+/g, ' ').trim());
// Load a preview state directly when the mockup supports ?state=.
const openState = async (url, state) => {
  await page.goto(state ? `${url}?state=${state}` : url);
};

// Example journey body:
// await page.setViewportSize({ width: 390, height: 844 });
// await check('openCapture', async () => {
//   await inBox('#pinned-list', 'button', 'Add expense').click();
//   return visibleScreens();
// });
// await check('pageErrors', () => pageErrors);
// return r;
