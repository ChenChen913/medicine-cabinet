package com.family.medicinecabinet.controller;

import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 药品创建入参校验用例（对应冻结契约 V1 / V2 / V3 / V7）
 *
 * <p>用完整 Spring 上下文 + MockMvc 走真实的参数解析 → 校验（请求体 {@code @Valid}
 * 与 {@code @RequestParam} 上的 {@code @Positive}）→ 全局异常处理链路，
 * 断言对象是最终响应体（HTTP 200 + body.code），而不是测试自己抛出的异常。
 * Mapper / 库存服务以 Mock 替换，确保被拒绝的请求既不落库也不进服务层。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicineValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicineMapper medicineMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("创建药品缺 name → body.code=400（原为 500），且不得落库")
    void createWithoutNameIsRejected() throws Exception {
        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dosageForm":"TABLET","unit":"粒","totalQuantity":10,"threshold":2,"expiryDate":"2030-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name")));

        verify(medicineMapper, never()).insert(any(Medicine.class));
    }

    @Test
    @DisplayName("创建药品缺 unit → body.code=400（原为 500），且不得落库")
    void createWithoutUnitIsRejected() throws Exception {
        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"阿莫西林","dosageForm":"TABLET","totalQuantity":10,"threshold":2,"expiryDate":"2030-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("unit")));

        verify(medicineMapper, never()).insert(any(Medicine.class));
    }

    @Test
    @DisplayName("创建药品 totalQuantity 为负数 → body.code=400，且不得落库")
    void createWithNegativeTotalQuantityIsRejected() throws Exception {
        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"阿莫西林","dosageForm":"TABLET","unit":"粒","totalQuantity":-5,"threshold":2,"expiryDate":"2030-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("totalQuantity")));

        verify(medicineMapper, never()).insert(any(Medicine.class));
    }

    @Test
    @DisplayName("创建药品缺 expiryDate → body.code=400，且不得落库")
    void createWithoutExpiryDateIsRejected() throws Exception {
        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"阿莫西林","dosageForm":"TABLET","unit":"粒","totalQuantity":10,"threshold":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("expiryDate")));

        verify(medicineMapper, never()).insert(any(Medicine.class));
    }

    @Test
    @DisplayName("补货数量为 -1 → body.code=400（不得变成 500），且不触达库存服务")
    void restockWithNegativeAmountIsRejected() throws Exception {
        mockMvc.perform(post("/medicines/{id}/restock", "med-1").param("amount", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("amount")));

        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("完整合法请求体 → body.code=200 且落库恰好一次")
    void createWithValidBodySucceeds() throws Exception {
        when(medicineMapper.insert(any(Medicine.class))).thenReturn(1);

        mockMvc.perform(post("/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"阿莫西林","dosageForm":"TABLET","unit":"粒","totalQuantity":10,"threshold":2,"expiryDate":"2030-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(medicineMapper, times(1)).insert(any(Medicine.class));
    }
}
