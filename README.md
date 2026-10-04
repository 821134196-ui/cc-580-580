# 课程证书签发与姓名更正系统

培训平台教务根据学员学习和考试结果签发证书;学员在签发前确认姓名,填错后通过申请更正;外部人员可公开核验证书真伪。

## 技术栈

- **后端**:Java 17 · Quarkus 3 · Hibernate ORM · SQLite(文件库 `backend/data/certificates.db`)
- **前端**:Svelte 4 + Vite,构建后由 Quarkus 作为静态资源托管
- **邮件**:本地模拟 —— 所有回执写入 `email_receipts` 表,在"模拟邮箱"页查看

## 一条命令启动

```bash
./start.sh
```

脚本依次:构建前端 → 拷贝到后端静态目录 → 打包 Quarkus → 启动 <http://localhost:8080>。
首次启动自动建表并写入演示数据(张三:可签发 / 李四:学时不足 / 王五:最近一次考试未通过)。

> 需要 JDK 17+、Maven 3.9+、Node 18+。`start.sh` 会自动识别 `JAVA_HOME` 或 `/home/node/tools` 下的工具链。

## 运行验证测试

```bash
cd backend && mvn test
```

覆盖题目要求的全部验证点:

| 验证点 | 测试 |
| --- | --- |
| 编号唯一(格式 + 全局不重号) | `CertificateIssuanceTest#concurrentIssuanceAcrossEnrollmentsProducesGloballyUniqueNumbers` |
| 并发签发同一报名只产生一张有效证 | `CertificateIssuanceTest#concurrentIssuanceOnSameEnrollmentYieldsExactlyOneValidCertificate` |
| 姓名更正后旧证作废、新证生效 | `NameCorrectionTest#fullCorrectionFlowVoidsOldCertificateAndIssuesNewVersion` |
| 补考后资格实时重算(不用过期快照) | `EligibilityReevaluationTest` |
| 已签发证书的课程信息不可直接修改 | `CourseImmutabilityTest` |
| 材料不足退回补件、补件后重审 | `NameCorrectionTest` |

## 业务规则实现要点

- **资格判定实时化**(`EligibilityService`):每次判定都现查当前学时与**最近一次**考试成绩,补考/重修立即生效,不存在成绩快照缓存。
- **唯一编号**(`CertificateService#nextNumber`):数据库序列表单行悲观锁发号 `CERT-年份-六位序号`,`certificates.certificate_number` 唯一约束兜底。
- **并发签发安全**:对报名记录加悲观写锁 + `active_certificates.enrollment_id` 唯一约束("当前有效证书"指针表),同一报名在数据库层面至多一张有效证。
- **更正即换版**:审核通过 → 同一事务内旧证置 `VOID`(终态,无任何重新激活入口)、签发 `version+1` 新证并继承旧证成绩/学时/课程快照;版本页完整保留更正原因与材料。
- **证书内容不可变**:课程/成绩/学时均为签发时刻快照,系统不提供证书修改接口;已签发证书的课程通过 `PUT /api/courses/{id}` 修改会被 409 拒绝。
- **退回补件**:`PENDING → RETURNED →(补件)→ PENDING → APPROVED`,退回必须注明缺少的材料。

## 主要 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/enrollments` | 报名列表(含实时资格判定) |
| POST | `/api/enrollments/{id}/attempts` | 录入成绩(补考=新记录) |
| POST | `/api/enrollments/{id}/hours` | 更新学时(重修) |
| POST | `/api/enrollments/{id}/confirm-name` | 学员签发前确认姓名 |
| POST | `/api/enrollments/{id}/issue` | 教务签发证书 |
| GET | `/api/enrollments/{id}/certificates` | 证书版本链 |
| POST | `/api/certificates/{id}/corrections` | 提交姓名更正申请 |
| POST | `/api/corrections/{id}/review` | 教务审核(APPROVE/RETURN) |
| POST | `/api/corrections/{id}/materials` | 退回后补件 |
| GET | `/api/verify/{number}` | 公开核验证书 |
| GET | `/api/emails` | 模拟邮箱回执 |

## 目录结构

```
backend/   Quarkus 后端(实体 / 服务 / REST / 测试)
frontend/  Svelte 前端(Vite 构建,产物由后端托管)
start.sh   一键启动
```
