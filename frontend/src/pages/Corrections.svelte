<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  let corrections = [];
  let activeCerts = [];
  let err = '';
  let msg = '';

  // 学员提交表单
  let formCert = '';
  let formName = '';
  let formReason = '';
  let formMaterials = '';

  // 教务操作
  let noteById = {};
  let reviewer = '教务-王老师';

  // 补件重提
  let resubmitMaterials = {};

  onMount(load);

  async function load() {
    err = '';
    try {
      [corrections, activeCerts] = await Promise.all([
        api.listCorrections(),
        api.listCertificates()
      ]);
      activeCerts = activeCerts.filter((c) => c.status === 'ACTIVE');
    } catch (e) {
      err = e.message;
    }
  }

  async function submit() {
    err = ''; msg = '';
    try {
      const materials = formMaterials.split('\n').map((s) => s.trim()).filter(Boolean);
      await api.submitCorrection({
        certificateId: Number(formCert),
        requestedName: formName,
        reason: formReason,
        materials
      });
      msg = '申请已提交，等待教务审核';
      formName = ''; formReason = ''; formMaterials = '';
      await load();
    } catch (e) { err = e.message; }
  }

  async function doReturn(r) {
    err = ''; msg = '';
    try {
      await api.returnCorrection(r.id, { note: noteById[r.id] || '', reviewedBy: reviewer });
      msg = `申请 #${r.id} 已退回补件`;
      await load();
    } catch (e) { err = e.message; }
  }

  async function doApprove(r) {
    err = ''; msg = '';
    try {
      const res = await api.approveCorrection(r.id, { reviewedBy: reviewer });
      msg = `申请 #${r.id} 已通过：旧证作废，新证 ${res.newCertNumber} 已签发`;
      await load();
    } catch (e) { err = e.message; }
  }

  async function doReject(r) {
    err = ''; msg = '';
    try {
      await api.rejectCorrection(r.id, { note: noteById[r.id] || '', reviewedBy: reviewer });
      msg = `申请 #${r.id} 已拒绝`;
      await load();
    } catch (e) { err = e.message; }
  }

  async function doResubmit(r) {
    err = ''; msg = '';
    try {
      const materials = (resubmitMaterials[r.id] || '').split('\n').map((s) => s.trim()).filter(Boolean);
      await api.resubmitCorrection(r.id, {
        certificateId: r.certificateId,
        requestedName: r.requestedName,
        reason: r.reason,
        materials
      });
      msg = `申请 #${r.id} 已补充材料并重新提交`;
      await load();
    } catch (e) { err = e.message; }
  }

  function statusBadge(s) {
    return { PENDING: ['待审核', 'amber'], RETURNED: ['退回补件', 'red'], APPROVED: ['已通过', 'green'], REJECTED: ['已拒绝', 'gray'] }[s] || [s, 'gray'];
  }
</script>

<div class="card">
  <h2>提交姓名更正申请（学员）</h2>
  <div class="row">
    <div>
      <label>选择有效证书</label>
      <select bind:value={formCert} style="min-width:260px">
        <option value="">请选择</option>
        {#each activeCerts as c}
          <option value={String(c.id)}>{c.certNumber} ｜ {c.holderName} ｜ {c.courseTitle}</option>
        {/each}
      </select>
    </div>
    <div>
      <label>更正后姓名</label>
      <input bind:value={formName} style="width:130px" />
    </div>
    <div>
      <label>更正原因</label>
      <input bind:value={formReason} style="width:240px" placeholder="如：同音错字，与身份证不符" />
    </div>
  </div>
  <div style="margin-top:10px">
    <label>证明材料（每行一项，如：身份证扫描件）</label>
    <textarea rows="2" bind:value={formMaterials} style="width:100%;max-width:640px"></textarea>
  </div>
  <div style="margin-top:10px">
    <button on:click={submit} disabled={!formCert || !formName || !formReason}>提交申请</button>
  </div>
  {#if msg}<div class="msg-ok">{msg}</div>{/if}
  {#if err}<div class="msg-err">{err}</div>{/if}
</div>

<div class="card">
  <h2>申请列表（教务审核）</h2>
  <div class="row" style="margin-bottom:10px">
    <label style="margin:0">审核人</label>
    <input bind:value={reviewer} style="width:140px" />
  </div>
  {#each corrections as r}
    {@const [label, color] = statusBadge(r.status)}
    <div class="corr-item">
      <div class="row" style="justify-content:space-between">
        <div>
          <strong>#{r.id}</strong>
          <a href="#/certificates/{r.certificateId}">{r.certNumber}</a>
          <span>{r.currentName} → <strong>{r.requestedName}</strong></span>
          <span class="badge {color}">{label}</span>
          <span class="muted">提交 {r.submitCount} 次 ｜ {fmtTime(r.createdAt)}</span>
        </div>
      </div>
      <div class="muted" style="margin-top:4px">原因：{r.reason}</div>
      <div class="muted">材料：
        {#if r.materials && r.materials.length}{r.materials.join('、')}{:else}未提交{/if}
      </div>
      {#if r.reviewNote}<div class="muted">审核意见（{r.reviewedBy}）：{r.reviewNote}</div>{/if}
      {#if r.newCertNumber}
        <div class="muted">已换发新证：<a href="#/verify/{r.newCertNumber}">{r.newCertNumber}</a></div>
      {/if}

      {#if r.status === 'PENDING'}
        <div class="row" style="margin-top:8px">
          <input bind:value={noteById[r.id]} placeholder="审核意见（退回/拒绝时必填）" style="flex:1;min-width:220px" />
          <button on:click={() => doApprove(r)}>通过并换发</button>
          <button class="warn" on:click={() => doReturn(r)}>退回补件</button>
          <button class="danger" on:click={() => doReject(r)}>拒绝</button>
        </div>
      {/if}
      {#if r.status === 'RETURNED'}
        <div class="row" style="margin-top:8px">
          <input bind:value={resubmitMaterials[r.id]} placeholder="补充材料（用换行分隔多项）" style="flex:1;min-width:220px" />
          <button class="ghost" on:click={() => doResubmit(r)}>补件并重新提交</button>
        </div>
      {/if}
    </div>
  {:else}
    <p class="muted">暂无更正申请</p>
  {/each}
</div>

<style>
  .corr-item { border: 1px solid #eef1f6; border-radius: 10px; padding: 12px 14px; margin-bottom: 10px; }
</style>
