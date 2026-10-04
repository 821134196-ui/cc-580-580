<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  let mails = [];
  let err = '';
  let open = null;

  onMount(load);

  async function load() {
    try {
      mails = await api.listMails();
    } catch (e) {
      err = e.message;
    }
  }
</script>

<div class="card">
  <h2>邮件回执（本地模拟发件箱）</h2>
  <p class="muted">系统不真正发送邮件，所有回执写入本地数据库，在此查看。</p>
  {#if err}<div class="msg-err">{err}</div>{/if}
  <table>
    <thead><tr><th>时间</th><th>收件人</th><th>主题</th><th>关联证书</th><th></th></tr></thead>
    <tbody>
      {#each mails as m}
        <tr>
          <td>{fmtTime(m.sentAt)}</td>
          <td>{m.recipient}</td>
          <td>{m.subject}</td>
          <td>{#if m.certNumber}<a href="#/verify/{m.certNumber}">{m.certNumber}</a>{:else}—{/if}</td>
          <td><button class="ghost" on:click={() => (open = open === m.id ? null : m.id)}>{open === m.id ? '收起' : '查看'}</button></td>
        </tr>
        {#if open === m.id}
          <tr><td colspan="5"><pre class="mail-body">{m.body}</pre></td></tr>
        {/if}
      {:else}
        <tr><td colspan="5" class="muted">暂无邮件</td></tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .mail-body {
    background: #f8fafc; border-radius: 8px; padding: 12px 16px;
    font-size: 13px; white-space: pre-wrap; margin: 4px 0;
  }
</style>
