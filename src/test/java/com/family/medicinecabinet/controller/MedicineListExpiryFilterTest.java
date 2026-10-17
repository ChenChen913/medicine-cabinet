package com.family.medicinecabinet.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.medicinecabinet.entity.Medicine;
import com.family.medicinecabinet.entity.enums.ExpiryStatusFilter;
import com.family.medicinecabinet.mapper.MedicineMapper;
import com.family.medicinecabinet.service.InventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 药品列表「按有效期状态筛选」契约用例。
 *
 * <p>覆盖：①三态筛选落到 WHERE 的日期边界值（今天-1 / 今天 / 今天+30 / 今天+31）；
 * ②不传参数时完全不追加有效期条件（保持原「查看全部」行为）；
 * ③与名称搜索、排序同时生效互不覆盖；④非法值（未知 / 小写 / 空串）返回 body.code=400 且不触达 Mapper。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicineListExpiryFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicineMapper medicineMapper;

    @MockBean
    private InventoryService inventoryService;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Medicine.class);
    }

    @BeforeEach
    void stubSelectPage() {
        when(medicineMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Medicine> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0L);
            return page;
        });
    }

    // ---------- 三态边界 ----------

    @Test
    @DisplayName("expiryStatus=EXPIRED → WHERE expiry_date <= 今天-1（到期当天不算已过期）")
    void expiredFiltersBeforeToday() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/medicines").param("expiryStatus", "EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedSql()).contains("EXPIRY_DATE<=");
        assertThat(normalizedSql()).doesNotContain("EXPIRY_DATE>=");
        assertThat(paramValues()).containsExactly(today.minusDays(1));
    }

    @Test
    @DisplayName("expiryStatus=EXPIRING_SOON → WHERE 今天 <= expiry_date <= 今天+30（两端含）")
    void expiringSoonIncludesBothBounds() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/medicines").param("expiryStatus", "EXPIRING_SOON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedSql()).contains("EXPIRY_DATE>=").contains("EXPIRY_DATE<=");
        assertThat(paramValues())
                .containsExactlyInAnyOrder(today, today.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS));
    }

    @Test
    @DisplayName("expiryStatus=VALID → WHERE expiry_date >= 今天+31（与即将过期互斥）")
    void validFiltersAfterWindow() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/medicines").param("expiryStatus", "VALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedSql()).contains("EXPIRY_DATE>=");
        assertThat(normalizedSql()).doesNotContain("EXPIRY_DATE<=");
        assertThat(paramValues()).containsExactly(today.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS + 1L));
    }

    // ---------- 不筛选 / 组合 ----------

    @Test
    @DisplayName("不传 expiryStatus → 不追加任何有效期条件（查看全部）")
    void noFilterMeansAll() throws Exception {
        mockMvc.perform(get("/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedSql()).doesNotContain("EXPIRY_DATE");
    }

    @Test
    @DisplayName("筛选 + 名称搜索 + 排序同时生效：LIKE 与日期条件并存，ORDER BY 保留")
    void filterCombinesWithNameAndSorting() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/medicines")
                        .param("name", "阿莫西林")
                        .param("expiryStatus", "EXPIRING_SOON")
                        .param("sortBy", "expiryDate")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String sql = normalizedSql();
        assertThat(sql).contains("NAMELIKE").contains("EXPIRY_DATE>=").contains("EXPIRY_DATE<=");
        assertThat(sql).contains("ORDERBYEXPIRY_DATEASC,IDASC");
        assertThat(paramValues()).contains(today, today.plusDays(ExpiryStatusFilter.EXPIRING_SOON_DAYS));
        // 名称模糊搜索的参数为 %name%（LIKE 通配由 MyBatis-Plus 拼接），这里只断言搜索词被绑定
        assertThat(paramValues()).anySatisfy(value -> assertThat(String.valueOf(value)).contains("阿莫西林"));
    }

    // ---------- 非法值 ----------

    @Test
    @DisplayName("expiryStatus=foo → body.code=400 + 可读 message（列出可选值），且不触达 Mapper")
    void unknownFilterRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("expiryStatus", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的过期状态")))
                .andExpect(jsonPath("$.message").value(containsString("EXPIRED/EXPIRING_SOON/VALID")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("expiryStatus=expired（大小写不符）→ body.code=400，不静默回落")
    void lowercaseFilterRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("expiryStatus", "expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的过期状态")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("expiryStatus=空串 → body.code=400（空串等同非法值）")
    void blankFilterRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("expiryStatus", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的过期状态")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    // ---------- 工具方法 ----------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<Medicine> capturedWrapper() {
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(medicineMapper).selectPage(any(), captor.capture());
        return captor.getValue();
    }

    /**
     * 归一化（去空白 + 大写）后的 WHERE 片段，便于稳定断言日期比较符。
     */
    private String normalizedSql() {
        return capturedWrapper().getSqlSegment().replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * 查询包装器上绑定的参数值（名称搜索串、日期边界等），用于断言落库前实际参与比较的日期。
     */
    private List<Object> paramValues() {
        return capturedWrapper().getParamNameValuePairs().values().stream().toList();
    }
}
