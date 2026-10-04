-- 课程
INSERT INTO course (id, code, title, required_hours, pass_score) VALUES (1, 'JAVA-BASIC', 'Java 程序设计基础', 32, 60);
INSERT INTO course (id, code, title, required_hours, pass_score) VALUES (2, 'DATA-STRUCT', '数据结构与算法', 48, 70);
INSERT INTO course (id, code, title, required_hours, pass_score) VALUES (3, 'CLOUD-NATIVE', '云原生应用开发', 24, 60);

-- 学员
INSERT INTO student (id, student_no, full_name, email, id_card_no) VALUES (1, 'S2026001', '张三', 'zhangsan@example.com', '110101200101011234');
INSERT INTO student (id, student_no, full_name, email, id_card_no) VALUES (2, 'S2026002', '李四', 'lisi@example.com', '110101200202022345');
INSERT INTO student (id, student_no, full_name, email, id_card_no) VALUES (3, 'S2026003', '王五', 'wangwu@example.com', '110101200303033456');
INSERT INTO student (id, student_no, full_name, email, id_card_no) VALUES (4, 'S2026004', '赵六', 'zhaoliu@example.com', '110101200404044567');

-- 学习进度：张三两门达标；李四学时达标但首考未过（演示补考）；王五学时不足；赵六已持证
INSERT INTO study_record (id, student_id, course_id, completed_hours, updated_at) VALUES (1, 1, 1, 36, CAST(strftime('%s','2026-08-20 10:00:00') AS INTEGER) * 1000);
INSERT INTO study_record (id, student_id, course_id, completed_hours, updated_at) VALUES (2, 1, 2, 50, CAST(strftime('%s','2026-08-25 10:00:00') AS INTEGER) * 1000);
INSERT INTO study_record (id, student_id, course_id, completed_hours, updated_at) VALUES (3, 2, 1, 32, CAST(strftime('%s','2026-08-22 10:00:00') AS INTEGER) * 1000);
INSERT INTO study_record (id, student_id, course_id, completed_hours, updated_at) VALUES (4, 3, 2, 20, CAST(strftime('%s','2026-08-23 10:00:00') AS INTEGER) * 1000);
INSERT INTO study_record (id, student_id, course_id, completed_hours, updated_at) VALUES (5, 4, 3, 30, CAST(strftime('%s','2026-08-10 10:00:00') AS INTEGER) * 1000);

-- 考试成绩：李四首考 45 分未通过，等待补考
INSERT INTO exam_attempt (id, student_id, course_id, attempt_no, score, taken_at) VALUES (1, 1, 1, 1, 88, CAST(strftime('%s','2026-08-28 14:00:00') AS INTEGER) * 1000);
INSERT INTO exam_attempt (id, student_id, course_id, attempt_no, score, taken_at) VALUES (2, 1, 2, 1, 75, CAST(strftime('%s','2026-08-29 14:00:00') AS INTEGER) * 1000);
INSERT INTO exam_attempt (id, student_id, course_id, attempt_no, score, taken_at) VALUES (3, 2, 1, 1, 45, CAST(strftime('%s','2026-08-28 14:00:00') AS INTEGER) * 1000);
INSERT INTO exam_attempt (id, student_id, course_id, attempt_no, score, taken_at) VALUES (4, 4, 3, 1, 90, CAST(strftime('%s','2026-08-15 14:00:00') AS INTEGER) * 1000);

-- 证书：赵六首版姓名有误（"赵陆"），已作废；第二版为当前有效证书
INSERT INTO certificate (id, cert_number, student_id, course_id, holder_name, course_code_snapshot, course_title_snapshot, required_hours_snapshot, pass_score_snapshot, completed_hours_snapshot, final_score_snapshot, version, status, issued_at, issued_by, revoked_at, revoke_reason, replaces_id) VALUES (1, 'CERT-2026-000001', 4, 3, '赵陆', 'CLOUD-NATIVE', '云原生应用开发', 24, 60, 30, 90, 1, 'REVOKED', CAST(strftime('%s','2026-09-01 10:00:00') AS INTEGER) * 1000, '教务-陈老师', CAST(strftime('%s','2026-09-15 09:30:00') AS INTEGER) * 1000, '姓名更正：同音错字「陆」应为「六」', NULL);
INSERT INTO certificate (id, cert_number, student_id, course_id, holder_name, course_code_snapshot, course_title_snapshot, required_hours_snapshot, pass_score_snapshot, completed_hours_snapshot, final_score_snapshot, version, status, issued_at, issued_by, revoked_at, revoke_reason, replaces_id) VALUES (2, 'CERT-2026-000002', 4, 3, '赵六', 'CLOUD-NATIVE', '云原生应用开发', 24, 60, 30, 90, 2, 'ACTIVE', CAST(strftime('%s','2026-09-15 09:30:00') AS INTEGER) * 1000, '教务-陈老师', NULL, NULL, 1);

-- 已完成的更正申请（与上面两张证书对应）
INSERT INTO correction_request (id, certificate_id, student_id, requested_name, reason, materials, status, review_note, created_at, reviewed_at, reviewed_by, submit_count, new_certificate_id) VALUES (1, 1, 4, '赵六', '报名时姓名录入为同音字「陆」，与身份证不符', '["身份证扫描件","报名信息更正表"]', 'APPROVED', NULL, CAST(strftime('%s','2026-09-10 08:00:00') AS INTEGER) * 1000, CAST(strftime('%s','2026-09-15 09:30:00') AS INTEGER) * 1000, '教务-陈老师', 1, 2);

-- 编号发号器：下一张证书从 3 开始
INSERT INTO cert_sequence (name, next_value) VALUES ('CERT', 3);

-- 历史邮件回执
INSERT INTO mail_outbox (id, recipient, subject, body, cert_number, sent_at) VALUES (1, 'zhaoliu@example.com', '证书签发回执 CERT-2026-000001', '您好 赵陆：' || char(10) || '您已完成课程《云原生应用开发》，证书编号 CERT-2026-000001。' || char(10) || '（本邮件为本地模拟发送）', 'CERT-2026-000001', CAST(strftime('%s','2026-09-01 10:00:00') AS INTEGER) * 1000);
INSERT INTO mail_outbox (id, recipient, subject, body, cert_number, sent_at) VALUES (2, 'zhaoliu@example.com', '姓名更正完成，新证书已签发 CERT-2026-000002', '您的姓名已更正为「赵六」。旧证书 CERT-2026-000001 已作废，新证书编号 CERT-2026-000002（版本 V2）。' || char(10) || '（本邮件为本地模拟发送）', 'CERT-2026-000002', CAST(strftime('%s','2026-09-15 09:30:00') AS INTEGER) * 1000);

-- 关键约束：同一学员同一课程最多一张"有效"证书（数据库级兜底，防并发双签发）
CREATE UNIQUE INDEX uq_certificate_active ON certificate(student_id, course_id) WHERE status = 'ACTIVE';
