package au.com.expressionless.nish.models.entity.archetypes;

import java.time.LocalDate;
import java.time.ZoneId;

import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@MappedSuperclass
public class StandardEntityBase extends PanacheEntityBase {

    private LocalDate created;

    private LocalDate updated;

    public StandardEntityBase() {
        
    }


    public LocalDate getCreated() {
        return created;
    }

    public LocalDate getUpdated() {
        return updated;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public void setUpdated(LocalDate updated) {
        this.updated = updated;
    }

    @Override
    public void persist() {
        this.updated = LocalDate.now(ZoneId.of("UTC"));
        if(!isPersistent())
            this.created = updated;
        super.persist();
    }
}
