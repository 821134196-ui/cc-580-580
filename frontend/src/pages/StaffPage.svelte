<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { toast, toastError } from '../stores.js'
  import Modal from '../components/Modal.svelte'

  let enrollments = []
  let courses = []
  let students = []
  let loading = true

  // 弹窗状态
  let scoreModal = null // { enrollment }
  let scoreValue = ''
  let hoursModal = null
  let hoursValue = ''
  let courseModal = null // { course? } 编辑或新增
  let courseForm = { code: '', title: '', requiredHours: 40, passingScore: 60 }
  let studentForm = { fullName: '', email: '' }
  let enrollForm = { studentId: '', courseId: '' }

  onMount(load)

  async function load() {
    loading = true
    try {
      ;[enrollments, courses, students] = await Promise.all([
        api.enrollments(),
        api.courses(),
        api.students()
      ])
    } catch (e) {
      toastError(e)
    } finally {
      loading = false
    }
  }

  async function issue(e) {
    try {
      const cert = await api.issue(e.id)
      toast(`已签发证书 ${cert.certificateNumber}(第 ${cert.version} 版)`, 'success')
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitScore() {
    try {
      await api.addAttempt(scoreModal.id, Number(scoreValue))
      toast('成绩已录入,资格已实时重算', 'success')
      scoreModal = null
      scoreValue = ''
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitHours() {
    try {
      await api.setHours(hoursModal.id, Number(hoursValue))
      toast('学时已更新,资格已实时重算', 'success')
      hoursModal = null
      hoursValue = ''
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitCourse() {
    try {
      if (courseModal?.course) {
        await api.updateCourse(courseModal.course.id, {
          title: courseForm.title,
          requiredHours: Number(courseForm.requiredHours),
          passingScore: Number(courseForm.passingScore)
        })
        toast('课程已更新', 'success')
      } else {
        await api.createCourse({
          code: courseForm.code,
          title: courseForm.title,
          requiredHours: Number(courseForm.requiredHours),
          passingScore: Number(courseForm.passingScore)
        })
        toast('课程已创建', 'success')
      }
      courseModal = null
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitStudent() {
    try {
      await api.createStudent(studentForm.fullName, studentForm.email)
      studentForm = { fullName: '', email: '' }
      toast('学员已创建', 'success')
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  async function submitEnrollment() {
    try {
      await api.createEnrollment(Number(enrollForm.studentId), Number(enrollForm.courseId))
      enrollForm = { studentId: '', courseId: '' }
      toast('报名成功', 'success')
      await load()
    } catch (err) {
      toastError(err)
    }
  }

  function eligibilityBadge(e) {
    if (e.eligibility.hasValidCertificate) return { cls: 'blue', text: '已持证' }
    if (e.eligibility.eligible) return { cls: 'green', text: '可签发' }
    return { cls: 'amber', text: '未达标' }
  }
</script>

<div class="card">
  <h2>报名与资格总览</h2>
  <p class="sub">资格实时判定:当前学时 ≥ 课程要求、最近一次考试成绩 ≥ 及格线、学员已确认姓名、且无有效证书。补考/重修后立即重算。</p>
  {#if loading}
    <p class="muted">加载中…</p>
  {:else}
    <table>
      <thead>
        <tr>
          <th>学员</th><th>课程</th><th>学时</th><th>最近成绩</th><th>姓名确认</th>
          <th>资格状态</th><th>未达标原因</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        {#each enrollments as e}
          {@const badge = eligibilityBadge(e)}
          <tr>
            <td>{e.studentName}<br /><span class="muted">{e.studentEmail}</span></td>
            <td>{e.courseTitle}<br /><span class="muted mono">{e.courseCode}</span></td>
            <td>{e.completedHours} / {e.eligibility.requiredHours}</td>
            <td>
              {#if e.eligibility.latestScore != null}
                第 {e.eligibility.attemptCount} 次 · <strong>{e.eligibility.latestScore}</strong> 分
                <span class="muted">(及格 {e.eligibility.passingScore})</span>
              {:else}
                <span class="muted">无记录</span>
              {/if}
            </td>
            <td>
              {#if e.eligibility.nameConfirmed}
                <span class="badge green">已确认:{e.eligibility.confirmedName}</span>
              {:else}
                <span class="badge gray">未确认</span>
              {/if}
            </td>
            <td><span class="badge {badge.cls}">{badge.text}</span></td>
            <td class="muted" style="max-width:220px">
              {#each e.eligibility.reasons as r}
                <div>· {r}</div>
              {/each}
              {#if e.eligibility.reasons.length === 0}—{/if}
            </td>
            <td style="white-space:nowrap">
              <button class="btn primary sm" disabled={!e.eligibility.eligible} on:click={() => issue(e)}>签发证书</button>
              <button class="btn sm" on:click={() => { scoreModal = e; scoreValue = '' }}>录成绩/补考</button>
              <button class="btn sm" on:click={() => { hoursModal = e; hoursValue = String(e.completedHours) }}>改学时</button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>

<div class="card">
  <h2>课程管理</h2>
  <p class="sub">已签发证书的课程会被锁定,课程信息不可直接修改(证书上是签发时刻的快照)。</p>
  <table>
    <thead>
      <tr><th>编号</th><th>名称</th><th>要求学时</th><th>及格分</th><th>状态</th><th></th></tr>
    </thead>
    <tbody>
      {#each courses as c}
        <tr>
          <td class="mono">{c.code}</td>
          <td>{c.title}</td>
          <td>{c.requiredHours}</td>
          <td>{c.passingScore}</td>
          <td>
            {#if c.lockedByIssuedCertificates}
              <span class="badge red">已锁定(有签发记录)</span>
            {:else}
              <span class="badge green">可编辑</span>
            {/if}
          </td>
          <td>
            <button class="btn sm" on:click={() => {
              courseModal = { course: c }
              courseForm = { code: c.code, title: c.title, requiredHours: c.requiredHours, passingScore: c.passingScore }
            }}>编辑</button>
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
  <div style="margin-top:12px">
    <button class="btn" on:click={() => {
      courseModal = {}
      courseForm = { code: '', title: '', requiredHours: 40, passingScore: 60 }
    }}>+ 新建课程</button>
  </div>
</div>

<div class="card">
  <h2>学员与报名</h2>
  <p class="sub">登记新学员,或为学员报名课程。</p>
  <div class="row" style="margin-bottom:14px">
    <label class="field">学员姓名<input bind:value={studentForm.fullName} placeholder="张三" /></label>
    <label class="field">邮箱<input bind:value={studentForm.email} placeholder="zhangsan@example.com" /></label>
    <button class="btn" on:click={submitStudent}>+ 添加学员</button>
  </div>
  <div class="row">
    <label class="field">学员
      <select bind:value={enrollForm.studentId}>
        <option value="">选择学员</option>
        {#each students as s}<option value={s.id}>{s.fullName}({s.email})</option>{/each}
      </select>
    </label>
    <label class="field">课程
      <select bind:value={enrollForm.courseId}>
        <option value="">选择课程</option>
        {#each courses as c}<option value={c.id}>{c.title}({c.code})</option>{/each}
      </select>
    </label>
    <button class="btn" disabled={!enrollForm.studentId || !enrollForm.courseId} on:click={submitEnrollment}>+ 报名</button>
  </div>
</div>

{#if scoreModal}
  <Modal title="录入考试成绩(补考即新增一次记录)" onClose={() => (scoreModal = null)}>
    <p class="muted">{scoreModal.studentName} · {scoreModal.courseTitle} — 当前已是第 {scoreModal.eligibility.attemptCount} 次考试,本次将记为第 {scoreModal.eligibility.attemptCount + 1} 次。资格判定永远取<strong>最新一次</strong>成绩。</p>
    <div class="row">
      <label class="field">成绩(0-100)<input type="number" min="0" max="100" bind:value={scoreValue} /></label>
      <button class="btn primary" disabled={scoreValue === ''} on:click={submitScore}>录入</button>
    </div>
  </Modal>
{/if}

{#if hoursModal}
  <Modal title="更新已完成学时(重修场景)" onClose={() => (hoursModal = null)}>
    <p class="muted">{hoursModal.studentName} · {hoursModal.courseTitle} — 课程要求 {hoursModal.eligibility.requiredHours} 学时。修改后资格立即重算。</p>
    <div class="row">
      <label class="field">已完成学时<input type="number" min="0" bind:value={hoursValue} /></label>
      <button class="btn primary" disabled={hoursValue === ''} on:click={submitHours}>保存</button>
    </div>
  </Modal>
{/if}

{#if courseModal}
  <Modal title={courseModal.course ? `编辑课程 ${courseModal.course.code}` : '新建课程'} onClose={() => (courseModal = null)}>
    {#if courseModal.course?.lockedByIssuedCertificates}
      <p class="muted" style="color:#b91c1c">该课程已签发证书,保存将被系统拒绝(409)。</p>
    {/if}
    <div class="row">
      {#if !courseModal.course}
        <label class="field">课程编号<input bind:value={courseForm.code} placeholder="JAVA-101" /></label>
      {/if}
      <label class="field">课程名称<input bind:value={courseForm.title} /></label>
      <label class="field">要求学时<input type="number" min="1" bind:value={courseForm.requiredHours} /></label>
      <label class="field">及格分<input type="number" min="0" max="100" bind:value={courseForm.passingScore} /></label>
      <button class="btn primary" on:click={submitCourse}>保存</button>
    </div>
  </Modal>
{/if}
