package tw.tib.financisto.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.model.Currency;

public class CurrencyEntityAdapter extends MyEntityAdapter<Currency> {
    private final LayoutInflater mInflater;
    private final int mResource;
    private final int mFieldId;

    private ForegroundColorSpan currencySymbolSpan;
    private ForegroundColorSpan currencyTitleSpan;

    public CurrencyEntityAdapter(Context context, int resource,
                           int textViewResourceId, List<Currency> objects)
    {
        super(context, resource, textViewResourceId, objects);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        mFieldId = textViewResourceId;

        var r = context.getResources();
        currencySymbolSpan = new ForegroundColorSpan(r.getColor(R.color.currency_symbol));
        currencyTitleSpan = new ForegroundColorSpan(r.getColor(R.color.currency_title));
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    private View createViewFromResource(LayoutInflater inflater, int position, View convertView, @NonNull ViewGroup parent, int resource) {
        final View view;
        final TextView text;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        text = view.findViewById(mFieldId);

        final Currency item = getItem(position);

        var ssb = new SpannableStringBuilder();
        ssb.append(item.name);
        if (item.symbol != null) {
            ssb.append(" ");
            ssb.append(item.symbol, currencySymbolSpan, 0);
        }
        if (item.title != null) {
            ssb.append(" ");
            ssb.append(item.title, currencyTitleSpan, 0);
        }
        text.setText(ssb);

        return view;
    }

}
