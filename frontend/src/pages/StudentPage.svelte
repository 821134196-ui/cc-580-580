<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { toast, toastError, fmtTime } from '../stores.js'
  import Modal from '../components/Modal.svelte'

  let students = []
  let selectedStudentId = ''
  let enrollments = []
  let corrections = []
  let confirmNames = {} // enrollmentId -> 输入的确认姓名

  let correctionModal = null // { cert }
  let correctionForm = { requestedName: '', reason: '', materials: '' }
  let supplementModal = null // { correction }
  let supplementValue = ''

  onMount(async () => {
    try {
      students = await api.students()
      if (students.length > 0) {
        selectedStudentId = String(students[0].id)
        await loadStudentData()
      }
    } catch (e) {
      toastError(e)
    }
  })

  async function loadStudentData() {
    if (!selectedStudentId) return
    try {
      const [all, allCorrections] = await Promise.all([api.enrollments(), api.corrections()])
      enrollments = all.filter((e) => String(e.studentId) === selectedStudentId)
      corrections = allCorrections
      for (const e of enrollments) {
        if (!(e.id in confirmNames)) confirmNames[e.id] = e.studentName
      }
    } catch (e) {
      toastError(e)
    }
  }

  async function confirmName(e) {
    try {
      await api.confirmName(e.id, confirmNames[e.id])
      toast(`姓名已确认:${confirmNames[e.id]}`, 'success')
      await loadStudentData()
    } catch (err) {
      toastError(err)
    }
  }

  async function loadChain(e) {
    try {
      const chain = await api.certificateChain(e.id)
      e._chain = chain
      enrollments = enrollments
    } catch (err) {
      toastError(err)
    }
  }

  async function submitCorrection() {
    try {
      await api.submitCorrection(correctionModal.cert.id, correctionForm)
      toast('更正申请已提交,等待教务审核', 'success')
      correctionModal = null
      correctionForm = { requestedName: '', reason: '', materials: '' }
      await loadStudentData()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitSupplement() {
    try {
      await api.supplement(supplementModal.id, supplementValue)
      toast('补件已提交,申请重新进入待审核', 'success')
      supplementModal = null
      supplementValue = ''
      await loadStudentData()
    } catch (err) {
      toastError(err)
    }
  }

  function myCorrectionsFor(certId) {
    return corrections.filter((c) => c.certificateId === certId)
  }
</script>

<div class="card">
  <h2>学员中心</h2>
  <p class="sub">选择学员身份,确认证书姓名、查看证书、申请姓名更正。</p>
  <label class="field" style="max-width:340px">当前学员
    <select bind:value={selectedStudentId} on:change={loadStudentData}>
      {#each students as s}<option value={s.id}>{s.fullName}({s.email})</option>{/each}
    </select>
  </label>
</div>

{#each enrollments as e}
  <div class="card">
    <h2>{e.courseTitle} <span class="muted mono">{e.courseCode}</span></h2>
    <p class="sub">
      学时 {e.completedHours}/{e.eligibility.requiredHours} ·
      {#if e.eligibility.latestScore != null}最近成绩 {e.eligibility.latestScore} 分(第 {e.eligibility.attemptCount} 次){:else}暂无考试成绩{/if}
      · {#if e.eligibility.hasValidCertificate}<span class="badge blue">已持证</span>
        {:else if e.eligibility.eligible}<span class="badge green">已满足签发条件,等待教务签发</span>
        {:else}<span class="badge amber">暂未达标</span>{/if}
    </p>

    {#if !e.eligibility.hasValidCertificate}
      <div class="row" style="margin-bottom:6px">
        <label class="field">签发前确认证书姓名
          <input bind:value={confirmNames[e.id]} disabled={e.eligibility.hasValidCertificate} />
        </label>
        <button class="btn" on:click={() => confirmName(e)}>确认姓名</button>
        {#if e.eligibility.nameConfirmed}
          <span class="badge green">已确认:{e.eligibility.confirmedName}</span>
        {/if}
      </div>
      <p class="muted">姓名一经签发即写入证书,签发后如需修改请使用"申请姓名更正"。</p>
    {/if}

    {#if !e._chain}
      <button class="btn sm" on:click={() => loadChain(e)}>查看我的证书</button>
    {:else}
      {#if e._chain.length === 0}
        <p class="muted">尚未签发证书。</p>
      {:else}
        <table>
          <thead>
            <tr><th>证书编号</th><th>版本</th><th>证书姓名</th><th>状态</th><th>签发时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            {#each e._chain as cert}
              <tr>
                <td class="mono">{cert.certificateNumber}</td>
                <td>v{cert.version}</td>
                <td>{cert.studentName}</td>
                <td>
                  {#if cert.status === 'VALID'}<span class="badge green">当前有效</span>
                  {:else}<span class="badge red">已作废</span>{/if}
                </td>
                <td>{fmtTime(cert.issuedAt)}</td>
                <td>
                  {#if cert.status === 'VALID'}
                    <button class="btn sm" on:click={() => {
                      correctionModal = { cert }
                      correctionForm = { requestedName: '', reason: '', materials: '' }
                    }}>申请姓名更正</button>
                  {/if}
                  {#each myCorrectionsFor(cert.id) as c}
                    <div style="margin-top:4px">
                      {#if c.status === 'PENDING'}<span class="badge amber">更正审核中</span>
                      {:else if c.status === 'RETURNED'}
                        <span class="badge red">已退回:{c.reviewNote}</span>
                        <button class="btn sm warn" on:click={() => { supplementModal = c; supplementValue = '' }}>补件</button>
                      {:else if c.status === 'APPROVED'}<span class="badge green">更正完成 → {c.issuedCertificateNumber}</span>{/if}
                    </div>
                  {/each}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    {/if}
  </div>
{/each}

{#if correctionModal}
  <Modal title={`申请姓名更正 — ${correctionModal.cert.certificateNumber}`} onClose={() => (correctionModal = null)}>
    <p class="muted">当前证书姓名:<strong>{correctionModal.cert.studentName}</strong>。审核通过后旧证将作废并签发新版本,旧证对外核验显示无效。</p>
    <div class="row" style="flex-direction:column;align-items:stretch">
      <label class="field">更正后姓名<input bind:value={correctionForm.requestedName} placeholder="与身份证件一致" /></label>
      <label class="field">更正原因<textarea rows="2" bind:value={correctionForm.reason} placeholder="如:报名时姓名填写错误"></textarea></label>
      <label class="field">证明材料<textarea rows="3" bind:value={correctionForm.materials} placeholder="列明材料,如:身份证复印件、公安机关姓名变更证明"></textarea></label>
      <div>
        <button class="btn primary"
          disabled={!correctionForm.requestedName || !correctionForm.reason || !correctionForm.materials}
          on:click={submitCorrection}>提交申请</button>
      </div>
    </div>
  </Modal>
{/if}

{#if supplementModal}
  <Modal title={`补件 — 更正申请 #${supplementModal.id}`} onClose={() => (supplementModal = null)}>
    <p class="muted">教务退回意见:<strong>{supplementModal.reviewNote}</strong></p>
    <p class="muted">已提交材料:</p>
    <pre class="muted" style="white-space:pre-wrap">{supplementModal.materials}</pre>
    <label class="field" style="margin-top:8px">补充材料
      <textarea rows="3" bind:value={supplementValue} placeholder="补充缺少的证明材料"></textarea>
    </label>
    <div style="margin-top:10px">
      <button class="btn primary" disabled={!supplementValue} on:click={submitSupplement}>提交补件</button>
    </div>
  </Modal>
{/if}
