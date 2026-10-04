package org.acme.cert;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * 姓名更正全流程：申请 → 材料不足退回补件 → 补件重提 → 审核通过换发。
 * 验证旧证作废且不可复活、新证有效、版本链完整。
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CorrectionFlowTest {

    private static int certId;
    private static String certNumber;
    private static int correctionId;
    private static String newCertNumber;

    @Test
    @Order(1)
    void issueCertificateForZhangSanDataStruct() {
        var response = given().contentType(ContentType.JSON)
                .body(Map.of("studentId", 1, "courseId", 2, "confirmedName", "张三", "issuedBy", "教务-测试"))
                .when().post("/api/certificates/issue")
                .then().statusCode(200)
                .body("holderName", equalTo("张三"))
                .body("status", equalTo("ACTIVE"))
                .body("version", equalTo(1))
                .body("certNumber", containsString("CERT-"))
                .extract().jsonPath();
        certId = response.getInt("id");
        certNumber = response.getString("certNumber");
    }

    @Test
    @Order(2)
    void submitCorrectionWithInsufficientMaterials() {
        var response = given().contentType(ContentType.JSON)
                .body(Map.of("certificateId", certId, "requestedName", "张叁",
                        "reason", "姓名中「三」应为「叁」（大写）", "materials", List.of()))
                .when().post("/api/corrections")
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .extract().jsonPath();
        correctionId = response.getInt("id");
    }

    @Test
    @Order(3)
    void staffReturnsForMoreMaterials() {
        given().contentType(ContentType.JSON)
                .body(Map.of("note", "请补充身份证扫描件与户口本姓名页", "reviewedBy", "教务-王老师"))
                .when().post("/api/corrections/" + correctionId + "/return")
                .then().statusCode(200)
                .body("status", equalTo("RETURNED"))
                .body("reviewNote", containsString("身份证"));
    }

    @Test
    @Order(4)
    void studentResubmitsWithMaterials() {
        given().contentType(ContentType.JSON)
                .body(Map.of("certificateId", certId, "requestedName", "张叁",
                        "reason", "姓名中「三」应为「叁」（大写）",
                        "materials", List.of("身份证扫描件", "户口本姓名页")))
                .when().post("/api/corrections/" + correctionId + "/resubmit")
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("submitCount", equalTo(2));
    }

    @Test
    @Order(5)
    void approveRevokesOldAndIssuesNew() {
        var response = given().contentType(ContentType.JSON)
                .body(Map.of("reviewedBy", "教务-王老师"))
                .when().post("/api/corrections/" + correctionId + "/approve")
                .then().statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("newCertNumber", not(certNumber))
                .extract().jsonPath();
        newCertNumber = response.getString("newCertNumber");

        // 旧证已作废，作废原因保留
        given().when().get("/api/certificates/" + certId)
                .then().statusCode(200)
                .body("status", equalTo("REVOKED"))
                .body("revokeReason", containsString("姓名更正"))
                .body("replacedByNumber", equalTo(newCertNumber));
    }

    @Test
    @Order(6)
    void verifyEndpointReflectsCorrection() {
        // 旧证：已作废，并指向替代证书
        given().when().get("/api/verify/" + certNumber)
                .then().statusCode(200)
                .body("found", equalTo(true))
                .body("status", equalTo("REVOKED"))
                .body("replacedByNumber", equalTo(newCertNumber));
        // 新证：有效，版本 V2
        given().when().get("/api/verify/" + newCertNumber)
                .then().statusCode(200)
                .body("found", equalTo(true))
                .body("status", equalTo("ACTIVE"))
                .body("holderName", equalTo("张叁"))
                .body("version", equalTo(2));
        // 不存在的编号
        given().when().get("/api/verify/CERT-1999-999999")
                .then().statusCode(200)
                .body("found", equalTo(false));
    }

    @Test
    @Order(7)
    void revokedCertCannotBeCorrectedOrReactivated() {
        // 已作废证书不能再申请更正
        given().contentType(ContentType.JSON)
                .body(Map.of("certificateId", certId, "requestedName", "张三",
                        "reason", "尝试复活旧证", "materials", List.of("身份证")))
                .when().post("/api/corrections")
                .then().statusCode(409)
                .body("error", containsString("已作废"));
        // 不存在"重新激活"入口
        given().contentType(ContentType.JSON)
                .when().post("/api/certificates/" + certId + "/reactivate")
                .then().statusCode(404);
        // 旧证状态仍然是 REVOKED
        given().when().get("/api/certificates/" + certId)
                .then().statusCode(200)
                .body("status", equalTo("REVOKED"));
    }

    @Test
    @Order(8)
    void versionChainShowsBothCertificates() {
        given().when().get("/api/certificates/" + certId + "/chain")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].version", equalTo(1))
                .body("[0].status", equalTo("REVOKED"))
                .body("[0].revokeReason", containsString("姓名更正"))
                .body("[1].version", equalTo(2))
                .body("[1].status", equalTo("ACTIVE"));
    }

    @Test
    @Order(9)
    void correctionRecordKeepsMaterialsAndReason() {
        given().when().get("/api/corrections/" + correctionId)
                .then().statusCode(200)
                .body("reason", containsString("叁"))
                .body("materials", equalTo(List.of("身份证扫描件", "户口本姓名页")))
                .body("newCertNumber", equalTo(newCertNumber));
    }

    @Test
    @Order(10)
    void mailsWereSent() {
        given().when().get("/api/mails")
                .then().statusCode(200)
                .body("certNumber", hasItem(newCertNumber))
                .body("subject", hasItem(containsString("姓名更正完成")));
    }

    @Test
    @Order(11)
    void cannotApproveTwice() {
        given().contentType(ContentType.JSON)
                .body(Map.of("reviewedBy", "教务-王老师"))
                .when().post("/api/corrections/" + correctionId + "/approve")
                .then().statusCode(409);
    }

    @Test
    @Order(12)
    void nullFieldsStayNull() {
        // 新证未作废，revokedAt/revokeReason 应为空
        given().when().get("/api/verify/" + newCertNumber)
                .then().statusCode(200)
                .body("revokedAt", nullValue())
                .body("revokeReason", nullValue());
    }
}
