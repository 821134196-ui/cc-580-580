package com.training.cert;

import com.training.cert.model.*;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;

/**
 * 演示数据:三名学员、两门课程、三种典型状态
 * (可签发 / 学时不足 / 最近一次补考未通过),以及证书编号发号器。
 */
@ApplicationScoped
public class SeedData {

    @Inject
    EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (em.createQuery("SELECT COUNT(s) FROM Student s", Long.class).getSingleResult() > 0) {
            return;
        }

        CertSequence seq = new CertSequence();
        seq.id = 1L;
        seq.nextValue = 1;
        em.persist(seq);

        Student zhangsan = student("张三", "zhangsan@example.com");
        Student lisi = student("李四", "lisi@example.com");
        Student wangwu = student("王五", "wangwu@example.com");

        Course java = course("JAVA-101", "Java 后端开发实战", 40, 60);
        Course quality = course("QC-201", "质量管理体系基础", 30, 70);

        // 张三:学时达标,第一次 58 分未过,补考 75 分通过,已确认姓名 -> 可签发
        Enrollment e1 = enroll(zhangsan, java, 42);
        attempt(e1, 58, 1);
        attempt(e1, 75, 2);
        confirm(e1, "张三");

        // 李四:学时不足(35/40),成绩已合格 -> 需补足学时
        Enrollment e2 = enroll(lisi, java, 35);
        attempt(e2, 80, 1);
        confirm(e2, "李四");

        // 王五:学时达标,最近一次考试 65 分未达 70 分及格线 -> 需补考
        Enrollment e3 = enroll(wangwu, quality, 30);
        attempt(e3, 65, 1);
        confirm(e3, "王五");
    }

    private Student student(String name, String email) {
        Student s = new Student();
        s.fullName = name;
        s.email = email;
        em.persist(s);
        return s;
    }

    private Course course(String code, String title, int hours, int pass) {
        Course c = new Course();
        c.code = code;
        c.title = title;
        c.requiredHours = hours;
        c.passingScore = pass;
        em.persist(c);
        return c;
    }

    private Enrollment enroll(Student s, Course c, int hours) {
        Enrollment e = new Enrollment();
        e.student = s;
        e.course = c;
        e.completedHours = hours;
        e.status = hours >= c.requiredHours ? Enrollment.STATUS_COMPLETED : Enrollment.STATUS_IN_PROGRESS;
        em.persist(e);
        return e;
    }

    private void attempt(Enrollment e, int score, int no) {
        ExamAttempt a = new ExamAttempt();
        a.enrollment = e;
        a.score = score;
        a.attemptNo = no;
        a.takenAt = Instant.now();
        em.persist(a);
    }

    private void confirm(Enrollment e, String name) {
        NameConfirmation n = new NameConfirmation();
        n.enrollment = e;
        n.confirmedName = name;
        n.confirmedAt = Instant.now();
        em.persist(n);
    }
}
