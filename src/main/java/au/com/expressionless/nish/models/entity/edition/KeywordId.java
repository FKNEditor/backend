package au.com.expressionless.nish.models.entity.edition;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

@Embeddable
public class KeywordId implements Serializable {
    
    @Column(name = "keyword")
    private String keyword;

    @Column(name = "edition_id")
    private Long editionId;

    public KeywordId() {
        /* no-args default constructor */
    }

    public KeywordId(String keyword, Long editionId) {
        this.keyword = keyword;
        this.editionId = editionId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof KeywordId otherId) {
            if(otherId.editionId != this.editionId)
                return false;

            return (otherId.keyword.equals(this.keyword));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(keyword)
                .append(editionId)
                .build();
    }
}
