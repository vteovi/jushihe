package com.github.tvbox.osc.base;

import android.text.TextUtils;

import androidx.multidex.MultiDexApplication;

import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LiveEmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.Utils;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;

    private static P2PClass p;
    public static String burl;

    public boolean isNormalStart;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        isNormalStart = true;
        initParams();
        // OKGo
        OkGoHelper.init(); //台标获取
        EpgUtil.init();
        // 初始化Web服务器
        ControlManager.init(this);
        //初始化数据库
        AppDataManager.init();
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .addCallback(new LiveEmptyCallback())
                .commit();
        AutoSizeConfig.getInstance()
                .setExcludeFontScale(true)
                .setCustomFragment(true)
                .getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();
        QuickJSLoader.init();
        FileUtils.cleanPlayerCache();
        initCrashConfig();
        Utils.initTheme();
    }

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        putDefault(HawkConfig.HOME_REC, 0);                  //推荐: 0=豆瓣热播, 1=站点推荐
        putDefault(HawkConfig.PLAY_TYPE, 2);                 //播放器: 0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码");           //IJK解码: 软解码, 硬解码
        putDefault(HawkConfig.BACKGROUND_PLAY_TYPE,2);           //后台播放: 0 关闭,1 开启,2 画中画
        putDefault(HawkConfig.DOH_URL, 0);                   //安全DNS: 0=关闭, 1=腾讯, 2=阿里, 3=360, 4=Google, 5=AdGuard, 6=Quad9
        putDefault(HawkConfig.PLAY_SCALE, 0);                //画面缩放: 0=默认, 1=16:9, 2=4:3, 3=填充, 4=原始, 5=裁剪
        putDefault(HawkConfig.HISTORY_NUM, 2);                //历史记录数量: 0=30, 1=50, 2=70
        putDefaultApi();
    }

    /**
     * 内置默认订阅源版本：增删 strings.xml 里的 api 数组后 +1。
     * 老用户升级后只补一次新增的默认源（按 URL 去重），之后不再回填，
     * 避免用户手动删掉的源每次启动又冒出来。
     */
    private static final int BUILTIN_SUBS_VERSION = 2;

    /**
     * v1.16.0 之前的默认源（龙门影视）。
     * 默认源改成饭太硬后，只有"还停在这个旧默认源上"的用户会被切到新默认源，
     * 自己手动选过其它源的不动。
     */
    private static final String LEGACY_DEFAULT_API = "http://box.iqinu.com";

    private void putDefaultApi() {
        String[] apis = getResources().getStringArray(R.array.api);
        String[] names = getResources().getStringArray(R.array.api_names);
        if (apis.length == 0 || TextUtils.isEmpty(apis[0])) return;

        // 首次安装（还没有订阅列表）：直接按默认列表生成
        if (!Hawk.contains(HawkConfig.SUBSCRIPTIONS)) {
            List<Subscription> subscriptions = new ArrayList<>();
            for (int i = 0; i < apis.length; i++) {
                if (TextUtils.isEmpty(apis[i])) continue;
                Subscription subscription = new Subscription(subName(names, i, subscriptions.size()), apis[i]);
                if (subscriptions.isEmpty() && TextUtils.isEmpty(Hawk.get(HawkConfig.API_URL, ""))) {
                    subscription.setChecked(true);
                    Hawk.put(HawkConfig.API_URL, apis[i]);
                }
                subscriptions.add(subscription);
            }
            Hawk.put(HawkConfig.SUBSCRIPTIONS, subscriptions);
            Hawk.put(HawkConfig.DEFAULT_SUBS_VERSION, BUILTIN_SUBS_VERSION);
            return;
        }

        // 老用户升级：补一次新增的默认源
        int applied = Hawk.get(HawkConfig.DEFAULT_SUBS_VERSION, 0);
        if (applied >= BUILTIN_SUBS_VERSION) return;
        List<Subscription> subscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<Subscription>());
        if (subscriptions == null) {
            subscriptions = new ArrayList<>();
        }
        Set<String> existUrls = new HashSet<>();
        for (Subscription item : subscriptions) {
            if (item != null && item.getUrl() != null) {
                existUrls.add(item.getUrl());
            }
        }
        boolean changed = false;
        for (int i = 0; i < apis.length; i++) {
            if (TextUtils.isEmpty(apis[i]) || existUrls.contains(apis[i])) continue;
            subscriptions.add(new Subscription(subName(names, i, subscriptions.size()), apis[i]));
            existUrls.add(apis[i]);
            changed = true;
        }
        // 之前一个源都没选中（比如被删光了），补完后默认用第一个
        if (TextUtils.isEmpty(Hawk.get(HawkConfig.API_URL, "")) && !subscriptions.isEmpty()) {
            for (Subscription item : subscriptions) {
                item.setChecked(false);
            }
            subscriptions.get(0).setChecked(true);
            Hawk.put(HawkConfig.API_URL, subscriptions.get(0).getUrl());
            changed = true;
        }
        // 默认源换成饭太硬：只切换"仍停在旧内置默认源"的用户
        String currentApi = Hawk.get(HawkConfig.API_URL, "");
        if (LEGACY_DEFAULT_API.equals(currentApi)) {
            for (Subscription item : subscriptions) {
                if (item == null || !apis[0].equals(item.getUrl())) continue;
                for (Subscription other : subscriptions) {
                    if (other != null) {
                        other.setChecked(false);
                    }
                }
                item.setChecked(true);
                Hawk.put(HawkConfig.API_URL, apis[0]);
                changed = true;
                break;
            }
        }
        if (changed) {
            Hawk.put(HawkConfig.SUBSCRIPTIONS, subscriptions);
        }
        Hawk.put(HawkConfig.DEFAULT_SUBS_VERSION, BUILTIN_SUBS_VERSION);
    }

    /**
     * 取订阅显示名称：优先用 api_names 对应的名字，缺失时退回「订阅: N」
     */
    private String subName(String[] names, int index, int fallbackIndex) {
        if (names != null && index < names.length && !TextUtils.isEmpty(names[index])) {
            return names[index];
        }
        return "订阅: " + (fallbackIndex + 1);
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.load();
    }

    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }


    private VodInfo vodInfo;
    public void setVodInfo(VodInfo vodinfo){
        this.vodInfo = vodinfo;
    }
    public VodInfo getVodInfo(){
        return this.vodInfo;
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(instance.getExternalCacheDir().getAbsolutePath());
            }
            return p;
        } catch (Exception e) {
            LOG.e(e.toString());
            return null;
        }
    }

    private void initCrashConfig(){
        //配置全局异常崩溃操作
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //背景模式,开启沉浸式
                .enabled(true) //是否启动全局异常捕获
                .showErrorDetails(true) //是否显示错误详细信息
                .showRestartButton(true) //是否显示重启按钮
                .trackActivities(true) //是否跟踪Activity
                .minTimeBetweenCrashesMs(2000) //崩溃的间隔时间(毫秒)
                .errorDrawable(R.drawable.app_icon) //错误图标
                .restartActivity(MainActivity.class) //重新启动后的activity
                .apply();
    }

}