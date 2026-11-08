package com.github.tvbox.osc.ui.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.base.BaseVbFragment
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.databinding.FragmentHistoryBinding
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.FavoritesActivity
import com.github.tvbox.osc.ui.adapter.HistoryAdapter
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.Utils
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 「历史记录」分栏。逻辑由原 HistoryActivity 搬过来，标题栏与清空按钮由宿主
 * [FavoritesActivity] 统一处理。
 */
class HistoryFragment : BaseVbFragment<FragmentHistoryBinding>() {

    private val historyAdapter = HistoryAdapter()

    override fun init() {
        setLoadSir(mBinding.mGridView)

        mBinding.mGridView.setHasFixedSize(true)
        // 列数按屏宽自适应：横版 16:9 海报在手机竖屏只排 1 列，竖版 2:3 至少 2 列
        mBinding.mGridView.layoutManager =
            GridLayoutManager(mContext!!, Utils.getPosterSpanCount(mContext!!))
        mBinding.mGridView.adapter = historyAdapter

        historyAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { _: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                FastClickCheckUtil.check(view)
                val vodInfo = historyAdapter.data.getOrNull(position)
                if (vodInfo != null) {
                    historyAdapter.remove(position)
                    RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo)
                    if (historyAdapter.data.isEmpty()) {
                        mBinding.topTip.visibility = View.GONE
                    }
                    notifyCount()
                }
                true
            }

        historyAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { _, view, position ->
                FastClickCheckUtil.check(view)
                val vodInfo = historyAdapter.data.getOrNull(position)
                if (vodInfo != null) {
                    val bundle = Bundle()
                    bundle.putString("id", vodInfo.id)
                    bundle.putString("sourceKey", vodInfo.sourceKey)
                    jumpActivity(DetailActivity::class.java, bundle)
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
        if (historyAdapter.data.isEmpty()) return
        XPopup.Builder(mActivity!!)
            .isDarkTheme(Utils.isDarkTheme())
            .asConfirm("提示", "确定清空历史记录?") {
                (mActivity as? BaseActivity)?.showLoadingDialog()
                lifecycleScope.launch(Dispatchers.IO) {
                    RoomDataManger.deleteVodRecordAll()
                    withContext(Dispatchers.Main) {
                        (mActivity as? BaseActivity)?.dismissLoadingDialog()
                        historyAdapter.setNewData(ArrayList())
                        mBinding.topTip.visibility = View.GONE
                        showEmpty()
                        notifyCount()
                    }
                }
            }.show()
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 与原历史记录页一致，只取最近 100 条
            val allVodRecord = RoomDataManger.getAllVodRecord(100)
            val vodInfoList: MutableList<VodInfo> = ArrayList()
            for (vodInfo in allVodRecord) {
                if (vodInfo.playNote != null && vodInfo.playNote.isNotEmpty()) {
                    vodInfo.note = vodInfo.playNote
                }
                vodInfoList.add(vodInfo)
            }
            withContext(Dispatchers.Main) {
                historyAdapter.setNewData(vodInfoList)
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
            FavoritesActivity.TAB_HISTORY,
            historyAdapter.data.size
        )
    }
}
