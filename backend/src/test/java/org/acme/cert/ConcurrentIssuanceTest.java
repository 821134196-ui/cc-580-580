package org.acme.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.acme.cert.dto.Dtos.CertificateDto;
import org.acme.cert.dto.Dtos.EligibilityDto;
import org.acme.cert.model.CertStatus;
import org.acme.cert.service.BusinessException;
import org.acme.cert.service.EligibilityService;
import org.acme.cert.service.IssuanceFacade;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * 并发签发验证：多人同时签发不得产生重复编号，也不得出现两张有效证书。
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConcurrentIssuanceTest {

    @Inject
    IssuanceFacade issuanceFacade;

    @Inject
    EligibilityService eligibilityService;

    @Inject
    EntityManager em;

    /** 同一学员同一课程被 8 个线程同时签发：只能成功 1 次，其余 409 */
    @Test
    @Order(1)
    void concurrentIssueSameStudentOnlyOneSucceeds() throws Exception {
        // 种子数据：张三(1) 的 JAVA-BASIC(1) 已满足学时+成绩
        EligibilityDto before = QuarkusTransaction.requiringNew().call(
                () -> eligibilityService.evaluate(1L, 1L));
        assertTrue(before.eligible(), "前置条件：张三应满足签发条件");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            tasks.add(() -> {
                ready.countDown();
                go.await();
                try {
                    CertificateDto dto = issuanceFacade.issue(1L, 1L, "张三", "并发测试员" + idx);
                    return "OK:" + dto.certNumber();
                } catch (BusinessException e) {
                    return "REJECTED:" + e.status();
                }
            });
        }
        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> task : tasks) {
            futures.add(pool.submit(task));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        long ok = results.stream().filter(r -> r.startsWith("OK")).count();
        long rejected = results.stream().filter(r -> r.equals("REJECTED:409")).count();
        assertEquals(1, ok, "并发签发同一学员只能成功一次，实际: " + results);
        assertEquals(threads - 1, rejected, "其余并发请求应全部 409，实际: " + results);

        Long activeCount = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(c) from Certificate c where c.student.id = 1 and c.course.id = 1 "
                        + "and c.status = :st", Long.class)
                .setParameter("st", CertStatus.ACTIVE).getSingleResult());
        assertEquals(1, activeCount, "数据库中只能有一张有效证书");
    }

    /** 重复签发（串行）同样被拒绝 */
    @Test
    @Order(2)
    void duplicateIssueRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> issuanceFacade.issue(1L, 1L, "张三", "教务"));
        assertEquals(409, e.status());
        assertTrue(e.getMessage().contains("已持有有效证书"));
    }

    /** 多名学员同时签发：全部成功且编号互不重复 */
    @Test
    @Order(3)
    void concurrentIssueDifferentStudentsUniqueNumbers() throws Exception {
        // 准备 6 名满足条件的学员（独立课程，避免与种子数据冲突）
        QuarkusTransaction.requiringNew().run(() -> {
            for (long sid = 101; sid <= 106; sid++) {
                long courseId = sid - 10;
                em.createNativeQuery("insert into course (id, code, title, required_hours, pass_score) "
                        + "values (:id, :code, :title, 10, 60)")
                        .setParameter("id", courseId).setParameter("code", "CON-" + courseId)
                        .setParameter("title", "并发课程" + courseId).executeUpdate();
                em.createNativeQuery("insert into student (id, student_no, full_name, email, id_card_no) "
                        + "values (:id, :no, :name, :email, :idcard)")
                        .setParameter("id", sid).setParameter("no", "S9" + sid)
                        .setParameter("name", "学员" + sid)
                        .setParameter("email", "s" + sid + "@example.com")
                        .setParameter("idcard", "IDCARD" + sid).executeUpdate();
                em.createNativeQuery("insert into study_record (student_id, course_id, completed_hours, "
                        + "updated_at) values (:sid, :cid, 20, 0)")
                        .setParameter("sid", sid).setParameter("cid", courseId).executeUpdate();
                em.createNativeQuery("insert into exam_attempt (student_id, course_id, attempt_no, score, "
                        + "taken_at) values (:sid, :cid, 1, 80, 0)")
                        .setParameter("sid", sid).setParameter("cid", courseId).executeUpdate();
            }
        });

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<CertificateDto>> futures = new ArrayList<>();
        for (long sid = 101; sid <= 106; sid++) {
            final long studentId = sid;
            final long courseId = sid - 10;
            futures.add(pool.submit(() -> {
                go.await();
                return issuanceFacade.issue(studentId, courseId, "学员" + studentId, "并发测试");
            }));
        }
        go.countDown();
        Set<String> numbers = new HashSet<>();
        for (Future<CertificateDto> f : futures) {
            CertificateDto dto = f.get(30, TimeUnit.SECONDS);
            assertNotNull(dto.certNumber());
            assertTrue(dto.certNumber().matches("CERT-\\d{4}-\\d{6}"), "编号格式: " + dto.certNumber());
            numbers.add(dto.certNumber());
        }
        pool.shutdown();
        assertEquals(threads, numbers.size(), "证书编号不得重复: " + numbers);
    }
}
