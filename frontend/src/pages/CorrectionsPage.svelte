<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { toast, toastError, fmtTime } from '../stores.js'
  import Modal from '../components/Modal.svelte'

  let corrections = []
  let filter = ''
  let returnModal = null
  let returnNote = ''

  onMount(load)

  async function load() {
    try {
      corrections = await api.corrections(filter || undefined)
    } catch (e) {
      toastError(e)
    }
  }

  async function approve(c) {
    try {
      const r = await api.review(c.id, 'APPROVE', '材料核对无误,予以更正')
      toast(`已通过并签发新证 ${r.issuedCertificateNumber},旧证 ${c.certificateNumber} 已作废`, 'success')
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitReturn() {
    try {
      await api.review(returnModal.id, 'RETURN', returnNote)
      toast('已退回,等待学员补件', 'success')
      returnModal = null
      returnNote = ''
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  function statusBadge(s) {
    if (s === 'PENDING') return { cls: 'amber', text: '待审核' }
    if (s === 'RETURNED') return { cls: 'red', text: '已退回待补件' }
    return { cls: 'green', text: '已通过' }
  }
</script>

<div class="card">
  <h2>姓名更正审核</h2>
  <p class="sub">核对学员提交的证明材料:材料不足退回补件;材料齐全则通过 —— 系统将在同一事务内作废旧证、签发新版本证书。</p>
  <div class="row" style="margin-bottom:12px">
    <label class="field">状态筛选
      <select bind:value={filter} on:change={load}>
        <option value="">全部</option>
        <option value="PENDING">待审核</option>
        <option value="RETURNED">已退回</option>
        <option value="APPROVED">已通过</option>
      </select>
    </label>
    <button class="btn" on:click={load}>刷新</button>
  </div>

  {#if corrections.length === 0}
    <p class="muted">暂无更正申请。</p>
  {:else}
    <table>
      <thead>
        <tr><th>申请</th><th>证书</th><th>姓名变更</th><th>原因与材料</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        {#each corrections as c}
          {@const badge = statusBadge(c.status)}
          <tr>
            <td>#{c.id}<br /><span class="muted">{fmtTime(c.submittedAt)}</span></td>
            <td class="mono">{c.certificateNumber}</td>
            <td>{c.studentNameOnCertificate} → <strong>{c.requestedName}</strong></td>
            <td style="max-width:300px">
              <div><span class="muted">原因:</span>{c.reason}</div>
              <div class="muted" style="white-space:pre-wrap">材料:{c.materials}</div>
              {#if c.reviewNote}<div style="color:#b45309">审核意见:{c.reviewNote}</div>{/if}
            </td>
            <td>
              <span class="badge {badge.cls}">{badge.text}</span>
              {#if c.issuedCertificateNumber}
                <div class="muted mono" style="margin-top:4px">新证 {c.issuedCertificateNumber}</div>
              {/if}
            </td>
            <td style="white-space:nowrap">
              {#if c.status === 'PENDING'}
                <button class="btn primary sm" on:click={() => approve(c)}>通过并签发新版</button>
                <button class="btn warn sm" on:click={() => { returnModal = c; returnNote = '' }}>退回补件</button>
              {:else}
                <span class="muted">已处理</span>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>

{#if returnModal}
  <Modal title={`退回补件 — 申请 #${returnModal.id}`} onClose={() => (returnModal = null)}>
    <p class="muted">请写明缺少的材料,学员补件后申请将重新进入待审核。</p>
    <label class="field" style="width:100%">退回原因 / 需补充的材料
      <textarea rows="3" bind:value={returnNote} placeholder="如:缺少公安机关出具的姓名变更证明"></textarea>
    </label>
    <div style="margin-top:10px">
      <button class="btn warn" disabled={!returnNote} on:click={submitReturn}>确认退回</button>
    </div>
  </Modal>
{/if}
