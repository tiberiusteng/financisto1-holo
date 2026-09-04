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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;

import tw.tib.financisto.R;
import tw.tib.financisto.service.NotificationCache;
import tw.tib.financisto.service.NotificationListener;

public class NotificationListActivity extends AppCompatActivity {
    private ListView list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.notification_list);

        setSupportActionBar(findViewById(R.id.toolbar));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.captionBar());
            if (v.getPaddingTop() == 0) {
                var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.height += insets.top;
                v.setLayoutParams(lp);
                v.setPadding(0, insets.top, 0, 0);
            }
            return WindowInsetsCompat.CONSUMED;
        });

        // Bottom insets: under edge-to-edge the last list item ends up behind the
        // navigation bar and cannot be scrolled clear of it. Do what the settings screen
        // (androidx preference) does: pad by the navigation bar height and turn off
        // clipToPadding, so content still scrolls under the bar but the last item comes
        // fully clear at the end of the list.
        //
        // The listener is attached to the root rather than the list: insets are dispatched
        // to the root first, and the toolbar listener above returns CONSUMED, so a listener
        // on the list itself may receive already-consumed insets. This one does not consume
        // them, leaving the toolbar's top handling untouched.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notification_list), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ListView lv = findViewById(android.R.id.list);
            lv.setPadding(lv.getPaddingLeft(), lv.getPaddingTop(), lv.getPaddingRight(), insets.bottom);
            lv.setClipToPadding(false);
            return windowInsets;
        });

        // opening this screen is what a user does when notifications stopped arriving,
        // so let it heal a listener the system silently unbound
        NotificationListener.requestRebindIfGranted(this);

        list = findViewById(android.R.id.list);
        list.setAdapter(new NotificationListAdapter(this));

        // Copy notification content to clipboard
        list.setOnItemClickListener((adapterView, view, i, l) -> {
            NotificationViewHolder holder = (NotificationViewHolder) view.getTag();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.notification_content),
                    holder.notification.title + "\n" + holder.notification.body);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.notification_copied, Toast.LENGTH_SHORT).show();
        });

        // Copy notification package name to clipboard
        list.setOnItemLongClickListener((adapterView, view, i, l) -> {
            NotificationViewHolder holder = (NotificationViewHolder) view.getTag();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.notification_package_name),
                    holder.notification.pkg);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.notification_package_name_copied, Toast.LENGTH_SHORT).show();

            return true;
        });

    }

    static class NotificationListAdapter extends BaseAdapter {
        private final ArrayList<NotificationListener.ParsedNotification> list;
        private final LayoutInflater inflater;

        public NotificationListAdapter(Context context) {
            list = new ArrayList<>(NotificationCache.getInstance().cache.values());
            // The cache is a HashMap, so its iteration order is bucket order: the list
            // comes out in no meaningful order and can reshuffle when a notification is
            // added. Show newest first instead.
            Collections.sort(list, (a, b) -> Long.compare(b.postTime, a.postTime));
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
        public TextView pkg;
        public TextView title;
        public TextView body;
        public NotificationListener.ParsedNotification notification;

        public NotificationViewHolder(@NonNull View itemView) {
            pkg = itemView.findViewById(R.id.pkg);
            title = itemView.findViewById(R.id.title);
            body = itemView.findViewById(R.id.body);
        }

        public void bindView(NotificationListener.ParsedNotification notification) {
            this.notification = notification;
            pkg.setText(notification.pkg);
            title.setText(notification.title);
            body.setText(notification.body);
        }
    }
}
