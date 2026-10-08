package tw.tib.financisto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import static tw.tib.financisto.db.DatabaseHelper.TAG_TABLE;
import static tw.tib.orb.EntityManager.DEF_SORT_COL;

import android.graphics.Color;

import androidx.core.content.ContextCompat;

import tw.tib.financisto.Application;
import tw.tib.financisto.R;

@Entity
@Table(name = TAG_TABLE)
public class Tag extends MyEntity implements SortableEntity {

    @Column(name = "color")
    public String color;

    @Column(name = DEF_SORT_COL)
    public long sortOrder;

    @Column(name = "updated_on")
    public long updatedOn;

    @Transient
    Integer colorInt;

    public Tag() {
        this.color = "";
    }

    public Tag(String title) {
        this.title = title;
        this.color = "";
        this.isActive = true;
    }

    public Tag(String title, boolean checked) {
        this.title = title;
        this.color = "";
        this.isActive = true;
        this.checked = checked;
    }

    public Integer getColorInt() {
        if (colorInt != null) return colorInt;
        try {
            colorInt = Color.parseColor(color);
        } catch (Exception e) {
            colorInt = ContextCompat.getColor(Application.getInstance(), R.color.tag_pill_bg);
        }
        return colorInt;
    }

    @Override
    public long getSortOrder() {
        return sortOrder;
    }
}
