package au.com.expressionless.nish.models.entity.edition.story;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import au.com.expressionless.nish.models.entity.archetypes.StandardEntity;

@Entity
@Table(name = "selection")
public class StorySelection extends StandardEntity {

    @NotNull
    @Embedded
    private Bounds bounds;

    @NotNull
    @Column(name = "page_num")
    private Integer pageNum;

    public StorySelection() {
        /* no args constructor */
    }

    public void setBounds(Bounds b) {
        this.bounds = b;
    }

    public Bounds getBounds() {
        return this.bounds;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageNum() {
        return pageNum;
    }
}
