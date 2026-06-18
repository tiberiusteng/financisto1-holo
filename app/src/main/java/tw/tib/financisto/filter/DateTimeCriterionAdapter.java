package tw.tib.financisto.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DateTimeCriterionAdapter implements JsonSerializer<DateTimeCriterion> {
    @Override
    public JsonElement serialize(DateTimeCriterion src, Type typeOfSrc, JsonSerializationContext context) {
        var array = new JsonArray();
        var period = src.getPeriod();
        array.add(1);
        array.add(period.type.name());
        array.add(period.start);
        array.add(period.end);
        return array;
    }
}
