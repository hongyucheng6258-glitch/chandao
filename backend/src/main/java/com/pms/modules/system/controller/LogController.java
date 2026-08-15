package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.system.entity.SysActionLog;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.mapper.SysActionLogMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysActionLogMapper actionLogMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<Page<SysActionLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String objectType,
                                           @RequestParam(required = false) String actorName) {
        LambdaQueryWrapper<SysActionLog> wrapper = new LambdaQueryWrapper<SysActionLog>()
                .eq(objectType != null && !objectType.isBlank(), SysActionLog::getObjectType, objectType)
                .like(actorName != null && !actorName.isBlank(), SysActionLog::getActorName, actorName)
                .orderByDesc(SysActionLog::getId);
        return Result.ok(actionLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    /** 某对象的动态时间线(详情页用), 登录即可 */
    @GetMapping("/timeline")
    public Result<Page<SysActionLog>> timeline(@RequestParam String objectType,
                                               @RequestParam Long objectId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "20") long pageSize) {
        LambdaQueryWrapper<SysActionLog> wrapper = new LambdaQueryWrapper<SysActionLog>()
                .eq(SysActionLog::getObjectType, objectType)
                .eq(SysActionLog::getObjectId, objectId)
                .orderByDesc(SysActionLog::getId);
        return Result.ok(actionLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    /** 业务对象评论: 写一条 action=comment 的动态, 登录即可发表 */
    @PostMapping("/comment")
    public Result<Void> addComment(@RequestBody CommentBody body) {
        if (body.getObjectType() == null || body.getObjectType().isBlank() || body.getObjectId() == null) {
            throw new BizException("评论对象不能为空");
        }
        if (body.getContent() == null || body.getContent().trim().isBlank()) {
            throw new BizException("评论内容不能为空");
        }
        if (body.getContent().length() > 2000) {
            throw new BizException("评论内容过长(最多2000字)");
        }
        SysUser user = SecurityUtil.getLoginUser().getUser();
        SysActionLog logEntry = new SysActionLog();
        logEntry.setObjectType(body.getObjectType());
        logEntry.setObjectId(body.getObjectId());
        logEntry.setAction("comment");
        logEntry.setActorId(SecurityUtil.getUserId());
        logEntry.setActorName(user.getRealName());
        logEntry.setDetail(body.getContent());
        actionLogMapper.insert(logEntry);
        return Result.ok();
    }

    /** 删除评论: 仅评论作者本人可删, 系统事件(action!=comment)不可删 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        SysActionLog log = actionLogMapper.selectById(id);
        if (log == null) {
            throw new BizException("记录不存在");
        }
        if (!"comment".equals(log.getAction())) {
            throw new BizException("仅能删除评论, 系统动态不可删除");
        }
        Long current = SecurityUtil.getUserId();
        if (!current.equals(log.getActorId())) {
            throw new BizException("只能删除自己发表的评论");
        }
        actionLogMapper.deleteById(id);
        return Result.ok();
    }

    @Data
    public static class CommentBody {
        private String objectType;
        private Long objectId;
        private String content;
    }
}
