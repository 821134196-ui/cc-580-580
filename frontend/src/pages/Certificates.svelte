<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  let certs = [];
  let filter = 'ALL';
  let err = '';

  onMount(load);

  async function load() {
    try {
      certs = await api.listCertificates();
    } catch (e) {
      err = e.message;
    }
  }

  $: shown = certs.filter((c) => filter === 'ALL' || c.status === filter);
</script>

<div class="card">
  <h2>证书管理</h2>
  <div class="row" style="margin-bottom:12px">
    <label style="margin:0">状态筛选</label>
    <select bind:value={filter}>
      <option value="ALL">全部</option>
      <option value="ACTIVE">当前有效</option>
      <option value="REVOKED">已作废</option>
    </select>
    <span class="muted">共 {shown.length} 张</span>
  </div>
  {#if err}<div class="msg-err">{err}</div>{/if}
  <table>
    <thead>
      <tr><th>证书编号</th><th>持证人</th><th>课程</th><th>版本</th><th>状态</th><th>签发时间</th><th>签发人</th><th></th></tr>
    </thead>
    <tbody>
      {#each shown as c}
        <tr>
          <td><a href="#/certificates/{c.id}">{c.certNumber}</a></td>
          <td>{c.holderName}</td>
          <td>{c.courseTitle}</td>
          <td>V{c.version}</td>
          <td>
            {#if c.status === 'ACTIVE'}
              <span class="badge green">当前有效</span>
            {:else}
              <span class="badge red">已作废</span>
            {/if}
          </td>
          <td>{fmtTime(c.issuedAt)}</td>
          <td>{c.issuedBy}</td>
          <td><a class="btn-link" href="#/verify/{c.certNumber}">核验</a></td>
        </tr>
      {:else}
        <tr><td colspan="8" class="muted">暂无证书</td></tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
  .btn-link { color: #2563eb; font-size: 13px; }
</style>
