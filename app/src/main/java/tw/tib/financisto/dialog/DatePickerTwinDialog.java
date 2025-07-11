package tw.tib.financisto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import tw.tib.financisto.R;

public class DatePickerTwinDialog extends DialogFragment {
    public static String TAG = "DatePickerTwinDialog";

    protected DatePicker dateCalendar;
    protected DatePicker dateSpinner;
    protected DatePicker.OnDateChangedListener listener;

    public static final String YEAR = "YEAR";
    public static final String MONTH = "MONTH";
    public static final String DAY = "DAY";

    protected boolean updating = false;

    public static DatePickerTwinDialog newInstance(int year, int month, int day, DatePicker.OnDateChangedListener listener) {
        DatePickerTwinDialog d = new DatePickerTwinDialog();

        Bundle args = new Bundle();
        args.putInt(YEAR, year);
        args.putInt(MONTH, month);
        args.putInt(DAY, day);
        d.setArguments(args);

        d.listener = listener;

        return d;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater;
        Context context = getActivity();

        if (Build.VERSION.SDK_INT >= 22) {
            inflater = LayoutInflater.from(context).cloneInContext(new ContextThemeWrapper(context,
                   android.R.style.Theme_DeviceDefault_Dialog_Alert));
        }
        else {
            inflater = getActivity().getLayoutInflater();
        }

        View view = inflater.inflate(R.layout.date_select_twin, null, false);

        dateCalendar = view.findViewById(R.id.dateCalendar);
        dateSpinner = view.findViewById(R.id.dateSpinner);

        Bundle args = getArguments();
        int year = args.getInt(YEAR), month = args.getInt(MONTH), day = args.getInt(DAY);

        dateCalendar.init(year, month, day, (v, pyear, pmonth, pday) -> {
            if (!updating) {
                updating = true;
                dateSpinner.updateDate(pyear, pmonth, pday);
                updating = false;
            }
        });

        dateSpinner.init(year, month, day, (v, pyear, pmonth, pday) -> {
            if (!updating) {
                updating = true;
                dateCalendar.updateDate(pyear, pmonth, pday);
                updating = false;
            }
        });

        int theme = 0;
        return new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton(R.string.ok,
                        (dialogInterface, i) -> listener.onDateChanged(dateCalendar,
                                dateCalendar.getYear(),
                                dateCalendar.getMonth(),
                                dateCalendar.getDayOfMonth()))
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {})
                .create();
    }
}
