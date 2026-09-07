package com.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pet.dto.CommunityCommentCreateDTO;
import com.pet.dto.CommunityPostCreateDTO;
import com.pet.entity.CommunityComment;
import com.pet.entity.CommunityLike;
import com.pet.entity.CommunityPost;
import com.pet.mapper.CommunityCommentMapper;
import com.pet.mapper.CommunityLikeMapper;
import com.pet.mapper.CommunityPostMapper;
import com.pet.security.LoginUser;
import com.pet.security.UserContext;
import com.pet.service.PetService;
import com.pet.vo.CommunityLikeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 社区发布、接单员答疑与爪印切换测试。 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    private static final long POST_ID = 12L;
    private static final long USER_ID = 2L;
    private static final long SITTER_ID = 3L;

    @Mock
    private CommunityPostMapper postMapper;

    @Mock
    private CommunityCommentMapper commentMapper;

    @Mock
    private CommunityLikeMapper likeMapper;

    @Mock
    private PetService petService;

    private CommunityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommunityServiceImpl(commentMapper, likeMapper, null, null, petService);
        ReflectionTestUtils.setField(service, "baseMapper", postMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private CommunityPost visiblePost(int likeCount) {
        CommunityPost post = new CommunityPost();
        post.setId(POST_ID);
        post.setAuthorId(USER_ID);
        post.setType(1);
        post.setTitle("奶糖今天学会握手啦");
        post.setContent("奖励了一小块冻干");
        post.setStatus(1);
        post.setLikeCount(likeCount);
        post.setCommentCount(0);
        return post;
    }

    @Test
    @DisplayName("宠物主人发布晒宠动态：只能关联自己的宠物并保存图片")
    void ownerCreatesPetPost() {
        UserContext.set(new LoginUser(USER_ID, "user", "USER"));
        when(postMapper.insert(any(CommunityPost.class))).thenReturn(1);
        CommunityPostCreateDTO dto = new CommunityPostCreateDTO();
        dto.setType(1);
        dto.setPetId(8L);
        dto.setTitle(" 奶糖今天学会握手啦 ");
        dto.setContent(" 奖励了一小块冻干 ");
        dto.setImageUrls(List.of("/uploads/common/a.jpg", "/uploads/common/b.jpg"));

        service.createPost(dto);

        verify(petService).requireMine(8L);
        ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getTitle()).isEqualTo("奶糖今天学会握手啦");
        assertThat(captor.getValue().getImages()).isEqualTo("/uploads/common/a.jpg,/uploads/common/b.jpg");
    }

    @Test
    @DisplayName("接单员可以回答社区问题并原子增加回复数")
    void sitterAnswersQuestion() {
        UserContext.set(new LoginUser(SITTER_ID, "sitter", "SITTER"));
        when(postMapper.selectById(POST_ID)).thenReturn(visiblePost(0));
        when(commentMapper.insert(any(CommunityComment.class))).thenReturn(1);
        when(postMapper.incrementComment(POST_ID)).thenReturn(1);
        CommunityCommentCreateDTO dto = new CommunityCommentCreateDTO();
        dto.setContent(" 建议先观察精神和饮水情况，持续异常请及时就医。 ");

        service.comment(POST_ID, dto);

        ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(SITTER_ID);
        assertThat(captor.getValue().getContent()).startsWith("建议先观察");
        verify(postMapper).incrementComment(POST_ID);
    }

    @Test
    @DisplayName("送出爪印：写入唯一点赞行并增加计数")
    void addsPawLike() {
        UserContext.set(new LoginUser(USER_ID, "user", "USER"));
        when(postMapper.selectById(POST_ID)).thenReturn(visiblePost(0), visiblePost(1));
        when(likeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(likeMapper.insert(any(CommunityLike.class))).thenReturn(1);
        when(postMapper.incrementLike(POST_ID)).thenReturn(1);

        CommunityLikeVO result = service.toggleLike(POST_ID);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(1);
        verify(postMapper).incrementLike(POST_ID);
    }

    @Test
    @DisplayName("收回爪印使用物理删除，之后还能再次送出")
    void removesPawLikePhysically() {
        UserContext.set(new LoginUser(USER_ID, "user", "USER"));
        when(postMapper.selectById(POST_ID)).thenReturn(visiblePost(1), visiblePost(0));
        CommunityLike existing = new CommunityLike();
        existing.setId(99L);
        existing.setPostId(POST_ID);
        existing.setUserId(USER_ID);
        when(likeMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(likeMapper.removeLike(99L)).thenReturn(1);
        when(postMapper.decrementLike(POST_ID)).thenReturn(1);

        CommunityLikeVO result = service.toggleLike(POST_ID);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.getLikeCount()).isZero();
        verify(likeMapper).removeLike(99L);
    }
}
