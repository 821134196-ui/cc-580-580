<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { toastError, fmtTime } from '../stores.js'

  let enrollments = []
  let selectedId = ''
  let chain = []

  onMount(async () => {
    try {
      const all = await api.enrollments()
      enrollments = all
      const withCert = all.find((e) => e.eligibility.hasValidCertificate)
      if (withCert) {
        selectedId = String(withCert.id)
        await load()
      }
    } catch (e) {
      toastError(e)
    }
  })

  async function load() {
    if (!selectedId) return
    try {
      chain = await api.certificateChain(Number(selectedId))
    } catch (e) {
      toastError(e)
    }
  }

  $: current = enrollments.find((e) => String(e.id) === selectedId)
</script>

<div class="card">
  <h2>证书版本链</h2>
  <p class="sub">同一报名下的全部证书版本:绿色为当前有效证书,灰色划线为已作废版本;每次姓名更正都会留下原因与材料记录。</p>
  <div class="row">
    <label class="field" style="min-width:360px">选择报名记录
      <select bind:value={selectedId} on:change={load}>
        <option value="">请选择</option>
        {#each enrollments as e}
          <option value={e.id}>{e.studentName} · {e.courseTitle}({e.courseCode})</option>
        {/each}
      </select>
    </label>
  </div>
</div>

{#if selectedId && chain.length === 0}
  <div class="card"><p class="muted">该报名尚未签发任何证书。</p></div>
{/if}

{#each chain as cert, i}
  <div class="card version" class:voided={cert.status === 'VOID'}>
    <div class="version-head">
      <div>
        <span class="vtag">v{cert.version}</span>
        <span class="mono num">{cert.certificateNumber}</span>
        {#if cert.status === 'VALID'}
          <span class="badge green">当前有效证书</span>
        {:else}
          <span class="badge red">已作废</span>
        {/if}
      </div>
      <span class="muted">签发于 {fmtTime(cert.issuedAt)}</span>
    </div>

    <div class="grid">
      <div><span class="muted">证书姓名</span><br /><strong>{cert.studentName}</strong></div>
      <div><span class="muted">课程</span><br />{cert.courseTitle} <span class="muted mono">{cert.courseCode}</span></div>
      <div><span class="muted">成绩快照</span><br />{cert.score} 分</div>
      <div><span class="muted">学时快照</span><br />{cert.completedHours} 学时</div>
    </div>

    {#if cert.status === 'VOID'}
      <div class="void-box">
        <strong>作废信息</strong> · {fmtTime(cert.voidedAt)}<br />
        {cert.voidReason || '原因未记录'}
      </div>
    {/if}

    {#if cert.replacesCertificateNumber}
      <p class="muted">替代旧证:<span class="mono">{cert.replacesCertificateNumber}</span></p>
    {/if}

    {#if cert.producedByCorrection}
      <div class="corr-box">
        <strong>本版由姓名更正产生</strong>(申请 #{cert.producedByCorrection.id} · {fmtTime(cert.producedByCorrection.submittedAt)} 提交)
        <div><span class="muted">更正原因:</span>{cert.producedByCorrection.reason}</div>
        <div class="muted" style="white-space:pre-wrap">更正材料:{cert.producedByCorrection.materials}</div>
        {#if cert.producedByCorrection.reviewNote}
          <div><span class="muted">审核意见:</span>{cert.producedByCorrection.reviewNote}</div>
        {/if}
      </div>
    {/if}
  </div>
{/each}

<style>
  .version { border-left: 5px solid #16a34a; }
  .version.voided { border-left-color: #dc2626; opacity: 0.88; }
  .version.voided .num { text-decoration: line-through; color: #9ca3af; }
  .version-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
  .vtag {
    background: #1e3a8a; color: #fff; font-weight: 700; font-size: 12px;
    border-radius: 6px; padding: 3px 8px; margin-right: 8px;
  }
  .num { font-size: 15px; margin-right: 10px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; font-size: 13px; }
  .void-box {
    margin-top: 12px; background: #fef2f2; border: 1px solid #fecaca; color: #991b1b;
    border-radius: 8px; padding: 10px 12px; font-size: 13px;
  }
  .corr-box {
    margin-top: 12px; background: #eff6ff; border: 1px solid #bfdbfe;
    border-radius: 8px; padding: 10px 12px; font-size: 13px;
  }
</style>
