package tw.tib.financisto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.model.MyEntity;

public class EntityListMultiChoiceAdapter<T extends MyEntity> extends BaseAdapter {
    private final LayoutInflater inflater;

    private List<T> entities;

    public EntityListMultiChoiceAdapter(Context context, List<T> entities) {
        this.entities = entities;
        this.inflater = LayoutInflater.from(context);
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (entities == null) {
            return 0;
        }
        return entities.size();
    }

    @Override
    public T getItem(int i) {
        return entities.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckedTextView v;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.simple_list_item_multiple_choice, parent, false);
            v = convertView.findViewById(android.R.id.text1);
            convertView.setTag(v);
        } else {
            v = (CheckedTextView) convertView.getTag();
        }

        MyEntity e = getItem(position);
        v.setText(e.title);
        if (e.isActive) {
            v.setEnabled(true);
        }
        else {
            v.setEnabled(false);
        }
        return convertView;
    }

}
