# 开发、测试与发布

## 正式与预研隔离

- 学习通作业读取实验在 `codex/chaoxing-homework` 独立 worktree 开发，任务页来自预研分支 `f1a576c`，现已同步最新正式基线 `2063f56`（2.6.3）；本次按用户要求保持与正式 App 隔离，不推送、不合并、不发布。这里只做 QA 预览，不能作为正式构建来源。
- 入口：任务 → 账号密码配置。提交后先通过学习通认证，返回登录成功才使用 Android Keystore 的 AES-GCM 密钥加密保存账号及密码；认证失败不替换原配置。配置输入框不回填密码，不参与实例保存 / 自动填充，不写日志或导出文件。
- 任务页右上角显示已验证连接的平台；保存配置后自动读取，首次进入任务页或点击刷新时使用已保存的配置登录。登录失效显示待验证，作业读取失败保留已有结果。密码配置弹窗单独提供，不占用作业详情区。
- 任务页剩余区域直接展示课程、作业标题、提交状态和截止时间，并支持待处理 / 已完成 / 全部筛选。结果暂不持久化；未知截止时间与部分失败保留明确说明。无年份日期不猜年份；已提交作业暂不额外请求详情。当前只支持手机号 / 超星账号，验证码或机构认证失败不会保存密码配置。
- 表单、按钮和弹窗继续复用 `SecondaryUi`、`form_field`、`SecondaryDialog`。按用户要求本轮不运行设备调试 / 仪器测试，只构建并安装 QA 包；新密码保存和任务布局由用户检查。
- 读取失败在界面显示安全的阶段原因（账号信息 / 课程列表 / 课程访问参数 / 作业列表 / 截止时间），不显示响应正文、Cookie、请求地址或访问签名。课程列表兼容 href 与 onclick 中的课程链接，机构编号兼容脚本声明及隐藏输入；网络超时按请求阶段报告。实际账号故障是否消除仍由用户刷新确认，本轮不运行设备调试或仪器测试。
- 官方 HTTP 重定向只在目标仍属于受信学习通域、无用户信息且使用标准端口时转换为 HTTPS 后请求；不启用明文流量，不跨域转发登录表单。连接错误显示安全异常类型及 R01–R06 跳转代码，不暴露地址、查询参数或凭据。
- 课程名称在与课程链接对应的独立卡片内读取，支持嵌套容器和空 title 时的正文回退；跨多个课程的父容器不借用相邻课程名称。未解析到名称时明确显示“课程名称未识别”，不再用课程编号充当名称。
- 协议调研参考：[只读客户端实现](https://github.com/DanMo661/Xuexitong-mcp/blob/main/xuexitong_mcp/client.py)，并核查了[官方登录入口](https://passport2.chaoxing.com/login?newversion=true)。这些网页接口没有稳定性保证；官方脚本地址本次返回不可访问页，未声称已完成真实账号协议验收。

- `C:\codex\CourseLens` 是日常维护目录，以最新 `origin/main` 为正式基线。`codex/development-isolation` 仅保存本隔离约定；后续正式修复从最新正式基线建立独立分支，并保留本节和 `AGENTS.md` 的默认规则。
- 未开放的任务页、底部导航、新界面与未来社交存放在独立 Git worktree，分支为 `codex/task-page-navigation`。使用 `git worktree list` 查找目录，不复制另一套仓库；只把正式修复同步进预研分支，不把预研分支合入日常分支。
- 用户未明确指定新功能时，“修复、推送、打包 App”均针对正式开发线；预研分支只在用户明确要求时推送。开放某项功能需单独授权、整理差异、验收并通过 PR 合入，合并及公开发布沿用既有授权边界。
- 预研界面以已选第三张浅色设计为目标；社交仅预留入口，平台接口暂不接入。设计图、手机截图和设备备份留在仓库外。
- 预览复用 `-PqaBuild` 与 `cn.courselens.qa`，不覆盖正式包；不同分支的 QA 包会相互覆盖，安装前仍按设备验证章节备份。第三张设计尚未实现时，不把现有原型称为最终界面。
- 每次打包前核对工作目录、`git status --short --branch`、来源提交和相对 `origin/main` 的差异；从已提交源码构建，交付注明正式或预览、分支和提交。正式构建不得直接使用预研 worktree，既有签名发布工作流仍只允许 `main`。

## 开发与构建

使用本公开仓库的 `main` 和短期功能分支；重要修改经 PR 和检查后合入。协作权限见 [AGENTS.md](AGENTS.md)。工程沿用单模块 Java / 原生 View 架构，版本和依赖以 `app/build.gradle` 为准。

安装 JDK 17、Python 3、Android SDK 34 与 build-tools 34.0.0。配置 `ANDROID_HOME`，或在被忽略的 `local.properties` 指定 `sdk.dir`。

```powershell
python tests/run_checks.py
python tests/ReleaseChecksTest.py
python tests/SigningChecks.py
.\gradlew.bat --no-daemon -PqaBuild assembleDebug assembleDebugAndroidTest lintDebug
```

Linux 使用 `python3` 和 `sh gradlew`。`run_checks.py` 是本地与 CI 共用的 Java 业务回归入口，另含本实验的学习通解析检查；Gradle 单元测试的 `NO-SOURCE` 不代表它们通过。

普通贡献者使用 `-PqaBuild`：Debug 包名为 `cn.courselens.qa`，使用开发签名；测试数据在测试 APK 中，Release 变体禁用。正式构建保持 `cn.courselens`，必须提供既有签名密钥并通过固定证书检查。

## 设备验证

先核对 `adb devices -l` 的目标序列号，备份 QA 包已有偏好。只覆盖安装 QA 主包和测试包，不卸载用户正式包。

```powershell
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -e saveOnly true cn.courselens.qa.test/cn.courselens.NativeChecks
```

`SERIAL` 替换为已核验序列号，手机保持解锁和 App 前台。参数可换为 `chaoxingOnly`（学习通只读客户端合成检查）、`navigationOnly`（任务页导航）、`choiceOnly`（课程与调课选择器、取消和草稿保留）、`dialogLayoutOnly`（普通与大字体弹窗正文、按钮分区）、`courseSlotsOnly`、`scrollOnly`、`conflictOnly`、`adjustmentOnly`、`widgetOnly`、`widgetGuideOnly`、`widgetPinOnly`、`motionOnly`、`gestureOnly`、`performanceOnly` 或 `remoteOnly`；不指定专项参数运行全量入口。`nextClassOnly` 是历史名称，首页已无下一节提示。

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
