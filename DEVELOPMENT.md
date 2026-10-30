# 开发、测试与发布

## 正式与预研隔离

- 任务页及学习通基础作业功能纳入正式开发范围：账号验证、本机加密配置、主动刷新、课程名称与作业展示、按账号离线保存。合并源码不等于发布 APK；正式签名与公开发布仍需单独授权。后续未开放的平台和社交继续隔离开发。
- `C:\codex\CourseLens` 是日常维护目录，以最新 `origin/main` 为正式基线。`codex/development-isolation` 仅保存本隔离约定；后续正式修复从最新正式基线建立独立分支，并保留本节和 `AGENTS.md` 的默认规则。
- 已开放的任务页、底部导航和学习通基础功能在正式线维护；未开放的平台、新界面与未来社交存放在独立 Git worktree，分支为 `codex/task-page-navigation`。使用 `git worktree list` 查找目录，不复制另一套仓库；只把正式修复同步进预研分支，不把预研分支合入日常分支。
- 用户未明确指定新功能时，“修复、推送、打包 App”均针对正式开发线；预研分支只在用户明确要求时推送。开放某项功能需单独授权、整理差异、验收并通过 PR 合入，合并及公开发布沿用既有授权边界。
- 每次打包前核对工作目录、`git status --short --branch`、来源提交和相对 `origin/main` 的差异；从已提交源码构建，交付注明正式或预览、分支和提交。正式构建不得直接使用预研 worktree，既有签名发布工作流仍只允许 `main`。

## 任务与学习通

- 入口：任务页右上角加号添加任务；点击同步状态芯片手动刷新，不使用下拉刷新。账号配置、课程加载和刷新预设放在任务页右上角 → 任务设置，任务恢复同时位于任务页右上角菜单及总设置 → 数据管理。提交后先通过学习通认证，返回登录成功才使用 Android Keystore 的 AES-GCM 密钥加密保存账号及密码；认证失败不替换原配置。配置输入框不回填密码，不参与实例保存 / 自动填充，不写日志或导出文件。
- 账号密码在登录验证成功后加密保存到本机，供后续主动刷新使用。首页首帧后仅在空闲时准备任务页和本机缓存，作业视图按帧分批构建与测量、单页准备完成后替换，期间保留旧列表与滚动位置。已有配置时，每天首次打开软件自动刷新一次；使用期间按任务设置中的间隔刷新，默认 60 分钟，界面提供 0（仅每天首次打开）/ 30 / 60 / 180 分钟预设；旧版自定义间隔仍按原值执行，直到选择新档位。启动仍先读缓存，不阻塞首页首帧；Activity 暂停后停止计时，恢复前台时重新判断。每次尝试前原子保存时间，失败保留已有任务，按间隔或下次跨天打开重试，手动刷新不受间隔限制。联网刷新期间保留任务按钮和长按操作，仅禁止重复刷新；网络请求与本机写入分开执行，本机修改和刷新提交在同一写入队列按顺序保存。刷新返回后重新读取最新缓存合并，编辑、删除、恢复、课程开关和间隔均不会被旧响应覆盖；重新配置账号会取消当前刷新。不安排退出后的后台唤醒。配置过程仍需联网验证登录是否成功。右上角区分已配置 / 本次已连接 / 待验证，作业读取失败保留本次页面已有结果。任务页右上角 → 任务设置可选择加载的课程和设置间隔；任务恢复可从本页右上角菜单及总设置 → 数据管理进入。关闭课程后跳过该课程的作业及详情请求，并隐藏缓存任务；重新开启可恢复缓存展示。任务列表长按可编辑或删除，右上角加号可手动添加（标题、课程 / 分类、截止时间）；手动任务单独保存在本机，未配置学习通和切换账号均可继续使用。学习通任务的本地编辑按原课程 / 作业标识保留，刷新不覆盖，远端暂时未返回也不清除。删除任务只移入本机删除历史，保留删除时间及原内容，点击记录恢复；刷新按课程 / 作业标识过滤已删任务，不覆盖删除记录。课程目录、开关、删除历史和刷新设置随同账号缓存使用 AtomicFile 保存，沿用 schema 1 的可选字段兼容旧缓存；不保存课程访问参数。
- 任务页按课程分组展示作业标题、提交状态和截止时间，取消完成状态分类与左右翻页，只在单一列表中显示未完成任务，已完成记录继续保存在本机，每门课程共用主题色圆角任务容器，任务间用细分隔线；课程名与待交数量位于容器上方。点击内容内联展开操作，全局只展开一条，展开和收起使用原生边界与透明度过渡，并遵循系统关闭动画设置。删除确认后移入历史，保存成功才播放移除动画。结果按账号保存在 App 私有文件，退出 / 重启后只从本机加载。缓存保留原 schema 1 字段，增加可选课程 / 作业标识用于精确合并，兼容旧缓存；不保存签名链接、访问参数或 Cookie。写入使用 AtomicFile，失败保留原文件。读取成功的课程替换其旧作业，确认空列表时清除该课程旧条目；失败和未读到的课程保留旧作业。课程作业分页时只合并已读取条目、不删除未见条目；截止时间未知时尽量保留同一作业的已知值。同名课程用课程 / 班级标识区分，旧缓存无法唯一对应时保留旧条目。问题集中在顶部可展开横幅，无年份日期不猜年份，已提交作业通常不额外请求详情；本地恢复为未完成且缺少截止时间时，按课程/作业标识补读详情。仅本地状态或标题改动不阻止补齐缺失时间，明确修改/清除过的时间通过可选 deadlineEdited 字段保留。页面仅提供开始时间时记为未提供截止时间，不把开始时间当作截止时间。没有识别到截止日期的任务不显示时间行、不留空位；可通过展开后的编辑入口设置时间，仍按课程归组。
- 任务页按截止时间排序，未知或无年份时间排序沉底；无年份但有效的月日时间保留在课程组内显示，不提示确认年份，不生成倒计时，补录使用日期与 24 小时时间滚轮选择器，不猜测年份。主卡统一按有效月日时间顺序选择待办任务，只显示月日时间，不因缺少年份而隐藏或降低优先级；仅在原始数据含明确年份时生成倒计时和七天进度，不推断缺少的年份。列表逐行显示状态，完成后自动从列表隐藏。列表移除本地完成圆钮，避免误触后任务隐藏；任务恢复同时提供已删除记录和旧版本本机标记完成记录的恢复，不改变学校平台提交状态。表单不编辑完成状态；软删除成功后显示 5 秒撤销，删除历史仍可恢复。同步芯片只显示连接/同步状态，结果与错误改为可展开、可关闭的顶部横幅，不在列表底部重复展示。自动刷新提供 0 / 30 / 60 / 180 分钟预设，既有其他间隔保留直到用户选择新档位。
- 任务表单的课程 / 分类保留手动输入，右侧独立按钮打开统一课程选择面板，选择或返回均保留编辑草稿，不使用随输入法和异步过滤开合的自动补全窗口。“加载的课程”使用课程开关列表、已开启数量和固定底部保存区，修改保存成功后才关闭；退出不保存草稿。
- 表单、按钮和弹窗继续复用 `SecondaryUi`、`form_field`、`SecondaryDialog`。设备调试 / 仪器检查按当前用户授权执行；用户手动验收不能代替正式签名、覆盖更新和发布检查。
- 读取失败在界面显示安全的阶段原因（账号信息 / 课程列表 / 课程访问参数 / 作业列表 / 截止时间），不显示响应正文、Cookie、请求地址或访问签名。课程列表兼容 href 与 onclick 中的课程链接，机构编号兼容脚本声明及隐藏输入；网络超时按请求阶段报告。第三方接口适配需以用户实际刷新结果确认，构建通过不代表平台端到端验收。
- 官方 HTTP 重定向只在目标仍属于受信学习通域、无用户信息且使用标准端口时转换为 HTTPS 后请求；不启用明文流量，不跨域转发登录表单。连接错误显示安全异常类型及 R01–R06 跳转代码，不暴露地址、查询参数或凭据。
- 课程名称在与课程链接对应的独立卡片内读取，支持嵌套容器和空 title 时的正文回退；跨多个课程的父容器不借用相邻课程名称。未解析到名称时明确显示“课程名称未识别”，不再用课程编号充当名称。
- 协议调研参考：[只读客户端实现](https://github.com/DanMo661/Xuexitong-mcp/blob/main/xuexitong_mcp/client.py)，并核查了[官方登录入口](https://passport2.chaoxing.com/login?newversion=true)。这些网页接口没有稳定性保证；官方脚本地址本次返回不可访问页，未声称已完成真实账号协议验收。

## 开发与构建

### 界面与目录约定

- 面向用户的名称统一为 **Morrow**，官网为 `https://morrow.xvyan.cn/`，仓库为 `fangfar/Morrow`；`cn.courselens` 包名、偏好键、签名、发布资产和工具参数保留兼容名称。
- `app/` 维护原生 Java/View 界面；`ocr-engine/` 供完整版与 `ocr-plugin/` 复用；`web/` 为官网源码，`remote/` 为发布配置；业务检查在 `tests/`，Android 单元与仪器检查分别在 `app/src/test/`、`app/src/androidTest/`。生成目录与验收截图不提交。
- README 介绍功能、安装、隐私与反馈；官网展示实际产品及下载；开发约定集中在本文，更新记录使用 Releases。历史发布说明按对应版本保留，不将新源码能力写成已发布能力。
- 系统无衬线字体，主页面标题 24sp、二级页标题 18sp、正文 14–16sp、元信息 12–13sp；用字重、对齐和间距分层，课程分组不再套装饰性胶囊。课表周网格、课程配色与底栏直接切换保留。
- Android 色板以 `values/colors.xml` 与 `values-night/colors.xml` 为准，快递色值通过资源别名复用；官网同步墨色 `#171717`、次要文字 `#626870`、浅底 `#F3F4F5`、动作蓝 `#0866D9`。仅状态和操作使用强调色，课程颜色继续承担区分课程的功能。
- 复用 `SecondaryUi` 的按钮、焦点和弹窗，48dp 触摸范围；页面间距以 8/16/24dp 为主，卡片约 16–24dp 圆角。Web 点击范围至少 44px，内容变窄时换行或纵向排列。动效沿用 `Motion`，遵循系统关闭动画及网页减少动画设置。
- 空状态提供清楚的状态和可执行操作；删除重复标题、标语与操作说明，不删除错误恢复、隐私、授权、数据覆盖提示和无障碍名称。设置按课表与应用分组，模块配置继续由各模块菜单直接进入。
- UI 修改同时验证原调用方、返回来源、草稿、滚动与状态恢复。可用 `UiConsistencyTest`、`TaskPageDesignTest`、`ParcelPageDesignTest` 和 `ModuleSettingsStyleTest` 覆盖布局及本机交互；原生 Canvas 渲染不能代替设备手势、键盘、系统安装器或真实启动器验收。截图使用合成数据，输出至仓库外；官网三张截图由 `UiConsistencyTest.productScreensUseSyntheticDataAndKeepActions` 的 360dp / xhdpi 场景生成，不使用私人课表。

### 共享交互规则

- 总设置按课表与应用分组，课表管理二级页提供课程管理、课表列表和新增课表，直接点击切换、长按重命名；开学日期、作息时间、桌面小组件、学校账号和统一数据管理保留直接入口；课表右上角菜单提供课程调整恢复和学校登录；课表清空、任务恢复及全部任务数据删除、全部快递数据删除统一放在“数据管理”中；任务页菜单另提供任务恢复快捷入口，其他全部数据删除不在模块菜单重复展示；单条任务和快递删除仍在各自条目中。全部任务删除会退出学习通账号、取消当前刷新并删除所有账号缓存及手动任务，不修改学校平台数据；快递删除沿用关闭后台功能、清除去重记录和设置的行为。每项删除分别确认，不提供一键删除三个模块。各模块右上角横向三点打开本模块设置菜单，关闭后保留来源页面与滚动；开学日期保存或取消、小组件和学校账号页取消后返回总设置；学校账号同步成功后回到课表页。作息时间返回总设置，课程管理和取消新增返回课表管理，新增成功后回到课表页。操作结果提供可见反馈。
- 侧栏收起动画结束前继续消费返回操作，避免快速连续返回穿透到页面；完成收起后恢复正常返回。沿用 `SecondaryUi` 和现有颜色资源、48dp 最小操作区；点击不使用水波纹扩散，保留静态键盘焦点描边与原生控件选中状态。底栏直接切换及完整周课表，不另建一套主题或导航组件。
- 栏目设置与操作菜单统一使用横向三点入口、右上角圆角细描边下拉面板和分组分隔线，不遮暗底页。下拉菜单宽度上限 168dp、上下内边距 8dp、文字 14sp，菜单行保持最小 48dp 点击区并缩小分隔留白；较长菜单限高滚动；下拉菜单用 220/180ms 进出缓动，底部面板用 340/280ms，上方保留圆角、底边贴齐并与系统手势区同色，关闭后恢复页面底色；编辑、选择与确认面板沿用足够的操作空间并使用同一描边与轻遮罩。打开菜单后底页暂时退出无障碍焦点，点外部或返回关闭后恢复。
- `navigationOnly` 覆盖课表入口迁入总设置、栏目右上角横向三点菜单与来源模块恢复，以及侧栏连续返回；截图随 QA 数据导出至仓库外，不能仅凭构建或 JVM 测试宣称视觉验收通过。

### 统一动效

- `Motion` 集中时长与自然缓动：按压 120ms、菜单 220/180ms、居中确认框 260/200ms、底部面板 340/280ms、二级页面 220/180ms、任务内联展开/收起 260/220ms。抽屉普通开关参考 340ms，选择菜单后用 220ms 归位，按剩余距离和拖动速度调整；回弹使用无过冲的临界阻尼弹簧响应。平台 `ValueAnimator` 自动遵守系统动画比例，关闭动画时直接提交最终状态。
- 原生 Java Views 保持现有页面结构。`Motion.Progress` 负责取消旧监听和从当前值续播；`MotionOverlay` 用一个进度驱动面板与遮罩，退出完成才移除容器。小菜单保持不遮暗底页；底部面板采用轻遮罩。**课表课程详情及由它进入的冲突课程详情保留当前半屏显示高度**，进出统一沿面板自身高度滑动，面板保持不透明，不再使用 48dp 短位移叠加整块淡变。面板显示高度继续沿用对应模块规则。
- `MotionSheetDialog` 让快递面板复用同一控制器，不依靠延迟猜测窗口退出；居中确认框及日期时间对话框使用 Android WindowManager 的原生窗口动画，退出由系统完成。按钮沿用原有点击区和键盘描边，用透明度反馈按压，不新增水波纹。
- 向下拖动面板实时更新统一进度，在内部滚动已到顶部且未触摸输入框时接管；松手综合位置与速度决定关闭或回弹。输入法由单一 Insets 监听调整可见区域，InsetsAnimation 补偿布局位移，不重新触发入场。任务、快递表单的已改草稿在返回、遮罩、拖动或取消时确认放弃；保存仍只在成功后关闭。
- 嵌套 `NavigationOverlay` 保留父面板的实色背景、原位置及滚动状态，随子面板退出自然露出，底部面板共用原遮罩；不额外淡化父面板，避免底页文字透出。手势取消沿当前进度恢复；子面板退出完成前消费重复返回，不重播父面板入场；课程详情的半屏显示高度不变。
- 课程、调课和课表来源的选择面板与普通弹层一起挂在外层 `windowContent`，不挂入已扣除系统栏边距且参与页面侧滑的 `stage`。系统栏与键盘边距只在弹层处理一次，半屏高度按键盘上方可用区域计算；退出表单时移除选择器，避免重复进入后遗留弹层。`PopupGeometryTest` 覆盖八个选择入口、系统栏、键盘和容器清理，并检查普通及深色底部面板的底边连续。
- API 34 起用隔离的 `MotionBack` 跟随预测性返回并处理取消；API 33 处理提交返回，旧设备保留返回键。抽屉的位移、圆角、菜单显现和遮罩共用进度，路由过程中保留回弹并拒绝重复路由，结束后解除锁。二级页面使用不透明整页侧滑与 8% 父页视差，返回时父页位于退出页面下方，不交叉淡变文字。进入过程中返回从当前帧反向运动；返回动画期间消费重复返回及新的返回手势，完成后恢复交互；从抽屉进入设置或切换栏目时立即替换承载页，仅由抽屉完成整体归位，不叠加页面横向入场；底栏仍直接切换。
- `MotionSystemTest` 通过显式推进动画时间验证中断和清理；真机专项：`adb -s SERIAL shell am instrument -w -e motionSystemOnly true cn.courselens.qa.test/cn.courselens.NativeChecks`，覆盖冷开关、课程详情半屏高度与不透明滑动、嵌套返回、拖动回弹、返回取消、抽屉三模块重复路由、草稿确认、输入法及快递窗口退出。它使用合成任务并输出帧时间与截图，运行前后必须按设备验证章节备份、隔离并恢复 QA 数据。模拟回调的进度检查不等于系统边缘手势的物理验收，低性能设备与厂商键盘仍需实机覆盖。


使用本公开仓库的 `main` 和短期功能分支；重要修改经 PR 和检查后合入。协作权限见 [AGENTS.md](AGENTS.md)。主应用沿用 Java / 原生 View 架构，版本和依赖以 `app/build.gradle` 为准。常规底部内容弹层与表单选择器占可用高度的一半；快递操作与设置弹层按内容展开并限制最大高度，正文不足时不保留半屏空白，内容超出时内部滚动。任务添加/编辑面板按内容展开并预留顶部空间，普通字号下表单与固定取消/保存区完整可见；键盘或大字体导致空间不足时仍允许内部滚动，避免裁切字段。快递添加/编辑面板按内容高度显示并预留顶部空间、固定取消/保存区，字段内部滚动；保存中禁止重复提交，保存成功后切到记录所属分类。已有的本机物流记录仍可展开，不提供在线查询。任务空页优先提供本机添加入口，学习通配置作为次级操作。

安装 JDK 17、Python 3、Android SDK 34 与 build-tools 34.0.0。配置 `ANDROID_HOME`，或在被忽略的 `local.properties` 指定 `sdk.dir`。

```powershell
python tests/run_checks.py
python tests/ReleaseChecksTest.py
python tests/SigningChecks.py
.\gradlew.bat --no-daemon -PqaBuild assembleDebug :app:assembleLiteDebugAndroidTest :app:assembleFullDebugAndroidTest :ocr-plugin:assembleDebugAndroidTest :app:lintLiteDebug :app:lintFullDebug :ocr-plugin:lintDebug
```

Linux 使用 `python3` 和 `sh gradlew`。`run_checks.py` 是本地与 CI 共用的 Java 业务回归入口，另含本实验的学习通解析检查；Gradle 单元测试的 `NO-SOURCE` 不代表它们通过。

普通贡献者使用 `-PqaBuild`：Debug 包名为 `cn.courselens.qa`，使用开发签名；测试数据在测试 APK 中，Release 变体禁用。正式构建保持 `cn.courselens`，必须提供既有签名密钥并通过固定证书检查。

## 快递中心

### 轻量版与完整版

- `lite` / `full` 是轻量版和内置 OCR 完整版两种构建变体，`assembleDebug` 同时生成两种 QA 包。两者保持相同包名、版本号、正式签名及数据格式，可以覆盖安装切换；无需卸载或清空数据。“关于软件 / 检查更新”标明当前类型。QA 仍独立使用 `cn.courselens.qa`，两种 QA 包会互相覆盖，安装前先备份。
- 安装包分别在 `app/build/outputs/apk/lite/` 和 `app/build/outputs/apk/full/`，测试和 Lint 报告也按变体分别保留。两套源只在 `LocalOcr` 接口处不同；`ocr-engine/` 由完整版和独立插件复用，轻量版不引用该库。完整版直接识别，轻量版已安装插件时直接打开图片选择器，只有未安装时进入下载页。
- PR CI 构建、测试轻量版和 OCR 插件，不构建完整版。发布草稿生成 `Morrow-VERSION-lite-vcCODE.apk`、`Morrow-VERSION-full-vcCODE.apk` 及独立 OCR 插件，校验文件同时覆盖三份 APK。公开发布之后才配置 `remote/config.json` 的 `android.liteApkUrl` / `android.fullApkUrl`；旧客户端的 `apkUrl` / `diskUrl` 继续保留。新版更新按当前类型选择链接，缺少对应地址时打开 Releases 让用户选择，不自动切换类型。
- 官网明确提供两种下载按钮并说明区别。当前历史 2.7.5 APK 已内置 OCR，因此只作为完整版地址；轻量版新产物未公开前其链接保持空，不拿旧完整版冒充轻量版。

### 可选图片识别插件

- `ocr-plugin/` 是轻量版可选安装的独立应用，正式包名 `cn.courselens.ocr`，QA 为 `cn.courselens.ocr.qa`。与主包使用相同签名配置，但独立安装、卸载和版本管理。轻量版主包不依赖 ML Kit、不包含 OCR 模型或原生库；通知、粘贴文字和手动录入无需插件。插件不声明网络权限，不上传截图。
- 「读取信息」和「识别设置」提供插件入口。主包从 `https://morrow.xvyan.cn/remote/ocr-plugin.json` 读取 HTTPS 下载地址、字节数、SHA-256、插件版本和协议。未发布时 `enabled: false`；不使用占位 APK 地址。下载交给系统 DownloadManager，界面显示进度、等待网络和失败重试，离开页面后可继续下载，再次进入恢复进度。下载后复制到私有目录，校验大小、SHA-256、包名、主包签名和协议 1，再临时授权系统安装器读取该 APK；取消下载会清除任务和安装缓存。QA 不下载正式插件，需安装同签名 QA 产物。
- Android 首次安装需用户允许 Morrow 安装应用，并在系统安装器确认；不静默安装。安装返回后重新检查插件并继续选择 / 识别图片，取消或拒绝授权可重试。插件只接受同签名且包名匹配的主包调用，读取单个临时 `content` URI，返回有长度和坐标边界的文本行，由主包沿用 `ParcelRules.extractImage` 提取快递信息；不传整张位图或保存截图。
- `.\gradlew.bat --no-daemon -PqaBuild assembleDebug :app:assembleLiteDebugAndroidTest :app:assembleFullDebugAndroidTest :ocr-plugin:assembleDebugAndroidTest :app:testLiteDebugUnitTest :app:testFullDebugUnitTest :app:lintLiteDebug :app:lintFullDebug :ocr-plugin:lintDebug` 同时检查主包和插件。`python tests/OcrPluginChecks.py --host app/build/outputs/apk/lite/debug/app-lite-debug.apk --plugin ocr-plugin/build/outputs/apk/debug/ocr-plugin-debug.apk` 检查同签名、包名、模型隔离和插件无网络权限。正式发布工作流额外生成插件 APK 和 `ocr-plugin.json` 至草稿产物；公开插件后才把已验证的描述文件通过 PR 更新到 `remote/ocr-plugin.json`。下载地址可使用经审核的 HTTPS 国内对象存储，内容必须与校验值一致。准备产物不代表上传、公开发布或下载链路验收。
- QA 下载测试可额外使用 `-PqaOcrManifest=http://127.0.0.1:8765/ocr-plugin.json`，通过 `adb -s SERIAL reverse tcp:8765 tcp:8765` 连接仅监听电脑回环地址的临时文件服务。描述文件必须指向 `cn.courselens.ocr.qa` 同签名插件；正式包拒绝 QA 插件及 HTTP 地址。只有 debug 网络配置放行精确 `127.0.0.1`，不放行局域网域名，也不部署 QA 下载配置到官网。安装 QA 主包和测试包后，可运行 `adb -s SERIAL shell am instrument -w -e ocrDownloadOnly true cn.courselens.qa.test/cn.courselens.NativeChecks` 启动下载专项；系统安装授权与确认仍需在手机界面完成，专项不打开业务页面或写入快递数据库。测试完删除 ADB 转发并停止服务，使用此测试包继续下载前需重新建立测试源。
- 发布前验收：未安装入口、下载失败 / 取消 / 网络恢复、进后台 / 进程重建后恢复、损坏及异签名文件拒绝、拒绝安装权限、取消安装、安装成功后识别、卸载回退、图片分享、协议不兼容、深色 / 大字体。设备测试使用合成截图和隔离 QA 数据。


- 入口在底部导航的「快递」，也可由 Android 文字 / 图片分享打开。三个底栏入口在主界面与快递页共用 `AppNavigation`，选中使用实心图标；返回课表 / 任务时复用主 Activity、保留原页面和相应选中项，并清除其上的快递页，避免返回键循环。独立 `ParcelActivity` 使用现有 `SecondaryUi`、原生 View 和半高滚动表单，头部菜单与添加按钮共用全应用 `SecondaryUi` 圆形图标样式，浅色沿用原色板，浅色与深色均复用共享色板。没有迁移课表、学习通缓存或账号数据，也没有改包名、版本号或正式签名。
- 快递页使用单一快递列表，不按状态或驿站分类，不左右翻页。点击条目内容内联展开编辑、删除、复制取件码、提醒及已有物流记录，同一时间只展开一条；展开后的「已取件」按钮标记完成，完成后保留在同一列表末尾，地址与取件码使用灰色、取件码加删除线，不另显示完成状态文字；提供 5 秒撤销和展开后的「恢复未取件」，恢复不自动重启旧提醒；既有已取件记录同样可见。未核对的识别结果在同一列表提示核对，确认前不能标记完成。顶部保留淘宝 / 菜鸟 / 拼多多官方身份码入口；「手动添加」直接打开表单，「读取信息」提供粘贴文字、选择截图及直达通知读取设置。截图读取结果回填文本，失败时保留文字和面板内反馈，便于修正或手动添加。填写取件码或运单号即可保存，沿用原有状态字段及去重规则；不提供在线物流查询。全部数据删除仍在总设置 → 数据管理。

- `ParcelDatabase` 是独立 Room schema 2 数据库 `parcels.db`，schema 保存在 `app/schemas/`。Java 使用 annotationProcessor，无 Compose / Hilt / Kotlin 插件；Room 2.6.1、WorkManager 2.9.1 保持现有 SDK 34 / JDK 17 构建兼容。升级 schema 时添加保留数据的 migration，不使用 destructive fallback。取件码和运单号分别保存；同运单或同地址同码去重，同码不同驿站保留。完成记录不因通知复活。运输中记录收到同运单取件通知时补全原记录，低评分信息需确认。单条删除移入本机快递恢复，并保留最长 14 天的 SHA-256 去重摘要；可从快递菜单或数据管理恢复，恢复不重启提醒，删除全部会清除记录、摘要和设置，并执行 Room 的 WAL checkpoint / VACUUM。
- `ParcelRules` 是本地版本化规则（当前 v2），评分是规则强度而非统计概率。只提取明确取件语境，排除验证码和动态身份码。只有单码、明确地址的高评分通知可以直接进入待取件；其他结果在快递列表中标记为待核对，由用户人工核对，截图 / 分享一律确认。通知 / 粘贴多码文本不自动配地址或运单，避免串件；截图按 ML Kit 文本行坐标关联标签、码值和附近取件点，同一标签含多码时仍留空地址待补。截图局部区域排除身份码 / 验证码，不因整屏其他位置出现无关安全词而丢弃有效取件码。没有保存通知全文或图片，没有短信、无障碍、相机或媒体库读取权限。截图使用文档选择器 / 分享授权和独立可选插件内的中文 ML Kit；只接收 content URI，限 10 MB / 8000 万输入像素并下采样处理，识别后释放图片和识别器。
- 通知识别默认关闭，需 App 内同意和系统通知访问两步授权；已授权时再次开启不重复跳转系统页面，返回后更新授权状态。开启后自动识别菜鸟、淘宝、拼多多、微信、QQ 及默认短信应用的**通知**，不需要选择来源；系统未返回默认短信应用时，已安装的 `com.android.mms` 可作为系统短信来源。不访问短信数据库。`ParcelNotificationListener` 由系统绑定权限保护，不扫描历史通知、不启动前台服务保活。关闭或清空数据会使排队中的旧识别结果失效。通知显示权限和读取通知访问相互独立。Android 15 及以上的敏感通知过滤、侧载应用受限设置、工作资料隔离和厂商限制都可能导致通知正文不可见；提供手动 / 分享入口，不绕过系统限制。
- `ParcelIdentity` 只打开包限定的官方路由，捕获跳转失败后尝试打开应用首页，不弹提示。深链成功发出 Intent 不证明已到身份码页面。拼多多入口通过 `tab=ID_CODE&entry_source=11` 指定身份码页签，已在小米 14 / Android 16 的拼多多 8.3.0 验证；淘宝、菜鸟及其他版本仍需分别验收，不据此称为当前所有版本有效。页面不生成、读取、抓取或缓存官方动态令牌。
- 在线物流查询与代理配置已移除。打开快递页时取消旧版 `parcel-tracking` 后台任务，并清除旧代理、后台查询开关与退避设置；旧版已排队的查询任务直接结束。保留已有物流字段，只供本机记录展示与旧数据兼容，不再发出物流查询请求。
- 新识别和自定义未领取提醒共用独立通知频道，默认提醒关闭；Android 13 起只在用户开启提醒时请求 `POST_NOTIFICATIONS`。通知不包含码值和地址，按记录替换，识别去重避免重复推送；完成 / 删除取消通知。未领取提醒用 WorkManager 每 15 分钟检查。无精确闹钟 / 前台保活，不承诺准点；强行停止、Doze、重启和厂商省电均需实机检查。关闭开关取消对应周期任务。
- `ParcelWidget` 独立于课表小组件，支持系统固定请求与手动添加。默认只显示待取数量；显示取件码与地址需在快递设置中主动开启。数据库修改后刷新，支持深色资源和 sp 字号。真实启动器添加、尺寸、大字体、点击与隐私开关需实机验收，不能以 `requestPinAppWidget` 返回值充当添加成功。

### 参考来源与许可边界

2026-10-25 重新核查了下列固定来源。取件识别采用可选插件内的 ML Kit 中文 OCR 与 `shareven/parcel` 的 MIT 取件标签、地址规则局部适配，未整体合并第三方应用。已在源码标注来源，并在 `app/src/main/assets/licenses/shareven-parcel.txt` 保留上游版权和许可全文；第三方许可不改变 Morrow 本身的许可状态。方案比较见 [取件识别调研](docs/parcel-recognition-research.md)。

| 来源 | 核查文件与兼容性 | 使用方式 |
| --- | --- | --- |
| [shareven/parcel @ d7e4c97](https://github.com/shareven/parcel/tree/d7e4c974ffd1e4de2da9291df0d295856cd89818) | MIT；`util/SmsParser.kt`、`util/ThirdPartyEntry.kt`、`service/ParcelNotificationListenerService.kt`、`app/build.gradle.kts`；Kotlin / Compose，minSdk 29 / compileSdk 35 | 适配明确取件标签与地址前缀，保留 MIT 声明；不移植其短信权限、保活或完整 UI |
| [pickup-code-app @ f5075d3](https://github.com/zixij644-elaborate/pickup-code-app/tree/f5075d3e4d2f4ffe4da259c699f472a790ec7013) | GPL-3.0；`extractor/CodeExtractor.kt`、`util/IdentityCodeLauncher.kt`、Gradle；Compose，minSdk 26 / compileSdk 35 | 仅参考确认流程、来源评分和包限定路由事实；未复制或翻译 GPL 实现 |
| [ExpressOCR @ b1aad68](https://github.com/baso4/ExpressOCR/tree/b1aad68b51bf8dc4b2bdead655489a99691647c1) | MIT；`MainActivity.kt`、`CameraActivity.kt`、Gradle；CameraX / ML Kit，minSdk 26 / compileSdk 35 | 参考截图 → 本机 ML Kit 的流程；直接使用官方 SDK，自行处理大小限制和释放；不采用敏感日志、相机扫描 |
| [KdAPI_Demo @ 603063c](https://github.com/APIGI/KdAPI_Demo/tree/603063c8a8e0a2acbf75f4de23def39f78982711) | 未找到 LICENSE；`2-Services/OrderService.cs`、README；.NET Core 3.1，不能直接作为 Android 库 | 仅核对服务端签名 / 查询职责，未移植代码、示例凭据或签名算法 |

Android 实现依据：[Room Java 配置](https://developer.android.com/training/data-storage/room)、[ML Kit 中文识别](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)、[NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)、[Android 13 通知权限](https://developer.android.com/develop/ui/views/notifications/notification-permission)、[Android 15 行为变化](https://developer.android.com/about/versions/15/behavior-changes-all)、[WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started)。这些依赖有各自许可和条款，不代表 Morrow 采用相同项目许可。

### 快递测试

```powershell
python tests/run_checks.py
.\gradlew.bat --no-daemon -PqaBuild assembleDebug :app:assembleLiteDebugAndroidTest :app:assembleFullDebugAndroidTest :ocr-plugin:assembleDebugAndroidTest :app:testLiteDebugUnitTest :app:testFullDebugUnitTest :app:lintLiteDebug :app:lintFullDebug :ocr-plugin:lintDebug
adb -s SERIAL shell am instrument -w -e parcelOnly true cn.courselens.qa.test/cn.courselens.NativeChecks
```

`ParcelRulesTest` 使用合成语料及 OCR 行坐标，覆盖标签别名、换行、多码、复制按钮干扰、同屏身份码和电话 / 日期排除；主包的 `parcelOcrOnly` 仪器专项验证插件结果协议与人工确认边界，不再调用识别模型。真实中文识别模型由独立 QA 插件专项验证：`adb -s SERIAL shell am instrument -w cn.courselens.ocr.qa.test/cn.courselens.ocr.OcrChecks`，只生成合成图片、不写快递数据库；该项不能代替主包下载 / 安装 / 回传的端到端验收。`ParcelAndroidTest` 用 Robolectric 模拟 Android 13 / 14 / 15，检查 Room 去重、运输中补全、删除/完成防复活、权限开关、旧查询任务取消与提醒保留。Android 16 需 JDK 21，可使用 `-PparcelTestSdks=33,34,35,36`；默认 JDK 17 不运行 API 36，不把未运行版本算通过。`ParcelChecks` 仪器专项使用唯一临时数据库，测试包名必须为 QA；设备安装和备份仍按「设备验证」执行。CI 运行 JVM / Robolectric 检查并保留报告，不运行设备测试。

每个可用 Android 13 / 14 / 15 / 16 及以上的 QA 设备还需检查：首次拒绝/允许通知、频道关闭、通知访问授权/撤销与重新绑定、后台识别关闭后无写入；不同来源/重复通知/敏感通知过滤；分享 grant、过大图片、截图 OCR 和多码确认；深色及 2 倍字体；Doze / 重启 / 强行停止后的提醒延迟和提醒开关；真实启动器固定请求、尺寸、取件后刷新和隐藏码值。无设备时只能交付构建及模拟结果，不能宣称这些端到端检查通过。64 位 ML Kit 原生库的 16 KiB ELF / APK 对齐检查也不能代替 Android 15+ 16 KiB 页设备运行。

## 多份课表与学校适配

- 当前课表 ID 和名称列表放在 `timetables` SharedPreferences；原有课表继续使用 Activity 原偏好文件，不复制或改写已有课程格式。设置中的当前课表与切换列表支持长按名称改名，名称校验沿用 1–40 个字；只更新索引，不搬移课程数据。从设置进入新增页，取消或手动创建后返回设置；首页入口返回课表页。新增课表使用独立偏好文件，课程、学期、备注、冲突选择、调课与 `classTimes` 随课表切换。小组件读取同一当前 ID，旧课表的小组件点击不会误匹配新课表。学习通账号和作业仍使用原有独立存储。
- `ClassTimes` 以每行 `HH:mm-HH:mm` 保存作息，1–30 节，检查顺序、重叠与课程节次上界。作息页按节次显示上课 / 下课时间，使用 24 小时选择器，支持新增一节与移除末节，并沿用保存 / 放弃草稿提示。未保存该字段的旧课表沿用宿迁学院 12 节作息。新增可手动创建或选择学校导入；徐州工程学院通过个人课表接口读取已选课程及各校区日节次，支持分段周次、单双周、单周调课与无时间实践备注。不同校区作息不一致时明确拒绝合并，保留旧课表。
- 徐工院仅放行 `jwglxt.xzit.edu.cn` 的标准 HTTP/HTTPS 端口，禁止跨域或 URL 用户信息，密码使用学校 RSA/PKCS1 协议；登录成功后按课表使用 Android Keystore 加密保存账号密码，供静默刷新，不保存 Cookie。Android 网络配置只对该精确域名允许 HTTP，不包含子域，其余流量保持 HTTPS。该校当前 HTTP 服务不提供传输完整性和会话保密性，登录页及 README 明示限制；HTTPS 可用后应移除此例外。
- 使用 `-e timetablesOnly true` 运行多课表与徐工院合成专项，涵盖旧数据保留、新建与切换、重建恢复、14 节编辑与小组件、RSA/解析及域名边界；测试只使用临时合成课表，结束恢复索引。真实账号读取只验证第三方链路，不把私人课表或凭据放入测试资产。
- 学校课表同步逻辑保留，课表管理页不显示刷新入口；`schoolImported` 随各课表保存，旧数据通过学校同步备注识别。每个课表绑定各自学校账号：登录成功后使用 Android Keystore 加密保存，刷新先复用当前会话，会话过期或进程重启后读取凭据静默登录；验证码、凭据失效或学校不匹配时进入登录页，账号可预填、密码不回填。清空当前课表时删除该课表保存的账号。仅刷新成功后覆盖课表；失败、无效数据或页面已切换时保留原课表。`timetablesOnly` 同时覆盖刷新入口、后台读取、成功保存、失败保留、静默登录、验证码回退和凭据存取。

## 设备验证

先核对 `adb devices -l` 的目标序列号，备份 QA 包已有偏好。只覆盖安装 QA 主包和测试包，不卸载用户正式包。

```powershell
adb -s SERIAL install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/lite/debug/app-lite-debug-androidTest.apk
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

### 复用隔离发布模拟器

在 Windows 上复用专用于发布验证的 AVD：

```powershell
.\tests\start_release_emulator.ps1 -AvdHome "C:\codex\CourseLens-backups\release-2.7.8-verification\avd"
```

脚本只启动或复用名为 `morrow-release-test` 的模拟器，不清空数据、不安装 APK；返回的 `emulator-5556` 是后续 adb 目标，不能替换为连接的真实手机。可以通过 `-AvdHome`、`-AvdName`、`-Port` 指定已建立的隔离 AVD；缺失 SDK 或 AVD 时先安装并创建，已有模拟器镜像及系统授权沿用，不重复下载。每次验证前确认该 AVD 只含合成数据，旧正式包、测试数据和新版签名 APK 分别核对来源与证书；按实际升级路径覆盖安装并验证课表、任务、快递与数据库迁移。脚本启动成功不是覆盖升级或真人体验验收。

## GitHub Actions

- `Android checks`：必需检查始终名为 `checks`。仅当 PR 或 main push 的差异严格只有 `remote/config.json` 时，运行配置和网站验证；空差异、无法判断、混合改动和手动运行均执行完整 Android 检查。完整检查缓存 Gradle 依赖，验证无密钥签名边界、QA 主/测试 APK、单元测试和 Lint；不执行设备测试。
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

1. 合入已批准并完成适用设备验收的源码；提高 versionCode 和 versionName。保留包名、偏好名称、存储格式及签名指纹；修改存储时另做旧数据兼容检查。提前并行核对版本与标签、准备隔离模拟器及合成数据；本地独立检查与远端 CI 可并行，不重复运行已通过且源码未改变的检查。
2. 经发布授权后，main 必需检查通过即运行 `Prepare signed release`，选择 main 和与 versionName 对应的新标签；在授权范围内处理 release 环境审核。签名构建和独立的隔离覆盖升级准备可并行，检查结果中的来源提交必须是获准提交。
3. 核对草稿 APK、`SHA256SUMS`、`release-metadata-lite.json` / `release-metadata-full.json`、证书及使用说明，完成隔离覆盖升级验证；经明确授权后公开草稿。现有标签和 APK 不替换。
4. 发布成功且 APK 可下载后，再通过 PR 更新 `remote/config.json` 中的版本、更新日志和 APK 地址，保留公告及人工维护的网盘地址。CI 成功或草稿 Release 不等于发布成功。
5. 核验 Pages 与 Raw 均返回 200，配置与 Git blob 一致；检查 APK 下载及旧客户端更新入口。网盘分享由维护者核对内容和有效期。

本地正式构建按 `signing.properties.example` 配置被忽略的签名属性，密钥保存在仓库外；执行 `python tests/SigningChecks.py --signed`、正式 `assembleRelease :app:lintLiteRelease :app:lintFullRelease :ocr-plugin:lintRelease`。`python tests/ReleaseChecks.py vVERSION --apk PATH --edition lite`（完整版改为 `--edition full`）使用 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 检查产物，输出到 `app/build/release/`，不发布。

## 公告配置与旧版兼容

网站介绍页在 `web/`，使用静态 HTML/CSS；`python tests/WebsiteChecks.py` 检查下载地址、站内锚点、图片属性及本地资源。预览时先运行 `python web/build.py`，再以 `preview/site/` 为站点根目录启动静态服务器。`Remote configuration` 在相关文件变化时检查页面与配置，main 部署时运行 `python web/build.py site`，将网站和 `remote/config.json` 打包到同一 Pages 站点，配置路径仍为 `/remote/config.json`。三个下载入口由 `web/download.js` 在点击时读取同站点的 `remote/config.json`，校验 `android.apkUrl` 后从 GitHub 直接下载 APK，不调用 GitHub 查询 API。最新版以更新配置为准；正式 APK 发布后按发布流程更新配置即可。配置请求超时、失败或地址无效时使用构建时的同版本地址下载；构建时从同一配置生成按钮的直接 APK 地址，禁用 JavaScript 时仍可下载对应版本。`node tests/WebsiteDownloadChecks.cjs` 检查下载选择、失败处理及重复点击。图片仅使用经过检查的合成示例，原始手机截图与设计验收截图保存在仓库外。

当前源码仅请求 `https://morrow.xvyan.cn/remote/config.json`，不再尝试 GitHub Pages 或 Raw 备用接口。连接、HTTP 或配置校验失败时保留有效缓存，并沿用检查失败提示；请求仍不跟随 HTTP 重定向。

仓库入口及配置中的 `repositoryUrl` 统一为 `https://github.com/fangfar/Morrow`，APK 下载链接使用新仓库路径，已发布资产的文件名保持实际名称。保持 `schemaVersion: 1` 和 `remote/config.json`。新版 App 的精确仓库校验与新配置必须配套部署；旧缓存中的仓库地址不再接受。已发布旧客户端的接口和仓库校验固化在 APK 内，无法由此次源码修改替换；旧版升级路径需在发布前单独核验。

公告 ID 唯一；修改同一 ID 不会重新提醒，需要新公告时用新 ID。配置最多 64 KiB / 20 条公告，链接为 HTTPS，版本以实际 versionCode 判断；检查失败保留有效缓存，不冒充“已是最新版”。线上配置校验与 App 的 `remoteOnly` 专项分别验证。

新打开 Activity 时在首帧后检查更新，从后台重新打开也会检查，绕过常规六小时缓存；Activity 重建沿用进程内请求和提醒状态。“设置 → 关于软件 → 软件更新”二级页面提供自动检查开关、手动检查及更新状态，关于页只显示更新入口，并提供官网与 GitHub 仓库入口，不显示网盘下载。自动检查开关默认开启，保存在独立 `appSettings` 偏好中，不随课表切换。关闭后不自动提示普通更新，但启动仍读取配置以判断重大更新策略。更新弹窗的“取消”只抑制本次打开；公告已读状态保持原行为。`remoteOnly` 覆盖请求、开关持久化、更新子页、强制提示与本地开关、过期策略、已安装版本、远程文案和取消去重。

更新配置沿用 `remote/config.json` 的 `android` 对象及 `schemaVersion: 1`，新增字段均可省略，旧客户端会忽略它们：

```json
{
  "forcePrompt": false,
  "promptTitle": "",
  "promptMessage": ""
}
```

`forcePrompt: true` 会在存在较新版本且配置未过期时，绕过本地自动检查开关显示更新弹窗，每次打开最多一次；它不强制安装，也不锁住 App。“取消 / 立即更新”继续可用。`promptTitle`（最多 160 个 UTF-16 单元）、`promptMessage`（最多 12000 个 UTF-16 单元）分别配置弹窗标题和正文，空文本回退到版本标题和 `changelog`。布尔值和文本类型严格校验，无效配置保留有效缓存。发布重大更新时先按发布流程发布 APK，再更新版本、下载地址、文案及 `forcePrompt`；不提高版本号时，已安装该版本的客户端不会提示。当前配置默认关闭强制提示，不代表已发布新的 APK。

## 原私有仓库归档条件

从公开 main 干净检出通过构建与业务回归、关键设备验收、签名和覆盖更新验证；确认必要文件、受控发布 secrets 及密钥备份齐全后，才切换本地远程。保留私有 Git 历史的安全备份，经单独授权后保持私有并归档，不删除。

公开历史和 v2.6 APK 曾包含真实样例课表。新源码改为合成测试数据不会清除旧记录；历史清理及旧发布资产处理需单独评估和授权，不以镜像推送、强推或重新公开私有历史解决。
