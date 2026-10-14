# 开发、测试与发布

## 开发与构建

使用本公开仓库的 `main` 和短期功能分支；重要修改经 PR 和检查后合入。协作权限见 [AGENTS.md](AGENTS.md)。工程沿用单模块 Java / 原生 View 架构，版本和依赖以 `app/build.gradle` 为准。

安装 JDK 17、Python 3、Android SDK 34 与 build-tools 34.0.0。配置 `ANDROID_HOME`，或在被忽略的 `local.properties` 指定 `sdk.dir`。

```powershell
python tests/run_checks.py
python tests/ReleaseChecksTest.py
python tests/SigningChecks.py
.\gradlew.bat --no-daemon -PqaBuild assembleDebug assembleDebugAndroidTest lintDebug
```

Linux 使用 `python3` 和 `sh gradlew`。`run_checks.py` 是本地与 CI 共用的五组 Java 业务回归入口；Gradle 单元测试的 `NO-SOURCE` 不代表它们通过。

普通贡献者使用 `-PqaBuild`：Debug 包名为 `cn.courselens.qa`，使用开发签名；测试数据在测试 APK 中，Release 变体禁用。正式构建保持 `cn.courselens`，必须提供既有签名密钥并通过固定证书检查。

## 设备验证

先核对 `adb devices -l` 的目标序列号，备份 QA 包已有偏好。只覆盖安装 QA 主包和测试包，不卸载用户正式包。

```powershell
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -e saveOnly true cn.courselens.qa.test/cn.courselens.NativeChecks
```

`SERIAL` 替换为已核验序列号，手机保持解锁和 App 前台。参数可换为 `courseSlotsOnly`、`scrollOnly`、`conflictOnly`、`adjustmentOnly`、`widgetOnly`、`widgetGuideOnly`、`widgetPinOnly`、`motionOnly`、`gestureOnly`、`performanceOnly` 或 `remoteOnly`；不指定专项参数运行全量入口。`nextClassOnly` 是历史名称，首页已无下一节提示。

测试使用合成数据；正式 Activity 与 QA Activity 的偏好文件名不同，按实际包与文件核实。测试结束或中断后恢复并比较备份，不能仅凭测试的 PASS 推断用户数据未变化。小组件添加、尺寸和点击另在真实启动器验收，测试宿主不能替代它。

`widgetOnly` 和 `widgetGuideOnly` 需要 QA 测试宿主的临时绑定权限；只对 QA 包授予，在 `finally` 中撤销。它不是厂商桌面快捷方式权限，也不证明真实启动器添加成功：

```powershell
adb -s SERIAL shell appwidget grantbind --package cn.courselens.qa --user 0
try {
  adb -s SERIAL shell am instrument -w -e widgetGuideOnly true cn.courselens.qa.test/cn.courselens.NativeChecks
} finally {
  adb -s SERIAL shell appwidget revokebind --package cn.courselens.qa --user 0
}
```

真实启动器验证使用 `widgetPinOnly`，先撤销测试宿主绑定权限；它确认实际新增绑定和系统成功回调，结束后保留 QA 组件供查看。厂商拒绝请求时记录失败及手动添加指引，不把指引当作成功，不更改厂商权限绕过。

正式发布前核验：已有课表启动恢复、同步失败保留数据、课程保存与取消、多时段、调课/停课、冲突选择、切周及滚动返回、小组件、公告和更新。覆盖更新在隔离设备上保留真实格式的合成数据验证，不卸载旧版本。

## GitHub Actions

- `Android checks`：PR、main push 和手动运行五组回归、发布校验测试、无密钥签名边界、QA 主/测试 APK 和 Lint。产物仅供 QA，不执行设备测试。
- `Remote configuration`：PR 只验证配置，main 上相关文件变化后部署现有 Pages；不构建或发布 APK。
- `Prepare signed release`：仅手动选择 main，拒绝已存在标签及不递增的 versionCode，构建与验证正式 APK，生成校验文件和来源元数据，创建**草稿** Release。

合入迁移 PR 后，为 main 启用 PR 与必需检查 `checks`，禁止强推；个人维护不要求第二位审批人。配置 `release` Environment 的部署分支仅允许 main，然后才放入以下 Environment secrets：

```text
COURSELENS_KEYSTORE_BASE64
COURSELENS_STORE_PASSWORD
COURSELENS_KEY_ALIAS
COURSELENS_KEY_PASSWORD
```

使用既有密钥，不生成新密钥；先完成仓库外的安全备份。密钥不提供给普通检查。工作流生成后不会自动迁移私有仓库 secrets，也不执行任何发布。

## 正式发布

1. 合入已批准并完成适用设备验收的源码；提高 versionCode 和 versionName。保留包名、偏好名称、存储格式及签名指纹；修改存储时另做旧数据兼容检查。
2. 经发布授权后，手动运行 `Prepare signed release`，选择 main 和与 versionName 对应的新标签。检查结果中的来源提交必须是获准提交。
3. 核对草稿 APK、`SHA256SUMS`、`release-metadata.json`、证书及使用说明；经明确授权后公开草稿。现有标签和 APK 不替换。
4. 发布成功且 APK 可下载后，再通过 PR 更新 `remote/config.json` 中的版本、更新日志和 APK 地址，保留公告及人工维护的网盘地址。CI 成功或草稿 Release 不等于发布成功。
5. 核验 Pages 与 Raw 均返回 200，配置与 Git blob 一致；检查 APK 下载及旧客户端更新入口。网盘分享由维护者核对内容和有效期。

本地正式构建按 `signing.properties.example` 配置被忽略的签名属性，密钥保存在仓库外；执行 `python tests/SigningChecks.py --signed`、正式 `assembleRelease lintRelease`。`python tests/ReleaseChecks.py vVERSION --apk PATH` 使用 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 检查产物，输出到 `app/build/release/`，不发布。

## 公告配置与旧版兼容

保留 `schemaVersion: 1`、`remote/config.json`、现有 Pages/Raw 地址及仓库名。v2.6 的请求地址写在 APK 中，不跟随 HTTP 重定向，仓库入口校验也限定为当前公开仓库。改名必须先制定旧版兼容方案。

公告 ID 唯一；修改同一 ID 不会重新提醒，需要新公告时用新 ID。配置最多 64 KiB / 20 条公告，链接为 HTTPS，版本以实际 versionCode 判断；检查失败保留有效缓存，不冒充“已是最新版”。线上配置校验与 App 的 `remoteOnly` 专项分别验证。

## 原私有仓库归档条件

从公开 main 干净检出通过构建与业务回归、关键设备验收、签名和覆盖更新验证；确认必要文件、受控发布 secrets 及密钥备份齐全后，才切换本地远程。保留私有 Git 历史的安全备份，经单独授权后保持私有并归档，不删除。

公开历史和 v2.6 APK 曾包含真实样例课表。新源码改为合成测试数据不会清除旧记录；历史清理及旧发布资产处理需单独评估和授权，不以镜像推送、强推或重新公开私有历史解决。
