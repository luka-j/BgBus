package rs.luka.android.bgbus.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import rs.luka.android.bgbus.R;

/**
 * Created by luka on 14.1.16..
 */
public class InfoDialog extends DialogFragment {
    private static final String ARG_TITLE      = "bgbus.dialog.title";
    private static final String ARG_MESSAGE    = "bgbus.dialog.message";
    private Callbacks callbacks;

    public static InfoDialog newInstance(String title, String message) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        InfoDialog fragment = new InfoDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks)activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        return new AlertDialog.Builder(getContext()).setTitle(args.getString(ARG_TITLE))
                                                    .setMessage(args.getString(ARG_MESSAGE))
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            callbacks.onDialogClosed(InfoDialog.this);
                                                        }
                                                    })
                                                    .create();
    }

    interface Callbacks {
        void onDialogClosed(DialogFragment dialog);
    }
}
