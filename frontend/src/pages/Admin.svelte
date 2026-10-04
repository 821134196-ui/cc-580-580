<script>
  import { onMount } from 'svelte';
  import { api, fmtTime } from '../api.js';

  let students = [];
  let courses = [];
  let selectedStudent = '';
  let selectedCourse = '';
  let eligibility = null;
  let attempts = [];
  let eligError = '';

  // 签发确认
  let confirmName = '';
  let issuedBy = '教务-陈老师';
  let nameChecked = false;
  let issueMsg = '';
  let issueErr = '';
  let issuing = false;

  // 录入学时 / 成绩
  let hoursInput = '';
  let scoreInput = '';
  let recordMsg = '';
  let recordErr = '';

  // 新建学员 / 课程
  let newStudent = { studentNo: '', fullName: '', email: '', idCardNo: '' };
  let newCourse = { code: '', title: '', requiredHours: 24, passScore: 60 };
  let adminMsg = '';
  let adminErr = '';

  onMount(loadAll);

  async function loadAll() {
    [students, courses] = await Promise.all([api.listStudents(), api.listCourses()]);
    if (!selectedStudent && students.length) selectedStudent = String(students[0].id);
    if (!selectedCourse && courses.length) selectedCourse = String(courses[0].id);
    await refreshEligibility();
  }

  async function refreshEligibility() {
    eligibility = null;
    eligError = '';
    attempts = [];
    issueMsg = '';
    issueErr = '';
    if (!selectedStudent || !selectedCourse) return;
    try {
      eligibility = await api.eligibility(selectedStudent, selectedCourse);
      attempts = await api.listAttempts(selectedStudent, selectedCourse);
      const stu = students.find((s) => String(s.id) === selectedStudent);
      confirmName = stu ? stu.fullName : '';
      nameChecked = false;
    } catch (e) {
      eligError = e.message;
    }
  }

  async function saveHours() {
    recordMsg = ''; recordErr = '';
    try {
      const r = await api.upsertStudy({
        studentId: Number(selectedStudent), courseId: Number(selectedCourse),
        completedHours: Number(hoursInput)
      });
      recordMsg = `学时已更新为 ${r.completedHours} 小时`;
      hoursInput = '';
      await refreshEligibility();
    } catch (e) { recordErr = e.message; }
  }

  async function saveScore() {
    recordMsg = ''; recordErr = '';
    try {
      const r = await api.addExam({
        studentId: Number(selectedStudent), courseId: Number(selectedCourse),
        score: Number(scoreInput)
      });
      recordMsg = `已登记第 ${r.attemptNo} 次考试成绩：${r.score} 分（补考/重修后资格将实时重判）`;
      scoreInput = '';
      await refreshEligibility();
    } catch (e) { recordErr = e.message; }
  }

  async function issue() {
    issueMsg = ''; issueErr = ''; issuing = true;
    try {
      const cert = await api.issue({
        studentId: Number(selectedStudent), courseId: Number(selectedCourse),
        confirmedName: confirmName, issuedBy
      });
      issueMsg = `签发成功！证书编号 ${cert.certNumber}（V${cert.version}），回执邮件已模拟发送。`;
      await refreshEligibility();
    } catch (e) {
      issueErr = e.message;
    } finally {
      issuing = false;
    }
  }

  async function addStudent() {
    adminMsg = ''; adminErr = '';
    try {
      await api.createStudent(newStudent);
      adminMsg = '学员已创建';
      newStudent = { studentNo: '', fullName: '', email: '', idCardNo: '' };
      await loadAll();
    } catch (e) { adminErr = e.message; }
  }

  async function addCourse() {
    adminMsg = ''; adminErr = '';
    try {
      await api.createCourse({
        code: newCourse.code, title: newCourse.title,
        requiredHours: Number(newCourse.requiredHours), passScore: Number(newCourse.passScore)
      });
      adminMsg = '课程已创建';
      newCourse = { code: '', title: '', requiredHours: 24, passScore: 60 };
      await loadAll();
    } catch (e) { adminErr = e.message; }
  }

  $: if (selectedStudent && selectedCourse) refreshEligibility();
</script>

<div class="grid">
  <div>
    <div class="card">
      <h2>资格判定（实时）</h2>
      <div class="row" style="margin-bottom:12px">
        <div>
          <label>学员</label>
          <select bind:value={selectedStudent}>
            {#each students as s}
              <option value={String(s.id)}>{s.fullName}（{s.studentNo}）</option>
            {/each}
          </select>
        </div>
        <div>
          <label>课程</label>
          <select bind:value={selectedCourse}>
            {#each courses as c}
              <option value={String(c.id)}>{c.title}（{c.code}）</option>
            {/each}
          </select>
        </div>
      </div>

      {#if eligError}
        <div class="msg-err">{eligError}</div>
      {/if}

      {#if eligibility}
        <table>
          <tbody>
            <tr>
              <td style="width:130px" class="muted">完成学时</td>
              <td>{eligibility.completedHours} / {eligibility.requiredHours} 学时
                {#if eligibility.hoursOk}<span class="badge green">达标</span>{:else}<span class="badge red">不足</span>{/if}
              </td>
            </tr>
            <tr>
              <td class="muted">最近一次考试</td>
              <td>
                {#if eligibility.latestScore === null}
                  暂无考试记录 <span class="badge red">未通过</span>
                {:else}
                  {eligibility.latestScore} 分 / 合格线 {eligibility.passScore} 分
                  {#if eligibility.examOk}<span class="badge green">通过</span>{:else}<span class="badge red">未通过</span>{/if}
                  <span class="muted">（共 {eligibility.attemptCount} 次考试，以最新一次为准）</span>
                {/if}
              </td>
            </tr>
            <tr>
              <td class="muted">有效证书</td>
              <td>
                {#if eligibility.hasActiveCert}
                  已持有 <a href="#/verify/{eligibility.activeCertNumber}">{eligibility.activeCertNumber}</a>
                  <span class="badge amber">不可重复签发</span>
                {:else}
                  无 <span class="badge green">可签发</span>
                {/if}
              </td>
            </tr>
            <tr>
              <td class="muted">综合结论</td>
              <td>
                {#if eligibility.eligible}
                  <span class="badge green">符合签发条件</span>
                {:else}
                  <span class="badge red">暂不符合</span>
                  <ul style="margin:6px 0 0;padding-left:18px">
                    {#each eligibility.reasons as r}<li class="muted">{r}</li>{/each}
                  </ul>
                {/if}
              </td>
            </tr>
          </tbody>
        </table>
      {/if}
    </div>

    <div class="card">
      <h2>教务录入</h2>
      <div class="row">
        <div>
          <label>更新完成学时</label>
          <input type="number" min="0" bind:value={hoursInput} placeholder="如 36" style="width:110px" />
        </div>
        <button class="ghost" on:click={saveHours} disabled={hoursInput === ''}>保存学时</button>
        <div>
          <label>登记考试成绩（补考/重修）</label>
          <input type="number" min="0" max="100" bind:value={scoreInput} placeholder="0-100" style="width:110px" />
        </div>
        <button class="ghost" on:click={saveScore} disabled={scoreInput === ''}>登记成绩</button>
      </div>
      {#if recordMsg}<div class="msg-ok">{recordMsg}</div>{/if}
      {#if recordErr}<div class="msg-err">{recordErr}</div>{/if}

      {#if attempts.length}
        <h3>考试记录</h3>
        <table>
          <thead><tr><th>次数</th><th>成绩</th><th>考试时间</th></tr></thead>
          <tbody>
            {#each attempts as a}
              <tr><td>第 {a.attemptNo} 次</td><td>{a.score} 分</td><td>{fmtTime(a.takenAt)}</td></tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>

    <div class="card">
      <h2>签发证书</h2>
      <p class="muted">签发前请学员核对姓名，系统将实时复核资格（不使用过期快照）。</p>
      <div class="row">
        <div>
          <label>学员确认姓名</label>
          <input bind:value={confirmName} style="width:160px" />
        </div>
        <div>
          <label>签发人</label>
          <input bind:value={issuedBy} style="width:140px" />
        </div>
        <label style="display:flex;align-items:center;gap:6px;margin-top:16px">
          <input type="checkbox" bind:checked={nameChecked} style="width:auto" /> 学员已确认姓名无误
        </label>
        <button on:click={issue} disabled={!nameChecked || !confirmName || issuing || !eligibility || !eligibility.eligible}>
          {issuing ? '签发中…' : '确认签发'}
        </button>
      </div>
      {#if issueMsg}<div class="msg-ok">{issueMsg} <a href="#/certificates">查看证书</a></div>{/if}
      {#if issueErr}<div class="msg-err">签发被拒：{issueErr}</div>{/if}
    </div>
  </div>

  <div>
    <div class="card">
      <h2>课程</h2>
      <table>
        <thead><tr><th>课程</th><th>要求学时</th><th>合格线</th><th>已发证</th></tr></thead>
        <tbody>
          {#each courses as c}
            <tr>
              <td>{c.title}<br /><span class="muted">{c.code}</span></td>
              <td>{c.requiredHours} h</td>
              <td>{c.passScore} 分</td>
              <td>
                {c.certificateCount} 张
                {#if c.locked}<span class="badge gray" title="已签发证书的课程信息不可修改">已锁定</span>{/if}
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
      <h3>新建课程</h3>
      <div class="row">
        <input bind:value={newCourse.code} placeholder="课程代码" style="width:110px" />
        <input bind:value={newCourse.title} placeholder="课程名称" style="width:150px" />
        <input type="number" bind:value={newCourse.requiredHours} style="width:80px" title="要求学时" />
        <input type="number" bind:value={newCourse.passScore} style="width:80px" title="合格线" />
        <button class="ghost" on:click={addCourse}>创建</button>
      </div>
    </div>

    <div class="card">
      <h2>学员</h2>
      <table>
        <thead><tr><th>学号</th><th>姓名</th><th>邮箱</th></tr></thead>
        <tbody>
          {#each students as s}
            <tr><td>{s.studentNo}</td><td>{s.fullName}</td><td class="muted">{s.email}</td></tr>
          {/each}
        </tbody>
      </table>
      <h3>新建学员</h3>
      <div class="row">
        <input bind:value={newStudent.studentNo} placeholder="学号" style="width:100px" />
        <input bind:value={newStudent.fullName} placeholder="姓名" style="width:100px" />
        <input bind:value={newStudent.email} placeholder="邮箱" style="width:170px" />
        <input bind:value={newStudent.idCardNo} placeholder="证件号" style="width:160px" />
        <button class="ghost" on:click={addStudent}>创建</button>
      </div>
      {#if adminMsg}<div class="msg-ok">{adminMsg}</div>{/if}
      {#if adminErr}<div class="msg-err">{adminErr}</div>{/if}
    </div>
  </div>
</div>

<style>
  .grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 18px; align-items: start; }
  @media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
</style>
