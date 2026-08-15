package com.pms.modules.attachment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.attachment.entity.SysAttachment;
import com.pms.modules.attachment.mapper.SysAttachmentMapper;
import com.pms.modules.system.entity.SysUser;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final SysAttachmentMapper attachmentMapper;

    @Value("${pms.upload-dir:./uploads}")
    private String uploadDirConfig;

    private Path uploadDir;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    /** 上传附件: 落盘 + 入库, 登录即可 */
    @PostMapping("/upload")
    public Result<SysAttachment> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam String objectType,
                                        @RequestParam Long objectId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        if (objectType == null || objectType.isBlank()) {
            throw new BizException("附件对象类型不能为空");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BizException("文件名非法");
        }
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > 0 && dot < original.length() - 1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }
        SysUser user = SecurityUtil.getLoginUser().getUser();
        SysAttachment att = new SysAttachment();
        att.setObjectType(objectType);
        att.setObjectId(objectId);
        att.setFileName(original);
        att.setStoredName(storedName);
        att.setFileExt(ext);
        att.setFileSize(file.getSize());
        att.setUploaderId(SecurityUtil.getUserId());
        att.setUploaderName(user.getRealName());
        attachmentMapper.insert(att);
        return Result.ok(att);
    }

    /** 某对象的附件列表 */
    @GetMapping
    public Result<List<SysAttachment>> list(@RequestParam String objectType, @RequestParam Long objectId) {
        List<SysAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<SysAttachment>()
                        .eq(SysAttachment::getObjectType, objectType)
                        .eq(SysAttachment::getObjectId, objectId)
                        .orderByDesc(SysAttachment::getId));
        return Result.ok(list);
    }

    /** 下载/(图片)预览 */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysAttachment att = attachmentMapper.selectById(id);
        if (att == null) {
            throw new BizException("附件不存在");
        }
        Path path = uploadDir.resolve(att.getStoredName());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BizException("文件已丢失");
        }
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(path));
        String inline = isImage(att.getFileExt()) ? "inline" : "attachment";
        String encodedName = URLEncoder.encode(att.getFileName(), "UTF-8").replace("+", "%20");
        response.setHeader("Content-Disposition",
                inline + "; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName);
        try (var out = response.getOutputStream()) {
            Files.copy(path, out);
            out.flush();
        }
    }

    /** 删除附件: 仅上传者本人可删 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysAttachment att = attachmentMapper.selectById(id);
        if (att == null) {
            throw new BizException("附件不存在");
        }
        if (!SecurityUtil.getUserId().equals(att.getUploaderId())) {
            throw new BizException("只能删除自己上传的附件");
        }
        attachmentMapper.deleteById(id);
        try {
            Files.deleteIfExists(uploadDir.resolve(att.getStoredName()));
        } catch (IOException ignored) {
            // 物理文件删除失败不影响记录删除
        }
        return Result.ok();
    }

    private boolean isImage(String ext) {
        if (ext == null) {
            return false;
        }
        return switch (ext.toLowerCase()) {
            case "png", "jpg", "jpeg", "gif", "webp", "bmp" -> true;
            default -> false;
        };
    }
}
