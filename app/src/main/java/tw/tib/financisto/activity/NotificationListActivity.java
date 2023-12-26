package tw.tib.financisto.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import tw.tib.financisto.R;
import tw.tib.financisto.service.NotificationCache;
import tw.tib.financisto.service.NotificationListener;

public class NotificationListActivity extends AppCompatActivity {
    private ListView list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.notification_list);
        list = findViewById(android.R.id.list);
        list.setAdapter(new NotificationListAdapter(this));
        list.setOnItemClickListener((adapterView, view, i, l) -> {
            NotificationViewHolder holder = (NotificationViewHolder) view.getTag();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.notification_content),
                    holder.notification.title + "\n" + holder.notification.body);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.notification_copied, Toast.LENGTH_SHORT).show();
        });
    }

    static class NotificationListAdapter extends BaseAdapter {
        private final ArrayList<NotificationListener.ParsedNotification> list;
        private final LayoutInflater inflater;

        public NotificationListAdapter(Context context) {
            list = new ArrayList<>(NotificationCache.getInstance().cache.values());
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            NotificationViewHolder notificationViewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.notification_list_item, parent, false);
                notificationViewHolder = new NotificationViewHolder(view);
                view.setTag(notificationViewHolder);
            }
            else {
                notificationViewHolder = (NotificationViewHolder) view.getTag();
            }
            notificationViewHolder.bindView(list.get(i));

            return view;
        }
    }

    static class NotificationViewHolder {
        public TextView title;
        public TextView body;
        public NotificationListener.ParsedNotification notification;

        public NotificationViewHolder(@NonNull View itemView) {
            title = itemView.findViewById(R.id.title);
            body = itemView.findViewById(R.id.body);
        }

        public void bindView(NotificationListener.ParsedNotification notification) {
            this.notification = notification;
            title.setText(notification.title);
            body.setText(notification.body);
        }
    }
}
