package com.enterprise.ad.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.util.WebUtil;
import com.enterprise.ad.module.notice.entity.Notice;
import com.enterprise.ad.module.notice.entity.NoticeSetting;
import com.enterprise.ad.module.notice.mapper.NoticeMapper;
import com.enterprise.ad.module.notice.mapper.NoticeSettingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Tag(name = "消息通知")
public class NoticeController {

    private final NoticeMapper noticeMapper;
    private final NoticeSettingMapper settingMapper;

    // ========== 通知列表 ==========

    @GetMapping
    @Operation(summary = "通知列表（分页）")
    public Result<PageResult<Notice>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead,
            HttpServletRequest request) {

        // ★ 修复：从 request attribute 获取用户信息
        Long userId = WebUtil.getCurrentUserId(request);

        Page<Notice> page = new Page<>(current, size);
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<Notice>()
            .eq(Notice::getDeleted, 0)
            .and(w -> w.isNull(Notice::getUserId).or().like(Notice::getUserIds, userId != null ? userId.toString() : ""))
            .eq(type != null, Notice::getType, type)
            .eq(isRead != null, Notice::getIsRead, isRead)
            .orderByDesc(Notice::getCreateTime);

        Page<Notice> result = noticeMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public Result<Long> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        long count = noticeMapper.selectCount(
            new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .eq(Notice::getIsRead, 0)
                .and(w -> w.isNull(Notice::getUserId).or().like(Notice::getUserIds, userId != null ? userId.toString() : ""))
        );
        return Result.ok(count);
    }

    @GetMapping("/latest")
    @Operation(summary = "最新通知（前5条）")
    public Result<?> latest(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        var qw = new LambdaQueryWrapper<Notice>()
            .eq(Notice::getDeleted, 0)
            .eq(Notice::getIsRead, 0)
            .and(w -> w.isNull(Notice::getUserId).or().like(Notice::getUserIds, userId != null ? userId.toString() : ""))
            .orderByDesc(Notice::getCreateTime)
            .last("LIMIT 5");
        return Result.ok(noticeMapper.selectList(qw));
    }

    @GetMapping("/{id}")
    @Operation(summary = "通知详情")
    public Result<Notice> getById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notice notice = noticeMapper.selectById(id);
        if (notice != null && notice.getIsRead() == 0) {
            notice.setIsRead(1);
            notice.setReadUserId(userId);
            notice.setReadTime(LocalDateTime.now());
            noticeMapper.updateById(notice);
        }
        return Result.ok(notice);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notice notice = new Notice();
        notice.setId(id);
        notice.setIsRead(1);
        notice.setReadUserId(userId);
        notice.setReadTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return Result.ok();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部已读")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notice update = new Notice();
        update.setIsRead(1);
        update.setReadUserId(userId);
        update.setReadTime(LocalDateTime.now());
        noticeMapper.update(update,
            new LambdaQueryWrapper<Notice>()
                .eq(Notice::getIsRead, 0)
                .eq(Notice::getDeleted, 0)
                .and(w -> w.isNull(Notice::getUserId).or().like(Notice::getUserIds, userId != null ? userId.toString() : ""))
        );
        return Result.ok();
    }

    @PostMapping
    @Operation(summary = "发送通知（仅管理员）")
    @PreAuthorize("hasAuthority('system:notice')")
    public Result<Void> create(@RequestBody Notice notice) {
        notice.setCreateTime(LocalDateTime.now());
        notice.setIsRead(0);
        noticeMapper.insert(notice);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    @PreAuthorize("hasAuthority('system:notice')")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        noticeMapper.deleteById(id);
        return Result.ok();
    }

    // ========== 通知设置 ==========

    @GetMapping("/setting")
    @Operation(summary = "获取通知设置")
    public Result<NoticeSetting> getSetting(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoticeSetting setting = settingMapper.selectOne(
            new LambdaQueryWrapper<NoticeSetting>()
                .eq(NoticeSetting::getUserId, userId)
        );
        if (setting == null) {
            setting = new NoticeSetting();
            setting.setUserId(userId);
            setting.setOrderNotify(1);
            setting.setFinanceNotify(1);
            setting.setSystemNotify(1);
            setting.setWarningNotify(1);
            setting.setCreateTime(LocalDateTime.now());
            settingMapper.insert(setting);
        }
        return Result.ok(setting);
    }

    @PutMapping("/setting")
    @Operation(summary = "更新通知设置")
    public Result<Void> updateSetting(HttpServletRequest request, @RequestBody NoticeSetting setting) {
        Long userId = (Long) request.getAttribute("userId");
        NoticeSetting existing = settingMapper.selectOne(
            new LambdaQueryWrapper<NoticeSetting>().eq(NoticeSetting::getUserId, userId)
        );
        if (existing != null) {
            setting.setId(existing.getId());
            setting.setUserId(userId);
            setting.setUpdateTime(LocalDateTime.now());
            settingMapper.updateById(setting);
        } else {
            setting.setUserId(userId);
            setting.setCreateTime(LocalDateTime.now());
            settingMapper.insert(setting);
        }
        return Result.ok();
    }
}
