package tw.tib.financisto.export.qif;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 9:39 PM
 */
public class QifBufferedWriter {

    private final BufferedWriter bw;

    public QifBufferedWriter(BufferedWriter bw) {
        this.bw = bw;
    }

    public QifBufferedWriter write(String str) throws IOException {
        bw.write(str);
        return this;
    }

    public void newLine() throws IOException {
        bw.write("\n");
    }

    public void end() throws IOException {
        bw.write("^\n");
    }

    public void writeAccountsHeader() throws IOException {
        bw.write("!Account");
        newLine();
    }

    public void writeCategoriesHeader() throws IOException {
        bw.write("!Type:Cat");
        newLine();
    }

}
