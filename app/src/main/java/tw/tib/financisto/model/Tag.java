package tw.tib.financisto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static tw.tib.financisto.db.DatabaseHelper.TAG_TABLE;
import static tw.tib.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = TAG_TABLE)
public class Tag extends MyEntity implements SortableEntity {

    @Column(name = DEF_SORT_COL)
    public long sortOrder;

    @Column(name = "updated_on")
    public long updatedOn;

    public Tag() {
    }

    public Tag(String title) {
        this.title = title;
        this.isActive = true;
    }

    @Override
    public long getSortOrder() {
        return sortOrder;
    }
}
