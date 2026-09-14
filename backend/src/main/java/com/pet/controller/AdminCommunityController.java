package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.AdminCommunityQuery;
import com.pet.security.RequireRole;
import com.pet.service.CommunityService;
import com.pet.vo.CommunityCommentVO;
import com.pet.vo.CommunityPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端社区审核", description = "查看、隐藏或恢复社区帖子与评论")
@Validated
@RestController
@RequestMapping("/admin/community")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class AdminCommunityController {

    private final CommunityService communityService;

    @Operation(summary = "社区帖子审核分页")
    @GetMapping("/posts/page")
    public Result<PageResult<CommunityPostVO>> posts(@Valid AdminCommunityQuery query) {
        return Result.success(communityService.adminPagePosts(query));
    }

    @Operation(summary = "查看帖子全部评论（含已隐藏）")
    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommunityCommentVO>> comments(@PathVariable Long postId) {
        return Result.success(communityService.adminListComments(postId));
    }

    @Operation(summary = "隐藏或恢复帖子")
    @PutMapping("/posts/{id}/status")
    public Result<Void> postStatus(@PathVariable Long id,
                                   @RequestParam @Min(0) @Max(1) int status) {
        communityService.setPostStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "隐藏或恢复评论")
    @PutMapping("/comments/{id}/status")
    public Result<Void> commentStatus(@PathVariable Long id,
                                      @RequestParam @Min(0) @Max(1) int status) {
        communityService.setCommentStatus(id, status);
        return Result.success();
    }
}
