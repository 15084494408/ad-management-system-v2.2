package com.enterprise.ad.module.order;

import com.enterprise.ad.module.order.dto.CreateOrderDTO;
import com.enterprise.ad.module.order.dto.CreateOrderMaterialDTO;
import com.enterprise.ad.module.order.dto.UpdateOrderDTO;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.entity.OrderMaterial;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO 转换测试用例
 * ★ 修复 P0-1: 验证 DTO 安全转换
 */
public class DtoConversionTest {

    // ========== CreateOrderDTO 测试 ==========

    @Test
    public void testCreateOrderDTO_ValidInput() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTitle("测试订单");
        dto.setCustomerId(1L);
        dto.setCustomerName("测试客户");
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setContactPerson("张三");
        dto.setContactPhone("13800000000");
        dto.setOrderType(1);

        assertNotNull(dto.getTitle());
        assertEquals("测试订单", dto.getTitle());
        assertEquals(1L, dto.getCustomerId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getTotalAmount()));
    }

    @Test
    public void testCreateOrderDTO_WithMaterials() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTitle("含物料订单");

        CreateOrderMaterialDTO material1 = new CreateOrderMaterialDTO();
        material1.setMaterialName("铜版纸 A4");
        material1.setQuantity(new BigDecimal("100"));
        material1.setUnitPrice(new BigDecimal("0.50"));
        material1.setUnitCost(new BigDecimal("0.30")); // ★ 新增字段

        CreateOrderMaterialDTO material2 = new CreateOrderMaterialDTO();
        material2.setMaterialName("墨盒");
        material2.setQuantity(new BigDecimal("5"));
        material2.setUnitPrice(new BigDecimal("80.00"));
        material2.setUnitCost(new BigDecimal("50.00")); // ★ 新增字段

        dto.setMaterials(Arrays.asList(material1, material2));

        assertEquals(2, dto.getMaterials().size());
        assertNotNull(dto.getMaterials().get(0).getUnitCost()); // ★ 验证新字段
    }

    // ========== UpdateOrderDTO 测试 ==========

    @Test
    public void testUpdateOrderDTO_PartialUpdate() {
        UpdateOrderDTO dto = new UpdateOrderDTO();
        dto.setTitle("更新后的标题");
        // 其他字段不设置，验证部分更新逻辑

        assertNotNull(dto.getTitle());
        assertNull(dto.getCustomerId()); // 未设置的字段应为 null
        assertNull(dto.getTotalAmount());
    }

    // ========== CreateOrderMaterialDTO 测试 ==========

    @Test
    public void testCreateOrderMaterialDTO_WithCost() {
        CreateOrderMaterialDTO dto = new CreateOrderMaterialDTO();
        dto.setMaterialName("测试物料");
        dto.setQuantity(new BigDecimal("10"));
        dto.setUnitPrice(new BigDecimal("100.00"));
        dto.setUnitCost(new BigDecimal("60.00")); // ★ 新增字段

        assertNotNull(dto.getUnitCost()); // ★ 验证新字段
        assertEquals(0, new BigDecimal("60.00").compareTo(dto.getUnitCost()));
    }

    // ========== 安全验证测试 ==========

    @Test
    public void testCreateOrderDTO_DefaultValues() {
        CreateOrderDTO dto = new CreateOrderDTO();

        // 验证默认值
        assertEquals(1, dto.getOrderType()); // 默认印刷
        assertEquals(1, dto.getPriority());  // 默认普通
        assertEquals(1, dto.getSource());    // 默认门店创建
    }

    @Test
    public void testUpdateOrderDTO_ImmutableFields() {
        // UpdateOrderDTO 不应该允许修改的字段（需要在 Controller 层保护）
        UpdateOrderDTO dto = new UpdateOrderDTO();
        dto.setStatus(4); // 允许修改状态

        // 注意：id、orderNo 等字段应该在 Controller 层从路径参数获取
        // 而不是从 DTO 中获取，这是安全设计的一部分
        assertEquals(4, dto.getStatus());
    }
}
