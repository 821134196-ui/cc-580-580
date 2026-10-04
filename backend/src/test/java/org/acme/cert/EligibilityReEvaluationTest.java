package org.acme.cert;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * 资格实时重判：补考/重修后重新判定，不使用过期成绩快照；
 * 已签发证书的课程信息禁止修改。
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EligibilityReEvaluationTest {

    @Test
    @Order(1)
    void liSiNotEligibleBeforeRetake() {
        // 李四 JAVA-BASIC：学时够，但最近一次考试 45 分未通过
        given().when().get("/api/eligibility?studentId=2&courseId=1")
                .then().statusCode(200)
                .body("eligible", equalTo(false))
                .body("hoursOk", equalTo(true))
                .body("examOk", equalTo(false))
                .body("latestScore", equalTo(45))
                .body("reasons", hasItem(containsString("未通过")));
        // 直接签发被拒
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 2, "courseId", 1, "confirmedName", "李四"))
                .when().post("/api/certificates/issue")
                .then().statusCode(409);
    }

    @Test
    @Order(2)
    void afterRetakeEligibilityIsRecomputed() {
        // 补考通过（78 分）
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 2, "courseId", 1, "score", 78))
                .when().post("/api/records/exam")
                .then().statusCode(200)
                .body("attemptNo", equalTo(2))
                .body("score", equalTo(78));

        // 资格立即变为满足——以最新成绩为准，而非旧快照
        given().when().get("/api/eligibility?studentId=2&courseId=1")
                .then().statusCode(200)
                .body("eligible", equalTo(true))
                .body("examOk", equalTo(true))
                .body("latestScore", equalTo(78))
                .body("attemptCount", equalTo(2));

        // 现在可以签发
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 2, "courseId", 1, "confirmedName", "李四", "issuedBy", "教务-测试"))
                .when().post("/api/certificates/issue")
                .then().statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("finalScore", equalTo(78));

        // 签发后再次判定：已有有效证书，不可重复签发
        given().when().get("/api/eligibility?studentId=2&courseId=1")
                .then().statusCode(200)
                .body("eligible", equalTo(false))
                .body("hasActiveCert", equalTo(true));
    }

    @Test
    @Order(3)
    void wangWuBecomesEligibleAfterHoursAndExam() {
        // 王五 DATA-STRUCT：学时 20/48 不足，且无考试记录
        given().when().get("/api/eligibility?studentId=3&courseId=2")
                .then().statusCode(200)
                .body("eligible", equalTo(false))
                .body("hoursOk", equalTo(false))
                .body("reasons", hasItem(containsString("学时不足")))
                .body("reasons", hasItem(containsString("尚无考试记录")));

        // 补足学时
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 3, "courseId", 2, "completedHours", 50))
                .when().put("/api/records/study")
                .then().statusCode(200)
                .body("completedHours", equalTo(50));

        // 登记考试成绩
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 3, "courseId", 2, "score", 82))
                .when().post("/api/records/exam")
                .then().statusCode(200);

        // 重新判定：满足条件
        given().when().get("/api/eligibility?studentId=3&courseId=2")
                .then().statusCode(200)
                .body("eligible", equalTo(true));
    }

    @Test
    @Order(4)
    void courseLockedAfterCertificateIssued() {
        // 新建一门课程：签发前可以修改
        int courseId = given().contentType(ContentType.JSON)
                .body(Map.of("code", "LOCK-DEMO", "title", "锁定演示课", "requiredHours", 8, "passScore", 60))
                .when().post("/api/courses")
                .then().statusCode(200)
                .body("locked", equalTo(false))
                .extract().jsonPath().getInt("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("code", "LOCK-DEMO", "title", "锁定演示课（改名）", "requiredHours", 8, "passScore", 60))
                .when().put("/api/courses/" + courseId)
                .then().statusCode(200)
                .body("title", equalTo("锁定演示课（改名）"));

        // 新学员完成该课程并获签发
        int studentId = given().contentType(ContentType.JSON)
                .body(Map.of("studentNo", "S2026099", "fullName", "测试员", "email", "tester@example.com",
                        "idCardNo", "110101199901019999"))
                .when().post("/api/students")
                .then().statusCode(200)
                .extract().jsonPath().getInt("id");
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", studentId, "courseId", courseId, "completedHours", 10))
                .when().put("/api/records/study").then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", studentId, "courseId", courseId, "score", 90))
                .when().post("/api/records/exam").then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("studentId", studentId, "courseId", courseId, "confirmedName", "测试员"))
                .when().post("/api/certificates/issue").then().statusCode(200);

        // 已签发证书 → 课程信息禁止修改
        given().contentType(ContentType.JSON)
                .body(Map.of("code", "LOCK-DEMO", "title", "试图再改", "requiredHours", 8, "passScore", 60))
                .when().put("/api/courses/" + courseId)
                .then().statusCode(409)
                .body("error", containsString("不可再修改"));

        // 列表中该课程标记为 locked
        given().when().get("/api/courses")
                .then().statusCode(200)
                .body("find { it.code == 'LOCK-DEMO' }.locked", equalTo(true));
    }
}
