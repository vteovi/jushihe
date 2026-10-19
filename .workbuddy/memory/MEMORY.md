# 聚视盒（TVBoxOS_Portrait / MBox）项目长期约定

## 版本与构建
- 包名 `com.jushihe.tvbox`；产物名模板 `MBox_v<versionName>_<buildType>[_yyyyMMdd].apk`。
- 每次进入 APK 的提交都同步升 `app/build.gradle` 的 versionCode(+1) / versionName（fix→PATCH、feat→MINOR）。
- 出包入口：根目录 `编译聚视盒.bat` / `Build-MBox.ps1`。
- 上游参考仓库（同衍生分支，壁纸/海报尺寸等功能更全）：`D:\WorkBuddy\TVBoxOS-Mobile`。本竖屏版**没有**壁纸、海报尺寸设置，`HawkConfig` 里也没有 `WALLPAPER_URL/WALLPAPER_HISTORY/POSTER_SIZE`，从上游拷代码时要剔除。

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

## 已知坑
- `HawkConfig` 只有字符串常量；`BackupManager.BACKUP_KEYS` 里出现未定义常量会直接编译失败。
