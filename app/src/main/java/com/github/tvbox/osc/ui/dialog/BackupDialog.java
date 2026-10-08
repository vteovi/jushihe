package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

public class BackupDialog extends BaseDialog {

    public interface ActionListener {
        /** 用户点击「立即备份」 */
        void onBackup();

        /** 用户点击「恢复备份」 */
        void onRestore();
    }

    public BackupDialog(@NonNull @NotNull Context context, ActionListener listener) {
        super(context);
        setContentView(R.layout.dialog_backup);
        findViewById(R.id.backupNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onBackup();
            }
        });
        findViewById(R.id.restoreNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onRestore();
            }
        });
    }
}
