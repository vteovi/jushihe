package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.angcyo.tablayout.delegate.ViewPager1Delegate.Companion.install
import com.blankj.utilcode.util.ConvertUtils
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.databinding.ActivityFavoritesBinding
import com.github.tvbox.osc.ui.fragment.CollectFragment
import com.github.tvbox.osc.ui.fragment.HistoryFragment

/**
 * 收藏 + 历史记录合并页。
 *
 * 以前「我的」里有「观看历史」「我的收藏」两个入口、两个独立 Activity；
 * 现在合成一页，顶部两个分栏（收藏 / 历史记录），左右滑动或点分栏切换。
 * 两个入口都跳这里，用 [EXTRA_TAB] 决定默认落在哪个分栏。
 *
 * 标题栏右侧的清空按钮按当前分栏生效，具体删除逻辑在各自的 Fragment 里。
 */
class FavoritesActivity : BaseVbActivity<ActivityFavoritesBinding>() {

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_COLLECT = 0
        const val TAB_HISTORY = 1

        /** Java 侧（MyFragment）跳转用，省得每处都手写 Bundle */
        @JvmStatic
        fun tabBundle(tab: Int): Bundle = Bundle().apply { putInt(EXTRA_TAB, tab) }
    }

    private var collectCount = 0
    private var historyCount = 0

    override fun init() {
        mBinding.tabLayout.addView(getTabTextView("收藏"))
        mBinding.tabLayout.addView(getTabTextView("历史记录"))

        mBinding.viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment =
                if (position == TAB_HISTORY) HistoryFragment() else CollectFragment()

            override fun getCount(): Int = 2
        }
        // 两个分栏都常驻，来回滑动不重建列表
        mBinding.viewPager.offscreenPageLimit = 1
        // tab 与 ViewPager 双向联动：点 tab 切页，滑动时 tab 指示器跟着走
        install(mBinding.viewPager, mBinding.tabLayout, true)

        // 入口决定默认分栏：从「观看历史」进来直接落在历史分栏
        mBinding.viewPager.currentItem =
            intent.getIntExtra(EXTRA_TAB, TAB_COLLECT).coerceIn(TAB_COLLECT, TAB_HISTORY)

        mBinding.titleBar.rightView.setOnClickListener { clearCurrentTab() }
        mBinding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                syncTitle()
            }
        })
        syncTitle()
    }

    /** Fragment 数据变化后回调，用来刷新标题上的数量 */
    fun onCountChanged(tab: Int, count: Int) {
        if (tab == TAB_COLLECT) collectCount = count else historyCount = count
        syncTitle()
    }

    /** 标题跟随分栏显示「收藏（12）」/「历史记录（36）」，数量为 0 时只显示名称 */
    private fun syncTitle() {
        val isCollect = mBinding.viewPager.currentItem == TAB_COLLECT
        val count = if (isCollect) collectCount else historyCount
        val name = if (isCollect) "收藏" else "历史记录"
        mBinding.titleBar.setTitle(if (count > 0) "$name（$count）" else name)
    }

    /**
     * ViewPager 里取当前 Fragment 用 tag 查，而不是持有字段引用：
     * 页面被系统回收重建时 FragmentPagerAdapter 会复用恢复出来的实例，字段引用会失效。
     */
    private fun currentFragment(): Fragment? = supportFragmentManager.findFragmentByTag(
        "android:switcher:${mBinding.viewPager.id}:${mBinding.viewPager.currentItem}"
    )

    private fun clearCurrentTab() {
        when (val fragment = currentFragment()) {
            is CollectFragment -> fragment.confirmClear()
            is HistoryFragment -> fragment.confirmClear()
        }
    }

    /** 分栏文字样式与首页 tab 保持一致 */
    private fun getTabTextView(text: String): TextView {
        val textView = TextView(mContext)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setPadding(
            ConvertUtils.dp2px(20f),
            ConvertUtils.dp2px(10f),
            ConvertUtils.dp2px(5f),
            ConvertUtils.dp2px(10f)
        )
        return textView
    }
}
