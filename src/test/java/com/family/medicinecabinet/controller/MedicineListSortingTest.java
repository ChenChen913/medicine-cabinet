package com.family.medicinecabinet.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.family.medicinecabinet.entity.Medicine;
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
 * 药品列表排序与 size 上限用例（对应与前端协商确定的 agreed-E1.md 编号 1-4、8）
 *
 * <p>覆盖三类契约：①排序参数经 {@code LambdaQueryWrapper} 落到 ORDER BY（含 id ASC 次级键）；
 * ②非法排序值 / size 越界一律 body.code=400 且不触达 Mapper；③缺省与组合语义
 * （不传 sortBy 无 ORDER BY、sortOrder 单独传被忽略、size=100 边界放行）。
 * Mapper 以 Mock 替换，被拒绝的请求既不查库也不返回数据。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicineListSortingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicineMapper medicineMapper;

    @MockBean
    private InventoryService inventoryService;

    /**
     * 手工注册 Medicine 的 TableInfo，使 {@code Wrapper#getSqlSegment()} 能解析 Lambda 列名。
     * 与 Spring 上下文（MyBatis 解析 Mapper 时）注册的表信息等价；缓存先到先得，重复调用无副作用。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Medicine.class);
    }

    /**
     * 统一桩：selectPage 被调用时填充传入的 Page（控制器返回的就是该对象），不触库。
     */
    @BeforeEach
    void stubSelectPage() {
        when(medicineMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Medicine> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(5L);
            return page;
        });
    }

    // ---------- 排序生效 ----------

    @Test
    @DisplayName("sortBy=name&sortOrder=asc → ORDER BY name ASC,id ASC")
    void sortByNameAsc() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "name").param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedOrderBy()).contains("ORDERBYNAMEASC,IDASC");
    }

    @Test
    @DisplayName("sortBy=totalQuantity&sortOrder=desc → ORDER BY total_quantity DESC,id ASC")
    void sortByTotalQuantityDesc() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "totalQuantity").param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedOrderBy()).contains("ORDERBYTOTAL_QUANTITYDESC,IDASC");
    }

    @Test
    @DisplayName("sortBy=expiryDate 不带 sortOrder → 默认 asc：ORDER BY expiry_date ASC,id ASC")
    void sortByExpiryDateDefaultsToAsc() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "expiryDate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedOrderBy()).contains("ORDERBYEXPIRY_DATEASC,IDASC");
    }

    @Test
    @DisplayName("sortBy=createdAt&sortOrder=desc → ORDER BY created_at DESC,id ASC")
    void sortByCreatedAtDesc() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "createdAt").param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedOrderBy()).contains("ORDERBYCREATED_ATDESC,IDASC");
    }

    // ---------- 缺省与组合（编号 2） ----------

    @Test
    @DisplayName("不传 sortBy → 无 ORDER BY；仅传 sortOrder 也被忽略（不报错）")
    void sortOrderAloneIsIgnored() throws Exception {
        mockMvc.perform(get("/medicines").param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(normalizedOrderBy()).doesNotContain("ORDERBY");
    }

    @Test
    @DisplayName("不传 size → 默认 size=10（响应 data.size=10）")
    void defaultSizeIsTen() throws Exception {
        mockMvc.perform(get("/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    // ---------- 非法排序值（编号 3） ----------

    @Test
    @DisplayName("sortBy=foo → body.code=400 + 可读 message，且不触达 Mapper")
    void illegalSortByRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的排序字段")))
                .andExpect(jsonPath("$.message").value(containsString("name/totalQuantity/expiryDate/createdAt")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("sortBy=NAME（大小写不符）→ body.code=400")
    void caseSensitiveSortByRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "NAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的排序字段")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("sortBy 传空串 → body.code=400（空串等同非法值）")
    void emptySortByRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的排序字段")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("sortOrder=ascend（未映射的 antd 原值）→ body.code=400，且不触达 Mapper")
    void illegalSortOrderRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("sortBy", "name").param("sortOrder", "ascend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("不支持的排序方向")))
                .andExpect(jsonPath("$.message").value(containsString("asc/desc")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    // ---------- size 上限（编号 4） ----------

    @Test
    @DisplayName("size=999999（超上限）→ body.code=400 + message 含『size 必须在 1-100 之间』，且不查询")
    void oversizedSizeRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("size", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("size 必须在 1-100 之间")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("size=101（刚越界）→ body.code=400，不静默截断")
    void sizeJustOverLimitRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("size 必须在 1-100 之间")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("size=0（<=0，绕过分页的全表导出通道）→ body.code=400")
    void zeroSizeRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("size 必须在 1-100 之间")));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("size=100（边界值）→ body.code=200，放行")
    void sizeAtLimitAccepted() throws Exception {
        mockMvc.perform(get("/medicines").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("size=abc（非整数）→ body.code=400（既有参数类型不匹配约定）")
    void nonIntegerSizeRejected() throws Exception {
        mockMvc.perform(get("/medicines").param("size", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(medicineMapper, never()).selectPage(any(), any());
    }

    // ---------- 排序不影响 total（编号 5）与 current 不新增校验（编号 7） ----------

    @Test
    @DisplayName("排序前后 total 相同（均来自 Mapper 填充的 page.total，与排序无关）")
    void totalUnaffectedBySorting() throws Exception {
        mockMvc.perform(get("/medicines"))
                .andExpect(jsonPath("$.data.total").value(5));

        mockMvc.perform(get("/medicines").param("sortBy", "totalQuantity").param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("current=0（历史容忍行为）→ 不新增校验，body.code=200（等同第一页语义由分页插件承担）")
    void zeroCurrentStillPasses() throws Exception {
        mockMvc.perform(get("/medicines").param("current", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 工具方法 ----------

    /**
     * 抓取控制器传给 Mapper 的 Wrapper，返回归一化（去空白 + 大写）后的 SQL 片段，
     * 便于对 ORDER BY 做稳定断言。
     */
    @SuppressWarnings("unchecked")
    private String normalizedOrderBy() {
        ArgumentCaptor<Wrapper<Medicine>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(medicineMapper).selectPage(any(), captor.capture());
        return captor.getValue().getSqlSegment().replaceAll("\\s+", "").toUpperCase();
    }
}
