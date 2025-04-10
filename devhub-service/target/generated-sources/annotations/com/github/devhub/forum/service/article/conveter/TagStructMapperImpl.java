package com.github.devhub.forum.service.article.conveter;

import com.github.devhub.forum.api.model.vo.article.SearchTagReq;
import com.github.devhub.forum.api.model.vo.article.TagReq;
import com.github.devhub.forum.api.model.vo.article.dto.TagDTO;
import com.github.devhub.forum.service.article.repository.entity.TagDO;
import com.github.devhub.forum.service.article.repository.params.SearchTagParams;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-10T20:18:25+0800",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 11.0.15.1 (Oracle Corporation)"
)
public class TagStructMapperImpl implements TagStructMapper {

    @Override
    public SearchTagParams toSearchParams(SearchTagReq req) {
        if ( req == null ) {
            return null;
        }

        SearchTagParams searchTagParams = new SearchTagParams();

        if ( req.getPageNumber() != null ) {
            searchTagParams.setPageNum( req.getPageNumber() );
        }
        if ( req.getPageSize() != null ) {
            searchTagParams.setPageSize( req.getPageSize() );
        }
        searchTagParams.setTag( req.getTag() );

        return searchTagParams;
    }

    @Override
    public TagDTO toDTO(TagDO tagDO) {
        if ( tagDO == null ) {
            return null;
        }

        TagDTO tagDTO = new TagDTO();

        tagDTO.setTagId( tagDO.getId() );
        tagDTO.setTag( tagDO.getTagName() );
        tagDTO.setStatus( tagDO.getStatus() );

        return tagDTO;
    }

    @Override
    public List<TagDTO> toDTOs(List<TagDO> list) {
        if ( list == null ) {
            return null;
        }

        List<TagDTO> list1 = new ArrayList<TagDTO>( list.size() );
        for ( TagDO tagDO : list ) {
            list1.add( toDTO( tagDO ) );
        }

        return list1;
    }

    @Override
    public TagDO toDO(TagReq tagReq) {
        if ( tagReq == null ) {
            return null;
        }

        TagDO tagDO = new TagDO();

        tagDO.setTagName( tagReq.getTag() );

        return tagDO;
    }
}
