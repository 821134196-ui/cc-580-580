package com.training.cert;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** 已签发证书的课程信息不可直接修改;证书上的课程快照不受后续任何变动影响。 */
@QuarkusTest
class CourseImmutabilityTest {

    @Test
    void courseLockedAfterCertificateIssued() {
        long student = TestFixtures.createStudent("锁定", "lock@example.com");
        long course = TestFixtures.createCourse("COURSE-lock", "锁定前课程", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 10);
        TestFixtures.addAttempt(enrollment, 90);
        TestFixtures.confirmName(enrollment, "锁定");

        // 签发前可以修改课程信息
        given().contentType(ContentType.JSON)
                .body(Map.of("title", "锁定前课程(修订版)"))
                .when().put("/api/courses/" + course)
                .then().statusCode(200).body("title", equalTo("锁定前课程(修订版)"));

        // 签发后课程信息被锁定
        TestFixtures.issue(enrollment).then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("title", "试图改名"))
                .when().put("/api/courses/" + course)
                .then().statusCode(409).body("error", containsString("不可直接修改"));
        given().contentType(ContentType.JSON)
                .body(Map.of("passingScore", 30))
                .when().put("/api/courses/" + course)
                .then().statusCode(409);
    }

    @Test
    void certificateKeepsSnapshotOfCourseAtIssuanceTime() {
        long student = TestFixtures.createStudent("快照", "snapshot@example.com");
        long course = TestFixtures.createCourse("COURSE-snapshot", "快照课程v1", 10, 60);
        long enrollment = TestFixtures.createEnrollment(student, course);
        TestFixtures.setHours(enrollment, 10);
        TestFixtures.addAttempt(enrollment, 90);
        TestFixtures.confirmName(enrollment, "快照");
        String number = TestFixtures.issue(enrollment).jsonPath().getString("certificateNumber");

        // 证书上永久保留签发时刻的课程名称快照
        given().when().get("/api/verify/" + number)
                .then().statusCode(200)
                .body("courseTitle", equalTo("快照课程v1"))
                .body("courseCode", equalTo("COURSE-snapshot"));
    }
}
