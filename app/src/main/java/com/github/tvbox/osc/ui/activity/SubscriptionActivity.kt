package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.Source
import com.github.tvbox.osc.bean.Subscription
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter
import com.github.tvbox.osc.ui.dialog.ChooseSourceDialog
import com.github.tvbox.osc.ui.dialog.SubsTipDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog.OnSubsciptionListener
import com.github.tvbox.osc.util.HawkConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lxj.xpopup.XPopup
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import java.io.File
import java.util.function.Consumer

class SubscriptionActivity : BaseVbActivity<ActivitySubscriptionBinding>() {

    private var mBeforeUrl = Hawk.get(HawkConfig.API_URL, "")
    private var mSelectedUrl = ""
    private var mSubscriptions: MutableList<Subscription> = Hawk.get(HawkConfig.SUBSCRIPTIONS, ArrayList())
    private var mSubscriptionAdapter = SubscriptionAdapter()
    private val mSources: MutableList<Source> = ArrayList()
    private val PICK_FILE_REQUEST = 10086
    private var mCheckedWhenPick = false

    override fun init() {

        mBinding.rv.setAdapter(mSubscriptionAdapter)
        mSubscriptions.forEach(Consumer { item: Subscription ->
            if (item.isChecked) {
                mSelectedUrl = item.url
            }
        })

        mSubscriptionAdapter.setNewData(mSubscriptions)
        mBinding.ivUseTip.setOnClickListener {
            XPopup.Builder(this)
                .asCustom(SubsTipDialog(this))
                .show()
        }

        mBinding.titleBar.rightView.setOnClickListener {//添加订阅
            XPopup.Builder(this)
                .autoFocusEditText(false)
                .asCustom(
                    SubsciptionDialog(
                        this,
                        "订阅: " + (mSubscriptions.size + 1),
                        object : OnSubsciptionListener {
                            override fun onConfirm(
                                name: String,
                                url: String,
                                checked: Boolean
                            ) { //只有addSub2List用到,看注释,单线路才生效,其余方法仅作为参数继续传递
                                for (item in mSubscriptions) {
                                    if (item.url == url) {
                                        ToastUtils.showLong("订阅地址与" + item.name + "相同")
                                        return
                                    }
                                }
                                addSubscription(name, url, checked)
                            }

                            override fun chooseLocal(checked: Boolean) { //本地导入:使用系统文件管理器选择文件
                                openSystemFilePicker(checked)
                            }
                        })
                ).show()
        }

        mSubscriptionAdapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            LogUtils.d("删除订阅")
            if (view.id == R.id.iv_del) {
                if (mSubscriptions.get(position).isChecked) {
                    ToastUtils.showShort("不能删除当前使用的订阅")
                    return@setOnItemChildClickListener
                }
                XPopup.Builder(this@SubscriptionActivity)
                    .asConfirm("删除订阅", "确定删除订阅吗？") {
                        mSubscriptions.removeAt(position)
                        //删除/选择只刷新,不触发重新排序
                        mSubscriptionAdapter.notifyDataSetChanged()
                    }.show()
            }
        }

        mSubscriptionAdapter.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, _: View?, position: Int ->  //选择订阅
            for (i in mSubscriptions.indices) {
                val subscription = mSubscriptions[i]
                if (i == position) {
                    subscription.setChecked(true)
                    mSelectedUrl = subscription.url
                } else {
                    subscription.setChecked(false)
                }
            }
            //删除/选择只刷新,不触发重新排序
            mSubscriptionAdapter.notifyDataSetChanged()
        }

        mSubscriptionAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View, position: Int ->
                val item = mSubscriptions[position]
                XPopup.Builder(this)
                    .atView(view.findViewById(R.id.tv_name))
                    .hasShadowBg(false)
                    .asAttachList(
                        arrayOf(
                            if (item.isTop) "取消置顶" else "置顶",
                            "重命名",
                            "复制地址"
                        ), null
                    ) { index: Int, _: String? ->
                        when (index) {
                            0 -> {
                                item.isTop = !item.isTop
                                mSubscriptions[position] = item
                                mSubscriptionAdapter.setNewData(mSubscriptions)
                            }
                            1 -> {
                                XPopup.Builder(this)
                                    .asInputConfirm(
                                        "更改为",
                                        "",
                                        item.name,
                                        "新的订阅名",
                                        { text ->
                                            if (!TextUtils.isEmpty(text)) {
                                                if (text.trim { it <= ' ' }.length > 8) {
                                                    ToastUtils.showShort("不要过长,不方便记忆")
                                                } else {
                                                    item.name = text.trim { it <= ' ' }
                                                    mSubscriptionAdapter.notifyItemChanged(position)
                                                }
                                            }
                                        },
                                        null,
                                        R.layout.dialog_input
                                    ).show()
                            }
                            2 -> {
                                ClipboardUtils.copyText(mSubscriptions.get(position).url)
                                ToastUtils.showLong("已复制")
                            }
                        }
                    }.show()
                true
            }
    }

    /**
     * 使用系统文件管理器选择订阅文件（SAF，无需任何存储权限）
     */
    private fun openSystemFilePicker(checked: Boolean) {
        mCheckedWhenPick = checked
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/json", "text/plain", "application/octet-stream")
            )
        }
        try {
            startActivityForResult(intent, PICK_FILE_REQUEST)
        } catch (e: Exception) {
            LogUtils.e(e)
            ToastUtils.showShort("无法打开系统文件管理器")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            handlePickedFile(uri)
        }
    }

    /**
     * 处理系统文件管理器选中的文件：
     * 1. 能解析出真实路径（在手机内置存储下）→ 直接引用原文件（clan:// 协议，App 内置服务动态读取）
     * 2. 解析不出路径（云盘/虚拟文件等）→ 读取内容并复制到 App 自己的外部目录，再以 clan:// 引用
     */
    private fun handlePickedFile(uri: Uri) {
        try {
            val name = getDisplayName(uri)
            val realPath = resolveRealPath(uri)
            val clanPath = if (!TextUtils.isEmpty(realPath) && realPath.startsWith(
                    Environment.getExternalStorageDirectory().getAbsolutePath()
                )
            ) {
                realPath.replace(
                    Environment.getExternalStorageDirectory().getAbsolutePath(),
                    "clan://localhost"
                )
            } else {
                // 拿不到真实路径：读取内容，复制到 App 自己的外部目录（无需存储权限）
                val content = readUriText(uri)
                if (content.isNullOrBlank()) {
                    ToastUtils.showShort("读取文件失败或内容为空")
                    return
                }
                copyToAppExternalDir(name, content) ?: run {
                    ToastUtils.showShort("保存订阅文件失败")
                    return
                }
            }
            for (item in mSubscriptions) {
                if (item.url == clanPath) {
                    ToastUtils.showLong("订阅地址与" + item.name + "相同")
                    return
                }
            }
            addSubscription(name, clanPath, mCheckedWhenPick)
        } catch (e: Exception) {
            LogUtils.e(e)
            ToastUtils.showShort("导入失败:" + e.message)
        }
    }

    /** 解析系统 Uri 对应的真实文件路径，解析不出返回空串 */
    private fun resolveRealPath(uri: Uri): String {
        if ("file" == uri.scheme) {
            return uri.path ?: ""
        }
        if ("content" != uri.scheme) {
            return ""
        }
        // 1) 直接查 _data 列（部分文件管理器仍会返回真实路径）
        val data = queryColumn(uri, "_data")
        if (!TextUtils.isEmpty(data)) {
            return data!!
        }
        // 2) DocumentsUI 的 document id：primary:Download/x.json
        try {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("primary:")) {
                return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                        docId.substring("primary:".length)
            }
        } catch (ignored: Throwable) {
        }
        return ""
    }

    private fun queryColumn(uri: Uri, column: String): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(column), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(column)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    /** 读取 Uri 指向文件的全部文本内容 */
    private fun readUriText(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            null
        }
    }

    /** 获取文件显示名（无真实路径时使用） */
    private fun getDisplayName(uri: Uri): String {
        var name = queryColumn(uri, OpenableColumns.DISPLAY_NAME)
        if (TextUtils.isEmpty(name)) {
            name = uri.lastPathSegment
        }
        if (TextUtils.isEmpty(name)) {
            name = "本地订阅.json"
        }
        val safe = name!!.substringAfterLast('/').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (safe.endsWith(".json", true) || safe.endsWith(".txt", true)) safe else "$safe.json"
    }

    /** 把内容复制到 App 外部私有目录 files/subscriptions 下，返回 clan:// 路径；失败返回 null */
    private fun copyToAppExternalDir(name: String, content: String): String? {
        return try {
            val base = File(
                Environment.getExternalStorageDirectory(),
                "Android/data/$packageName/files/subscriptions"
            )
            if (!base.exists()) {
                base.mkdirs()
            }
            var target = File(base, name)
            var index = 1
            while (target.exists()) {
                val dot = name.lastIndexOf('.')
                val prefix = if (dot > 0) name.substring(0, dot) else name
                val suffix = if (dot > 0) name.substring(dot) else ""
                target = File(base, "$prefix($index)$suffix")
                index++
            }
            target.writeText(content)
            target.absolutePath.replace(
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                "clan://localhost"
            )
        } catch (e: Exception) {
            LogUtils.e(e)
            null
        }
    }

    private fun addSubscription(name: String, url: String, checked: Boolean) {
        if (url.startsWith("clan://")) {
            addSub2List(name, url, checked)
            mSubscriptionAdapter.setNewData(mSubscriptions)
        } else if (url.startsWith("http")) {
            showLoadingDialog()
            OkGo.get<String>(url)
                .tag("get_subscription")
                .execute(object : AbsCallback<String?>() {
                    override fun onSuccess(response: Response<String?>) {
                        dismissLoadingDialog()
                        try {
                            val json = JsonParser.parseString(response.body()).asJsonObject
                            // 多线路?
                            val urls = json["urls"]
                            // 多仓?
                            val storeHouse = json["storeHouse"]
                            if (urls != null && urls.isJsonArray) { // 多线路
                                if (checked) {
                                    ToastUtils.showLong("多条线路请主动选择")
                                }
                                val urlList = urls.asJsonArray
                                if (urlList != null && urlList.size() > 0 && urlList[0].isJsonObject
                                    && urlList[0].asJsonObject.has("url")
                                    && urlList[0].asJsonObject.has("name")
                                ) { //多线路格式
                                    for (i in 0 until urlList.size()) {
                                        val obj = urlList[i] as JsonObject
                                        val name = obj["name"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["url"].asString.trim { it <= ' ' }
                                        mSubscriptions.add(Subscription(name, url))
                                    }
                                }
                            } else if (storeHouse != null && storeHouse.isJsonArray) { // 多仓
                                val storeHouseList = storeHouse.asJsonArray
                                if (storeHouseList != null && storeHouseList.size() > 0 && storeHouseList[0].isJsonObject
                                    && storeHouseList[0].asJsonObject.has("sourceName")
                                    && storeHouseList[0].asJsonObject.has("sourceUrl")
                                ) { //多仓格式
                                    mSources.clear()
                                    for (i in 0 until storeHouseList.size()) {
                                        val obj = storeHouseList[i] as JsonObject
                                        val name = obj["sourceName"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["sourceUrl"].asString.trim { it <= ' ' }
                                        mSources.add(Source(name, url))
                                    }
                                    XPopup.Builder(this@SubscriptionActivity)
                                        .asCustom(
                                            ChooseSourceDialog(
                                                this@SubscriptionActivity,
                                                mSources
                                            ) { position: Int, _: String? ->
                                                // 再根据多线路格式获取配置,如果仓内是正常多线路模式,name没用,直接使用线路的命名
                                                addSubscription(
                                                    mSources[position].sourceName,
                                                    mSources[position].sourceUrl,
                                                    checked
                                                )
                                            })
                                        .show()
                                }
                            } else { // 单线路/其余
                                addSub2List(name, url, checked)
                            }
                        } catch (th: Throwable) {
                            addSub2List(name, url, checked)
                        }
                        mSubscriptionAdapter.setNewData(mSubscriptions)
                    }

                    @Throws(Throwable::class)
                    override fun convertResponse(response: okhttp3.Response): String {
                        return response.body()!!.string()
                    }

                    override fun onError(response: Response<String?>) {
                        super.onError(response)
                        dismissLoadingDialog()
                        ToastUtils.showLong("订阅失败,请检查地址或网络状态")
                    }
                })
        } else {
            ToastUtils.showShort("订阅格式不正确")
        }
    }

    /**
     * 仅当选中本地文件和添加的为单线路时,使用此订阅生效。多线路会直接解析全部并添加,多仓会展开并选择,最后也按多线路处理,直接添加
     * @param name
     * @param url
     * @param checkNewest
     */
    private fun addSub2List(name: String, url: String, checkNewest: Boolean) {
        if (checkNewest) { //选中最新的,清除以前的选中订阅
            for (subscription in mSubscriptions) {
                if (subscription.isChecked) {
                    subscription.setChecked(false)
                }
            }
            mSelectedUrl = url
            mSubscriptions.add(Subscription(name, url).setChecked(true))
        } else {
            mSubscriptions.add(Subscription(name, url).setChecked(false))
        }
    }

    override fun onPause() {
        super.onPause()
        // 更新缓存
        Hawk.put(HawkConfig.API_URL, mSelectedUrl)
        Hawk.put<List<Subscription>?>(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
    }

    override fun finish() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && mBeforeUrl != mSelectedUrl) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        OkGo.getInstance().cancelTag("get_subscription")
    }
}