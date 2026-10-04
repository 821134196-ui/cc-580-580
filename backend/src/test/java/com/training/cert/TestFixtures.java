package com.training.cert;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/** 测试夹具:通过 REST API 准备数据,返回各资源 id。 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static long createStudent(String name, String email) {
        return post("/api/students", Map.of("fullName", name, "email", email), 200);
    }

    public static long createCourse(String code, String title, int requiredHours, int passingScore) {
        return post("/api/courses", Map.of("code", code, "title", title,
                "requiredHours", requiredHours, "passingScore", passingScore), 200);
    }

    public static long createEnrollment(long studentId, long courseId) {
        return post("/api/enrollments", Map.of("studentId", studentId, "courseId", courseId), 200);
    }

    public static void setHours(long enrollmentId, int hours) {
        given().contentType(ContentType.JSON).body(Map.of("completedHours", hours))
                .when().post("/api/enrollments/" + enrollmentId + "/hours")
                .then().statusCode(200);
    }

    public static void addAttempt(long enrollmentId, int score) {
        given().contentType(ContentType.JSON).body(Map.of("score", score))
                .when().post("/api/enrollments/" + enrollmentId + "/attempts")
                .then().statusCode(200);
    }

    public static void confirmName(long enrollmentId, String name) {
        given().contentType(ContentType.JSON).body(Map.of("confirmedName", name))
                .when().post("/api/enrollments/" + enrollmentId + "/confirm-name")
                .then().statusCode(200);
    }

    public static Response issue(long enrollmentId) {
        return given().contentType(ContentType.JSON)
                .when().post("/api/enrollments/" + enrollmentId + "/issue");
    }

    /** 构造一个完全满足签发条件的报名,返回报名 id。 */
    public static long eligibleEnrollment(String suffix) {
        long student = createStudent("学员" + suffix, "student" + suffix + "@example.com");
        long course = createCourse("COURSE-" + suffix, "课程" + suffix, 10, 60);
        long enrollment = createEnrollment(student, course);
        setHours(enrollment, 10);
        addAttempt(enrollment, 90);
        confirmName(enrollment, "学员" + suffix);
        return enrollment;
    }

    private static long post(String path, Object body, int expectedStatus) {
        Object id = given().contentType(ContentType.JSON).body(body)
                .when().post(path)
                .then().statusCode(expectedStatus)
                .extract().path("id");
        return ((Number) id).longValue();
    }
}
