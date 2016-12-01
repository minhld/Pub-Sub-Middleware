package com.minhld.servertest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by minhld on 01/28/2016.
 */
public class UITools {
    public static final String GO_IP = "192.168.49.1";
    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    /**
     * display confirmation YES/NO
     *
     * @param c
     * @param message
     * @param listener
     */
    public static void showYesNo(Context c, String message, final ConfirmListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("confirm");
        builder.setMessage(message);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (listener != null) {
                    listener.confirmed();
                }
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

//    /**
//     * open a dialog to prompt text
//     *
//     * @param c
//     * @param listener
//     */
//    public static void showInputDialog(Context c, final InputDialogListener listener, String... defs) {
//        // get prompts.xml view
//        LayoutInflater layoutInflater = LayoutInflater.from(c);
//        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(c);
//        alertDialogBuilder.setView(promptView);
//
//        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
//        if (defs.length > 0) editText.setText(defs[0]);
//
//        // setup a dialog window
//        alertDialogBuilder.setCancelable(false)
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        listener.inputDone(editText.getText().toString());
//                    }
//                })
//                .setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.cancel();
//                            }
//                        });
//
//        // create an alert dialog
//        AlertDialog alert = alertDialogBuilder.create();
//        alert.show();
//    }

    public interface InputDialogListener {
        public void inputDone(String resultStr);
    }

    /**
     * write the log out to the main screen
     *
     * @param c
     * @param log
     * @param msg
     */
    public static void writeLog(Activity c, final TextView log, final String msg){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": " + msg + "\r\n");
            }
        });
    }

    /**
     * write the logout with prefix and exception
     *
     * @param c
     * @param log
     * @param prefix
     * @param e
     */
    public static void writeLog(Activity c, final TextView log, final String prefix, final Exception e){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": [" + prefix + "] " + e.getMessage() + "\r\n");
                e.printStackTrace();
            }
        });
    }

    public interface ConfirmListener {
        public void confirmed();
    }

}
