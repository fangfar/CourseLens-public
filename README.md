# CourseLens · 课表镜

轻量的 Android 课表应用，支持按周查看课程、课程详情、手动编辑、单次调课与停课、冲突课程选择和桌面小组件。已导入的课表可离线查看。

支持 Android 8.0 及以上；学校同步目前适配宿迁学院，其他学校可手动添加。课表镜是独立应用，并非学校官方应用。

## 下载与开始使用

当前正式版本为 **2.6**。在 [GitHub Releases](https://github.com/fangfar/CourseLens-public/releases/latest) 下载正式 APK，版本更新说明和校验文件也在 Releases。已有用户优先覆盖安装；若无法覆盖，先保存重要数据，不要直接卸载旧应用。

1. 从右上角更多菜单进入“学校登录”导入，或在“课程管理”中手动添加课程。
2. 在“完整设置”确认开学日期；左右滑动切换教学周，点击课程查看详情。
3. 通过课程详情或课程管理校对安排。重新同步会以学校数据覆盖手动修改；取消编辑不保存。
4. 在完整设置的小组件入口申请添加，或从桌面系统的小组件列表选择课表镜。提供 2×2、4×2、4×4 尺寸，实际占用格数取决于启动器。

小组件读取本机安排，支持调课、停课和冲突选择；点击课程进入详情，点击顶部背景刷新。修改课表后更新，平时由系统约每 30 分钟请求更新，省电模式可能延后。部分手机使用快捷添加时可能需要额外桌面设置，也可从系统列表手动添加。

教学周支持 1–30 周，节次支持 1–12 节；内置学校作息表，导入后请核对课程、周次和地点。

## 数据与隐私

课表、备注和学期设置保存在本机。学校账号密码不长期保存，仅主动同步时向学校服务发送登录信息；同步失败保留已有课表。应用通过 HTTPS 读取公开公告及版本配置，不上传课表、账号或密码。

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
