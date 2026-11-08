package com.github.tvbox.osc.callback;

import com.github.tvbox.osc.R;
import com.kingja.loadsir.callback.Callback;

/**
 * 直播页专属空状态：没有可用直播频道时在页面内展示（带「切换直播源」入口），
 * 替代原来直接弹 XPopup 的做法。
 */
public class LiveEmptyCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.live_empty_layout;
    }
}
