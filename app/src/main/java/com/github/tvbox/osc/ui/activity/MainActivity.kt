package com.github.tvbox.osc.ui.activity

import android.content.res.Configuration
import android.os.Process
import android.view.MenuItem
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.databinding.ActivityMainBinding
import com.github.tvbox.osc.ui.fragment.GridFragment
import com.github.tvbox.osc.ui.fragment.HomeFragment
import com.github.tvbox.osc.ui.fragment.MyFragment
import com.google.android.material.navigation.NavigationBarView
import kotlin.system.exitProcess

class MainActivity : BaseVbActivity<ActivityMainBinding>() {

    var fragments = listOf(HomeFragment(), MyFragment())
    var useCacheConfig = false
    private var exitTime = 0L

    /**
     * 导航栏与内容区统一用 NavigationBarView / ViewPager 获取：
     * 竖屏是底部 BottomNavigationView，平板（layout-sw600dp）是左侧 NavigationRailView，
     * 两者都继承 NavigationBarView，用基类接住即可，避免 ViewBinding 跨布局变体的类型差异。
     */
    private lateinit var mNavigationBar: NavigationBarView
    private lateinit var mViewPager: ViewPager

    override fun init() {

        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false) ?: false

        mViewPager = findViewById(R.id.vp)
        // 竖屏是底部 BottomNavigationView(bottom_nav)，平板是左侧 NavigationRailView(nav_rail)，
        // 两者都继承 NavigationBarView，用基类接住即可
        val bottomNav = findViewById<View?>(R.id.bottom_nav) as? NavigationBarView
        val navRail = findViewById<View?>(R.id.nav_rail) as? NavigationBarView
        mNavigationBar = bottomNav ?: navRail!!

        mViewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }

        mNavigationBar.setOnItemSelectedListener { menuItem: MenuItem ->
            mViewPager.setCurrentItem(menuItem.order, false)
            true
        }
        mViewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mNavigationBar.menu.getItem(position).isChecked = true
            }
        })

        applyNavRailStatusBarPadding()
    }

    /**
     * 平板侧边栏：背景/阴影允许一直铺到屏幕顶端（状态栏底下），
     * 但图标和文字不能压到状态栏上。用 statusBars inset 的真实高度重算 paddingTop，
     * 布局里写的 32dp 只是没拿到 inset 时的兜底值。
     * 手机布局没有 nav_rail 这个 id，会直接返回。
     */
    private fun applyNavRailStatusBarPadding() {
        val rail = findViewById<View>(R.id.nav_rail) ?: return
        val basePadding = rail.paddingTop
        val gap = (8 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(rail) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val target = maxOf(basePadding, statusTop + gap)
            if (target != view.paddingTop) {
                view.setPadding(view.paddingLeft, target, view.paddingRight, view.paddingBottom)
            }
            insets
        }
        ViewCompat.requestApplyInsets(rail)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 清单声明了 configChanges，横竖屏切换不会重建页面；状态栏高度可能变化，重新取一次 inset
        findViewById<View>(R.id.nav_rail)?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onBackPressed() {
        if (mViewPager.currentItem == 1) {
            mViewPager.currentItem = 0
            return
        }
        val homeFragment = fragments[0] as HomeFragment
        if (!homeFragment.isAdded) { // 资源不足销毁重建时未挂载到activity时getChildFragmentManager会崩溃
            confirmExit()
            return
        }
        val childFragments = homeFragment.allFragments
        if (childFragments.isEmpty()) { //加载中(没有tab)
            confirmExit()
            return
        }
        val fragment: Fragment = childFragments[homeFragment.tabIndex]
        if (fragment is GridFragment) { // 首页数据源动态加载的tab
            if (!fragment.restoreView()) { // 有回退的view,先回退(AList等文件夹列表),没有可回退的,返到主页tab
                if (!homeFragment.scrollToFirstTab()) {
                    confirmExit()
                }
            }
        } else {
            confirmExit()
        }
    }

    private fun confirmExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            ActivityUtils.finishAllActivities(true)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
}
