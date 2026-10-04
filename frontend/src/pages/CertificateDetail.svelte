<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  export let id;

  let chain = [];
  let corrections = [];
  let err = '';

  onMount(load);

  async function load() {
    err = '';
    try {
      chain = await api.certificateChain(id);
      const all = await api.listCorrections();
      const certIds = new Set(chain.map((c) => c.id));
      corrections = all.filter((r) => certIds.has(r.certificateId));
    } catch (e) {
      err = e.message;
    }
  }

  $: current = chain.find((c) => c.status === 'ACTIVE') || chain[chain.length - 1];
</script>

{#if err}<div class="msg-err">{err}</div>{/if}

{#if current}
  <div class="card cert-card" class:revoked={current.status !== 'ACTIVE'}>
    <div class="cert-head">
      <div>
        <div class="muted">证书编号</div>
        <div class="cert-no">{current.certNumber}</div>
      </div>
      {#if current.status === 'ACTIVE'}
        <span class="badge green big">当前有效</span>
      {:else}
        <span class="badge red big">已作废</span>
      {/if}
    </div>
    <div class="cert-body">
      <div><span class="muted">持证人</span><strong>{current.holderName}</strong></div>
      <div><span class="muted">课程</span><strong>{current.courseTitle}（{current.courseCode}）</strong></div>
      <div><span class="muted">完成学时</span><strong>{current.completedHours} / {current.requiredHours} 学时</strong></div>
      <div><span class="muted">考试成绩</span><strong>{current.finalScore} 分（合格线 {current.passScore}）</strong></div>
      <div><span class="muted">版本</span><strong>V{current.version}</strong></div>
      <div><span class="muted">签发时间</span><strong>{fmtTime(current.issuedAt)}</strong></div>
      <div><span class="muted">签发人</span><strong>{current.issuedBy}</strong></div>
    </div>
    <div class="row">
      <a href="#/verify/{current.certNumber}"><button class="ghost">前往核验</button></a>
      <a href="#/certificates"><button class="ghost">返回列表</button></a>
    </div>
  </div>

  <div class="card">
    <h2>版本链（{chain.length} 个版本）</h2>
    <p class="muted">姓名更正通过"作废旧证 + 签发新证"完成，旧证永久作废、不可重新激活。</p>
    <div class="timeline">
      {#each chain as c}
        <div class="t-item" class:t-active={c.status === 'ACTIVE'}>
          <div class="t-dot"></div>
          <div class="t-body">
            <div class="row" style="justify-content:space-between">
              <div>
                <a href="#/certificates/{c.id}"><strong>{c.certNumber}</strong></a>
                <span class="badge blue">V{c.version}</span>
                {#if c.status === 'ACTIVE'}
                  <span class="badge green">当前有效</span>
                {:else}
                  <span class="badge red">已作废</span>
                {/if}
              </div>
              <span class="muted">{fmtTime(c.issuedAt)}</span>
            </div>
            <div class="muted">持证人：{c.holderName} ｜ 签发人：{c.issuedBy}</div>
            {#if c.status === 'REVOKED'}
              <div class="revoke-box">
                <div>作废时间：{fmtTime(c.revokedAt)}</div>
                <div>作废原因：{c.revokeReason || '—'}</div>
                {#if c.replacedByNumber}
                  <div>替代证书：<a href="#/verify/{c.replacedByNumber}">{c.replacedByNumber}</a></div>
                {/if}
              </div>
            {/if}
            {#each corrections.filter((r) => r.certificateId === c.id) as r}
              <div class="corr-box">
                <div><strong>更正申请 #{r.id}</strong>：{r.currentName} → {r.requestedName}
                  <span class="badge amber">{r.status}</span>
                </div>
                <div>原因：{r.reason}</div>
                <div>证明材料：
                  {#if r.materials && r.materials.length}
                    <ul>{#each r.materials as m}<li>{m}</li>{/each}</ul>
                  {:else}
                    <span class="muted">未提交（被退回补件）</span>
                  {/if}
                </div>
                {#if r.reviewNote}<div>审核意见：{r.reviewNote}</div>{/if}
                {#if r.newCertNumber}<div>换发新证：<a href="#/verify/{r.newCertNumber}">{r.newCertNumber}</a></div>{/if}
              </div>
            {/each}
          </div>
        </div>
      {/each}
    </div>
  </div>
{/if}

<style>
  .cert-card { border: 2px solid #bbf7d0; }
  .cert-card.revoked { border-color: #fecaca; }
  .cert-head { display: flex; justify-content: space-between; align-items: center; }
  .cert-no { font-size: 22px; font-weight: 700; letter-spacing: 1px; }
  .badge.big { font-size: 14px; padding: 4px 14px; }
  .cert-body {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 10px 24px; margin: 16px 0;
  }
  .cert-body span { display: block; font-size: 12px; margin-bottom: 2px; }
  .timeline { margin-top: 14px; }
  .t-item { display: flex; gap: 14px; position: relative; padding-bottom: 18px; }
  .t-item::before {
    content: ''; position: absolute; left: 7px; top: 18px; bottom: 0;
    width: 2px; background: #e5e7eb;
  }
  .t-item:last-child::before { display: none; }
  .t-dot {
    width: 16px; height: 16px; border-radius: 50%; background: #cbd5e1;
    margin-top: 3px; flex-shrink: 0; z-index: 1;
  }
  .t-active .t-dot { background: #16a34a; }
  .t-body { flex: 1; background: #f8fafc; border-radius: 10px; padding: 12px 16px; }
  .revoke-box {
    margin-top: 8px; background: #fef2f2; border: 1px solid #fecaca;
    border-radius: 8px; padding: 8px 12px; font-size: 12.5px; color: #991b1b;
  }
  .corr-box {
    margin-top: 8px; background: #fffbeb; border: 1px solid #fde68a;
    border-radius: 8px; padding: 8px 12px; font-size: 12.5px; color: #92400e;
  }
  .corr-box ul { margin: 4px 0; padding-left: 18px; }
</style>
