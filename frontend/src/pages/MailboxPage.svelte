<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { toastError, fmtTime } from '../stores.js'

  let emails = []

  onMount(load)

  async function load() {
    try {
      emails = await api.emails()
    } catch (e) {
      toastError(e)
    }
  }
</script>

<div class="card">
  <h2>模拟邮箱</h2>
  <p class="sub">系统不真正发送邮件,所有回执落库并在此展示(签发通知、退回补件通知、更正完成通知)。</p>
  <button class="btn sm" on:click={load}>刷新</button>
  {#if emails.length === 0}
    <p class="muted" style="margin-top:12px">暂无邮件。</p>
  {:else}
    <table style="margin-top:12px">
      <thead>
        <tr><th>时间</th><th>收件人</th><th>主题</th><th>正文</th></tr>
      </thead>
      <tbody>
        {#each emails as m}
          <tr>
            <td class="muted" style="white-space:nowrap">{fmtTime(m.sentAt)}</td>
            <td>{m.recipient}</td>
            <td>{m.subject}</td>
            <td class="muted" style="white-space:pre-wrap;max-width:420px">{m.body}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>
