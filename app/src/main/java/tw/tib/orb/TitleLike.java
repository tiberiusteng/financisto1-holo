package tw.tib.orb;

import java.util.Arrays;
import java.util.Collections;

public class TitleLike implements Expression {
    private final String value;

    TitleLike(String value) {
        this.value = value;
    }

    @Override
    public Selection toSelection(EntityDefinition ed) {
        if (ed.supportAliases) {
            return new Selection("("+ed.getColumnForField("title")+" LIKE ? OR a.alias LIKE ?)", Arrays.asList(String.valueOf(value), String.valueOf(value)));
        }
        else {
            return new Selection("("+ed.getColumnForField("title")+" LIKE ?)", Collections.singletonList(String.valueOf(value)));
        }
    }
}
