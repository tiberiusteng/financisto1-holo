package tw.tib.financisto.report;

import tw.tib.financisto.graph.GraphUnit;
import tw.tib.financisto.model.Total;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/28/11 9:16 PM
 */
public class ReportData {

    public final List<GraphUnit> units;
    public final Total total;

    public ReportData(List<GraphUnit> units, Total total) {
        this.units = units;
        this.total = total;
    }

}
