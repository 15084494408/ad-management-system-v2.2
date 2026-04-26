package com.enterprise.ad.module.design.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.design.entity.DesignFile;
import com.enterprise.ad.module.design.entity.FileVersion;
import com.enterprise.ad.module.design.mapper.DesignFileMapper;
import com.enterprise.ad.module.design.mapper.FileVersionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/design/file")
@RequiredArgsConstructor
@Tag(name = "设计文件管理")
@PreAuthorize("hasAuthority('design:file')")
public class DesignFileController {

    private final DesignFileMapper fileMapper;
    private final FileVersionMapper versionMapper;

    @Value("${file.upload-dir:uploads/design}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "文件列表（分页）")
    public Result<PageResult<DesignFile>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer status) {
        Page<DesignFile> page = new Page<>(current, size);
        LambdaQueryWrapper<DesignFile> qw = new LambdaQueryWrapper<DesignFile>()
            .eq(DesignFile::getDeleted, 0)
            .eq(orderId != null, DesignFile::getOrderId, orderId)
            .eq(status != null, DesignFile::getStatus, status)
            .like(name != null, DesignFile::getName, name)
            .orderByDesc(DesignFile::getCreateTime);
        Page<DesignFile> result = fileMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文件详情")
    public Result<DesignFile> getById(@PathVariable Long id) {
        DesignFile file = fileMapper.selectById(id);
        return Result.ok(file);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "文件版本历史")
    public Result<List<FileVersion>> versions(@PathVariable Long id) {
        List<FileVersion> versions = versionMapper.selectList(
            new LambdaQueryWrapper<FileVersion>()
                .eq(FileVersion::getFileId, id)
                .orderByDesc(FileVersion::getVersion)
        );
        return Result.ok(versions);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<DesignFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String description,
            HttpServletRequest request) {

        // ★ 修复：从 request attribute 获取用户信息
        String username = (String) request.getAttribute("username");

        // ★ 修复：实际保存文件到磁盘
        String savedPath = saveFile(file);

        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf(".")) : "";

        DesignFile designFile = new DesignFile();
        designFile.setName(savedPath);  // 存储实际路径
        designFile.setOriginalName(originalName);
        designFile.setExtension(extension);
        designFile.setSize(file.getSize());
        designFile.setMimeType(file.getContentType());
        designFile.setOrderId(orderId);
        designFile.setUploaderName(username != null ? username : "unknown");
        designFile.setVersion(1);
        designFile.setDescription(description);
        designFile.setStatus(1);
        designFile.setCreateTime(LocalDateTime.now());
        fileMapper.insert(designFile);

        // 记录版本
        FileVersion version = new FileVersion();
        version.setFileId(designFile.getId());
        version.setVersion(1);
        version.setSize(file.getSize());
        version.setChangeDesc("初始版本");
        version.setOperatorName(username != null ? username : "unknown");
        version.setCreateTime(LocalDateTime.now());
        versionMapper.insert(version);

        return Result.ok(designFile);
    }

    @PostMapping("/{id}/new-version")
    @Operation(summary = "上传新版本")
    public Result<DesignFile> uploadNewVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam String changeDesc,
            HttpServletRequest request) {

        // ★ 修复：从 request attribute 获取用户信息
        String username = (String) request.getAttribute("username");

        // ★ 修复：实际保存文件
        String savedPath = saveFile(file);

        DesignFile fileEntity = fileMapper.selectById(id);
        if (fileEntity == null || fileEntity.getDeleted() == 1) {
            return Result.fail(400, "文件不存在");
        }

        int newVersion = fileEntity.getVersion() + 1;
        fileEntity.setVersion(newVersion);
        fileEntity.setName(savedPath);  // 更新为新版本路径
        fileEntity.setSize(file.getSize());
        fileEntity.setUpdateTime(LocalDateTime.now());
        fileMapper.updateById(fileEntity);

        // 记录版本
        FileVersion version = new FileVersion();
        version.setFileId(id);
        version.setVersion(newVersion);
        version.setSize(file.getSize());
        version.setChangeDesc(changeDesc);
        version.setOperatorName(username != null ? username : "unknown");
        version.setCreateTime(LocalDateTime.now());
        versionMapper.insert(version);

        return Result.ok(fileEntity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文件信息")
    public Result<Void> update(@PathVariable Long id, @RequestBody DesignFile file) {
        file.setId(id);
        file.setUpdateTime(LocalDateTime.now());
        fileMapper.updateById(file);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "审核文件")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) String remark) {
        DesignFile file = new DesignFile();
        file.setId(id);
        file.setStatus(status);
        file.setRemark(remark);
        file.setUpdateTime(LocalDateTime.now());
        fileMapper.updateById(file);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    public Result<Void> delete(@PathVariable Long id) {
        DesignFile f = new DesignFile();
        f.setId(id);
        f.setDeleted(1);
        f.setUpdateTime(LocalDateTime.now());
        fileMapper.updateById(f);
        return Result.ok();
    }

    /**
     * ★ 新增：实际保存文件到磁盘
     */
    private String saveFile(MultipartFile file) {
        try {
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dirPath = Paths.get(uploadDir, dateDir);
            Files.createDirectories(dirPath);

            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String savedName = UUID.randomUUID().toString() + extension;

            Path filePath = dirPath.resolve(savedName);
            file.transferTo(filePath.toFile());

            // 返回相对路径，供前端访问
            return dateDir + "/" + savedName;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }
}
