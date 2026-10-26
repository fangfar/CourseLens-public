(() => {
  const buttons = [...document.querySelectorAll('[data-download]')];
  const feedback = document.querySelector('.download-feedback');
  const status = document.querySelector('[data-download-status]');
  let busy = false;

  feedback.querySelector('button').addEventListener('click', () => {
    feedback.hidden = true;
  });

  async function download(event, edition) {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button > 0) return;
    event.preventDefault();
    if (busy) return;
    busy = true;
    feedback.hidden = false;
    status.textContent = '正在获取最新版下载地址…';
    buttons.forEach(button => {
      button.setAttribute('aria-disabled', 'true');
      button.setAttribute('aria-busy', 'true');
    });
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 12000);
    try {
      const response = await fetch('remote/config.json', {
        headers: { Accept: 'application/json' },
        credentials: 'omit',
        cache: 'no-cache',
        signal: controller.signal,
      });
      if (!response.ok) throw new Error('Configuration request failed');
      const config = await response.json();
      const link = edition === 'full' ? config.android?.fullApkUrl : edition === 'lite' ? config.android?.liteApkUrl : config.android?.apkUrl;
      if (config.schemaVersion !== 1 || typeof link !== 'string' || !link) throw new Error('Edition not published');
      const url = new URL(link);
      if (url.protocol !== 'https:' || url.hostname !== 'github.com' || url.port
          || url.username || url.password || url.search || url.hash
          || !url.pathname.startsWith('/fangfar/Morrow/releases/download/')
          || !url.pathname.endsWith('.apk')) throw new Error('Invalid APK URL');
      window.location.assign(url.href);
      status.textContent = '已请求下载最新正式版。若未开始，可重试或手动下载。';
    } catch (error) {
      status.textContent = error.message === 'Edition not published'
        ? '所选版本尚未发布，请前往 GitHub 查看可用版本并手动下载。'
        : '暂时无法获取最新版本，请重试或前往 GitHub 手动下载。';
    } finally {
      clearTimeout(timeout);
      busy = false;
      buttons.forEach(button => {
        button.removeAttribute('aria-disabled');
        button.removeAttribute('aria-busy');
      });
    }
  }
  buttons.forEach(button => button.addEventListener('click', event => download(event, button.getAttribute('data-download'))));
})();
