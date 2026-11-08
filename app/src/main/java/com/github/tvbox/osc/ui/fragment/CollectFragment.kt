package com.github.tvbox.osc.ui.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.base.BaseVbFragment
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.databinding.FragmentCollectBinding
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.FastSearchActivity
import com.github.tvbox.osc.ui.activity.FavoritesActivity
import com.github.tvbox.osc.ui.adapter.CollectAdapter
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.Utils
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 「收藏」分栏。逻辑由原 CollectActivity 搬过来，只把标题栏和清空按钮交给宿主
 * [FavoritesActivity]（一个标题栏要同时服务两个分栏）。
 */
class CollectFragment : BaseVbFragment<FragmentCollectBinding>() {

    private val collectAdapter = CollectAdapter()

    override fun init() {
        setLoadSir(mBinding.mGridView)

        mBinding.mGridView.setHasFixedSize(true)
        // 列数按屏宽自适应：横版 16:9 海报在手机竖屏只排 1 列，竖版 2:3 至少 2 列
        mBinding.mGridView.layoutManager =
            GridLayoutManager(mContext!!, Utils.getPosterSpanCount(mContext!!))
        mBinding.mGridView.adapter = collectAdapter

        collectAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { _: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                FastClickCheckUtil.check(view)
                val vodInfo = collectAdapter.data.getOrNull(position)
                if (vodInfo != null) {
                    collectAdapter.remove(position)
                    RoomDataManger.deleteVodCollect(vodInfo.id)
                    if (collectAdapter.data.isEmpty()) {
                        mBinding.topTip.visibility = View.GONE
                    }
                    notifyCount()
                }
                true
            }

        collectAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { _, view, position ->
                FastClickCheckUtil.check(view)
                val vodInfo = collectAdapter.data.getOrNull(position)
                if (vodInfo != null) {
                    if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                        val bundle = Bundle()
                        bundle.putString("id", vodInfo.vodId)
                        bundle.putString("sourceKey", vodInfo.sourceKey)
                        jumpActivity(DetailActivity::class.java, bundle)
                    } else {
                        // 收藏时用的源已不在当前订阅里，按片名去搜索
                        val newIntent = Intent(mContext, FastSearchActivity::class.java)
                        newIntent.putExtra("title", vodInfo.name)
                        newIntent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                        startActivity(newIntent)
                    }
                }
            }

        initData()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 清单声明了 configChanges，旋转不重建页面，需手动更新列数
        (mBinding.mGridView.layoutManager as? GridLayoutManager)?.spanCount =
            Utils.getPosterSpanCount(mContext!!)
    }

    /** 宿主标题栏右侧的清空按钮会调这里 */
    fun confirmClear() {
        if (collectAdapter.data.isEmpty()) return
        XPopup.Builder(mActivity!!)
            .isDarkTheme(Utils.isDarkTheme())
            .asConfirm("提示", "确定清空收藏?") {
                (mActivity as? BaseActivity)?.showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {
                    RoomDataManger.deleteVodCollectAll()
                    withContext(Dispatchers.Main) {
                        (mActivity as? BaseActivity)?.dismissLoadingDialog()
                        collectAdapter.setNewData(ArrayList())
                        mBinding.topTip.visibility = View.GONE
                        showEmpty()
                        notifyCount()
                    }
                }
            }.show()
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allVodRecord = RoomDataManger.getAllVodCollect()
            val vodInfoList: MutableList<VodCollect> = ArrayList()
            for (vodInfo in allVodRecord) {
                vodInfoList.add(vodInfo)
            }
            withContext(Dispatchers.Main) {
                collectAdapter.setNewData(vodInfoList)
                notifyCount()
                if (vodInfoList.isNotEmpty()) {
                    showSuccess()
                    mBinding.topTip.visibility = View.VISIBLE
                } else {
                    showEmpty()
                }
            }
        }
    }

    private fun notifyCount() {
        (mActivity as? FavoritesActivity)?.onCountChanged(
            FavoritesActivity.TAB_COLLECT,
            collectAdapter.data.size
        )
    }
}
