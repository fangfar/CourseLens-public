# CourseLens · 课表镜

轻量的 Android 课表应用，支持按周查看课程、课程详情、手动编辑、单次调课与停课、冲突课程选择和桌面小组件。已导入的课表可离线查看。

支持 Android 8.0 及以上；学校同步适配宿迁学院和徐州工程学院，其他学校可手动添加。

## 下载与开始使用

在 [GitHub Releases](https://github.com/fangfar/CourseLens-public/releases/latest) 下载正式 APK，版本更新说明和校验文件也在 Releases。

1. 从右上角更多菜单进入“新增课表”，手动创建或选择学校导入；通过“切换课表”保留并使用多份课表。已有课表可继续从“学校登录”更新，或在“课程管理”中手动添加课程。
2. 在“完整设置”确认开学日期；左右滑动切换教学周，点击课程查看详情。
3. 通过课程详情或课程管理校对安排。重新同步会以学校数据覆盖手动修改；取消编辑不保存。
4. 在完整设置的小组件入口申请添加，或从桌面系统的小组件列表选择课表镜。提供 2×2、4×2、4×4 尺寸，实际占用格数取决于启动器。

小组件读取本机安排，支持调课、停课和冲突选择；点击课程进入详情，点击顶部背景刷新。修改课表后更新，平时由系统约每 30 分钟请求更新，省电模式可能延后。部分手机使用快捷添加时可能需要额外桌面设置，也可从系统列表手动添加。

教学周支持 1–30 周，每天支持 1–30 节。各课表独立保存课程、备注、学期、调课、冲突选择和作息，小组件跟随当前课表。徐州工程学院从个人课表读取已选课程和对应校区作息，支持 14 节课；无具体时间的实践课程保留在备注中。在完整设置的“作息时间”中可按实际节数调整，导入后请核对课程、周次和地点。

## 学习通作业

在“任务”页配置学习通账号密码，验证登录成功后加密保存在本机。点击“刷新作业”查看课程名称、作业标题、提交状态和截止时间，可按待处理、已完成或全部筛选。

读取结果保存在本机，退出或重启后仍可查看。进入页面不会自动刷新；成功读取的课程更新，失败课程保留本机作业，问题在列表末尾说明。切换账号后使用对应账号的本机数据。截止时间未知或列表存在分页时会明确说明；目前仅支持手机号 / 超星账号，验证码和机构认证尚未适配。

以上介绍对应当前源码，已发布版本的功能以 Releases 说明为准。

## 数据与隐私

课表、备注和学期设置保存在本机。学校账号密码不长期保存，仅主动同步时向学校服务发送登录信息；同步失败保留已有课表。学习通账号密码在验证成功后使用 Android Keystore 加密保存，仅在主动验证或刷新时用于学习通认证；作业数据保存在应用私有目录。应用通过 HTTPS 读取公开公告及版本配置，不向公告或更新服务上传课表、账号或密码。

徐州工程学院当前教务入口使用 HTTP，仅对该校域名开放连接；密码按学校协议使用 RSA 加密，但账号、课表和会话传输未受 HTTPS 保护。请在可信网络中同步。

课表不会通过系统备份自动恢复或迁移到新手机。更换手机、卸载或清空应用数据前，请保存重要信息。

## 问题反馈

在 [Issues](https://github.com/fangfar/CourseLens-public/issues) 提供版本、机型、Android 版本及复现步骤；请勿上传账号、密码、私人课表或完整个人截图。


## Star History

<a href="https://www.star-history.com/?repos=fangfar%2Fcourselens-public&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=fangfar/courselens-public&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=fangfar/courselens-public&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=fangfar/courselens-public&type=date&legend=top-left" />
 </picture>
</a>
