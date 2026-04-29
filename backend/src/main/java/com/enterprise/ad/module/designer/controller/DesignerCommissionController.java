package com.enterprise.ad.module.designer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.dto.CommissionConfigRequest;
import com.enterprise.ad.module.designer.entity.DesignerCommissionConfig;
import com.enterprise.ad.module.designer.mapper.DesignerCommissionConfigMapper;
import com.enterprise.ad.module.system.user.entity.SysUser;
import com.enterprise.ad.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("designerCommissionConfigController")
@RequestMapping("/designer/commission")
@RequiredArgsConstructor
@Tag(name = "设计师提成管理")
public class DesignerCommissionController {

    private final DesignerCommissionConfigMapper commissionMapper;
    private final SysUserMapper userMapper;

    @GetMapping("/list")
    @Operation(summary = "设计师提成列表（含所有设计师）")
    @PreAuthorize("hasAuthority('system:user')")
    public Result<List<Map<String, Object>>> list() {
        // 获取所有设计师角色的用户
        List<SysUser> designers = userMapper.selectDesignerUsers();

        // 获取已配置提成的设计师
        List<DesignerCommissionConfig> configs = commissionMapper.selectList(
            new LambdaQueryWrapper<DesignerCommissionConfig>()
                .eq(DesignerCommissionConfig::getDeleted, 0)
        );
        Map<Long, DesignerCommissionConfig> configMap = configs.stream()
            .collect(Collectors.toMap(DesignerCommissionConfig::getDesignerId, c -> c, (a, b) -> a));

        // 合并数据（未配置的显示默认0%）
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : designers) {
            DesignerCommissionConfig cfg = configMap.get(user.getId());
            java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", cfg != null ? cfg.getId() : null);
            row.put("designerId", user.getId());
            row.put("designerName", user.getRealName() != null ? user.getRealName() : user.getUsername());
            row.put("phone", user.getPhone());
            row.put("commissionRate", cfg != null && cfg.getCommissionRate() != null
                ? cfg.getCommissionRate() : BigDecimal.ZERO);
            row.put("enabled", cfg != null ? cfg.getEnabled() : 1);
            row.put("isConfigured", cfg != null);
            result.add(row);
        }
        return Result.ok(result);
    }

    @PutMapping("/config")
    @Operation(summary = "设置/更新设计师提成比例")
    @PreAuthorize("hasAuthority('system:user')")
    @Transactional
    public Result<Void> config(@Valid @RequestBody CommissionConfigRequest req) {
        Long designerId = req.getDesignerId();
        BigDecimal rate = req.getCommissionRate();
        Boolean enabled = req.getEnabled();

        // 获取设计师姓名
        SysUser designer = userMapper.selectById(designerId);
        if (designer == null) {
            return Result.fail("设计师不存在");
        }

        DesignerCommissionConfig config = commissionMapper.selectOne(
            new LambdaQueryWrapper<DesignerCommissionConfig>()
                .eq(DesignerCommissionConfig::getDesignerId, designerId)
                .eq(DesignerCommissionConfig::getDeleted, 0)
        );

        if (config != null) {
            // 更新
            config.setCommissionRate(rate);
            config.setEnabled(enabled ? 1 : 0);
            config.setDesignerName(designer.getRealName() != null ? designer.getRealName() : designer.getUsername());
            config.setUpdatedTime(LocalDateTime.now());
            commissionMapper.updateById(config);
        } else {
            // 新增
            config = new DesignerCommissionConfig();
            config.setDesignerId(designerId);
            config.setDesignerName(designer.getRealName() != null ? designer.getRealName() : designer.getUsername());
            config.setCommissionRate(rate);
            config.setEnabled(enabled ? 1 : 0);
            config.setCreateTime(LocalDateTime.now());
            config.setDeleted(0);
            commissionMapper.insert(config);
        }
        return Result.ok();
    }

    @GetMapping("/rate/{designerId}")
    @Operation(summary = "获取设计师提成比例（供订单计算用）")
    public Result<BigDecimal> getRate(@PathVariable Long designerId) {
        DesignerCommissionConfig config = commissionMapper.selectOne(
            new LambdaQueryWrapper<DesignerCommissionConfig>()
                .eq(DesignerCommissionConfig::getDesignerId, designerId)
                .eq(DesignerCommissionConfig::getEnabled, 1)
                .eq(DesignerCommissionConfig::getDeleted, 0)
        );
        BigDecimal rate = config != null && config.getCommissionRate() != null
            ? config.getCommissionRate() : BigDecimal.ZERO;
        return Result.ok(rate);
    }
}
