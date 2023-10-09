package tw.tib.financisto.export.drive;

import com.google.api.client.util.DateTime;

public class GoogleDriveFileInfo implements Comparable<GoogleDriveFileInfo> {
    public final String id;
    public final String name;
    public final DateTime created;

    public GoogleDriveFileInfo(String id, String name, DateTime created) {
        this.id = id;
        this.name = name;
        this.created = created;
    }

    @Override
    public int compareTo(GoogleDriveFileInfo another) {
        if (created.getValue() < another.created.getValue()) {
            return -1;
        }
        else if (created.getValue() == another.created.getValue()) {
            return 0;
        }
        else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
