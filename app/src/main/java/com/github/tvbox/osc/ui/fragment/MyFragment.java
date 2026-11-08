package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.text.TextUtils;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.BaseVbFragment;
import com.github.tvbox.osc.databinding.FragmentMyBinding;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FavoritesActivity;
import com.github.tvbox.osc.ui.activity.LiveActivity;
import com.github.tvbox.osc.ui.activity.LocalPlayActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.SubscriptionActivity;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;

import java.util.Arrays;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class MyFragment extends BaseVbFragment<FragmentMyBinding> {


    @Override
    protected void init() {
        mBinding.tvVersion.setText("v"+ AppUtils.getAppVersionName());

        mBinding.addrPlay.setOnClickListener(v ->{
            new XPopup.Builder(getContext())
                    .asInputConfirm("播放", "", isPush(ClipboardUtils.getText().toString())?ClipboardUtils.getText():"", "地址", text -> {
                        if (!TextUtils.isEmpty(text)){
                            Intent newIntent = new Intent(mContext, DetailActivity.class);
                            newIntent.putExtra("id", text);
                            newIntent.putExtra("sourceKey", "push_agent");
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(newIntent);
                        }
                    }, null, R.layout.dialog_input).show();
        });
        //mBinding.tvLive.setOnClickListener(v -> jumpActivity(LivePlayActivity.class));
        mBinding.tvLive.setOnClickListener(v -> jumpActivity(LiveActivity.class));

        mBinding.tvSetting.setOnClickListener(v -> jumpActivity(SettingActivity.class));

        // 收藏与历史记录已合并为一页，两个入口按需定位到对应分栏
        mBinding.tvHistory.setOnClickListener(v -> jumpActivity(
                FavoritesActivity.class,
                FavoritesActivity.tabBundle(FavoritesActivity.TAB_HISTORY)));

        mBinding.tvFavorite.setOnClickListener(v -> jumpActivity(
                FavoritesActivity.class,
                FavoritesActivity.tabBundle(FavoritesActivity.TAB_COLLECT)));

        mBinding.llSubscription.setOnClickListener(v -> jumpActivity(SubscriptionActivity.class));

        mBinding.llAbout.setOnClickListener(v -> {
            new XPopup.Builder(mActivity)
                    .asCustom(new AboutDialog(mActivity))
                    .show();
        });
    }

    private boolean isPush(String text) {
        return !TextUtils.isEmpty(text) && Arrays.asList("smb", "http", "https", "thunder", "magnet", "ed2k", "mitv", "jianpian").contains(Uri.parse(text).getScheme());
    }

}