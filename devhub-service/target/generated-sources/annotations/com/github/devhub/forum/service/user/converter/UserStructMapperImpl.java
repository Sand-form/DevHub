package com.github.devhub.forum.service.user.converter;

import com.github.devhub.forum.api.model.vo.user.SearchZsxqUserReq;
import com.github.devhub.forum.service.user.repository.params.SearchZsxqWhiteParams;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-10T20:18:25+0800",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 11.0.15.1 (Oracle Corporation)"
)
public class UserStructMapperImpl implements UserStructMapper {

    @Override
    public SearchZsxqWhiteParams toSearchParams(SearchZsxqUserReq req) {
        if ( req == null ) {
            return null;
        }

        SearchZsxqWhiteParams searchZsxqWhiteParams = new SearchZsxqWhiteParams();

        if ( req.getPageNumber() != null ) {
            searchZsxqWhiteParams.setPageNum( req.getPageNumber() );
        }
        searchZsxqWhiteParams.setStatus( req.getState() );
        if ( req.getPageSize() != null ) {
            searchZsxqWhiteParams.setPageSize( req.getPageSize() );
        }
        searchZsxqWhiteParams.setStarNumber( req.getStarNumber() );
        searchZsxqWhiteParams.setName( req.getName() );
        searchZsxqWhiteParams.setUserCode( req.getUserCode() );

        return searchZsxqWhiteParams;
    }
}
