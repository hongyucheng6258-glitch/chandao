package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.attachment.entity.SysAttachment;
import com.pms.modules.attachment.mapper.SysAttachmentMapper;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.entity.SysUserRole;
import com.pms.modules.system.mapper.SysUserMapper;
import com.pms.modules.system.mapper.SysUserRoleMapper;
import com.pms.modules.system.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysAttachmentMapper attachmentMapper;
    private final SysConfigService configService;

    @Value("${pms.upload-dir:./uploads}")
    private String uploadDirConfig;

    private Path uploadDir;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long deptId,
                                      @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByAsc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setRoleIds(
                userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, u.getId()))
                        .stream().map(SysUserRole::getRoleId).toList()));
        return Result.ok(page);
    }

    /** 全量简单列表(下拉框用), 登录即可访问 */
    @GetMapping("/options")
    public Result<List<SysUser>> options() {
        return Result.ok(userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName)
                .orderByAsc(SysUser::getId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    @Transactional
    public Result<Void> create(@RequestBody SysUser user) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (exists > 0) {
            throw new BizException("账号已存在");
        }
        user.setId(null);
        user.setPassword(passwordEncoder.encode(
                user.getPassword() == null || user.getPassword().isBlank() ? "123456" : user.getPassword()));
        userMapper.insert(user);
        saveUserRoles(user.getId(), user.getRoleIds());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        user.setUsername(null); // 账号不允许修改
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不修改密码
        }
        userMapper.updateById(user);
        if (user.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            saveUserRoles(id, user.getRoleIds());
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            throw new BizException("内置管理员不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(body.get("status"));
        userMapper.updateById(update);
        return Result.ok();
    }

    /** 上传头像: 登录用户可上传自己的头像 */
    @PostMapping("/avatar")
    @Transactional
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
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
        // 头像只允许图片类型
        Set<String> imageExts = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
        if (ext.isEmpty() || !imageExts.contains(ext)) {
            throw new BizException("头像只支持图片格式: png/jpg/jpeg/gif/webp/bmp");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }
        Long userId = SecurityUtil.getUserId();
        SysUser loginUser = SecurityUtil.getLoginUser().getUser();
        // 保存附件记录
        SysAttachment att = new SysAttachment();
        att.setObjectType("avatar");
        att.setObjectId(userId);
        att.setFileName(original);
        att.setStoredName(storedName);
        att.setFileExt(ext);
        att.setFileSize(file.getSize());
        att.setUploaderId(userId);
        att.setUploaderName(loginUser.getRealName());
        attachmentMapper.insert(att);
        // 更新用户头像字段为附件下载URL
        String avatarUrl = "/api/attachments/" + att.getId() + "/download";
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        userMapper.updateById(update);
        // 更新SecurityContext中的用户信息
        loginUser.setAvatar(avatarUrl);
        return Result.ok(Map.of("avatar", avatarUrl));
    }

    /** 获取当前登录用户信息(含头像) */
    @GetMapping("/me")
    public Result<SysUser> me() {
        Long userId = SecurityUtil.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId ->
                userRoleMapper.insert(new SysUserRole(null, userId, roleId)));
    }
}
