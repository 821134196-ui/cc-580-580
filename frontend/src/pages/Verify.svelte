<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  export let presetNumber = null;

  let number = '';
  let result = null;
  let err = '';
  let loading = false;

  onMount(() => {
    if (presetNumber) {
      number = presetNumber;
      doVerify();
    }
  });

  async function doVerify() {
    if (!number.trim()) return;
    result = null; err = ''; loading = true;
    try {
      result = await api.verify(number.trim());
    } catch (e) {
      err = e.message;
    } finally {
      loading = false;
    }
  }
</script>

<div class="card">
  <h2>证书核验（对外开放）</h2>
  <p class="muted">输入证书编号即可核验真伪与当前状态，无需登录。</p>
  <div class="row">
    <input bind:value={number} placeholder="如 CERT-2026-000002" style="width:260px"
      on:keydown={(e) => e.key === 'Enter' && doVerify()} />
    <button on:click={doVerify} disabled={loading || !number.trim()}>{loading ? '核验中…' : '核验'}</button>
  </div>
  {#if err}<div class="msg-err">{err}</div>{/if}
</div>

{#if result}
  {#if !result.found}
    <div class="card verdict invalid">
      <h2>✕ 未查询到该证书</h2>
      <p>{result.message}</p>
      <p class="muted">编号：{result.certNumber}</p>
    </div>
  {:else if result.status === 'ACTIVE'}
    <div class="card verdict valid">
      <h2>✓ 证书有效</h2>
      <table>
        <tbody>
          <tr><td class="muted" style="width:120px">证书编号</td><td>{result.certNumber}（V{result.version}）</td></tr>
          <tr><td class="muted">持证人</td><td>{result.holderName}</td></tr>
          <tr><td class="muted">课程</td><td>{result.courseTitle}（{result.courseCode}）</td></tr>
          <tr><td class="muted">签发时间</td><td>{fmtTime(result.issuedAt)}</td></tr>
        </tbody>
      </table>
    </div>
  {:else}
    <div class="card verdict revoked">
      <h2>⚠ 证书已作废</h2>
      <table>
        <tbody>
          <tr><td class="muted" style="width:120px">证书编号</td><td>{result.certNumber}（V{result.version}）</td></tr>
          <tr><td class="muted">原持证人</td><td>{result.holderName}</td></tr>
          <tr><td class="muted">课程</td><td>{result.courseTitle}（{result.courseCode}）</td></tr>
          <tr><td class="muted">作废时间</td><td>{fmtTime(result.revokedAt)}</td></tr>
          <tr><td class="muted">作废原因</td><td>{result.revokeReason || '—'}</td></tr>
          {#if result.replacedByNumber}
            <tr>
              <td class="muted">替代证书</td>
              <td><a href="#/verify/{result.replacedByNumber}">{result.replacedByNumber}</a>（点击核验新证）</td>
            </tr>
          {/if}
        </tbody>
      </table>
    </div>
  {/if}
{/if}

<style>
  .verdict h2 { margin-top: 0; }
  .verdict.valid { border: 2px solid #86efac; }
  .verdict.valid h2 { color: #15803d; }
  .verdict.revoked { border: 2px solid #fca5a5; }
  .verdict.revoked h2 { color: #b91c1c; }
  .verdict.invalid { border: 2px solid #d1d5db; }
  .verdict.invalid h2 { color: #4b5563; }
</style>
