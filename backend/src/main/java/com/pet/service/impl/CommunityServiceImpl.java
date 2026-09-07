package com.pet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.api.PageResult;
import com.pet.common.api.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.common.util.CommaListUtil;
import com.pet.dto.CommunityCommentCreateDTO;
import com.pet.dto.CommunityPostCreateDTO;
import com.pet.dto.CommunityPostQuery;
import com.pet.entity.CommunityComment;
import com.pet.entity.CommunityLike;
import com.pet.entity.CommunityPost;
import com.pet.entity.Pet;
import com.pet.entity.User;
import com.pet.mapper.CommunityCommentMapper;
import com.pet.mapper.CommunityLikeMapper;
import com.pet.mapper.CommunityPostMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.UserContext;
import com.pet.service.CommunityService;
import com.pet.service.PetService;
import com.pet.vo.CommunityCommentVO;
import com.pet.vo.CommunityLikeVO;
import com.pet.vo.CommunityPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost>
        implements CommunityService {

    private static final int POST_VISIBLE = 1;
    private static final int POST_SHARE = 1;
    private static final int POST_QUESTION = 2;

    private final CommunityCommentMapper commentMapper;
    private final CommunityLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final PetService petService;

    @Override
    public PageResult<CommunityPostVO> pagePosts(CommunityPostQuery query) {
        Page<CommunityPost> page = page(query.toPage(), Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, POST_VISIBLE)
                .eq(query.getType() != null, CommunityPost::getType, query.getType())
                .orderByDesc(CommunityPost::getId));
        return new PageResult<>(toPostVOs(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CommunityPostVO getPost(Long postId) {
        return toPostVOs(List.of(requirePost(postId))).getFirst();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(CommunityPostCreateDTO dto) {
        if (dto.getPetId() != null) {
            // 关联宠物必须属于当前主人，不能借别人的宠物档案发布内容。
            petService.requireMine(dto.getPetId());
        }
        String images = CommaListUtil.join(dto.getImageUrls());
        if (images != null && images.length() > 1500) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "帖子图片地址总长度超出限制");
        }
        CommunityPost post = new CommunityPost();
        post.setAuthorId(UserContext.userId());
        post.setType(dto.getType());
        post.setPetId(dto.getPetId());
        post.setTitle(StrUtil.trim(dto.getTitle()));
        post.setContent(StrUtil.trim(dto.getContent()));
        post.setImages(images);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(POST_VISIBLE);
        save(post);
        return post.getId();
    }

    @Override
    public List<CommunityCommentVO> listComments(Long postId) {
        requirePost(postId);
        List<CommunityComment> comments = commentMapper.selectList(Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, POST_VISIBLE)
                .orderByAsc(CommunityComment::getId));
        Set<Long> authorIds = comments.stream().map(CommunityComment::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(authorIds);
        return comments.stream().map(comment -> {
            CommunityCommentVO vo = new CommunityCommentVO();
            vo.setId(comment.getId());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            User author = users.get(comment.getAuthorId());
            if (author != null) {
                vo.setAuthorName(displayName(author));
                vo.setAuthorAvatar(author.getAvatar());
                vo.setAuthorRole(author.getRole());
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void comment(Long postId, CommunityCommentCreateDTO dto) {
        requirePost(postId);
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setAuthorId(UserContext.userId());
        comment.setContent(StrUtil.trim(dto.getContent()));
        comment.setStatus(POST_VISIBLE);
        if (commentMapper.insert(comment) == 0 || baseMapper.incrementComment(postId) == 0) {
            throw new BusinessException(ResultCode.COMMUNITY_POST_NOT_FOUND);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityLikeVO toggleLike(Long postId) {
        requirePost(postId);
        Long userId = UserContext.userId();
        CommunityLike existing = likeMapper.selectOne(Wrappers.<CommunityLike>lambdaQuery()
                .eq(CommunityLike::getPostId, postId)
                .eq(CommunityLike::getUserId, userId));
        boolean liked;
        if (existing == null) {
            CommunityLike like = new CommunityLike();
            like.setPostId(postId);
            like.setUserId(userId);
            if (likeMapper.insert(like) == 0 || baseMapper.incrementLike(postId) == 0) {
                throw new BusinessException(ResultCode.COMMUNITY_POST_NOT_FOUND);
            }
            liked = true;
        } else {
            if (likeMapper.removeLike(existing.getId()) == 0 || baseMapper.decrementLike(postId) == 0) {
                throw new BusinessException(ResultCode.COMMUNITY_POST_NOT_FOUND);
            }
            liked = false;
        }
        CommunityPost latest = requirePost(postId);
        return new CommunityLikeVO(liked, latest.getLikeCount());
    }

    private CommunityPost requirePost(Long postId) {
        CommunityPost post = getById(postId);
        if (post == null || !Integer.valueOf(POST_VISIBLE).equals(post.getStatus())) {
            throw new BusinessException(ResultCode.COMMUNITY_POST_NOT_FOUND);
        }
        return post;
    }

    private List<CommunityPostVO> toPostVOs(List<CommunityPost> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<Long> authorIds = posts.stream().map(CommunityPost::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(authorIds);

        Set<Long> petIds = posts.stream().map(CommunityPost::getPetId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Pet> pets = petIds.isEmpty() ? Map.of() : petMapper.selectSnapshots(petIds).stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));

        Set<Long> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toSet());
        Set<Long> likedIds = likeMapper.selectList(Wrappers.<CommunityLike>lambdaQuery()
                        .eq(CommunityLike::getUserId, UserContext.userId())
                        .in(CommunityLike::getPostId, postIds)).stream()
                .map(CommunityLike::getPostId).collect(Collectors.toSet());

        return posts.stream().map(post -> {
            CommunityPostVO vo = new CommunityPostVO();
            vo.setId(post.getId());
            vo.setType(post.getType());
            vo.setTypeText(Integer.valueOf(POST_QUESTION).equals(post.getType()) ? "养宠问答" : "晒宠分享");
            vo.setPetId(post.getPetId());
            vo.setTitle(post.getTitle());
            vo.setContent(post.getContent());
            vo.setImageUrls(CommaListUtil.split(post.getImages()));
            vo.setLikeCount(post.getLikeCount());
            vo.setCommentCount(post.getCommentCount());
            vo.setLiked(likedIds.contains(post.getId()));
            vo.setCreateTime(post.getCreateTime());
            User author = users.get(post.getAuthorId());
            if (author != null) {
                vo.setAuthorName(displayName(author));
                vo.setAuthorAvatar(author.getAvatar());
                vo.setAuthorRole(author.getRole());
            }
            Pet pet = pets.get(post.getPetId());
            if (pet != null) {
                vo.setPetName(pet.getName());
                vo.setPetAvatar(pet.getAvatar());
            }
            return vo;
        }).toList();
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String displayName(User user) {
        return StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }
}
