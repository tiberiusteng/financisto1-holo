package tw.tib.financisto.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import tw.tib.financisto.Application;
import tw.tib.financisto.datetime.PeriodType;

public class CriterionAdapter implements JsonSerializer<Criterion>, JsonDeserializer<Criterion> {
    @Override
    public Criterion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        var array = json.getAsJsonArray();
        var t = array.get(0).getAsInt();
        if (t == 0) {
            // Criterion
            return new Criterion(
                    array.get(1).getAsString(),
                    WhereFilter.Operation.valueOf(array.get(2).getAsString()),
                    context.deserialize(array.get(3), String[].class),
                    context.deserialize(array.get(4), Criterion[].class));
        }
        else {
            // DateTimeCriterion
            PeriodType p = PeriodType.valueOf(array.get(1).getAsString());
            var start = array.get(2).getAsLong();
            var end = array.get(3).getAsLong();
            if (p == PeriodType.CUSTOM) {
                return new DateTimeCriterion(start, end);
            }
            else {
                return new DateTimeCriterion(p);
            }
        }
    }

    @Override
    public JsonElement serialize(Criterion src, Type typeOfSrc, JsonSerializationContext context) {
        var array = new JsonArray();
        array.add(0);
        array.add(src.columnName);
        array.add(src.operation.name());
        array.add(context.serialize(src.getValues()));
        array.add(context.serialize(src.getChildren()));
        return array;
    }
}
