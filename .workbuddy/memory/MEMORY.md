# 聚视盒（TVBoxOS_Portrait / MBox）项目长期约定

## 版本与构建
- 包名 `com.jushihe.tvbox`；产物名模板 `MBox_v<versionName>_<buildType>[_yyyyMMdd].apk`。
- 每次进入 APK 的提交都同步升 `app/build.gradle` 的 versionCode(+1) / versionName（fix→PATCH、feat→MINOR）。
- 出包入口：根目录 `编译聚视盒.bat` / `Build-MBox.ps1`。
- 上游参考仓库（同衍生分支，壁纸/海报尺寸等功能更全）：`D:\WorkBuddy\TVBoxOS-Mobile`。本竖屏版**没有**壁纸、海报尺寸设置，`HawkConfig` 里也没有 `WALLPAPER_URL/WALLPAPER_HISTORY/POSTER_SIZE`，从上游拷代码时要剔除。

## 分支流程（v1.11.6 起）
- **`master` = 稳定版**，只接收经过实机验证的合并（GitHub 远端默认分支也是 master，不要改成 main）。
- **`dev` = 开发/测试分支**，以后所有新功能、改造、试错都在 dev 上提交；稳定后再合回 master。
- 合并：`git checkout master && git merge --no-ff dev`（保留合并节点，便于回溯整条功能线），合并后 master 上打 tag（如 `v1.12.0`）并推送。
- 版本号规则不变：dev 上每次进 APK 的提交照常升 versionCode/versionName，合到 master 时版本号已经是新的，不需要再升。

## 本地文件型配置（订阅源 / 影视源 / 直播源）
- 本地导入统一用 `util/LocalImport.kt`：复制到 `filesDir/<subDir>/`，以 `file://` 绝对路径引用（私有目录、免权限、重启不失效）。不要再写 `Android/data/<pkg>/...` + `clan://localhost/`（跨设备同步后接收端没有该文件，必然 404）。
- `ApiConfig.loadConfig` 已支持 `assets://` / `content://` / `file://` 直接读文件；`clan://` 仍走内置 HTTP 服务的 `/file/`（外存根路径）。
- 跨设备同步 / 备份还原必须"文件内容 + URL"一起传：`BackupManager` 的 `localFiles` 以原 URL 为 key 存 Base64，接收端在 `filesDir/<subDir>/<md5>.json` 重建并重写成 `file://`。`isLocalUrl()` 需覆盖 `file://`、`content://`、`clan://localhost/`。

## Hawk 存储（重要）
- 依赖 `com.orhanobut:hawk:2.0.1`，其 SharedPreferences 文件名是常量 `STORAGE_TAG_DO_NOT_CHANGE = "Hawk2"`。
  任何绕过 Hawk API 直接读写设置的代码（如 `BackupManager`）必须用 `"Hawk2"`，写成 `"Hawk"` 会静默失效（导出读空、导入写错文件）。
- 设置项在 sp 里是 Hawk 的 "type#value" 包装串：要取真实值需走 `Hawk.get`，要写回真实值需走 `Hawk.put`；
  原样搬运（备份还原）则直接复制串即可，复杂对象（List/Set）也能无损还原。
- 导入后若会立即重启 App，落盘用 `commit()` 而不是 `apply()`。

## 屏幕适配（v1.10.0 起支持大屏平板横屏）
- 判定统一用 `Utils.isTablet(context)`（`smallestScreenWidthDp >= 600`），与 `layout-sw600dp/` 限定符一致，不要另造标准。
- 方向不写在清单里：`BaseActivity.applyScreenOrientation()` 统一控制（手机锁竖屏、平板 `FULL_SENSOR`）；播放类页面覆盖 `lockPortraitOnPhone()` 返回 false。
- autosize 设计稿是 360dp 竖屏稿，`BaseActivity.getSizeInDp()` 在 sw600dp 时返回 `screenWidthDp`（1:1），否则平板横屏会被放大数倍。清单已声明 `configChanges`，所以必须在 `onConfigurationChanged` 里手动 `AutoSize.autoConvertDensity`。
- 新增平板布局放 `layout-sw600dp/`。**同一 id 在不同 layout 变体里必须是同一控件类型**，否则 ViewBinding 报冲突；需要不同控件（如底部导航 vs 侧边导航）就用不同 id + 代码 `findViewById` 取共同基类。
- 网格列数由 `Utils.getPosterSpanCount()` 按屏宽动态计算（竖版 180dp/列、横版 400dp/列），不要再写死 2 列。

## 已知坑
- `HawkConfig` 只有字符串常量；`BackupManager.BACKUP_KEYS` 里出现未定义常量会直接编译失败。
