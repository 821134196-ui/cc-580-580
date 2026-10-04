package com.training.cert;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 姓名更正:退回补件、作废旧证发新版、旧证永久失效。 */
@QuarkusTest
class NameCorrectionTest {

    @Test
    void fullCorrectionFlowVoidsOldCertificateAndIssuesNewVersion() {
        long enrollment = TestFixtures.eligibleEnrollment("correct-flow");
        String oldNumber = TestFixtures.issue(enrollment).jsonPath().getString("certificateNumber");
        long oldCertId = TestFixtures.issue(enrollment).statusCode(); // 409,仅确认重复签发被拒
        assertEquals(409, oldCertId);

        // 1. 学员提交更正申请(材料不足)
        long certId = certificateIdOf(enrollment);
        Response submit = given().contentType(ContentType.JSON)
                .body(Map.of("requestedName", "学员correct-flow(曾用名)", "reason", "报名时姓名填写错误",
                        "materials", "身份证复印件"))
                .when().post("/api/certificates/" + certId + "/corrections");
        submit.then().statusCode(200).body("status", equalTo("PENDING"));
        long requestId = submit.jsonPath().getLong("id");

        // 2. 教务核对材料,认为不足 -> 退回补件
        given().contentType(ContentType.JSON)
                .body(Map.of("decision", "RETURN", "reviewNote", "缺少公安机关出具的姓名变更证明"))
                .when().post("/api/corrections/" + requestId + "/review")
                .then().statusCode(200).body("status", equalTo("RETURNED"));

        // 退回后不能重复审核
        given().contentType(ContentType.JSON)
                .body(Map.of("decision", "APPROVE", "reviewNote", ""))
                .when().post("/api/corrections/" + requestId + "/review")
                .then().statusCode(409);

        // 3. 学员补件 -> 回到待审
        given().contentType(ContentType.JSON)
                .body(Map.of("materials", "公安机关姓名变更证明扫描件"))
                .when().post("/api/corrections/" + requestId + "/materials")
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("materials", containsString("姓名变更证明"));

        // 4. 教务审核通过 -> 旧证作废,签发第 2 版新证
        Response approved = given().contentType(ContentType.JSON)
                .body(Map.of("decision", "APPROVE", "reviewNote", "材料齐全,予以更正"))
                .when().post("/api/corrections/" + requestId + "/review");
        approved.then().statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("issuedCertificateNumber", notNullValue())
                .body("issuedCertificateNumber", not(equalTo(oldNumber)));
        String newNumber = approved.jsonPath().getString("issuedCertificateNumber");

        // 5. 核验:旧证作废且载明原因,新证有效
        given().when().get("/api/verify/" + oldNumber)
                .then().statusCode(200)
                .body("found", equalTo(true))
                .body("valid", equalTo(false))
                .body("status", equalTo("VOID"))
                .body("voidReason", containsString("姓名更正"))
                .body("replacedByNumber", equalTo(newNumber));
        given().when().get("/api/verify/" + newNumber)
                .then().statusCode(200)
                .body("valid", equalTo(true))
                .body("version", equalTo(2))
                .body("studentName", equalTo("学员correct-flow(曾用名)"));

        // 6. 版本链:v1 已作废 + v2 当前有效,更正原因与材料保留在新版记录上
        List<Map<String, Object>> chain = given().when()
                .get("/api/enrollments/" + enrollment + "/certificates")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertEquals(2, chain.size());
        assertEquals("VOID", chain.get(0).get("status"));
        assertEquals("VALID", chain.get(1).get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> correction = (Map<String, Object>) chain.get(1).get("producedByCorrection");
        assertNotNull(correction);
        assertEquals("报名时姓名填写错误", correction.get("reason"));
        assertTrue(correction.get("materials").toString().contains("身份证复印件"));
        assertTrue(correction.get("materials").toString().contains("姓名变更证明"));

        // 7. 旧证作废后不得再申请更正(不可重新激活,只能以新证为准)
        given().contentType(ContentType.JSON)
                .body(Map.of("requestedName", "另一个名字", "reason", "再次更正", "materials", "材料"))
                .when().post("/api/certificates/" + certId + "/corrections")
                .then().statusCode(409)
                .body("error", containsString("有效证书"));

        // 8. 邮件回执:签发、退回、更正完成各一封
        given().when().get("/api/emails").then().statusCode(200)
                .body("subject", hasItems(
                        "证书已签发:" + oldNumber,
                        "姓名更正申请被退回,请补充材料",
                        "姓名更正完成,新证书已签发:" + newNumber));
    }

    @Test
    void rejectsCorrectionWithSameNameOrMissingMaterials() {
        long enrollment = TestFixtures.eligibleEnrollment("correct-bad");
        TestFixtures.issue(enrollment);
        long certId = certificateIdOf(enrollment);

        given().contentType(ContentType.JSON)
                .body(Map.of("requestedName", "学员correct-bad", "reason", "测试", "materials", "材料"))
                .when().post("/api/certificates/" + certId + "/corrections")
                .then().statusCode(400).body("error", containsString("一致"));

        given().contentType(ContentType.JSON)
                .body(Map.of("requestedName", "新名字", "reason", "测试", "materials", " "))
                .when().post("/api/certificates/" + certId + "/corrections")
                .then().statusCode(400).body("error", containsString("证明材料"));
    }

    @Test
    void rejectsDuplicateOpenCorrectionRequests() {
        long enrollment = TestFixtures.eligibleEnrollment("correct-dup");
        TestFixtures.issue(enrollment);
        long certId = certificateIdOf(enrollment);
        Map<String, String> body = Map.of("requestedName", "新名字", "reason", "填错", "materials", "身份证");
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/certificates/" + certId + "/corrections")
                .then().statusCode(200);
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/certificates/" + certId + "/corrections")
                .then().statusCode(409).body("error", containsString("处理中"));
    }

    @Test
    void cannotConfirmNameDirectlyAfterIssuance() {
        long enrollment = TestFixtures.eligibleEnrollment("correct-lock");
        TestFixtures.issue(enrollment);
        given().contentType(ContentType.JSON).body(Map.of("confirmedName", "想改名"))
                .when().post("/api/enrollments/" + enrollment + "/confirm-name")
                .then().statusCode(409).body("error", containsString("更正流程"));
    }

    private long certificateIdOf(long enrollmentId) {
        List<Map<String, Object>> chain = given().when()
                .get("/api/enrollments/" + enrollmentId + "/certificates")
                .then().statusCode(200).extract().jsonPath().getList("$");
        return ((Number) chain.get(chain.size() - 1).get("id")).longValue();
    }
}
