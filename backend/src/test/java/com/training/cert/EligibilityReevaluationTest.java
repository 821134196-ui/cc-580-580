package com.training.cert;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 补考/重修后资格必须实时重算:判定永远读取当前学时与最近一次考试成绩,
 * 不允许沿用旧的"成绩快照"。
 */
@QuarkusTest
class EligibilityReevaluationTest {

    @Test
    void retakeExamImmediatelyChangesEligibility() {
        long student = TestFixtures.createStudent("补考生", "retake@example.com");
        long course = TestFixtures.createCourse("COURSE-retake", "补考课程", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 10);
        TestFixtures.confirmName(enrollment, "补考生");

        // 第一次考试 55 分,不及格 -> 不可签发
        TestFixtures.addAttempt(enrollment, 55);
        eligibility(enrollment, false, 55);
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", containsString("未达及格线"));

        // 补考 88 分通过 -> 立即可签发(取最新成绩,而非历史快照)
        TestFixtures.addAttempt(enrollment, 88);
        eligibility(enrollment, true, 88);
        TestFixtures.issue(enrollment).then().statusCode(200)
                .body("score", equalTo(88));
    }

    @Test
    void failingRetakeInvalidatesPreviouslyPassingSnapshot() {
        long student = TestFixtures.createStudent("倒退生", "regress@example.com");
        long course = TestFixtures.createCourse("COURSE-regress", "倒退课程", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 10);
        TestFixtures.confirmName(enrollment, "倒退生");

        // 曾经考过 90 分(旧快照合格)
        TestFixtures.addAttempt(enrollment, 90);
        eligibility(enrollment, true, 90);

        // 重考只拿了 40 分:最新成绩不合格,不得沿用旧的 90 分快照签发
        TestFixtures.addAttempt(enrollment, 40);
        eligibility(enrollment, false, 40);
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", containsString("未达及格线"));
    }

    @Test
    void retakingCourseResetsEligibilityViaHours() {
        long student = TestFixtures.createStudent("重修生", "relearn@example.com");
        long course = TestFixtures.createCourse("COURSE-relearn", "重修课程", 20, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 20);
        TestFixtures.addAttempt(enrollment, 75);
        TestFixtures.confirmName(enrollment, "重修生");
        eligibility(enrollment, true, 75);

        // 重修导致学时清零 -> 资格立即失效
        TestFixtures.setHours(enrollment, 5);
        eligibility(enrollment, false, 75);
        TestFixtures.issue(enrollment).then().statusCode(409)
                .body("error", containsString("学时不足"));

        // 补足学时后恢复
        TestFixtures.setHours(enrollment, 22);
        eligibility(enrollment, true, 75);
    }

    private void eligibility(long enrollmentId, boolean expectedEligible, int expectedLatestScore) {
        given().when().get("/api/enrollments/" + enrollmentId + "/eligibility")
                .then().statusCode(200)
                .body("eligible", equalTo(expectedEligible))
                .body("latestScore", equalTo(expectedLatestScore));
    }
}
