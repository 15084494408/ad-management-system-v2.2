package com.enterprise.ad.module.design.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.design.entity.DesignFile;
import com.enterprise.ad.module.design.entity.FileVersion;
import com.enterprise.ad.module.design.mapper.DesignFileMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.enterprise.ad.module.design.service.DesignFileService;
import com.enterprise.ad.module.design.service.FileVersionService;

@Slf4j
@RestController
@RequestMapping("/design/file")
@RequiredArgsConstructor
@Tag(name = "设计文件管理")
@PreAuthorize("hasAuthority('design:file')")
public class DesignFileController {

    private final DesignFileService designFileService;
    private final DesignFileMapper designFileMapper;
    private final FileVersionService fileVersionService;

    @Value("${file.upload-dir:uploads/design}")
    private String uploadDir;

    /** ★ P1-05 修复：允许上传的文件类型白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        // 图片
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "tif",
        // 设计文件
        "psd", "ai", "cdr", "indd", "eps", "pdf",
        // 文档
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
        // 压缩包
        "zip", "rar", "7z", "tar", "gz"
    );

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
        Page<DesignFile> result = designFileService.page(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文件详情")
    public Result<DesignFile> getById(@PathVariable Long id) {
        DesignFile file = designFileService.getById(id);
        return Result.ok(file);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "文件版本历史")
    public Result<List<FileVersion>> versions(@PathVariable Long id) {
        List<FileVersion> versions = fileVersionService.list(
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
        // 设置下载URL
        designFileService.save(designFile);
        designFile.setUrl("/api/design/file/download/" + designFile.getId());
        designFileService.updateById(designFile);

        // 记录版本
        FileVersion version = new FileVersion();
        version.setFileId(designFile.getId());
        version.setVersion(1);
        version.setSize(file.getSize());
        version.setChangeDesc("初始版本");
        version.setOperatorName(username != null ? username : "unknown");
        version.setCreateTime(LocalDateTime.now());
        fileVersionService.save(version);

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

        DesignFile fileEntity = designFileService.getById(id);
        if (fileEntity == null || fileEntity.getDeleted() == 1) {
            return Result.fail(400, "文件不存在");
        }

        int newVersion = fileEntity.getVersion() + 1;
        fileEntity.setVersion(newVersion);
        fileEntity.setName(savedPath);  // 更新为新版本路径
        fileEntity.setSize(file.getSize());
        fileEntity.setUpdateTime(LocalDateTime.now());
        designFileService.updateById(fileEntity);

        // 记录版本
        FileVersion version = new FileVersion();
        version.setFileId(id);
        version.setVersion(newVersion);
        version.setSize(file.getSize());
        version.setChangeDesc(changeDesc);
        version.setOperatorName(username != null ? username : "unknown");
        version.setCreateTime(LocalDateTime.now());
        fileVersionService.save(version);

        return Result.ok(fileEntity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文件信息")
    public Result<Void> update(@PathVariable Long id, @RequestBody DesignFile file) {
        file.setId(id);
        file.setUpdateTime(LocalDateTime.now());
        designFileService.updateById(file);
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
        designFileService.updateById(file);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件（物理删除文件及数据库记录）")
    public Result<Void> delete(@PathVariable Long id) {
        // 获取文件记录
        DesignFile fileEntity = designFileService.getById(id);
        if (fileEntity == null) {
            return Result.fail(400, "文件不存在");
        }
        
        // 物理删除磁盘文件
        String relativePath = fileEntity.getName();
        if (relativePath != null) {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(relativePath).normalize();
            try {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info("物理文件已删除: {}", filePath);
                } else {
                    log.warn("物理文件不存在，跳过: {}", filePath);
                }
            } catch (IOException e) {
                log.error("物理文件删除失败: {}", filePath, e);
            }
        }
        
        // 物理删除数据库记录
        designFileMapper.deleteById(id);
        log.info("数据库记录已删除: id={}", id);
        
        return Result.ok();
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载/预览文件")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        DesignFile fileEntity = designFileService.getById(id);
        if (fileEntity == null) {
            response.setStatus(404);
            return;
        }

        String relativePath = fileEntity.getName();
        if (relativePath == null) {
            response.setStatus(404);
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(relativePath).normalize();

            // ★ P1-06 修复：路径穿越校验，确保文件在上传目录内
            if (!filePath.startsWith(uploadPath)) {
                log.warn("路径穿越攻击: id={}, path={}", id, filePath);
                response.setStatus(403);
                return;
            }

            if (!Files.exists(filePath)) {
                log.warn("物理文件不存在: {}", filePath);
                response.setStatus(404);
                return;
            }

            // 设置响应头
            String fileName = fileEntity.getOriginalName() != null ? fileEntity.getOriginalName() : relativePath;
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

            // 判断是否为图片（可预览）
            String contentType = fileEntity.getMimeType();
            if (contentType == null) {
                String ext = (fileEntity.getExtension() != null) ? fileEntity.getExtension().toLowerCase() : "";
                if (ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp|svg)")) {
                    contentType = "image/" + ext.replace(".", "");
                } else {
                    contentType = "application/octet-stream";
                }
            }

            // 图片直接预览，其他格式弹下载
            boolean isImage = contentType != null && contentType.startsWith("image/");
            if (isImage) {
                response.setContentType(contentType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + encodedFileName + "\"");
            } else {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            }
            response.setHeader("Content-Length", String.valueOf(Files.size(filePath)));

            // 流式输出文件
            try (InputStream is = Files.newInputStream(filePath);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            log.error("文件下载失败: id={}", id, e);
            response.setStatus(500);
        }
    }

    /**
     * ★ 修复：实际保存文件到磁盘
     * 使用绝对路径并手动复制文件内容，避免 Tomcat 嵌入式环境的临时目录问题
     */
    private String saveFile(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase() : "";

            // ★ P1-05 修复：文件类型白名单校验
            if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new RuntimeException("不支持的文件类型: " + extension + "，允许的类型: " + ALLOWED_EXTENSIONS);
            }

            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            
            // ★ 修复：使用绝对路径
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dirPath = uploadPath.resolve(dateDir);
            
            // 创建完整目录结构
            Files.createDirectories(dirPath);

            String savedName = UUID.randomUUID().toString() + "." + extension;

            Path filePath = dirPath.resolve(savedName);
            
            // ★ 修复：手动复制文件内容，避免 transferTo() 的 Tomcat 临时目录问题
            Files.copy(file.getInputStream(), filePath);

            log.info("文件保存成功: {}", filePath);
            
            // 返回相对路径，供前端访问
            return dateDir + "/" + savedName;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }
}
