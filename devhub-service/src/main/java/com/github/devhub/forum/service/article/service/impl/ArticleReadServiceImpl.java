package com.github.devhub.forum.service.article.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.devhub.forum.api.model.enums.CollectionStatEnum;
import com.github.devhub.forum.api.model.enums.CommentStatEnum;
import com.github.devhub.forum.api.model.enums.DocumentTypeEnum;
import com.github.devhub.forum.api.model.enums.HomeSelectEnum;
import com.github.devhub.forum.api.model.enums.OperateTypeEnum;
import com.github.devhub.forum.api.model.enums.PraiseStatEnum;
import com.github.devhub.forum.api.model.exception.ExceptionUtil;
import com.github.devhub.forum.api.model.vo.PageListVo;
import com.github.devhub.forum.api.model.vo.PageParam;
import com.github.devhub.forum.api.model.vo.PageVo;
import com.github.devhub.forum.api.model.vo.article.dto.ArticleDTO;
import com.github.devhub.forum.api.model.vo.article.dto.CategoryDTO;
import com.github.devhub.forum.api.model.vo.article.dto.SimpleArticleDTO;
import com.github.devhub.forum.api.model.vo.article.dto.TagDTO;
import com.github.devhub.forum.api.model.vo.constants.StatusEnum;
import com.github.devhub.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.devhub.forum.core.cache.RedisClient;
import com.github.devhub.forum.core.util.ArticleUtil;
import com.github.devhub.forum.core.util.SpringUtil;
import com.github.devhub.forum.service.article.conveter.ArticleConverter;
import com.github.devhub.forum.service.article.repository.dao.ArticleDao;
import com.github.devhub.forum.service.article.repository.dao.ArticleTagDao;
import com.github.devhub.forum.service.article.repository.entity.ArticleDO;
import com.github.devhub.forum.service.article.service.ArticleReadService;
import com.github.devhub.forum.service.article.service.CategoryService;
import com.github.devhub.forum.service.constant.EsFieldConstant;
import com.github.devhub.forum.service.constant.EsIndexConstant;
import com.github.devhub.forum.service.constant.RedisConstant;
import com.github.devhub.forum.service.statistics.service.CountService;
import com.github.devhub.forum.service.user.repository.entity.UserFootDO;
import com.github.devhub.forum.service.user.service.UserFootService;
import com.github.devhub.forum.service.user.service.UserService;
import com.github.devhub.forum.service.utils.RedisLuaUtil;
import com.github.devhub.forum.service.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文章查询相关服务类
 *
 * @author louzai
 * @date 2022-07-20
 */
@Slf4j
@Service
public class ArticleReadServiceImpl implements ArticleReadService {

    @Autowired
    private ArticleDao articleDao;

    @Autowired
    private ArticleTagDao articleTagDao;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisLuaUtil redisLuaUtil;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Value("${spring.redis.isOpen}")
    private Boolean openRedis;

    /**
     * 在一个项目中，UserFootService 就是内部服务调用
     * 拆微服务时，这个会作为远程服务访问
     */
    @Autowired
    private UserFootService userFootService;

    @Autowired
    private CountService countService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    // 是否开启ES
    @Value("${elasticsearch.open:false}")
    private Boolean openES;

    @Override
    public ArticleDO queryBasicArticle(Long articleId) {
        return articleDao.getById(articleId);
    }

    @Override
    public String generateSummary(String content) {
        return ArticleUtil.pickSummary(content);
    }

    @Override
    public PageVo<TagDTO> queryTagsByArticleId(Long articleId) {
        List<TagDTO> tagDTOS = articleTagDao.queryArticleTagDetails(articleId);
        return PageVo.build(tagDTOS, 1, 10, tagDTOS.size());
    }

    @Override
    public ArticleDTO queryDetailArticleInfo(Long articleId) {
//        ArticleDTO article = articleDao.queryArticleDetail(articleId);
//        if (article == null) {
//            throw ExceptionUtil.of(StatusEnum.ARTICLE_NOT_EXISTS, articleId);
//        }
//        // 更新分类相关信息
//        CategoryDTO category = article.getCategory();
//        category.setCategory(categoryService.queryCategoryName(category.getCategoryId()));
//
//        // 更新标签信息
//        article.setTags(articleTagDao.queryArticleTagDetails(articleId));
//        return article;
        // TODO ygl:引入Redis缓存
        ArticleDTO article = null;
        // 兼容是否开启Redis
        if (openRedis) {
            String redisCacheKey = RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_CACHE + articleId;
            String articleStr = RedisClient.getStr(redisCacheKey);

            if (!ObjectUtils.isEmpty(articleStr)) {
                article = JSONUtil.toBean(articleStr, ArticleDTO.class);
            } else {
                // 存在缓存击穿问题，引入分布式锁
            /*
            第四种方式：
                优点：解决了第三种方式无法设置合适过期时间
            */
                article = this.checkArticleByDBFour(articleId);

            }
            if (article != null) {
                RedisClient.setStr(redisCacheKey, JSONUtil.toJsonStr(article));
            }

        } else {
            article = articleDao.queryArticleDetail(articleId);
        }

        if (article == null) {
            throw ExceptionUtil.of(StatusEnum.ARTICLE_NOT_EXISTS, articleId);
        }

        // 更新分类相关信息
        CategoryDTO category = article.getCategory();
        category.setCategory(categoryService.queryCategoryName(category.getCategoryId()));

        // 更新标签信息
        article.setTags(articleTagDao.queryArticleTagDetails(articleId));
        return article;
    }

    /**
     * Redis分布式锁第四种方法
     *
     * @param articleId
     * @return ArticleDTO
     */
    private ArticleDTO checkArticleByDBFour(Long articleId) {

        ArticleDTO article = null;
        String redisLockKey =
                RedisConstant.REDIS_PAI + RedisConstant.REDIS_PRE_ARTICLE + RedisConstant.REDIS_LOCK + articleId;
        RLock lock = redissonClient.getLock(redisLockKey);
        //lock.lock();

        try {
            //尝试加锁,最大等待时间3秒，上锁30秒自动解锁
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                article = articleDao.queryArticleDetail(articleId);
            } else {
                // 未获得分布式锁线程睡眠一下；然后再去获取数据
                Thread.sleep(200);
                this.queryDetailArticleInfo(articleId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //判断该lock是否已经锁 并且 锁是否是自己的
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

        }
        return article;
    }


    /**
     * 查询文章所有的关联信息，正文，分类，标签，阅读计数，当前登录用户是否点赞、评论过
     *
     * @param articleId
     * @param readUser
     * @return
     */
    @Override
    public ArticleDTO queryFullArticleInfo(Long articleId, Long readUser) {
        ArticleDTO article = queryDetailArticleInfo(articleId);

        // 文章阅读计数+1
        countService.incrArticleReadCount(article.getAuthor(), articleId);

        // 文章的操作标记
        if (readUser != null) {
            // 更新用于足迹，并判断是否点赞、评论、收藏
            UserFootDO foot = userFootService.saveOrUpdateUserFoot(DocumentTypeEnum.ARTICLE, articleId,
                    article.getAuthor(), readUser, OperateTypeEnum.READ);
            article.setPraised(Objects.equals(foot.getPraiseStat(), PraiseStatEnum.PRAISE.getCode()));
            article.setCommented(Objects.equals(foot.getCommentStat(), CommentStatEnum.COMMENT.getCode()));
            article.setCollected(Objects.equals(foot.getCollectionStat(), CollectionStatEnum.COLLECTION.getCode()));
        } else {
            // 未登录，全部设置为未处理
            article.setPraised(false);
            article.setCommented(false);
            article.setCollected(false);
        }

        // 更新文章统计计数
        article.setCount(countService.queryArticleStatisticInfo(articleId));

        // 设置文章的点赞列表
        article.setPraisedUsers(userFootService.queryArticlePraisedUsers(articleId));
        return article;
    }


    /**
     * 查询文章列表
     *
     * @param categoryId
     * @param page
     * @return
     */
    @Override
    public PageListVo<ArticleDTO> queryArticlesByCategory(Long categoryId, PageParam page) {
        List<ArticleDO> records = articleDao.listArticlesByCategoryId(categoryId, page);
        return buildArticleListVo(records, page.getPageSize());
    }

    /**
     * 查询置顶的文章列表
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<ArticleDTO> queryTopArticlesByCategory(Long categoryId) {
        PageParam page = PageParam.newPageInstance(PageParam.DEFAULT_PAGE_NUM, PageParam.TOP_PAGE_SIZE);
        List<ArticleDO> articleDTOS = articleDao.listArticlesByCategoryId(categoryId, page);
        return articleDTOS.stream().map(this::fillArticleRelatedInfo).collect(Collectors.toList());
    }

    @Override
    public Long queryArticleCountByCategory(Long categoryId) {
        return articleDao.countArticleByCategoryId(categoryId);
    }

    @Override
    public Map<Long, Long> queryArticleCountsByCategory() {
        return articleDao.countArticleByCategoryId();
    }

    @Override
    public PageListVo<ArticleDTO> queryArticlesByTag(Long tagId, PageParam page) {
        List<ArticleDO> records = articleDao.listRelatedArticlesOrderByReadCount(null, Arrays.asList(tagId), page);
        return buildArticleListVo(records, page.getPageSize());
    }

    @Override
    public List<SimpleArticleDTO> querySimpleArticleBySearchKey(String key) {
        // todo 当key为空时，返回热门推荐
        if (StringUtils.isBlank(key)) {
            return Collections.emptyList();
        }
        key = key.trim();
        if (!openES) {
            List<ArticleDO> records = articleDao.listSimpleArticlesByBySearchKey(key);
            return records.stream().map(s -> new SimpleArticleDTO().setId(s.getId()).setTitle(s.getTitle()))
                    .collect(Collectors.toList());
        }
        // TODO ES整合
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(key,
                EsFieldConstant.ES_FIELD_TITLE,
                EsFieldConstant.ES_FIELD_SHORT_TITLE);
        searchSourceBuilder.query(multiMatchQueryBuilder);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsIndexConstant.ES_INDEX_ARTICLE},
                searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = SpringUtil.getBean(RestHighLevelClient.class).search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("failed to query from es: key", e);
        }
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hitsList = hits.getHits();
        List<Integer> ids = new ArrayList<>();
        for (SearchHit documentFields : hitsList) {
            ids.add(Integer.parseInt(documentFields.getId()));
        }
        if (ObjectUtils.isEmpty(ids)) {
            return null;
        }
        List<ArticleDO> records = articleDao.selectByIds(ids);
        return records.stream().map(s -> new SimpleArticleDTO().setId(s.getId()).setTitle(s.getTitle()))
                .collect(Collectors.toList());
    }

    @Override
    public PageListVo<ArticleDTO> queryArticlesBySearchKey(String key, PageParam page) {
        List<ArticleDO> records = articleDao.listArticlesByBySearchKey(key, page);
        return buildArticleListVo(records, page.getPageSize());
    }


    @Override
    public PageListVo<ArticleDTO> queryArticlesByUserAndType(Long userId, PageParam pageParam, HomeSelectEnum select) {
        List<ArticleDO> records = null;
        if (select == HomeSelectEnum.ARTICLE) {
            // 用户的文章列表
            records = articleDao.listArticlesByUserId(userId, pageParam);
        } else if (select == HomeSelectEnum.READ) {
            // 用户的阅读记录
            List<Long> articleIds = userFootService.queryUserReadArticleList(userId, pageParam);
            records = CollectionUtils.isEmpty(articleIds) ? Collections.emptyList() : articleDao.listByIds(articleIds);
            records = sortByIds(articleIds, records);
        } else if (select == HomeSelectEnum.COLLECTION) {
            // 用户的收藏列表
            List<Long> articleIds = userFootService.queryUserCollectionArticleList(userId, pageParam);
            records = CollectionUtils.isEmpty(articleIds) ? Collections.emptyList() : articleDao.listByIds(articleIds);
            records = sortByIds(articleIds, records);
        }

        if (CollectionUtils.isEmpty(records)) {
            return PageListVo.emptyVo();
        }
        return buildArticleListVo(records, pageParam.getPageSize());
    }

    /**
     * fixme @楼仔 这个排序逻辑看着像是有问题的样子
     *
     * @param articleIds
     * @param records
     * @return
     */
    private List<ArticleDO> sortByIds(List<Long> articleIds, List<ArticleDO> records) {
        List<ArticleDO> articleDOS = new ArrayList<>();
        Map<Long, ArticleDO> articleDOMap = records.stream().collect(Collectors.toMap(ArticleDO::getId, t -> t));
        articleIds.forEach(articleId -> {
            if (articleDOMap.containsKey(articleId)) {
                articleDOS.add(articleDOMap.get(articleId));
            }
        });
        return articleDOS;
    }

    @Override
    public PageListVo<ArticleDTO> buildArticleListVo(List<ArticleDO> records, long pageSize) {
        List<ArticleDTO> result = records.stream().map(this::fillArticleRelatedInfo).collect(Collectors.toList());
        return PageListVo.newVo(result, pageSize);
    }

    /**
     * 补全文章的阅读计数、作者、分类、标签等信息
     *
     * @param record
     * @return
     */
    private ArticleDTO fillArticleRelatedInfo(ArticleDO record) {
        ArticleDTO dto = ArticleConverter.toDto(record);
        // 分类信息
        dto.getCategory().setCategory(categoryService.queryCategoryName(record.getCategoryId()));
        // 标签列表
        dto.setTags(articleTagDao.queryArticleTagDetails(record.getId()));
        // 阅读计数统计
        dto.setCount(countService.queryArticleStatisticInfo(record.getId()));
        // 作者信息
        BaseUserInfoDTO author = userService.queryBasicUserInfo(dto.getAuthor());
        dto.setAuthorName(author.getUserName());
        dto.setAuthorAvatar(author.getPhoto());
        return dto;
    }

    @Override
    public PageListVo<SimpleArticleDTO> queryHotArticlesForRecommend(PageParam pageParam) {
        List<SimpleArticleDTO> list = articleDao.listHotArticles(pageParam);
        return PageListVo.newVo(list, pageParam.getPageSize());
    }

    @Override
    public int queryArticleCount(long authorId) {
        return articleDao.countArticleByUser(authorId);
    }

    @Override
    public Long getArticleCount() {
        return articleDao.countArticle();
    }
}
