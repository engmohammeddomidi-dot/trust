package com.trust.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * اختبار ارتداد لخلل حقيقي: صف بسعر تكلفة سالب كان يُقبل بصمت لأن @Valid لا يتسلسل
 * تلقائيًا داخل عناصر List في DTO، والتحقق اليدوي داخل bulkImport() هو خط الدفاع الفعلي.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemBulkImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bulkImport_createsValidRow_andRejectsInvalidRowWithDescriptiveError() throws Exception {
        String token = loginAndGetToken("owner@trust.demo", "password123");

        // DataSeeder ينشئ مؤسسة "سوبرماركت النجمة" وفرعها الأول بمعرّف 1 في كل سياق اختبار جديد
        String body = """
                {
                  "branchId": 1,
                  "items": [
                    {"name": "Valid Item", "subCategory": "Snacks", "costPrice": 5, "salePrice": 10, "quantity": 20},
                    {"name": "Invalid Item", "subCategory": "Snacks", "costPrice": -3, "salePrice": 10, "quantity": 20}
                  ]
                }
                """;

        mockMvc.perform(post("/api/items/bulk")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("سعر التكلفة")));
    }

    @Test
    void listItems_forBranchOfAnotherOrganization_isRejected() throws Exception {
        String token = loginAndGetToken("owner@trust.demo", "password123");

        // الفرع رقم 2 يخص "صيدلية الشفاء" (المؤسسة الثانية التي يبذرها DataSeeder) وليس مؤسسة owner@trust.demo
        mockMvc.perform(get("/api/items?branchId=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
