package com.pet.service;

import com.pet.common.api.PageResult;
import com.pet.dto.CommunityCommentCreateDTO;
import com.pet.dto.CommunityPostCreateDTO;
import com.pet.dto.CommunityPostQuery;
import com.pet.vo.CommunityCommentVO;
import com.pet.vo.CommunityLikeVO;
import com.pet.vo.CommunityPostVO;

import java.util.List;

public interface CommunityService {

    PageResult<CommunityPostVO> pagePosts(CommunityPostQuery query);

    CommunityPostVO getPost(Long postId);

    Long createPost(CommunityPostCreateDTO dto);

    List<CommunityCommentVO> listComments(Long postId);

    void comment(Long postId, CommunityCommentCreateDTO dto);

    CommunityLikeVO toggleLike(Long postId);
}
