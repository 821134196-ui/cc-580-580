package com.training.cert;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 签发:资格条件、编号唯一、并发安全。 */
@QuarkusTest
class CertificateIssuanceTest {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("CERT-\\d{4}-\\d{6}");

    @Test
    void issuesCertificateWithUniqueNumberWhenEligible() {
        long enrollment = TestFixtures.eligibleEnrollment("issue-ok");
        Response response = TestFixtures.issue(enrollment);
        response.then().statusCode(200)
                .body("certificateNumber", matchesPattern(NUMBER_PATTERN))
                .body("status", equalTo("VALID"))
                .body("version", equalTo(1))
                .body("score", equalTo(90))
                .body("studentName", equalTo("学员issue-ok"));

        String number = response.jsonPath().getString("certificateNumber");
        given().when().get("/api/verify/" + number)
                .then().statusCode(200)
                .body("found", equalTo(true))
                .body("valid", equalTo(true));
    }

    @Test
    void rejectsIssuanceWhenNotEligible() {
        long student = TestFixtures.createStudent("未达标", "not-eligible@example.com");
        long course = TestFixtures.createCourse("COURSE-not-eligible", "未达标课程", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        // 学时不足、无考试、未确认姓名
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", allOf(containsString("学时不足"), containsString("尚无考试记录"),
                        containsString("尚未确认姓名")));
    }

    @Test
    void rejectsSecondIssuanceForSameEnrollment() {
        long enrollment = TestFixtures.eligibleEnrollment("issue-twice");
        TestFixtures.issue(enrollment).then().statusCode(200);
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", containsString("已存在有效证书"));

        // 版本链上仍然只有一张证书
        given().when().get("/api/enrollments/" + enrollment + "/certificates")
                .then().statusCode(200).body("size()", equalTo(1));
    }

    @Test
    void concurrentIssuanceOnSameEnrollmentYieldsExactlyOneValidCertificate() throws Exception {
        long enrollment = TestFixtures.eligibleEnrollment("issue-race");
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statuses = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    statuses.add(TestFixtures.issue(enrollment).statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        long successes = statuses.stream().filter(s -> s == 200).count();
        assertEquals(1, successes, "并发签发同一报名,必须恰好成功一次,实际状态: " + statuses);
        assertEquals(threads - 1, statuses.stream().filter(s -> s == 409).count());

        // 数据库中该报名只有一张有效证书
        List<Map<String, Object>> chain = given().when()
                .get("/api/enrollments/" + enrollment + "/certificates")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertEquals(1, chain.size());
        assertEquals("VALID", chain.get(0).get("status"));
    }

    @Test
    void concurrentIssuanceAcrossEnrollmentsProducesGloballyUniqueNumbers() throws Exception {
        int threads = 8;
        long[] enrollments = new long[threads];
        for (int i = 0; i < threads; i++) {
            enrollments[i] = TestFixtures.eligibleEnrollment("issue-uniq-" + i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> numbers = new ConcurrentLinkedQueue<>();
        for (long enrollment : enrollments) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    Response r = TestFixtures.issue(enrollment);
                    assertEquals(200, r.statusCode());
                    numbers.add(r.jsonPath().getString("certificateNumber"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(threads, numbers.size());
        Set<String> distinct = Set.copyOf(numbers);
        assertEquals(threads, distinct.size(), "证书编号必须全局唯一: " + numbers);
        numbers.forEach(n -> assertTrue(NUMBER_PATTERN.matcher(n).matches(), n));
    }

    @Test
    void requiresConfirmedNameBeforeIssuance() {
        long student = TestFixtures.createStudent("未确认", "no-confirm@example.com");
        long course = TestFixtures.createCourse("COURSE-no-confirm", "未确认课程", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 10);
        TestFixtures.addAttempt(enrollment, 90);
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", containsString("尚未确认姓名"));
    }
}
