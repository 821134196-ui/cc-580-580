<script>
  import { api } from '../api.js'
  import { fmtTime } from '../stores.js'

  let number = ''
  let result = null
  let notFound = false
  let searching = false

  async function search() {
    if (!number.trim()) return
    searching = true
    result = null
    notFound = false
    try {
      result = await api.verify(number.trim())
    } catch (e) {
      if (e.status === 404) {
        notFound = true
      } else {
        notFound = true
      }
    } finally {
      searching = false
    }
  }
</script>

<div class="card">
  <h2>证书核验(对外开放)</h2>
  <p class="sub">任何人均可凭证书编号核验证书真伪与当前状态。已作废的证书会明确显示无效及作废原因。</p>
  <div class="row">
    <label class="field" style="flex:1;min-width:280px">证书编号
      <input class="mono" bind:value={number} placeholder="CERT-2026-000001"
        on:keydown={(e) => e.key === 'Enter' && search()} />
    </label>
    <button class="btn primary" disabled={searching || !number.trim()} on:click={search}>
      {searching ? '核验中…' : '核验'}
    </button>
  </div>
</div>

{#if notFound}
  <div class="card result invalid">
    <div class="verdict">✕ 核验未通过</div>
    <p>未查询到编号为 <span class="mono">{number}</span> 的证书,证书不存在或编号有误。</p>
  </div>
{/if}

{#if result}
  <div class="card result" class:valid={result.valid} class:invalid={!result.valid}>
    <div class="verdict">{result.valid ? '✓ 证书有效' : '✕ 证书已作废'}</div>
    <p>{result.message}</p>
    <div class="grid">
      <div><span class="muted">证书编号</span><br /><span class="mono">{result.certificateNumber}</span></div>
      <div><span class="muted">持证人</span><br /><strong>{result.studentName}</strong></div>
      <div><span class="muted">课程</span><br />{result.courseTitle} <span class="muted mono">{result.courseCode}</span></div>
      <div><span class="muted">版本</span><br />第 {result.version} 版</div>
      <div><span class="muted">签发时间</span><br />{fmtTime(result.issuedAt)}</div>
      {#if result.voidedAt}
        <div><span class="muted">作废时间</span><br />{fmtTime(result.voidedAt)}</div>
      {/if}
      {#if result.voidReason}
        <div style="grid-column:1/-1"><span class="muted">作废原因</span><br />{result.voidReason}</div>
      {/if}
      {#if result.replacedByNumber}
        <div style="grid-column:1/-1">
          <span class="muted">替代新证</span><br />
          <button class="btn sm" on:click={() => { number = result.replacedByNumber; search() }}>
            查看 {result.replacedByNumber} →
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .result { border-left: 6px solid; }
  .result.valid { border-color: #16a34a; background: #f0fdf4; }
  .result.invalid { border-color: #dc2626; background: #fef2f2; }
  .verdict { font-size: 20px; font-weight: 700; margin-bottom: 6px; }
  .result.valid .verdict { color: #15803d; }
  .result.invalid .verdict { color: #b91c1c; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; font-size: 13px; margin-top: 10px; }
</style>
