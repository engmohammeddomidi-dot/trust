package com.trust.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * يغطي مصفوفة رموز حالة HTTP للمصادقة/التفويض: غياب/عدم صلاحية الرمز يجب أن يُرجع 401 دائمًا،
 * وامتلاك رمز صالح بدور غير كافٍ يجب أن يُرجع 403 - وليس العكس. هذا اختبار ارتداد (regression)
 * لخلل حقيقي وُجِد أثناء الاختبار اليدوي: استخدام response.sendError() مع entryPoint/accessDeniedHandler
 * مخصصين كان يُعيد توجيه الطلب داخليًا إلى /error فيُعاد تشغيله عبر سلسلة الأمان فيتحول 403 إلى 401 خطأً.
 * التعديل الحالي يستخدم response.setStatus() مباشرة لتفادي إعادة التوجيه هذه.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard?organizationId=1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard?organizationId=1")
                        .header("Authorization", "Bearer corrupted.invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenWrongRole_onAdminOnlyRoute_returns403NotUnauthorized() throws Exception {
        String token = loginAndGetToken("owner@trust.demo", "password123");

        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validTokenCorrectRole_onAdminOnlyRoute_returns200() throws Exception {
        String token = loginAndGetToken("admin@trust.demo", "admin123");

        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void validToken_onAuthenticatedRoute_returns200() throws Exception {
        String token = loginAndGetToken("owner@trust.demo", "password123");

        mockMvc.perform(get("/api/dashboard?organizationId=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
