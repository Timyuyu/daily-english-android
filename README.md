# Daily English Android V1.8

[下载 V1.8 APK、完整源码和男声试听](https://github.com/Timyuyu/daily-english-android/releases/tag/v1.8)

本仓库是私有仓库，下载需要登录拥有访问权限的GitHub账号。APK可覆盖安装，保留本机数据，不要先卸载。

克隆源码前安装Git LFS；模型和下载包由Git LFS管理。如果模型仍是文本指针，先运行 `git lfs pull`。版本页的完整源码ZIP包含离线模型，解压后不依赖Git LFS。


简洁原生安卓口语练习应用，面向 vivo X300 / OriginOS 6 / Android 16。最低 Android 15（API 35），目标 Android 16（API 36）。

## V1.8 修复与数据导出

- 设置新增“导出数据包”：Markdown与ZIP两种格式。通过系统文件保存窗口选择位置，默认文件名为最新数据包加北京时间时间戳。无需全部存储权限，保存后可在文件管理中分享。
- 导出本机所有未删除篇章，包含完整英文、完整中文、表达、时段、日期、来源、收藏状态；生词本包含原释义、词性、原句和复习进度；精句本包含原句和来源标题。ZIP另含data.json和SHA256文件清单，不包含密钥、设置、词典或语音模型。当前提供导出，未提供导入。
- 全部发音改用Kokoro 82M v1.0 int8的同一个美国男声am_michael（ID16），单词和句子共用美式发音词表及en-us前端。词表中的obsessed为ə b s ˈ ɛ s t。另提供obsessed、comfortable、boundary及两句英文的WAV试听。合成语音仍可能在极短词、同形异音词上出错，不把试听评估替代为真人词典录音保证。
- 横屏重建时恢复当前篇章ID、时段、英文/中文/表达页签、全屏状态和滚动偏移。恢复等待数据库载入；旧通知中的篇章ID不再覆盖已经选择的篇章。全新启动仍按当前时段选择最新篇章。
- 目录每行增加“移除”，确认后只删除所选篇章。数据库增加删除记录，启动原文导入和后台生成均不会补回被删篇章；若正在生成时删除，相同事务内的入库检查也会阻止恢复。删除当前篇章会选取该时段下一篇；独立生词本和精句本保留。
- Room篇章库由版本1迁移到版本2，只新增删除记录表，不重建原篇章表；生词和精句数据库保持不变。离线词典及20篇历史原文字节保持不变。

## V1.7 新增

- 主菜单增加独立“精句本”。英文正文每句小喇叭下方增加“＋”按钮，点击将该英文原句及当前篇章标题存入精句本。精句本按最近加入顺序展示，可复制、离线朗读和逐条移除。同一英文原句重复加入只保留一条，不覆盖首次来源。
- 生词本每行增加“移除”按钮，仅删除所选词及该词的复习记录，保留其他词与进度。精句本每条也有“移除”，仅删除所选句。
- 移除在当前列表更新条目和数量，不关闭列表；清空到最后一条时显示空列表提示。删除收藏不修改原篇章，之后可以重新收藏。
- 原生词库vocabulary.db及Room篇章库结构完全不变；精句使用新增的独立sentences.db，不清空旧数据。内置历史原文、离线词典和男声模型与V1.6逐字节一致。
- 精句本只保存来源中已有的英文原句和篇章标题，不根据完整中文篇章猜测逐句翻译。

## V1.6 修复与优化

- 主菜单提供独立“生词本”和“背诵复习”按钮。生词本只列全部生词，按A–Z排列，每行显示单词、词性、中文释义和一个发音喇叭；无练习、到期筛选或详情弹窗。词性来自已有释义中的标准缩写；没有词性时显示“未标注”，不猜测。
- 背诵复习单独提供看中文拼英文、听音拼英文和到期复习。练习结束或本轮完成后回到背诵复习入口，不跳转生词本。
- 发音喇叭使用列表内普通按钮。旧版详情把发音写在AlertDialog确认按钮中，Android会自动dismiss详情弹窗，造成返回上一页的体验；现在移除该详情路径，发音时保持列表和滚动位置。正文查词及复习的发音按钮同样保持所在页面。
- 存储结构与数据库名称完全不变，原有生词、原句和复习进度保留；语音、词库和历史篇章资产与V1.5逐字节相同。

## V1.5 新增

- 内置Piper en_US-ryan-high美国英语男声，逐句／全文／单词均在本机合成。提供独立WAV试听供用户判断口音；不声称达到真人词典录音或所有单词的多音字均正确。男声使用RyanSpeech数据集，其CC BY-NC-SA 4.0条款适用于个人非商业预览；商业发布前必须重新审查授权。
- 英文正文和表达页的单词可以直接点按，弹出离线中文释义、男声发音和加入生词本。查询原词、词形别名和所有格原型；未收录的词明确提示，不编造释义。常见缩略形式使用应用自写的解释。
- 内置ECDICT 399213个单英文词条和56659个词形别名，过滤短语及无中文释义的行；原释义不改写。词库出处、MIT许可、固定提交和哈希均随源码提供。这不是牛津词典，也不包含牛津释义或音频；当前没有牛津离线数据的分发授权。ECDICT主要提供词典义，不承诺自动选出当前句子的唯一词义。
- 生词本保存词条、释义和原句，防止重复加入；列表按A–Z展示所有单词、词性、中文释义和行内喇叭。背诵复习为另一个独立入口。支持看中文输入英文、听英文输入英文及到期复习。听音题在提交前隐藏英文和中文答案；提交后显示结果。英文判题忽略大小写和两端空格，统一弯引号，不模糊接受拼错。
- 正确后按连续正确次数安排1／3／7／14／30天后复习，错误重置进度并安排10分钟后复习。新词立即到期，进度存在独立本地SQLite库，关闭APP后保留；没有额外背词通知。
- 新篇章生成提示强调当代美国口语、自然缩略和常见搭配，避免书面腔与堆砌俚语。历史原文保持逐字不变，不能保证第三方模型每篇都完全符合主观自然度要求。

## 保留的V1.4修复

- 20个内置篇章按正文主题明确分类：白天13篇（早餐、通勤、城市观察和便利），晚上7篇（关系边界、独处和放慢生活）。两个目录互不重复；不再提供“历史原文”目录及“原时段未标注”标签，不补造原始发布日期。
- 升级时只更新导入篇章的时段分类，保留原文、收藏和已生成内容；无需清空数据库或卸载。
- 语音改为不带原生回调的合成接口。v1.13.8 JNI要求回调类包含`invoke(float[]) -> Integer`，而Java lambda只有泛型擦除后的`invoke(Object)`；该签名不匹配会令JNI异常未清理。现在完全绕开该回调路径，合成完成后再播放，取消时丢弃过期结果。JNI根因检查见`NativeCallbackTest`。

## 保留的阅读与语音功能

- 默认全屏阅读：收起顶部菜单，中文仅保留页签及退出入口；英文另保留紧凑的全文／暂停／继续及语音状态。退出全屏后可选择目录、时段、收藏、语速和循环。返回键先退出全屏，旋转屏幕保留阅读状态。
- 白天／晚上目录只显示各自分类的内容；选择目录中的篇章后直接进入全屏阅读。
- 内置Kokoro Michael美国英语男声与sherpa-onnx1.13.8推理引擎，完全在手机上合成英语；无需系统TTS语音包、联网、密钥或另外下载。首次准备语音数据需要稍候。安装包大小见验证记录，适配vivo的arm64-v8a架构。第三方原始源码、许可及模型卡一并提供。

## 功能

- 白天版08:00、晚上版23:00，统一 Asia/Shanghai。08:00之前默认进入前一天晚上；08:00–22:59为白天；23:00后为晚上。打开应用展示该时段最新缓存篇章，可手动切换。
- 独立英文、独立完整中文、3–5个表达及中文解释；每句内置美式 en-US 离线朗读；全文、暂停／继续、0.75–1.5倍语速、循环全文。继续从暂停句开头重播。
- 篇章目录、全文搜索、收藏、Room本地保存、离线阅读、新篇章通知。
- DeepSeek Flash 直接生成；Android Keystore AES-GCM 加密保存用户输入的密钥。备份及设备迁移排除密钥，输入窗口禁止截图、自动填充和状态保存。没有第三方统计或日志上传。
- WorkManager定时尝试、联网约束、自动重试、打开应用补生成。以日期+时段作为主键并建立唯一索引，重复执行不会覆盖原文或收藏。

## 手机预览

1. 将 `DailyEnglish-V1.8-preview.apk` 传到手机，直接覆盖安装V1.2／V1.3／V1.4／V1.5／V1.6／V1.7，不要先卸载。包名和签名保持一致，版本号递增；已保存的原文、收藏和配置保留。这是已签名的debug预览包，适合个人体验，不是商店正式发布包。
2. 无需密钥即可离线阅读内置的20个英文版本及对应中文，正文保持来源原文，按主题分为白天／晚上。源消息记录保存在源码数据中，界面不显示技术来源ID。部分表达说明是新增学习注释。
3. 撤销／轮换曾在聊天中公开的DeepSeek密钥，然后在应用“设置”中输入新密钥并保存。不要把密钥写入源码或发到聊天。API调用会从你的DeepSeek账户扣除用量。
4. 打开后等待内置离线语音准备完成，点逐句小喇叭或全文播放。首次点击时若正在加载，会在就绪后自动朗读。离线朗读不使用DeepSeek，也不产生API费用。若听不到，检查媒体音量及蓝牙耳机连接。
5. 在OriginOS中允许自启动、后台运行和通知，并在最近任务中锁定应用；电池设置允许较高后台耗电。菜单名称以实际系统为准，应用提供系统设置入口。

## 后台更新边界

**08:00和23:00是安排尝试的时间，不承诺系统一定在这一秒执行。** WorkManager受省电、Doze、网络、任务配额和厂商后台策略影响。强制停止后需手动打开应用；联网与打开时补齐首次配置密钥后的缺失篇章，每轮最多4篇，剩余在下一轮继续补齐。不会伪造安装前历史，也不会追溯生成全部聊天中没能读取的内容。

45–60秒是自然口语的预计时长，新内容校验为120–140词；实际时长随系统语音、语速与停顿变化。DeepSeek响应还会验证字段、完整结束标记、中文和3–5个出自正文的表达，不合格内容不入库。

本机单进程生成锁和Room唯一键防止重入及重复入库。若API已经响应而进程在写库前被杀死，下次可能再次请求并消耗用量；没有服务端幂等接口时不能承诺“恰好一次收费”。

## 构建

需要JDK17、Android SDK Platform36及Build Tools35或36、网络访问Google Maven与Maven Central。项目自带Gradle8.13 Wrapper，Android Gradle Plugin8.13.2。打开Android Studio后设置SDK，或创建未提交的 `local.properties`：

```properties
sdk.dir=C\:/your/android-sdk
```

在项目目录执行：

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Windows若遇到Java“Unable to establish loopback connection”或Robolectric路径包含空格的问题，可运行最新的 `Build-preview-V2.ps1`；它仅为此进程使用项目内的短临时路径和测试缓存路径，不更改系统代理或DNS。V1保留为早期脚本版本。

APK位于 `app/build/outputs/apk/debug/app-debug.apk`。设备测试需连接已授权USB调试的手机：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

设备测试只使用人工合成的无效测试字符串，不使用真实API密钥。普通构建／单元测试不会请求DeepSeek。

Android16 x86_64模拟器测试可使用 `-PvoiceTestAbi=x86_64 :app:connectedDebugAndroidTest`；连接真实vivo时不要带此参数。日常构建和交付默认arm64-v8a。

## 验证与限制

具体结果见交付的 `验证记录-V7.md`，V1／V2／V3／V4／V5保留旧版本结果。包含分类升级、目录互斥、原文收藏保留及JNI回调签名复现。真实vivo扬声器播放和OriginOS全天后台触发仍需手机验证；未输入有效新密钥，故没有进行收费的DeepSeek实网生成测试。不能把这些未测项目当作已通过。

## 官方参考

- [Android Gradle Plugin8.13兼容信息](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [WorkManager约束与执行时间](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Android系统TTS接口](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [内置美式语音模型与接口](https://k2-fsa.github.io/sherpa/onnx/tts/all/English/vits-piper-en_US-ljspeech-high.html)
- [实际使用版本的JNI回调源码](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/sherpa-onnx/jni/offline-tts.cc)
- [DeepSeek模型及接口](https://api-docs.deepseek.com/quick_start/pricing/)
- [DeepSeek JSON输出](https://api-docs.deepseek.com/guides/json_mode/)

## 词库与语音来源

- ECDICT固定版本：https://github.com/skywind3000/ECDICT/tree/bc015ed2e24a7abef49fc6dbbb7fe32c1dadaf8b 。应用未调用外部词典或翻译API。
- 当前Kokoro模型：[官方模型与声音](https://huggingface.co/hexgrad/Kokoro-82M)，[Sherpa接口与ID映射](https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html)，Apache-2.0许可见assets/voice/LICENSE。转换包SHA256：4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e。
- 旧版Ryan模型卡：https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/ryan/high/MODEL_CARD 。模型转换包哈希：6a71edf4d308b9cb2eaeadc8d1f3c6bf96120ecb7fe52c29a2b6e139c59760ed。
- 牛津官方数据入口：https://languages.oup.com/products/oxford-dictionaries-api/ 。当前版本使用独立开源英汉词库；正式牛津词典需要另外取得可内置分发的授权数据。

预览APK为arm64版本；设备测试使用同版本源码、同一语音与词库的x86_64 Android16模拟器。未在用户vivo实机测试，也未使用真实DeepSeek密钥产生付费内容。
