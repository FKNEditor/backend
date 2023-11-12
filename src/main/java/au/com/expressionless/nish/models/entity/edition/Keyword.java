package au.com.expressionless.nish.models.entity.edition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Query;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import au.com.expressionless.nish.models.entity.archetypes.StandardEntityBase;
import au.com.expressionless.nish.utils.GeneralUtils;

/**
 * <p>Represents the Keyword model as a Java object, links to the keyword table in the database</p>
 * <p>Contains:
    * <ul>
    *  <li> {@link Keyword#word Word}: The keyword string that is being represented by this object </li>
    *  <li> {@link Keyword#edition Keyword}: The keyword string that is being represented by this object </li>
    * </ul>
 * </p>
 * <p>Fields inherited from {@link ChronoEntity}:
 * <ul>
 *  <li> {@link ChronoEntity#getCreated() Created}: The date this story was created</li>
 *  <li> {@link ChronoEntity#getUpdated() Updated}: The last date this story was updated (== created for unmodified stories)</li>
 * </ul>
 * 
 */

@Entity
@Table(name = "keyword")
public class Keyword extends StandardEntityBase {

    @EmbeddedId
    public KeywordId keywordId;

    @Column(name = "ratio", nullable = false)
    public Double ratio;

    public Keyword() {

    }

    public Keyword(String word, Edition edition, Double ratio) {
        this.ratio = ratio;
        this.keywordId = new KeywordId(word, edition.id);
    }

    public String getKeyword() {
        return keywordId.getKeyword();
    }

    public void setKeyword(String keyword) {
        this.keywordId.setKeyword(keyword);
    }

    public static List<Keyword> listByEdition(long editionId) {
        return list("keywordId.editionId = ?1", editionId);
    }

    public static Keyword findByEditionAndWord(String word, long editionId) {
        return find("keywordId.editionId = ?1 and keyword = ?2", editionId, word).firstResult();
    }
    
    public static void deleteByEdition(long editionId) {
        delete("keywordId.editionId = ?1", editionId);
    }

    public static void deleteByWord(String word) {
        delete("keyword = ?1", word);
    }

    public static Set<Edition> listEditionsByKeywordSearch(String keyword, Boolean published, int num) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("select a from Keyword key join Edition a with key.keywordId.editionId = a.id");

        boolean hasPublished = published != null;
        boolean hasKeyword = StringUtils.isNotBlank(keyword);
        if(hasPublished) {
            queryString.append(" and a.published = ?0");
        }

        if(hasKeyword) {
            queryString.append(" and key.keywordId.keyword like ?");
            queryString.append(hasPublished ? "1" : "0");
            keyword = GeneralUtils.makeLike(keyword.toLowerCase());
        }

        queryString.append(" order by key.ratio desc");

        Query query = getEntityManager().createQuery(queryString.toString());
        if(hasPublished && hasKeyword) {
            query.setParameter(0, published);
            query.setParameter(1, keyword);
        } else {
            if(hasPublished) {
                query.setParameter(0, published);
            } else if (hasKeyword) {
                query.setParameter(0, keyword);
            }
        }

        List<Edition> objects = query.getResultList();
        Set<Edition> editions = new LinkedHashSet<>();
        Iterator<Edition> iter = objects.iterator();
        while(editions.size() < num && iter.hasNext()) {
            editions.add(iter.next());
        }

        return editions;
    }

    public static Set<Edition> listEditionsByKeywordSearch(String keyword, int num) {
        return listEditionsByKeywordSearch(keyword, null, num);
    }
}
