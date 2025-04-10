package com.github.devhub.forum.service.article.conveter;

import com.github.devhub.forum.api.model.vo.article.SearchArticleReq;
import com.github.devhub.forum.service.article.repository.params.SearchArticleParams;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-10T20:18:25+0800",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 11.0.15.1 (Oracle Corporation)"
)
public class ArticleStructMapperImpl implements ArticleStructMapper {

    @Override
    public SearchArticleParams toSearchParams(SearchArticleReq req) {
        if ( req == null ) {
            return null;
        }

        SearchArticleParams searchArticleParams = new SearchArticleParams();

        searchArticleParams.setPageNum( req.getPageNumber() );
        searchArticleParams.setPageSize( req.getPageSize() );
        searchArticleParams.setTitle( req.getTitle() );
        searchArticleParams.setArticleId( req.getArticleId() );
        searchArticleParams.setUserId( req.getUserId() );
        searchArticleParams.setUserName( req.getUserName() );
        searchArticleParams.setStatus( req.getStatus() );
        searchArticleParams.setOfficalStat( req.getOfficalStat() );
        searchArticleParams.setToppingStat( req.getToppingStat() );

        return searchArticleParams;
    }
}
