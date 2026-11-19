# Android AI Coding 試點交付

日期：2026-11-16（Asia/Taipei）。本輪完成最小品質閉環；沒有推送、合併、首次 Git 提交或大型重構。

## 1. 盤點與試點

唯一 Android 根專案：排課小幫手，位於 <project-root>。此目錄本身就是專案，不是多專案容器。使用者已確認選為試點。規模小、單模組、可成功建置、已有 JUnit / instrumentation 基礎，適合先建立低侵入的驗證入口。

|項目|實際結果|
|---|---|
|模組|:app；單模組|
|原始語言|30 個 .kt（28 個正式來源、2 個測試）；沒有 .java|
|UI|Jetpack Compose；16 個 XML 為 Manifest／資源／設定，未發現 layout XML|
|Gradle / AGP|Wrapper 9.6.0 / AGP 9.3.3|
|Kotlin / KSP|專案 Kotlin plugin 2.2.10 / KSP 2.3.6；Gradle 自身內嵌 Kotlin 2.3.21 不等於專案 plugin|
|SDK|compileSdk / targetSdk 36，minSdk 24；本機有 platform 34、35、36、37.0、37.2；build-tools 36.0.0|
|SDK 路徑|<android-sdk>（local.properties 的 sdk.dir）|
|JDK|Shell 原本沒有 JAVA_HOME、PATH java；Android Studio JBR 25.0.2 可用；Gradle daemon criteria 指定 Java 21，已有快取 JDK 21；來源／bytecode target 11|
|主要依賴|Compose BOM 2024.02.00、Room 2.8.5、DataStore 1.0.0、Navigation Compose 2.7.7、Glance 1.1.1、Ads 25.5.0|
|既有品質工具|Android Lint、JUnit 4.13.2、AndroidJUnitRunner、Espresso core 3.5.1；沒有 Detekt／ktlint 設定|
|Lint 設定|未發現自訂 lint.xml、baseline 或 android.lint 區塊；沿用既有預設，不降低規則|
|Git|master 尚無首次提交，66 個 A、43 個 AM；原有未追蹤 app/release/；沒有 remote|
|CI/CD / 指示檔|未發現 CI workflow、AGENTS.md、CLAUDE.md、GEMINI.md 或專用 Agent 指示檔|

## 2. 安全還原點

修改前已保存 316 個檔案、12,992,580 bytes，包括工作樹、原始 .git/index 與 .idea；排除 build、.gradle、.kotlin 產物／快取。封存為 outputs/pilot-before.tar.gz，逐檔 SHA-256 為 evidence/snapshot-manifest.json。

原始 Git index 維持原狀。對照快照，唯一被修改的既有檔案是 ExampleInstrumentedTest.kt；其他變更均為新增檔案。未將既有使用者內容提交。還原時先解壓至另一個空目錄比對，選擇性還原本輪檔案；不要直接覆蓋後續工作或整個 .git。

## 3. 原始品質基準

先成功執行 gradlew.bat tasks --all，再使用確實存在的任務：

- :app:assembleDebug：成功。
- :app:compileDebugKotlin：成功（初次基準為 UP-TO-DATE）。
- :app:compileDebugJavaWithJavac：NO-SOURCE；沒有 Java 來源，不宣稱執行了 Java 編譯。
- :app:lintDebug：成功，0 error、51 warning。
- :app:testDebugUnitTest：1 test，0 failure、0 error、0 skip。
- :app:connectedDebugAndroidTest：1 test、1 failure。舊測試預期的 applicationId 與實際的 com.czy.timetable 不一致。

Lint warning 分類：OldTargetApi 1、UnusedAttribute 3、AndroidGradlePluginVersion 1、GradleDependency 12、NewerVersionAvailable 6、UnusedResources 7、MonochromeLauncherIcon 2、IconDuplicates 5、UseTomlInstead 14。另有 AGP 舊 DSL／舊屬性與 applicationVariants 等棄用警告；本輪保留，未以 suppress 或關閉檢查處理。

## 4. 實作及工程限制

|檔案（相對試點根目錄）|用途|
|---|---|
|scripts/verify.ps1|固定必要 Gradle tasks、--continue 收集失敗、傳遞原始非零 exit code、清楚錯誤訊息；可從其他工作目錄呼叫|
|app/src/test/java/com/czy/timetable/calendar/CalendarUtilsTest.kt|四個真實月曆需求：閏年二月、平年二月、週日無前置空格、不能修改傳入的日期|
|app/src/androidTest/java/com/czy/timetable/ExampleInstrumentedTest.kt|依使用者確認的正式套件 com.czy.timetable 修正過時契約；保留獨立字面值，避免從受測值推導期待值|
|docs/ai-coding-quality.md|本報告、驗證流程、限制與後續措施|
|docs/quality-incidents.md|錯誤根因、修復及回歸記錄|

未新增 Gradle plugin、相依套件、自訂 Lint、全域 API 禁令或 AGENTS.md。月曆行為以單元測試限制；套件識別以裝置契約測試限制。沒有更動正式程式、applicationId、SDK、建置版本或既有品質設定。

## 5. 統一驗證指令

在試點根目錄執行：

    pwsh -NoProfile -File .\scripts\verify.ps1

有連線模擬器／裝置時：

    pwsh -NoProfile -File .\scripts\verify.ps1 -Device

必要時加 -JavaHome '<android-studio>/jbr'。腳本接受 JAVA_HOME／PATH java；兩者皆無時才使用 Android Studio 內建的 JBR。只在腳本程序內調整 JAVA_HOME 並復原，不修改使用者系統環境。PowerShell 7 已實測；程式使用 5.1 語法，但 Windows PowerShell 5.1 尚未另外實測。

一般模式：Debug APK、Kotlin／Java 編譯任務、Lint、unit tests。裝置模式額外要求 connectedDebugAndroidTest；無裝置即應失敗，不靜默跳過。一般模式成功不能用來宣稱裝置測試／UI 成功。Detekt／ktlint 原專案不存在，本輪不引入。

## 6. 實際驗證與閉環

1. 原始 Build／Lint／unit 基準通過。
2. 原始裝置契約失敗；經統一入口 -Device 重現 exit 1，證明失敗未被吞掉。
3. 使用者明確確認正式套件是 com.czy.timetable，據此識別為過時的測試契約；單次修復，沒有修改 App 套件來遷就測試。
4. 重跑 verify.ps1 -Device：exit 0，Build／編譯／Lint／unit／instrumentation 全部通過。
5. 最終 unit tests 5/5、instrumentation 1/1；Lint 仍為 0 error、51 warning，無新增 warning。

腳本另以隔離 fake wrapper 正反夾具驗證：exit 0 原樣成功、exit 7 原樣失敗（evidence/verify-contract.log）。這是腳本退出碼測試，不是 Android 建置結果。真實專案的失敗及成功另有獨立紀錄。

後續 Agent 每次修改後使用同一入口；先讀具體失敗與 XML／SARIF，依根因修復，重跑相關檢查後再跑統一入口。同一根因最多修復三輪；超過即保留失敗回報阻礙。只有通過必要驗證才能回報完成；保留原始需求、測試及規則。不是可通用自動修復所有錯誤的常駐 Agent；本輪已實際走完「發現失敗→確認需求→修復→再次驗證」一次。

## 7. Runtime 與 UI 邊界

ADB 37.0.1 可用。Pixel_10 AVD 的映像已安裝，WHPX 可用；本輪以無視窗方式啟動 emulator-5554，API 37。成功安裝 Debug APK，啟動 MainActivity、拒絕通知權限後可見「我的課表」主畫面。當次 crash buffer 無紀錄，lastanr 顯示開機後無 ANR；不是長時間穩定性保證。截圖見 evidence/runtime-screen.png。

- Build Success：已驗證。
- Test Success：5 個 unit 與 1 個套件識別 instrumentation 已驗證。
- Runtime Success：安裝、啟動、主畫面冒煙檢查已驗證。
- UI Behavior Verified：僅首次拒絕通知權限後顯示課表；新增／編輯課程、資料持久化、提醒、Widget、網路與所有 UI 行為尚未驗證。未建立 Espresso／Compose UI 自動化。

Instrumentation 執行完會移除測試安裝；本輪的獨立 runtime smoke 是另行安裝後執行。最後一輪 instrumentation 可能再次移除 APK；不要以是否仍安裝推翻已保存的啟動證據。模擬器由本輪啟動。

## 8. 風險分析與下一階段

以下為靜態閱讀所得候選風險，尚未全部重現，不能視為已修復：

|類別|最低成本的工程方案|本輪狀態|
|---|---|---|
|鬧鐘 dayIndex 不合法可能使 while 永遠找不到星期|先抽出純排程計算，於邊界驗證 0..6，再補非法值與跨午夜測試；避免全面禁止 AlarmManager|待實作|
|日期字串依賴 Locale，可能影響假日／考試日期鍵|集中日期序列化契約，補不同語系／時區測試|待實作|
|資料庫 singleton synchronized 內缺第二次 INSTANCE 檢查|以並發測試確認，再修正 instance 建立邊界|待實作|
|CoroutineScope 生命週期|接收器已有 goAsync / finally finish，不一律禁止；再檢查逾時與取消語意|待驗證|
|Room migration|已有 MIGRATION_1_2，沒有 allowMainThreadQueries／destructive fallback；加入 schema 與 migration 測試需另評估|待實作|
|AGP 棄用設定／依賴更新|分次處理並比較基準；不能為消除警告直接批次升級|待實作|

先建立上述風險的最小可重現測試，再決定型別／封裝或靜態規則。新增自訂分析規則前必須有違規與合法案例；本輪沒有自訂規則，所以沒有宣稱做過規則正反測試。

## 9. CI 與權限

本機 Git 沒有 remote，也沒有 CI，所以沒有建立 GitHub workflow、變更分支保護、設定 auto-merge 或推送。

下一階段在正式遠端確定後，把統一驗證納入 PR gate；同時評估主要分支保護、必要狀態檢查、限制可繞過者、最小權限 token，以及對 scripts／workflow／安全政策的受信任審查。僅將 YAML 與規則放在同倉庫不足以阻止 Agent 同時修改檢查。需要儲存庫管理端與受信任的外部執行邊界；本輪未驗證任何伺服器政策。

## 10. 交付狀態

已完成且驗證：唯讀盤點、試點確認、修改前快照、原始基準、統一驗證入口、退出碼正反測試、四個回歸測試、過時套件契約修復、完整 -Device 通過、啟動截圖與有限 Crash／ANR 檢查。

已實作但尚未驗證：腳本在 Windows PowerShell 5.1 的實際執行（PowerShell 7 已驗證）。

尚未實作：自訂 Lint／Detekt／ktlint、完整 UI 自動化、上述候選風險修復、release／簽章驗證、長時間穩定性測試、常駐自動修復引擎。

因環境／配置無法執行：遠端 CI 與伺服器分支保護（沒有 remote）；不能聲稱主分支已受保護。

其餘問題：51 個歷史 Lint warning 與 AGP 棄用警告仍在；既有唯一 unit test 是算術範例，測試涵蓋率仍低。原始 Git 尚無首次提交，後續由使用者檢閱既有工作後建立正式版本歷史。
