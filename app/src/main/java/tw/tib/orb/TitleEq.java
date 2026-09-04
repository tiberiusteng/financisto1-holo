package tw.tib.orb;

import java.util.Arrays;
import java.util.Collections;

public class TitleEq implements Expression {
    private final String value;

    TitleEq(String value) {
        this.value = value;
    }

    @Override
    public Selection toSelection(EntityDefinition ed) {
        if (ed.supportAliases) {
            return new Selection("("+ed.getColumnForField("title")+" = ? OR a.alias = ?)", Arrays.asList(String.valueOf(value), String.valueOf(value)));
        }
        else {
            return new Selection("("+ed.getColumnForField("title")+" = ?)", Collections.singletonList(String.valueOf(value)));
        }
    }
}
