<script>
  import { onMount } from 'svelte';
  import Admin from './pages/Admin.svelte';
  import Certificates from './pages/Certificates.svelte';
  import CertificateDetail from './pages/CertificateDetail.svelte';
  import Corrections from './pages/Corrections.svelte';
  import Verify from './pages/Verify.svelte';
  import Mails from './pages/Mails.svelte';

  let route = { page: 'admin', param: null };

  function parseHash() {
    const hash = location.hash.replace(/^#/, '') || '/';
    const parts = hash.split('/').filter(Boolean);
    if (parts[0] === 'certificates' && parts[1]) {
      route = { page: 'cert-detail', param: parts[1] };
    } else if (parts[0] === 'certificates') {
      route = { page: 'certificates', param: null };
    } else if (parts[0] === 'corrections') {
      route = { page: 'corrections', param: null };
    } else if (parts[0] === 'verify') {
      route = { page: 'verify', param: parts[1] || null };
    } else if (parts[0] === 'mails') {
      route = { page: 'mails', param: null };
    } else {
      route = { page: 'admin', param: null };
    }
  }

  onMount(() => {
    parseHash();
    window.addEventListener('hashchange', parseHash);
    return () => window.removeEventListener('hashchange', parseHash);
  });

  const nav = [
    { hash: '#/', label: '教务工作台', page: 'admin' },
    { hash: '#/certificates', label: '证书管理', page: 'certificates' },
    { hash: '#/corrections', label: '更正申请', page: 'corrections' },
    { hash: '#/verify', label: '证书核验', page: 'verify' },
    { hash: '#/mails', label: '邮件回执', page: 'mails' }
  ];
</script>

<div class="shell">
  <header>
    <div class="brand">
      <span class="logo">证</span>
      <div>
        <h1>在线课程证书平台</h1>
        <p>签发 · 姓名更正 · 对外核验</p>
      </div>
    </div>
    <nav>
      {#each nav as item}
        <a href={item.hash} class:active={route.page === item.page || (item.page === 'certificates' && route.page === 'cert-detail')}>{item.label}</a>
      {/each}
    </nav>
  </header>

  <main>
    {#if route.page === 'admin'}
      <Admin />
    {:else if route.page === 'certificates'}
      <Certificates />
    {:else if route.page === 'cert-detail'}
      <CertificateDetail id={route.param} />
    {:else if route.page === 'corrections'}
      <Corrections />
    {:else if route.page === 'verify'}
      <Verify presetNumber={route.param} />
    {:else if route.page === 'mails'}
      <Mails />
    {/if}
  </main>

  <footer>本地演示环境 · 邮件为模拟发送 · 数据存储于 SQLite</footer>
</div>

<style>
  :global(*) { box-sizing: border-box; }
  :global(body) {
    margin: 0;
    font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', system-ui, sans-serif;
    background: #f3f5f9;
    color: #1f2937;
  }
  .shell { min-height: 100vh; display: flex; flex-direction: column; }
  header {
    background: linear-gradient(120deg, #1e3a8a, #2563eb);
    color: #fff;
    padding: 18px 32px 0;
  }
  .brand { display: flex; align-items: center; gap: 14px; }
  .logo {
    width: 46px; height: 46px; border-radius: 12px;
    background: rgba(255,255,255,.16);
    display: flex; align-items: center; justify-content: center;
    font-size: 24px; font-weight: 700;
  }
  .brand h1 { font-size: 20px; margin: 0; }
  .brand p { margin: 2px 0 0; font-size: 12px; opacity: .75; }
  nav { display: flex; gap: 4px; margin-top: 14px; }
  nav a {
    color: rgba(255,255,255,.82); text-decoration: none;
    padding: 10px 18px; border-radius: 10px 10px 0 0; font-size: 14px;
  }
  nav a:hover { background: rgba(255,255,255,.12); }
  nav a.active { background: #f3f5f9; color: #1e3a8a; font-weight: 600; }
  main { flex: 1; padding: 24px 32px; max-width: 1280px; width: 100%; margin: 0 auto; }
  footer { text-align: center; color: #9ca3af; font-size: 12px; padding: 18px; }

  :global(.card) {
    background: #fff; border-radius: 14px; padding: 20px 22px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, .08);
    margin-bottom: 18px;
  }
  :global(.card h2) { margin: 0 0 14px; font-size: 17px; }
  :global(.card h3) { margin: 18px 0 10px; font-size: 15px; }
  :global(table) { width: 100%; border-collapse: collapse; font-size: 13.5px; }
  :global(th) { text-align: left; color: #6b7280; font-weight: 600; padding: 8px 10px; border-bottom: 2px solid #eef1f6; }
  :global(td) { padding: 9px 10px; border-bottom: 1px solid #f1f4f9; }
  :global(tr:hover td) { background: #f8fafc; }
  :global(button) {
    font: inherit; cursor: pointer; border: none; border-radius: 8px;
    padding: 7px 14px; background: #2563eb; color: #fff; font-size: 13px;
  }
  :global(button:hover) { background: #1d4ed8; }
  :global(button:disabled) { background: #cbd5e1; cursor: not-allowed; }
  :global(button.ghost) { background: #eef2ff; color: #1e40af; }
  :global(button.ghost:hover) { background: #e0e7ff; }
  :global(button.danger) { background: #dc2626; }
  :global(button.warn) { background: #d97706; }
  :global(input), :global(select), :global(textarea) {
    font: inherit; padding: 7px 10px; border: 1px solid #d4d9e3; border-radius: 8px;
    font-size: 13.5px; background: #fff;
  }
  :global(input:focus), :global(select:focus), :global(textarea:focus) {
    outline: 2px solid #93c5fd; border-color: #2563eb;
  }
  :global(label) { font-size: 13px; color: #4b5563; display: block; margin-bottom: 4px; }
  :global(.badge) {
    display: inline-block; padding: 2px 10px; border-radius: 999px;
    font-size: 12px; font-weight: 600;
  }
  :global(.badge.green) { background: #dcfce7; color: #15803d; }
  :global(.badge.red) { background: #fee2e2; color: #b91c1c; }
  :global(.badge.gray) { background: #e5e7eb; color: #4b5563; }
  :global(.badge.amber) { background: #fef3c7; color: #b45309; }
  :global(.badge.blue) { background: #dbeafe; color: #1d4ed8; }
  :global(.muted) { color: #6b7280; font-size: 12.5px; }
  :global(.row) { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
  :global(.msg-ok) { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; padding: 10px 14px; border-radius: 10px; font-size: 13px; margin: 10px 0; }
  :global(.msg-err) { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; padding: 10px 14px; border-radius: 10px; font-size: 13px; margin: 10px 0; }
</style>
