# 快递取件信息识别方案调研

核查日期：2026-10-25。目标是为现有 Java / 原生 View 快递单列表识别取件码、取件点及可确定的运单号，保留官方身份码入口。本文件记录方案依据，运行和发布流程仍以 `DEVELOPMENT.md` 为准。

## 选定方案

采用 **Google ML Kit Text Recognition v2 的随包中文模型 + shareven/parcel 的 MIT 文本规则局部适配**。前者负责截图中文字与位置，后者补齐实际快递文本的标签和地址写法；Morrow 继续负责候选确认、校验、去重、完成状态和本机保存。没有查到可直接替换整个功能、同时可靠处理任意快递截图并覆盖所有字段的 Java 成品库；不把参考项目的发布次数当作识别准确率证明。

ML Kit 官方当前 Android 文档提供 Java 接入，推荐随包中文依赖 `com.google.mlkit:text-recognition-chinese:16.0.1`，与本项目已安装版本一致；模型随包可立即使用，避免首次动态下载导致无结果。官方 API 提供文本块、行和元素及坐标，可用于按空间邻近关系找到标签对应的码值。[官方接入](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)、[Text.Line API](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line)、[Text.TextBlock API](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.TextBlock)

## 候选核查

| 候选及固定提交 | 能力与维护证据 | 对 Morrow 的取舍 |
| --- | --- | --- |
| [shareven/parcel @ d7e4c974ffd1e4de2da9291df0d295856cd89818](https://github.com/shareven/parcel/tree/d7e4c974ffd1e4de2da9291df0d295856cd89818) | `SmsParser` 处理地址、取件码、多码、柜号和格口；有柜号解析测试。GitHub 有连续的 v1.0.68 / 1.0.69 / 1.0.70 发布，最新发布为 2026-10-12，项目未归档。 | 选择明确取件标签及地址前缀，适配为现有 Java 规则；不搬运 Compose 页面、短信权限、后台保活或自定义规则管理。 |
| [baso4/ExpressOCR @ b1aad68b51bf8dc4b2bdead655489a99691647c1](https://github.com/baso4/ExpressOCR/tree/b1aad68b51bf8dc4b2bdead655489a99691647c1) | MIT；截图实现按 OCR 行提取三段数字码；另有 CameraX 扫描。构建版本仍为 1.0。 | 仅能作为码型识别参考；没有完整取件点/运单提取，源码会记录码值日志，不作为整套成熟方案移植。 |
| [pickup-code-app @ f5075d3e4d2f4ffe4da259c699f472a790ec7013](https://github.com/zixij644-elaborate/pickup-code-app/tree/f5075d3e4d2f4ffe4da259c699f472a790ec7013) | 仓库许可为 GPL-3.0，包含独立提取器和测试。 | 不复制或翻译其实现到本次改动，不借本次识别接入改变 Morrow 项目许可。 |

核查依据：[shareven 解析器](https://github.com/shareven/parcel/blob/d7e4c974ffd1e4de2da9291df0d295856cd89818/app/src/main/java/com/xxxx/parcel/util/SmsParser.kt)、[解析测试](https://github.com/shareven/parcel/blob/d7e4c974ffd1e4de2da9291df0d295856cd89818/app/src/test/java/com/xxxx/parcel/SmsParserLockerNumberTest.kt)、[发布记录](https://github.com/shareven/parcel/releases)、[Android 配置](https://github.com/shareven/parcel/blob/d7e4c974ffd1e4de2da9291df0d295856cd89818/app/build.gradle.kts)；[ExpressOCR 截图实现](https://github.com/baso4/ExpressOCR/blob/b1aad68b51bf8dc4b2bdead655489a99691647c1/app/src/main/java/com/oz/expressocr/MainActivity.kt)、[构建配置](https://github.com/baso4/ExpressOCR/blob/b1aad68b51bf8dc4b2bdead655489a99691647c1/app/build.gradle.kts)；[pickup-code-app 许可](https://github.com/zixij644-elaborate/pickup-code-app/blob/f5075d3e4d2f4ffe4da259c699f472a790ec7013/LICENSE)。

## 适配范围和边界

- 文本入口复用通知、粘贴和 Android 分享。沿用现有通知来源授权；不因采用短信文本规则而增加短信数据库读取权限。取件码、地址和运单号分别处理，避免将电话、时间、订单号当取件码。
- 对 shareven 的规则做有选择的适配：补充明确的取件标签和地址前缀。其原始标签还包含可能与运单混淆的写法，不能不经校验整份照搬；柜号/格口与普通取件码也不应混为一类。这里的取舍来自对上述 `SmsParser` 源码的检查。
- 截图使用已有中文 OCR，利用 `Text.Line` 的边界框处理跨文本块、标签与码值分离、右侧“复制”等按钮干扰；标签附近的取件区域单独交给解析器。此邻近匹配是 Morrow 的适配层，不能声称上游提供现成淘宝截图模板。[官方布局 API](https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.Line)
- “身份码”“验证码”等安全上下文按候选区域判断，避免整屏有无关安全词就丢弃另一区域的有效取件码。仍排除动态身份令牌，身份码按钮继续跳转官方应用。截图和分享候选应先核对，无法可靠关联多码地址时留空待补。
- 保留原有 Room schema、候选确认与去重流程。手动添加是识别失败时的直接恢复路径，不通过自动保存低可信结果掩盖错误。

上述适配边界是针对本项目的工程判断，并非第三方识别率承诺。低清晰度、遮挡、字形混淆和平台版式变化仍需真实设备样本验收；测试必须区分 OCR 文字是否正确与字段提取是否正确。[官方图像质量说明](https://developers.google.com/ml-kit/vision/text-recognition/v2/android#input-image-guidelines)

## 许可与隐私

直接适配 shareven 的源码规则时，在对应代码标注固定来源，随 APK 保留上游版权及 MIT 许可全文，例如 `app/src/main/assets/licenses/shareven-parcel.txt`。这是所采用第三方部分的许可，不意味着 Morrow 整个项目获得或改为 MIT。[shareven 固定许可](https://github.com/shareven/parcel/blob/d7e4c974ffd1e4de2da9291df0d295856cd89818/LICENSE)

ML Kit SDK 有其独立服务条款。官方说明图片、文字及识别输出在设备上处理，不上传这些输入输出；SDK 仍可能联系 Google 获取兼容性/更新信息并发送性能和使用指标。因此可说明本机识别，不应宣传为整个 SDK 完全不联网。[ML Kit Terms & Privacy](https://developers.google.com/ml-kit/terms)

## 验收建议

用合成数据覆盖：显式取件码、多种取件标签、没有冒号、空格/横杠/换行、标签与码值分块、右侧复制按钮、同屏身份码提示、多个包裹、只有运单号，以及电话/日期/验证码等负例。随后在设备上对用户授权的原图调用实际 ML Kit，核对候选码与取件点；不把图片上传、提交入仓库或输出私人全文。保留身份码跳转、核对后保存、重复读取去重、完成后不复活、手动添加与旧数据兼容的回归。
