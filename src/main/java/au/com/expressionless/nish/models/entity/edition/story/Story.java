package au.com.expressionless.nish.models.entity.edition.story;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.transaction.Transactional;

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import au.com.expressionless.nish.models.entity.archetypes.MinioEntity;
import au.com.expressionless.nish.models.entity.edition.Edition;
import au.com.expressionless.nish.models.entity.mapper.IntegerSetMapper;

/**
 * <p>Represents the Story model as a Java object, links to the story table in the database</p>
 * <p>Contains:
    * <ul>
    *  <li> {@link Story#edition Edition}: The edition this story is connected to </li>
    *  <li> {@link Story#pageNums Page Numbers}: The Page numbers this story appears on</li>
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
@Table(name = "story")
public class Story extends MinioEntity {

    private String title;
    private String author;

    @ManyToOne
    @JoinColumn(name = "edition_id")
    private Edition edition;

    @Column(name = "text", columnDefinition = "text")
    private String text;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "story_id")
    private List<StorySelection> selections;

    public Story() { 
        this.selections = new ArrayList<>();
    }

    @Transactional
    public Story addSelection(int x, int y, int width, int height, int pageNum, int order) {
        StorySelection s = new StorySelection();
        Bounds b = new Bounds(x, y, width, height, order);
        s.setBounds(b);
        s.setPageNum(pageNum);
        return addSelection(s);
    }

    @Transactional
    public List<StorySelection> addSelections(Map<Integer, List<Bounds>> pageBoundsMap) {

        List<StorySelection> newSelections = new ArrayList<>(); 

        // add selections to story 
        for (Map.Entry<Integer, List<Bounds>> entry: pageBoundsMap.entrySet()) {

            // add bounds in order, page to page
            Integer pageNum = entry.getKey();
            List<Bounds> bounds = entry.getValue();

            for (Bounds b : bounds) {
                StorySelection s = new StorySelection();
                s.setBounds(b);
                s.setPageNum(pageNum);
                addSelection(s);
                newSelections.add(s);
            }
        }
        
        // return the newly added selections
        return newSelections;
    }

    @Transactional
    public Story addSelection(StorySelection selection) {
        if(!selection.isPersistent())
            selection.persist();
        selections.add(selection);
        return this;
    }

    public Edition getEdition() {
        return this.edition;
    }

    public void setEdition(Edition edition) {
        this.edition = edition;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getAuthor() {
        return this.author;
    }

    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }

    public List<StorySelection> getSelections() {
        return this.selections;
    }

    public void setSelections(List<StorySelection> selections) {
        this.selections = selections;
    }

    @Transactional 
    public void deleteSelections() {
        this.selections.clear();
    }

    // ====== DATABASE METHODS ========

    public static List<Story> findByTitle(String title) {
        return list("title", title);
    }

    public static List<Story> findByAuthor(String author) {
        return list("author", author);
    }

    public static List<Story> findByPageAndEdition(long editionId, int pageNum) {
        return list("edition_id = ?1 and page_num like '%" + Integer.toString(pageNum) + "%'", editionId);
    }

    public static List<Story> findByAuthorAndEdition(String author, long editionId) {
        return list("author = ?1 and edition_id = ?2", author, editionId);
    }

    public static List<Story> findByTitleAndEdition(String title, long editionId) {
        return list("title = ?1 and edition_id = ?2", title, editionId);
    }

    public static List<Story> findByEdition(long editionId) {
        return list("edition_id = ?1", editionId);
    }
}
