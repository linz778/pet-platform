package com.pet.controller;

import com.pet.common.api.PageResult;
import com.pet.common.api.Result;
import com.pet.dto.CommunityCommentCreateDTO;
import com.pet.dto.CommunityPostCreateDTO;
import com.pet.dto.CommunityPostQuery;
import com.pet.security.RequireRole;
import com.pet.service.CommunityService;
import com.pet.vo.CommunityCommentVO;
import com.pet.vo.CommunityLikeVO;
import com.pet.vo.CommunityPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "宠物社区", description = "宠物主人晒宠与提问，接单员参与答疑")
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@RequireRole({"USER", "SITTER", "ADMIN"})
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "社区动态分页", description = "type 为空表示全部，1晒宠分享，2养宠问答")
    @GetMapping("/posts")
    public Result<PageResult<CommunityPostVO>> posts(@Valid CommunityPostQuery query) {
        return Result.success(communityService.pagePosts(query));
    }

    @Operation(summary = "社区动态详情")
    @GetMapping("/posts/{id}")
    public Result<CommunityPostVO> post(@PathVariable Long id) {
        return Result.success(communityService.getPost(id));
    }

    @Operation(summary = "发布晒宠动态或养宠问题")
    @PostMapping("/posts")
    @RequireRole({"USER"})
    public Result<Long> create(@Valid @RequestBody CommunityPostCreateDTO dto) {
        return Result.success(communityService.createPost(dto));
    }

    @Operation(summary = "动态评论与回答")
    @GetMapping("/posts/{id}/comments")
    public Result<List<CommunityCommentVO>> comments(@PathVariable Long id) {
        return Result.success(communityService.listComments(id));
    }

    @Operation(summary = "发表评论或回答", description = "接单员的回答会在社区显示专业身份标签")
    @PostMapping("/posts/{id}/comments")
    @RequireRole({"USER", "SITTER"})
    public Result<Void> comment(@PathVariable Long id, @Valid @RequestBody CommunityCommentCreateDTO dto) {
        communityService.comment(id, dto);
        return Result.success();
    }

    @Operation(summary = "送出或收回爪印")
    @PostMapping("/posts/{id}/like")
    @RequireRole({"USER", "SITTER"})
    public Result<CommunityLikeVO> like(@PathVariable Long id) {
        return Result.success(communityService.toggleLike(id));
    }
}
