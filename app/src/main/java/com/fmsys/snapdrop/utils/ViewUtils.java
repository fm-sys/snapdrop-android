package com.fmsys.snapdrop.utils;

import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import com.fmsys.snapdrop.R;
import com.fmsys.snapdrop.SnapdropApplication;

public class ViewUtils {

    private static final Resources APP_RESOURCES = SnapdropApplication.getInstance() == null || SnapdropApplication.getInstance().getApplicationContext() == null ? null : SnapdropApplication.getInstance().getApplicationContext().getResources();

    private ViewUtils() {
        //no instance
    }

    public static int dpToPixel(final float dp) {
        return (int) (dp * (APP_RESOURCES == null ? 20f : APP_RESOURCES.getDisplayMetrics().density));
    }

    public static void showEditTextWithResetPossibility(final Fragment fragment, final CharSequence title, final String prefix, final String initialValue, final Link link, final Consumer<String> resultCallback) {
        final View dialogView = LayoutInflater.from(new ContextThemeWrapper(fragment.requireContext(), R.style.AlertDialogTheme)).inflate(R.layout.edit_text_dialog, null);
        final EditText editText = dialogView.findViewById(R.id.textInput);
        editText.setTag(prefix);
        editText.setText(initialValue);
        editText.requestFocus();

        if (link != null) {
            final TextView helperText = dialogView.findViewById(R.id.helperText);
            helperText.setVisibility(View.VISIBLE);
            helperText.setText(link.description);
            helperText.setOnClickListener(v -> ShareUtils.openUrl(fragment, link.url));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> resultCallback.accept(editText.getText().toString().trim()))
                .setNegativeButton(R.string.reset, (dialog, id) -> resultCallback.accept(null));
        builder.create().show();
    }
}
