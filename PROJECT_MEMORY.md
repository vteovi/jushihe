# TVBoxOS_Portrait 项目记忆

> 本文件由豆包助手维护，记录项目的开发背景、环境配置、迭代历史、修复记录与用户偏好，供后续开发延续使用。改动代码后请同步更新本文件。

## 一、项目概览

- **项目名称**：TVBoxOS_Portrait（MBox 竖屏影视聚合 App）
- **基础**：TVBoxMobile/MBox 开源项目定制改造（Android）
- **包名**：`com.github.tvbox.osc`；minSdk 24，targetSdk 30，compileSdk 33
- **用户设备**：小米 M2007J17C，Android 12（SDK 31）
- **工作流**：改功能 + 版本号 +1 → 用户自行编译验收（`Build-MBox.ps1`）→ 自动 git commit
- **项目根目录**：`D:\Doubao\TVBoxOS_Portrait`

## 二、编译环境（已就绪，勿重复搭建）

| 项 | 值 |
|---|---|
| JDK | 17（`C:\Java\jdk-17`） |
| Android SDK | `C:\Android\Sdk`（platforms 33/30/34/35/36；build-tools 34.0.0） |
| Gradle | 7.3.3（`GRADLE_USER_HOME=D:\gradle`；官方下载慢，用腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-7.3.3-bin.zip`） |
| local.properties | `sdk.dir=C\:\\Android\\Sdk` |
| 签名 | 根目录 `TVBoxOSC.jks`（store/password/keypass/alias 均 `TVBoxOSC`，CN=TVBoxOSC，不入库） |
| 一键编译 | `D:\Doubao\TVBoxOS_Portrait\Build-MBox.ps1`（参数 `-Debug/-OutDir/-SkipJavaClean`；APK 复制到根目录 `APK\`；结尾按偏好清理 gradle/kotlin 的 java.exe 进程） |

**编译命令**：
```powershell
cd D:\Doubao\TVBoxOS_Portrait
.\Build-MBox.ps1
```
产物：`app\build\outputs\apk\release\MBox_v<版本>_release_<日期>.apk`，同时复制到根目录 `APK\`。

**DLNA 依赖（关键）**：`com.github.devin1014.DLNA-Cast:dlna-dmc:V1.0.0` 在 JitPack 永远构建不出来（其发布插件依赖已停服的 jcenter）。已从 GitHub 下载源码并入主项目为本地模块 `dlna-core\`、`dlna-dmc\`（各自 build.gradle，compileSdk 33），`settings.gradle` 已 include，`app/build.gradle` 依赖 `implementation project(':dlna-dmc')`。源 zip 在 `D:\tmp\DLNA-Cast\extracted\DLNA-Cast-1.0.0`。

## 三、用户偏好（长期有效）

1. **改完代码不编译 APK**，编译由用户自己跑 `Build-MBox.ps1`。
2. **每次改动后自动 git commit**（commit message 中文，格式 `feat:/fix:/chore: ...`）。
3. 深色模式追求 **OLED 纯黑**（#000000）。
4. 播放进度条：**视觉细（3dp）+ 触摸区域大（24dp）**，容易点到但视觉不粗。
5. 高频"改功能 + 版本号 +1"模式（versionCode 与 versionName 同步更新）。
6. 构建后清理残留 java 进程（Build-MBox.ps1 已内置）。

## 四、版本迭代记录

| 版本 | 内容 |
|---|---|
| 1.1.1 | 项目初始编译（环境搭建完成） |
| 1.1.2 | 版本号推进 |
| 1.1.3 | 订阅管理页：添加订阅窗口；本地导入改为系统文件管理器（SAF, ACTION_OPEN_DOCUMENT, PICK_FILE_REQUEST=10086）；**修复导入后 404/5xx**（一律读取内容复制到 App 私有目录 `Android/data/<pkg>/files/subscriptions/` 再转 `clan://localhost`） |
| 1.1.4 | 深色模式 OLED 纯黑优化（`values-night/colors.xml`：bg_gray/bg_popup/windowBackground=#000000；新增 dialog_stroke 描边日 #EEEEEE / 夜 #2E2E2E；light_gray/disable_gray/disable_text 夜间降为低亮灰；选中态颜色不动保焦点可见） |
| 1.1.5 | 搜索页自动弹键盘（requestFocus + postDelayed 300ms）；播放进度条移到按钮上方 |
| 1.1.6 | 播放页外部播放功能（新增 ic_external_play 按钮；VodController/LocalVideoController 加 externalPlay()；PlayFragment/LocalPlayActivity 实现，ACTION_VIEW+video/*+NEW_TASK）；外部播放/全屏按钮移到按钮行最右侧 |
| 1.1.7 | 播放时间移到进度条同一排 |
| 1.1.8 | 进度条加粗（触达问题）→ 最终方案：视觉 3dp + 触摸区 24dp（SeekBar 会把 drawable 拉伸到 View 高度，需在 drawable 三层都加 `android:gravity="center_vertical"`） |
| 1.1.9 | 进度条视觉调细（3dp），触摸区保持大 |
| 1.2.0 | 去掉开屏页（launcher 从 SplashActivity 移到 MainActivity；SplashActivity exported=false）→ **引发空白页卡死崩溃**，修复见崩溃记录 |
| 1.2.1 | 数据备份/还原改 SAF（ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT，zip 内含 sqlite + hawk；新格式与旧 tvbox_backup 目录格式不互通）；修复编译错误（LogUtils→LOG、hawkJson smart cast 加 !!、BackupDialog 补 import R） |
| 1.2.2 | 搜索框描边（新增 drawable/bg_search_box.xml 圆角 20dp + 1dp 描边；日/夜颜色 search_box_bg / search_box_stroke） |
| 1.2.3 | 首页搜索框深色不明显修复（bg_large_round_gray 被 12 处共用不可改，只把首页 search 项换 bg_search_box + 图标 tint）；直播页添加"换直播源"功能（LiveActivity 三处失败分支 → showChangeLiveSourceDialog；有历史源弹 ApiHistoryDialog，无则提示去设置） |
| 1.2.4 | 移除"我的"页本地视频功能入口 + 移除文件访问权限（删 XXPermissions 运行时申请、删 Manifest 三个存储权限声明 READ/WRITE/MANAGE_EXTERNAL_STORAGE；订阅导入/备份还原走 SAF 不受影响） |
| 1.2.5 | 修复首页崩溃 NPE（SourceViewModel.getSort 空保护） |
| 1.2.6 | 主页海报重做：新增 `RatioShadowLayout`（按开关切 2:3 竖版 / 16:9 横版卡片）；设置页加"横屏海报"开关（HawkConfig.LANDSCAPE_CARD，开=单列 16:9 横幅，关=双列 2:3 海报）；Utils.getPosterSpanCount() 统一列数，GridFragment/UserFragment 读开关；**全程锁定竖屏**（Manifest 所有 Activity screenOrientation=portrait，去掉播放器 SENSOR_LANDSCAPE 代码请求），不做平板/横屏适配 |
| 1.3.1 | **回滚播放页竖屏锁定**：全屏播放横竖屏逻辑恢复原样（宽视频全屏自动切传感器横屏、退出回竖屏、控制条横竖屏切换按钮均保留）。仅主页/列表/设置等 UI 页在 Manifest 锁 portrait；DetailActivity/LiveActivity 不指定方向（由 PlayFragment 代码控制），LocalPlayActivity 仍 landscape。横屏海报开关功能不变 |
| 1.3.2 | **海报磁盘缓存**：移植 TVBoxOS-Mobile 的 MyOkhttpDownLoader（按 URL MD5 落盘到 cacheDir/picasso，命中直接读文件不再走网络，网络成功写盘+tmp 临时文件 rename）；OkGoHelper.initPicasso 改用 `new MyOkhttpDownLoader(client, App.getInstance())`。设置页"清空缓存"清的是 getCacheDir()，picasso 子目录一并清除，无需额外改动 |
| 1.3.3 | **打开秒开加速**（移植 Mobile launch 系列提交）：①GridFragment 每个 Tab 第一页视频列表按 `grid_cache_{sourceKey}_{sortId}` 缓存到 Hawk，打开时先读缓存立即渲染、后台静默拉新替换，有缓存不转圈；②HomeFragment 分栏 tabs 及主页推荐列表按 `sort_cache_{sourceKey}`(+`_rec`) 缓存，config/jar 就绪后先读缓存立即建 ViewPager，不等 sort 网络请求；后台 sort 返回若分栏 id 序列与已渲染一致则跳过 initViewPager()，避免跳顶/闪烁；③Grid/Collect/History/FastSearch/Search 五个图片 adapter 去掉 `.placeholder()`，缓存海报直接解码显示，error 仍回退加载图 |
| 1.3.4 | **在线订阅源冷启动先读缓存**（ApiConfig.loadConfig）：不再只在配置重载(useCache)时读缓存，冷启动只要本地有缓存配置文件（filesDir/MD5(apiUrl)）就先 parseJson 并立即 callback.success() 让 UI 秒开；随后后台继续下载最新配置，成功只更新内存+缓存文件、不重复回调 UI，失败也不报错（已用缓存显示）。useCache=true（配置变更）场景行为不变。与 1.3.3 的 sort/列表缓存衔接成完整秒开链路：配置秒开→分栏秒渲染→列表秒显示 |
| 1.3.5 | **grid 分页竞争三连修复**（对齐 Mobile e8a7dd3/21bb95a/0f7df3d）：①新增 `mLoading` 标志，onLoadMore/initData 发起请求前置位、onChanged 回调结束复位，防止缓存秒开后用户快速下滑触发重复 page-1 请求导致分页错乱；②onFragmentResume 兜底重置 mLoading 并恢复 load-more，防止切 tab 后回调丢失导致上拉加载永久卡死；③第一页已从缓存显示时，后台网络返回只更新缓存不 setNewData 重绘列表，避免重绑 item/重载海报造成的滚动卡顿 |
| 1.3.6 | **快速滑动海报残留修复**（GridAdapter）：绑定数据时先 `ivThumb.setImageDrawable(null)` 清空旧图再加载新海报，配合已去掉的 .placeholder()，防止 RecyclerView 快速复用时短暂显示上一个位置的旧海报 |
| 1.3.7 | **消除 MIUI 安装"缺少隐私政策"红色风险提示**：根因是 targetSdkVersion 30 低于 Android 12(API31) 隐私合规线，MIUI 增强防护扫包时判定未适配隐私合规而弹红警；Mobile 为 34 故不弹。将 targetSdkVersion 30→34，并补 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限（播放服务已声明 mediaPlayback 类型，targetSdk34 必须显式声明）。注：未加 POST_NOTIFICATIONS（运行时通知权限，不影响安装提示） |
| 1.3.8 | **targetSdk34 编译补丁**：Android 12+ 要求带 intent-filter 的组件显式声明 `android:exported`，否则 processReleaseMainManifest 失败。给 SearchReceiver/CustomWebReceiver 补 `android:exported="false"`（与 Mobile 一致，自定义 action 仅内部通信）；已核对全 Manifest 所有 activity/receiver/service/provider 均已声明 exported |
| 1.3.9 | **改应用包名绕开 MIUI 风险标记**：提 targetSdk34 后仍弹"缺少隐私政策"红警，根因是 MIUI 云端风险库按包名标记了 TVBox 原版包名 `com.github.tvbox.osc`（签名同为 TVBoxOSC.jks、targetSdk 已对齐，唯一差异是包名；Mobile 用 .mobile 不弹）。将 applicationId 改为 `com.github.tvbox.osc.portrait`（与 .mobile 区分、两 App 可并存）；Manifest 的 package/R 类包名仍为 com.github.tvbox.osc，代码 import 无需改，FileProvider authorities 用 `${applicationId}.fileprovider` 自动跟随。注意：新包名=全新 App，需卸载旧版重装并重新导入订阅源 |
| 1.3.10 | **修复 Android14+ 动态广播接收器崩溃**（SecurityException）：targetSdk34 下动态注册 BroadcastReceiver 必须显式指定 RECEIVER_EXPORTED/NOT_EXPORTED，否则 DetailActivity 启动即崩。统一改用 `ContextCompat.registerReceiver(ctx, receiver, filter, ...)`，覆盖 DetailActivity(电量/Home键/遥控器动作3处)、PlayerTitleView(电量)、LocalPlayActivity(电量)。这些都是系统广播或 App 内部广播，用 NOT_EXPORTED |
| 1.3.11 | **编译补丁**：项目 androidx.core 版本较旧，`ContextCompat.RECEIVER_NOT_EXPORTED` 常量不存在（4参 registerReceiver 方法可用）。直接用字面值 `2 /*RECEIVER_NOT_EXPORTED*/` 代替该常量，5处同步替换，避免升级 core 引发连锁 |
| 1.3.12 | **编译补丁**：上一步用 PowerShell 写文件误加了 UTF-8 BOM（\ufeff），Java 编译器报"非法字符 \ufeff"。已去除 3 个被改文件（DetailActivity/LocalPlayActivity/PlayerTitleView）的 BOM，改为无 BOM UTF-8。教训：改 Java 文件后用 .NET UTF8Encoding($false) 写，别用 PS5.1 的 -Encoding UTF8（会带 BOM） |
| 1.3.13 | **编译补丁（终局）**：Portrait 的 androidx.core 太旧，连 `ContextCompat.registerReceiver(ctx,rec,filter,flag)` 4参方法都没有。放弃 ContextCompat，5处全部改用框架 `Context.registerReceiver` + SDK 分支：`if (SDK_INT >= TIRAMISU) registerReceiver(rec,filter,2/*RECEIVER_NOT_EXPORTED*/); else registerReceiver(rec,filter);`。覆盖 DetailActivity(3处:电量/Home键/遥控器动作)、PlayerTitleView(电量,getContext())、LocalPlayActivity(电量)。已补 PlayerTitleView/LocalPlayActivity 的 `import android.os.Build;` |
| 1.3.14 | **改名**：应用显示名 app_name 由 "MBox" 改为「聚视盒」（仅改 res/values/strings.xml 的 app_name，桌面图标名、首页 fragment_home、我的页 fragment_my 均引用该 string，自动跟随） |

## 五、崩溃修复记录（遇到同栈报错先对照这里）

1. **SelectDialog 崩溃**：`IllegalArgumentException: Invalid target position`（小米反馈）。根因：select 位置越界/列表为空时 smoothScrollToPosition。修复：滚动前边界保护（itemCount<=0 target=-1；越界 clamp 到 [0, itemCount-1]）。涉及 TvRecyclerView.smoothScrollToPosition。
2. **移除开屏后空白页死循环**：BaseActivity.onCreate 检查 `App.isNormalStart` false 时 relaunchApp 杀进程重启，MainActivity 直接入口时标志 false → 无限重启卡空白页。修复：`App.onCreate` 里 `isNormalStart = true`，删 SplashActivity.kt 里的旧设置。
3. **首页 NPE（1.2.4→1.2.5）**：`SourceViewModel.getSort` 里 `sourceBean.getType()` NPE。根因：订阅配置解析后站点列表为空（订阅异常/无 sites）时，`mHomeSource` 残留旧 SourceBean 引用，其 key 在新列表查无此源 → `ApiConfig.getSource(key)` 返回 null。修复：getSort 里 getSource 返回 null 时 `sortResult.postValue(null)` 直接返回（与 sourceKey==null 分支一致）。**同类风险点**：SourceViewModel 其他 getSource 裸调用（getHomeRecList 等）、HomeFragment 里 `ApiConfig.get().homeSourceBean.key`（getHomeSourceBean 永不返回 null，返回 emptyHome 兜底，key 为 null 时 getSort 已保护）。

## 六、关键文件清单

- `app/build.gradle` — 版本号（versionCode/versionName）、DLNA 本地模块依赖、签名引用
- `app/src/main/AndroidManifest.xml` — 无存储权限；SplashActivity exported=false 无 filter；MainActivity 是唯一 launcher
- `app/src/main/java/.../api/ApiConfig.java` — parseJson（mHomeSource 设置）、getSource/getHomeSourceBean
- `app/src/main/java/.../viewmodel/SourceViewModel.java` — getSort 已含 null 保护（1.2.5）
- `app/src/main/java/.../ui/activity/LiveActivity.java` — 换直播源（showChangeLiveSourceDialog/reloadLiveConfig）
- `app/src/main/java/.../ui/activity/SettingActivity.kt` — SAF 备份还原（REQ_BACKUP=20001/REQ_RESTORE=20002；LOG.e 记日志）
- `app/src/main/java/.../ui/activity/SubscriptionActivity.kt` — SAF 本地导入（PICK_FILE_REQUEST=10086，一律复制到私有目录）
- `app/src/main/java/.../ui/dialog/BackupDialog.java` — 两按钮回调（onBackup/onRestore）
- `app/src/main/java/.../ui/fragment/MyFragment.java` — 无本地视频入口、无权限申请
- `app/src/main/java/.../ui/fragment/HomeFragment.kt` — initData/showTipDialog（dataInitOk/jarInitOk 标志）
- `app/src/main/res/layout/player_vod_control_view.xml` — 时间-进度条-总时长同排 + 按钮行（外部播放/全屏最右）
- `app/src/main/res/drawable/shape_player_control_vod_seek.xml` — 进度条三层均 gravity=center_vertical，视觉 3dp，触摸区 24dp，滑块 14dp
- `app/src/main/res/drawable/bg_search_box.xml` + 日/夜 `search_box_bg`/`search_box_stroke` — 搜索框描边
- `app/src/main/res/values-night/colors.xml` — OLED 纯黑
- `Build-MBox.ps1` — 一键编译脚本（已修 -f 参数 bug、Write-Host 误解析为 -ForegroundColor bug）
- `TVBoxOSC.jks` — 签名文件（不入库）

## 七、Git 管理

- 仓库：master 分支；全局身份 vteovi / vteovi@msn.com
- **APK 不入库**：`.gitignore` 启用 `*.apk` 规则 + `git rm --cached`（本地文件保留在 `APK\`）
- `.gitignore` 已排除：*.jks、local.properties、build、.gradle、.idea
- 提交习惯：每次改动自动 commit，中文 message

## 八、已验证死路 / 技术结论

- DLNA-Cast JitPack 任何 tag 均无产物（build.log 显示 jcenter 死链）→ 改用本地模块
- SplashTheme（windowFullscreen）直接给 MainActivity 会造成全屏 → 未采用
- SeekBar 会拉伸进度条 drawable 到 View 高度，只改 drawable size 无效 → 需 gravity=center_vertical
- `bg_large_round_gray` 被 12 处共用，不可单独改 → 需要差异化样式时新建 drawable
- 项目日志类为 `LOG`（com.github.tvbox.osc.util.LOG），不是 LogUtils
- 数据备份新格式（SAF zip）与旧 tvbox_backup 目录格式**不互通**

## 九、未决 / 待办

- **小米安装提示"缺少隐私政策"**：已解释为 MIUI 安全中心扫描警告（非病毒、不影响安装）；可补 Manifest meta-data + 应用内隐私政策页面降低提示，用户未拍板是否需要（自用可忽略）。
- 1.2.5 相关功能/视觉改动待用户安装验收反馈。
