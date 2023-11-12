package au.com.expressionless.nish.models.entity.edition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import au.com.expressionless.nish.models.entity.archetypes.MinioEntity;
import au.com.expressionless.nish.models.entity.edition.story.Story;

@Entity
@Table(name = "edition")
public class Edition extends MinioEntity {

    @Column(name = "author")
    private String author;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "last_page")
    private Integer lastPageNum;

    @Column(name = "is_published")
    private Boolean published;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "edition_id")
    private List<Story> stories;

    public Edition() {
        this.stories = new ArrayList<>();
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String name) {
        this.fileName = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setIsPublished(Boolean published) {
        this.published = published;
    }

    public String getAuthor() {
        return this.author;
    }

    public List<Story> getStories() {
        return this.stories;
    }

    public void setStories(List<Story> stories) {
        this.stories = stories;
    }

    public Edition addStory(Story s) {
        if (!s.isPersistent())
            s.persist();
        stories.add(s);
        return this;
    }

    public Boolean isPublished() {
        return this.published;
    }

    public void setToDraft() {
        setIsPublished(false);
    }

    public void setToPublished() {
        setIsPublished(true);
    }

    /**
     * Find an Edition by file name
     * @param fileName
     * @return an optional containing the edition matching the filename (if exists)
     */
    public static Optional<Edition> findByName(String fileName) {
        return find("fileName", fileName).firstResultOptional();
    }

    public static List<Edition> findEditionsByIds(long[] ids) {
        StringBuilder sb = new StringBuilder();
        if(ids == null || ids.length == 0) {
            return new ArrayList<>();
        }

        int i;
        for(i = 0; i < ids.length - 1; i++) {
            sb.append("id = ").append(ids[i]).append(" or ");
        }
        sb.append("id = ").append(ids[i]);
        return list(sb.toString());
    }
}
