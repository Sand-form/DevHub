package com.github.devhub.forum.service.article.service;

import com.github.devhub.forum.api.model.vo.PageParam;
import com.github.devhub.forum.api.model.vo.PageVo;
import com.github.devhub.forum.api.model.vo.article.dto.TagDTO;

/**
 * 标签Service
 *
 * @author louzai
 * @date 2022-07-20
 */
public interface TagService {

    PageVo<TagDTO> queryTags(String key, PageParam pageParam);

    Long queryTagId(String tag);
}
