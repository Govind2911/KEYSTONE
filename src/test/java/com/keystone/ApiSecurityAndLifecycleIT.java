package com.keystone;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks against a real MySQL (Testcontainers) with the Flyway migrations + demo seed applied.
 * They call the API directly, exactly as an attacker would, to prove the rules hold without the UI.
 * Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "DB_USERNAME=test", "DB_PASSWORD=test",
        "keystone.jwt.secret=test-secret-test-secret-test-secret-123",
        "keystone.storage.dir=target/test-uploads",
        "keystone.sla.scan-interval-ms=3600000"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiSecurityAndLifecycleIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired MockMvc mvc;

    private static Long createdId;

    private String login(String email) throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions get(String url, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", token));
    }

    private ResultActions post(String url, String token, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------------------- authentication

    @Test @Order(1)
    void requestsWithoutAToken_areRejected() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/work-orders")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/work-orders").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test @Order(2)
    void wrongPassword_returns401_withStructuredError() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"manager@meridian.test\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test @Order(3)
    void thereIsNoPublicSelfRegistration() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eve\",\"email\":\"eve@x.test\",\"password\":\"Password@123\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(4)
    void validationErrors_useTheSharedShape() throws Exception {
        String mgr = login("manager@meridian.test");
        post("/api/work-orders", mgr, "{\"title\":\"\",\"priority\":\"HIGH\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")));
    }

    // ------------------------------------------- threat 1: customer reading another customer's data

    @Test @Order(5)
    void customerCannotReadAnotherCustomersWorkOrder_byChangingTheId() throws Exception {
        String northgate = login("customer1@northgate.test");   // customer 1
        get("/api/work-orders/2", northgate).andExpect(status().isNotFound());   // WO 2 belongs to customer 2
        get("/api/work-orders/2/history", northgate).andExpect(status().isNotFound());
        get("/api/work-orders/2/attachments", northgate).andExpect(status().isNotFound());
        get("/api/customers/2/sites", northgate).andExpect(status().isNotFound());
    }

    @Test @Order(6)
    void customerList_isScopedToTheirOwnOrganisation() throws Exception {
        get("/api/work-orders?size=100", login("customer1@northgate.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].customer.id", everyItem(is(1))));
    }

    @Test @Order(7)
    void customerView_hidesInternalFields() throws Exception {
        get("/api/work-orders/4", login("customer1@northgate.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.slaDueAt").doesNotExist())
                .andExpect(jsonPath("$.partsCost").doesNotExist())
                .andExpect(jsonPath("$.history[0].note").doesNotExist());
    }

    @Test @Order(8)
    void customerCannotRaiseARequestForSomeoneElsesSite() throws Exception {
        post("/api/work-orders", login("customer1@northgate.test"),
                "{\"title\":\"x\",\"description\":\"y\",\"priority\":\"LOW\",\"siteId\":3}")
                .andExpect(status().isNotFound());
    }

    @Test @Order(9)
    void customerCannotReachStaffEndpoints() throws Exception {
        String c = login("customer1@northgate.test");
        get("/api/reports/summary", c).andExpect(status().isForbidden());
        get("/api/users", c).andExpect(status().isForbidden());
        get("/api/customers", c).andExpect(status().isForbidden());
        post("/api/work-orders/4/status", c, "{\"toStatus\":\"CANCELLED\"}").andExpect(status().isForbidden());
    }

    // -------------------------------------------------- threat 2: role abuse on the lifecycle

    @Test @Order(10)
    void technicianCannotCloseAJob_andCannotTouchAnotherTechniciansJob() throws Exception {
        String tech1 = login("tech1@meridian.test");   // id 3, assigned to WO 10 (COMPLETED)
        post("/api/work-orders/10/status", tech1, "{\"toStatus\":\"CLOSED\"}").andExpect(status().isForbidden());
        String tech2 = login("tech2@meridian.test");   // id 4: WO 4 belongs to tech1
        post("/api/work-orders/4/status", tech2, "{\"toStatus\":\"IN_PROGRESS\"}").andExpect(status().isNotFound());
    }

    @Test @Order(11)
    void illegalLifecycleJump_returns409() throws Exception {
        post("/api/work-orders/1/status", login("manager@meridian.test"), "{\"toStatus\":\"COMPLETED\"}")
                .andExpect(status().isConflict());
        post("/api/work-orders/12/status", login("manager@meridian.test"), "{\"toStatus\":\"NEW\"}")   // CLOSED is terminal
                .andExpect(status().isConflict());
    }

    @Test @Order(12)
    void dispatcherCannotSeeManagerReports() throws Exception {
        get("/api/reports/summary", login("dispatcher@meridian.test")).andExpect(status().isForbidden());
        get("/api/reports/summary", login("manager@meridian.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusCounts.NEW").exists());
    }

    // ----------------------------------------------------------- full lifecycle, end to end

    @Test @Order(20)
    void fullLifecycle_createAssignStartLogCompleteClose() throws Exception {
        String dispatcher = login("dispatcher@meridian.test");
        String tech1 = login("tech1@meridian.test");
        String manager = login("manager@meridian.test");

        String created = post("/api/work-orders", dispatcher,
                "{\"title\":\"IT lifecycle\",\"description\":\"end to end\",\"priority\":\"HIGH\",\"customerId\":1,\"siteId\":1}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.code", startsWith("WO-")))
                .andReturn().getResponse().getContentAsString();
        createdId = ((Number) JsonPath.read(created, "$.id")).longValue();
        String base = "/api/work-orders/" + createdId;

        // cannot start before it is assigned, and a manager may not start on a technician's behalf
        post(base + "/status", tech1, "{\"toStatus\":\"IN_PROGRESS\"}").andExpect(status().isNotFound());
        post(base + "/assign", dispatcher, "{\"technicianId\":3}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        post(base + "/status", manager, "{\"toStatus\":\"IN_PROGRESS\"}").andExpect(status().isForbidden());

        post(base + "/status", tech1, "{\"toStatus\":\"IN_PROGRESS\"}").andExpect(status().isOk());

        // parts + time while in progress
        post(base + "/parts", tech1, "{\"partId\":1,\"quantity\":2}").andExpect(status().isCreated());
        post(base + "/time", tech1, "{\"minutes\":45,\"note\":\"diagnosis\"}").andExpect(status().isCreated());

        post(base + "/status", tech1, "{\"toStatus\":\"COMPLETED\"}").andExpect(status().isOk());
        post(base + "/status", tech1, "{\"toStatus\":\"CLOSED\"}").andExpect(status().isForbidden());
        post(base + "/status", manager, "{\"toStatus\":\"CLOSED\",\"note\":\"signed off\"}").andExpect(status().isOk());

        get(base, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.labourMinutes").value(45))
                .andExpect(jsonPath("$.partsCost", greaterThan(0.0)))
                .andExpect(jsonPath("$.history.length()").value(5));   // created, assigned, started, completed, closed

        // closed orders are immutable
        mvc.perform(put(base).header("Authorization", manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"edited\",\"description\":\"x\",\"priority\":\"LOW\"}"))
                .andExpect(status().isConflict());
        post(base + "/assign", dispatcher, "{\"technicianId\":4}").andExpect(status().isConflict());
    }

    // ------------------------------------------------ transactional parts: stock never negative

    @Test @Order(21)
    void insufficientStock_isRejected_andStockIsUnchanged() throws Exception {
        String dispatcher = login("dispatcher@meridian.test");
        String tech1 = login("tech1@meridian.test");
        String created = post("/api/work-orders", dispatcher,
                "{\"title\":\"stock test\",\"description\":\"d\",\"priority\":\"MEDIUM\",\"customerId\":1,\"siteId\":1}")
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();
        post("/api/work-orders/" + id + "/assign", dispatcher, "{\"technicianId\":3}").andExpect(status().isOk());
        post("/api/work-orders/" + id + "/status", tech1, "{\"toStatus\":\"IN_PROGRESS\"}").andExpect(status().isOk());

        // part 13 (scroll compressor) has exactly 1 unit in stock
        post("/api/work-orders/" + id + "/parts", tech1, "{\"partId\":13,\"quantity\":5}").andExpect(status().isConflict());
        get("/api/parts?q=CMP-SCROLL", tech1).andExpect(jsonPath("$.content[0].stockQty").value(1));
        get("/api/work-orders/" + id + "/parts", tech1).andExpect(jsonPath("$.length()").value(0));

        post("/api/work-orders/" + id + "/parts", tech1, "{\"partId\":13,\"quantity\":1}").andExpect(status().isCreated());
        post("/api/work-orders/" + id + "/parts", tech1, "{\"partId\":13,\"quantity\":1}").andExpect(status().isConflict());
    }

    @Test @Order(22)
    void customerRequest_entersTheSamePipeline() throws Exception {
        String customer = login("customer1@northgate.test");
        post("/api/work-orders", customer, "{\"title\":\"Door jammed\",\"description\":\"Main door\",\"priority\":\"LOW\",\"siteId\":1}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("NEW"));
        post("/api/work-orders", customer, "{\"title\":\"Fire\",\"description\":\"Urgent\",\"priority\":\"URGENT\",\"siteId\":1}")
                .andExpect(status().isBadRequest());
        get("/api/work-orders?status=NEW&q=Door", login("dispatcher@meridian.test"))
                .andExpect(jsonPath("$.content[0].title").value("Door jammed"));
    }

    @Test @Order(23)
    void attachments_uploadStreamDownloadDelete_andNeverCrossCustomers() throws Exception {
        String c1 = login("customer1@northgate.test");
        String c2 = login("customer2@harbormed.test");
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        String body = mvc.perform(multipart("/api/work-orders/4/attachments")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png)).header("Authorization", c1))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.storage").value("LOCAL"))
                .andExpect(jsonPath("$.publicUrl").doesNotExist()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        get("/api/attachments/" + id + "/view", c1).andExpect(status().isOk());
        get("/api/attachments/" + id + "/download", c1).andExpect(status().isOk());
        get("/api/attachments/" + id + "/view", c2).andExpect(status().isNotFound());       // another customer: invisible
        get("/api/work-orders/4/attachments", c1).andExpect(jsonPath("$[0].fileName").value("photo.png"));
        mvc.perform(multipart("/api/work-orders/4/attachments")                                // executable/HTML types are refused
                        .file(new MockMultipartFile("file", "x.html", "text/html", "<script>".getBytes())).header("Authorization", c1))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/attachments/" + id).header("Authorization", c2)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/attachments/" + id).header("Authorization", c1)).andExpect(status().isNoContent());
    }

    @Test @Order(24)
    void skillBasedDispatch_putsMatchingTechniciansFirst() throws Exception {
        get("/api/users/technicians?category=PLUMBING", login("dispatcher@meridian.test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].skillMatch").value(true));
    }

    @Test @Order(25)
    void auditLog_isManagerOnly() throws Exception {
        get("/api/audit", login("dispatcher@meridian.test")).andExpect(status().isForbidden());
        get("/api/audit?size=5", login("manager@meridian.test")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].workOrderCode").exists());
    }
}
