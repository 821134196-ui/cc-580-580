<script>
  import { toasts } from './stores.js'
  import StaffPage from './pages/StaffPage.svelte'
  import StudentPage from './pages/StudentPage.svelte'
  import CorrectionsPage from './pages/CorrectionsPage.svelte'
  import VersionsPage from './pages/VersionsPage.svelte'
  import VerifyPage from './pages/VerifyPage.svelte'
  import MailboxPage from './pages/MailboxPage.svelte'

  const tabs = [
    { id: 'staff', label: '教务工作台', component: StaffPage, group: '教务' },
    { id: 'corrections', label: '更正审核', component: CorrectionsPage, group: '教务' },
    { id: 'student', label: '学员中心', component: StudentPage, group: '学员' },
    { id: 'versions', label: '证书版本', component: VersionsPage, group: '公开' },
    { id: 'verify', label: '证书核验', component: VerifyPage, group: '公开' },
    { id: 'mailbox', label: '模拟邮箱', component: MailboxPage, group: '公开' }
  ]

  let active = 'staff'
  $: current = tabs.find((t) => t.id === active)
</script>

<header>
  <div class="brand">
    <span class="logo">证</span>
    <div>
      <h1>课程证书签发与姓名更正系统</h1>
      <p>培训平台教务管理 · Quarkus + SQLite + Svelte</p>
    </div>
  </div>
  <nav>
    {#each tabs as tab}
      <button class:active={active === tab.id} on:click={() => (active = tab.id)}>
        <span class="group">{tab.group}</span>{tab.label}
      </button>
    {/each}
  </nav>
</header>

<main>
  {#key active}
    <svelte:component this={current.component} />
  {/key}
</main>

<div class="toasts">
  {#each $toasts as t (t.id)}
    <div class="toast {t.kind}">{t.message}</div>
  {/each}
</div>

<style>
  :global(*) { box-sizing: border-box; }
  :global(body) {
    margin: 0;
    font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', system-ui, sans-serif;
    background: #f3f5f9;
    color: #1f2937;
  }
  :global(button) { font-family: inherit; }
  :global(input), :global(select), :global(textarea) { font-family: inherit; }

  header {
    background: linear-gradient(120deg, #1e3a8a, #2563eb);
    color: #fff;
    padding: 18px 28px 0;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.25);
  }
  .brand { display: flex; align-items: center; gap: 14px; }
  .logo {
    width: 46px; height: 46px; border-radius: 12px;
    background: rgba(255, 255, 255, 0.16);
    display: flex; align-items: center; justify-content: center;
    font-size: 24px; font-weight: 700;
  }
  h1 { font-size: 19px; margin: 0; }
  .brand p { margin: 2px 0 0; font-size: 12px; opacity: 0.75; }
  nav { display: flex; gap: 4px; margin-top: 14px; flex-wrap: wrap; }
  nav button {
    border: none; background: transparent; color: rgba(255, 255, 255, 0.82);
    padding: 10px 16px; font-size: 14px; cursor: pointer;
    border-bottom: 3px solid transparent; border-radius: 6px 6px 0 0;
  }
  nav button:hover { background: rgba(255, 255, 255, 0.1); }
  nav button.active { background: #f3f5f9; color: #1e3a8a; font-weight: 600; border-bottom-color: #2563eb; }
  nav .group {
    font-size: 10px; background: rgba(255, 255, 255, 0.18); border-radius: 4px;
    padding: 1px 5px; margin-right: 6px; vertical-align: 1px;
  }
  nav button.active .group { background: #dbeafe; color: #1d4ed8; }

  main { max-width: 1180px; margin: 22px auto; padding: 0 20px 40px; }

  :global(.card) {
    background: #fff; border-radius: 12px; padding: 20px 22px;
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08); margin-bottom: 18px;
  }
  :global(.card h2) { margin: 0 0 4px; font-size: 16px; }
  :global(.card .sub) { margin: 0 0 14px; font-size: 12px; color: #6b7280; }
  :global(table) { width: 100%; border-collapse: collapse; font-size: 13px; }
  :global(th) {
    text-align: left; padding: 8px 10px; color: #6b7280; font-weight: 600;
    border-bottom: 2px solid #e5e7eb; white-space: nowrap;
  }
  :global(td) { padding: 9px 10px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }
  :global(tr:hover td) { background: #f8fafc; }

  :global(.btn) {
    border: 1px solid #d1d5db; background: #fff; color: #374151;
    border-radius: 8px; padding: 6px 12px; font-size: 13px; cursor: pointer;
  }
  :global(.btn:hover) { background: #f9fafb; }
  :global(.btn.primary) { background: #2563eb; border-color: #2563eb; color: #fff; }
  :global(.btn.primary:hover) { background: #1d4ed8; }
  :global(.btn.danger) { background: #dc2626; border-color: #dc2626; color: #fff; }
  :global(.btn.warn) { background: #d97706; border-color: #d97706; color: #fff; }
  :global(.btn:disabled) { opacity: 0.45; cursor: not-allowed; }
  :global(.btn.sm) { padding: 3px 9px; font-size: 12px; border-radius: 6px; }

  :global(.badge) {
    display: inline-block; padding: 2px 9px; border-radius: 999px;
    font-size: 12px; font-weight: 600; white-space: nowrap;
  }
  :global(.badge.green) { background: #dcfce7; color: #15803d; }
  :global(.badge.red) { background: #fee2e2; color: #b91c1c; }
  :global(.badge.amber) { background: #fef3c7; color: #b45309; }
  :global(.badge.gray) { background: #e5e7eb; color: #4b5563; }
  :global(.badge.blue) { background: #dbeafe; color: #1d4ed8; }

  :global(input), :global(select), :global(textarea) {
    border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 10px; font-size: 13px;
    background: #fff;
  }
  :global(input:focus), :global(select:focus), :global(textarea:focus) {
    outline: 2px solid #bfdbfe; border-color: #2563eb;
  }
  :global(label.field) { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: #4b5563; }
  :global(.row) { display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end; }
  :global(.muted) { color: #6b7280; font-size: 12px; }
  :global(.mono) { font-family: ui-monospace, 'SF Mono', Consolas, monospace; }

  .toasts { position: fixed; right: 18px; bottom: 18px; display: flex; flex-direction: column; gap: 8px; z-index: 99; }
  .toast {
    padding: 10px 16px; border-radius: 10px; font-size: 13px; color: #fff;
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.25); max-width: 420px;
    animation: slide-in 0.18s ease-out;
  }
  .toast.info { background: #1f2937; }
  .toast.success { background: #15803d; }
  .toast.error { background: #b91c1c; }
  @keyframes slide-in { from { transform: translateY(8px); opacity: 0; } }
</style>
