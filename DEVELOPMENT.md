# 开发、测试与发布

## 正式与预研隔离

- 任务页及学习通基础作业功能纳入正式开发范围：账号验证、本机加密配置、主动刷新、课程名称与作业展示、按账号离线保存。合并源码不等于发布 APK；正式签名与公开发布仍需单独授权。后续未开放的平台和社交继续隔离开发。
- 入口：任务 → 右上角账号图标（账号密码配置），旁边为刷新图标（刷新作业）；长按图标可查看名称。提交后先通过学习通认证，返回登录成功才使用 Android Keystore 的 AES-GCM 密钥加密保存账号及密码；认证失败不替换原配置。配置输入框不回填密码，不参与实例保存 / 自动填充，不写日志或导出文件。
- 账号密码在登录验证成功后加密保存到本机，供后续主动刷新使用。首页首帧后仅在空闲时准备任务页和本机缓存，作业视图按帧分批构建与测量、单页准备完成后替换，期间保留旧列表与滚动位置。已有配置时，每天首次打开软件自动刷新一次；使用期间按任务设置中的间隔刷新，默认 60 分钟，可设为 0（仅每天首次打开）或 1–10080 分钟。启动仍先读缓存，不阻塞首页首帧；Activity 暂停后停止计时，恢复前台时重新判断。每次尝试前原子保存时间，失败保留已有任务，按间隔或下次跨天打开重试，手动刷新不受间隔限制。不安排退出后的后台唤醒。配置过程仍需联网验证登录是否成功。右上角区分已配置 / 本次已连接 / 待验证，作业读取失败保留本次页面已有结果。任务 → 任务设置可选择加载的课程、查看删除历史和设置间隔。关闭课程后跳过该课程的作业及详情请求，并隐藏缓存任务；重新开启可恢复缓存展示。删除任务只移入本机删除历史，保留删除时间及原内容，点击记录恢复；刷新按课程 / 作业标识过滤已删任务，不覆盖删除记录。课程目录、开关、删除历史和刷新设置随同账号缓存使用 AtomicFile 保存，沿用 schema 1 的可选字段兼容旧缓存；不保存课程访问参数。
- 任务页按课程分组展示作业标题、提交状态和截止时间，支持待处理 / 已完成 / 全部筛选；点击胶囊选项或左右滑动切换，选中背景与内容跟随同一翻页进度，松手沿原生减速曲线落位，各分类保留独立纵向滚动位置。结果按账号保存在 App 私有文件，退出 / 重启后只从本机加载。缓存保留原 schema 1 字段，增加可选课程 / 作业标识用于精确合并，兼容旧缓存；不保存签名链接、访问参数或 Cookie。写入使用 AtomicFile，失败保留原文件。读取成功的课程替换其旧作业，确认空列表时清除该课程旧条目；失败和未读到的课程保留旧作业。课程作业分页时只合并已读取条目、不删除未见条目；截止时间未知时尽量保留同一作业的已知值。同名课程用课程 / 班级标识区分，旧缓存无法唯一对应时保留旧条目。问题在列表末尾说明，无年份日期不猜年份，已提交作业暂不额外请求详情。
- 表单、按钮和弹窗继续复用 `SecondaryUi`、`form_field`、`SecondaryDialog`。设备调试 / 仪器检查按当前用户授权执行；用户手动验收不能代替正式签名、覆盖更新和发布检查。
- 读取失败在界面显示安全的阶段原因（账号信息 / 课程列表 / 课程访问参数 / 作业列表 / 截止时间），不显示响应正文、Cookie、请求地址或访问签名。课程列表兼容 href 与 onclick 中的课程链接，机构编号兼容脚本声明及隐藏输入；网络超时按请求阶段报告。第三方接口适配需以用户实际刷新结果确认，构建通过不代表平台端到端验收。
- 官方 HTTP 重定向只在目标仍属于受信学习通域、无用户信息且使用标准端口时转换为 HTTPS 后请求；不启用明文流量，不跨域转发登录表单。连接错误显示安全异常类型及 R01–R06 跳转代码，不暴露地址、查询参数或凭据。
- 课程名称在与课程链接对应的独立卡片内读取，支持嵌套容器和空 title 时的正文回退；跨多个课程的父容器不借用相邻课程名称。未解析到名称时明确显示“课程名称未识别”，不再用课程编号充当名称。
- 协议调研参考：[只读客户端实现](https://github.com/DanMo661/Xuexitong-mcp/blob/main/xuexitong_mcp/client.py)，并核查了[官方登录入口](https://passport2.chaoxing.com/login?newversion=true)。这些网页接口没有稳定性保证；官方脚本地址本次返回不可访问页，未声称已完成真实账号协议验收。

- `C:\codex\CourseLens` 是日常维护目录，以最新 `origin/main` 为正式基线。`codex/development-isolation` 仅保存本隔离约定；后续正式修复从最新正式基线建立独立分支，并保留本节和 `AGENTS.md` 的默认规则。
- 已开放的任务页、底部导航和学习通基础功能在正式线维护；未开放的平台、新界面与未来社交存放在独立 Git worktree，分支为 `codex/task-page-navigation`。使用 `git worktree list` 查找目录，不复制另一套仓库；只把正式修复同步进预研分支，不把预研分支合入日常分支。
- 用户未明确指定新功能时，“修复、推送、打包 App”均针对正式开发线；预研分支只在用户明确要求时推送。开放某项功能需单独授权、整理差异、验收并通过 PR 合入，合并及公开发布沿用既有授权边界。
- 预研界面以已选第三张浅色设计为目标；社交仅预留入口，平台接口暂不接入。设计图、手机截图和设备备份留在仓库外。
- 预览复用 `-PqaBuild` 与 `cn.courselens.qa`，不覆盖正式包；不同分支的 QA 包会相互覆盖，安装前仍按设备验证章节备份。第三张设计尚未实现时，不把现有原型称为最终界面。
- 每次打包前核对工作目录、`git status --short --branch`、来源提交和相对 `origin/main` 的差异；从已提交源码构建，交付注明正式或预览、分支和提交。正式构建不得直接使用预研 worktree，既有签名发布工作流仍只允许 `main`。

## 开发与构建

使用本公开仓库的 `main` 和短期功能分支；重要修改经 PR 和检查后合入。协作权限见 [AGENTS.md](AGENTS.md)。工程沿用单模块 Java / 原生 View 架构，版本和依赖以 `app/build.gradle` 为准。底部内容弹层与表单选择器统一占可用高度的一半，长内容在弹层内滚动。

安装 JDK 17、Python 3、Android SDK 34 与 build-tools 34.0.0。配置 `ANDROID_HOME`，或在被忽略的 `local.properties` 指定 `sdk.dir`。

```powershell
python tests/run_checks.py
python tests/ReleaseChecksTest.py
python tests/SigningChecks.py
.\gradlew.bat --no-daemon -PqaBuild assembleDebug assembleDebugAndroidTest lintDebug
```

Linux 使用 `python3` 和 `sh gradlew`。`run_checks.py` 是本地与 CI 共用的 Java 业务回归入口，另含本实验的学习通解析检查；Gradle 单元测试的 `NO-SOURCE` 不代表它们通过。

普通贡献者使用 `-PqaBuild`：Debug 包名为 `cn.courselens.qa`，使用开发签名；测试数据在测试 APK 中，Release 变体禁用。正式构建保持 `cn.courselens`，必须提供既有签名密钥并通过固定证书检查。

## 多份课表与学校适配

- 当前课表 ID 和名称列表放在 `timetables` SharedPreferences；原有课表继续使用 Activity 原偏好文件，不复制或改写已有课程格式。设置中的当前课表与切换列表支持长按名称改名，名称校验沿用 1–40 个字；只更新索引，不搬移课程数据。从设置进入新增页，取消或手动创建后返回设置；首页入口返回课表页。新增课表使用独立偏好文件，课程、学期、备注、冲突选择、调课与 `classTimes` 随课表切换。小组件读取同一当前 ID，旧课表的小组件点击不会误匹配新课表。学习通账号和作业仍使用原有独立存储。
- `ClassTimes` 以每行 `HH:mm-HH:mm` 保存作息，1–30 节，检查顺序、重叠与课程节次上界。作息页按节次显示上课 / 下课时间，使用 24 小时选择器，支持新增一节与移除末节，并沿用保存 / 放弃草稿提示。未保存该字段的旧课表沿用宿迁学院 12 节作息。新增可手动创建或选择学校导入；徐州工程学院通过个人课表接口读取已选课程及各校区日节次，支持分段周次、单双周、单周调课与无时间实践备注。不同校区作息不一致时明确拒绝合并，保留旧课表。
- 徐工院仅放行 `jwglxt.xzit.edu.cn` 的标准 HTTP/HTTPS 端口，禁止跨域或 URL 用户信息，密码使用学校 RSA/PKCS1 协议；不落盘凭据或 Cookie。Android 网络配置只对该精确域名允许 HTTP，不包含子域，其余流量保持 HTTPS。该校当前 HTTP 服务不提供传输完整性和会话保密性，登录页及 README 明示限制；HTTPS 可用后应移除此例外。
- 使用 `-e timetablesOnly true` 运行多课表与徐工院合成专项，涵盖旧数据保留、新建与切换、重建恢复、14 节编辑与小组件、RSA/解析及域名边界；测试只使用临时合成课表，结束恢复索引。真实账号读取只验证第三方链路，不把私人课表或凭据放入测试资产。

## 设备验证

先核对 `adb devices -l` 的目标序列号，备份 QA 包已有偏好。只覆盖安装 QA 主包和测试包，不卸载用户正式包。

```powershell
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -e saveOnly true cn.courselens.qa.test/cn.courselens.NativeChecks
```

`SERIAL` 替换为已核验序列号，手机保持解锁和 App 前台。参数可换为 `tabStartupOnly`（冷进程下 54 份合成缓存作业的首次 / 后续底栏点击、首帧及主线程停顿）、`capsuleOnly`（冷启动恢复非零筛选、加载布局变化后的首次滑动、反向取消、点击动画及底栏无水波纹）、`chaoxingOnly`（学习通只读客户端、课程开关、删除恢复与刷新保留的合成检查）、`navigationOnly`（任务页导航、紧凑操作及普通 / 大字体布局）、`choiceOnly`（课程与调课选择器、取消和草稿保留）、`dialogLayoutOnly`（普通与大字体弹窗正文、按钮分区）、`courseSlotsOnly`、`scrollOnly`、`conflictOnly`、`adjustmentOnly`、`widgetOnly`、`widgetGuideOnly`、`widgetPinOnly`、`motionOnly`、`gestureOnly`、`performanceOnly` 或 `remoteOnly`；不指定专项参数运行全量入口。`nextClassOnly` 是历史名称，首页已无下一节提示。

`navigationOnly`、`choiceOnly`、`dialogLayoutOnly` 可追加 `-e normalFontOnly true`，只检查设备当前字号，不临时放大字体；未指定时仍覆盖普通与大字体。

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

网站介绍页在 `web/`，使用静态 HTML/CSS；`python tests/WebsiteChecks.py` 检查下载地址、站内锚点、图片属性及本地资源。预览时先运行 `python web/build.py`，再以 `preview/site/` 为站点根目录启动静态服务器。`Remote configuration` 在相关文件变化时检查页面与配置，main 部署时运行 `python web/build.py site`，将网站和 `remote/config.json` 打包到同一 Pages 站点，配置路径仍为 `/remote/config.json`。三个下载入口由 `web/download.js` 在点击时读取同站点的 `remote/config.json`，校验 `android.apkUrl` 后从 GitHub 直接下载 APK，不调用 GitHub 查询 API。最新版以更新配置为准；正式 APK 发布后按发布流程更新配置即可。配置请求超时、失败或地址无效时显示重试及 GitHub 手动下载入口；禁用 JavaScript 时直接进入 GitHub 最新发布页。`node tests/WebsiteDownloadChecks.cjs` 检查下载选择、失败处理及重复点击。图片仅使用经过检查的合成示例，原始手机截图与设计验收截图保存在仓库外。

2.7.3 起优先读取 `https://courselens.xvyan.cn/remote/config.json`，连接或 HTTP 请求失败时依次使用原 Pages、Raw 地址。已发布的 2.7.2 首选地址为 `https://update.xvyan.cn/remote/config.json`。保留 `schemaVersion: 1`、`remote/config.json`、现有 Pages/Raw 地址及仓库名，供旧客户端及备用请求使用。v2.6 的请求地址写在 APK 中，不跟随 HTTP 重定向，仓库入口校验也限定为当前公开仓库。改名必须先制定旧版兼容方案。

公告 ID 唯一；修改同一 ID 不会重新提醒，需要新公告时用新 ID。配置最多 64 KiB / 20 条公告，链接为 HTTPS，版本以实际 versionCode 判断；检查失败保留有效缓存，不冒充“已是最新版”。线上配置校验与 App 的 `remoteOnly` 专项分别验证。

## 原私有仓库归档条件

从公开 main 干净检出通过构建与业务回归、关键设备验收、签名和覆盖更新验证；确认必要文件、受控发布 secrets 及密钥备份齐全后，才切换本地远程。保留私有 Git 历史的安全备份，经单独授权后保持私有并归档，不删除。

公开历史和 v2.6 APK 曾包含真实样例课表。新源码改为合成测试数据不会清除旧记录；历史清理及旧发布资产处理需单独评估和授权，不以镜像推送、强推或重新公开私有历史解决。
