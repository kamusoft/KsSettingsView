# Verify 001: align-sample-parity

- 検証日: 2026-08-01
- ベースコミット: `ef821e9`
- 検証対象: 作業ツリーの未コミット変更すべて (samples/ios, samples/android)
- 検証対象デルタスペック: `specs/samples-ios/spec.md` (6 Requirement / 8 Scenario)、`specs/samples-android/spec.md` (5 Requirement / 6 Scenario)

## 前提

design.md Decision 4 により、本変更は samples (デモアプリ) のみで単体テストの対象となる公開挙動を持たず、自動 UI テストも導入しない。したがって「テスト」欄には実装コードの該当箇所ではなく **実施済みの検証手段** (ビルド / 実機・シミュレータ操作確認 / tasks グループ 3 の対照確認) を記載する。

## 対応表: samples-ios

| Requirement / Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| **ルートメニューのデモ/検証グループ分離** ／ Scenario: メニューのグループ構成 | `samples/ios/KsSettingsViewSample/ContentView.swift:20-30` (`Section("デモ")` / `Section("検証")`)、`samples/ios/KsSettingsViewSample/SampleScreen.swift:40` (demos 6件・順序)、`SampleScreen.swift:43` (verifications = minimalDiffable) | iOS ビルド成功 (本検証で再実行、`** BUILD SUCCEEDED **`)、tasks 1.9 シミュレータ確認、mock/approved.png 照合 | ✅ 一致 |
| **メニュー文言と画面タイトルの同一性** ／ Scenario: 遷移先タイトルの一致 | `SampleScreen.swift:27-37` (表示名の単一定義)、参照側 7 箇所: `StoreDemoView.swift:54` / `DSLDemoView.swift:73` / `BasicCellsDemoView.swift:178` / `InputCellsDemoView.swift:233` / `UnifyCellCommonFieldsDemoView.swift:135` / `VisibilityDemoView.swift:103` / `MinimalDiffableDemoView.swift:67`。メニュー側は `ContentView.swift:22,28` が同じ `screen.title` を参照 | 全7画面が同一定数を参照するため構造的に一致。tasks 1.9 で全7遷移先を操作確認 | ✅ 一致 |
| **入力 Cell 5 種デモの様式統一** ／ Scenario: 直近イベント表示への置き換え | `InputCellsDemoView.swift:87` (「最後のイベント: \(lastEvent)」1行表示)、`:83` (初期値 `(none)`)、`:242-260` (`tracked` による「<title> → <値>」生成)。現在値プレビュー領域は削除 (base 比 `preview` / `themeLabel` / `notifLabel` を削除) | iOS ビルド成功、tasks 1.9 シミュレータで各 Cell 操作確認 | ✅ 一致 |
| 同上 ／ Scenario: 受け付けられない操作では更新されない | `InputCellsDemoView.swift:250` (`guard newValue != source.wrappedValue else { return }`)、対象 Cell は `:160-166` (通知種別、`maxSelectedNumber: 3` は `:167`) | tasks 1.9 シミュレータで 4 件目選択を操作確認 | ✅ 一致 |
| 同上 ／ Scenario: 日付 Cell の状態独立 | `InputCellsDemoView.swift:76` (`birthdayDate`) / `:78` (`reservationDate` 初期値 2026/06/01)、バインド先は `:205` と `:222` で別状態 | tasks 1.9 シミュレータで誕生日変更時の予約日不変を確認 | ✅ 一致 |
| 同上 ／ Scenario: callback 経路の意図明示 | `InputCellsDemoView.swift:94-97` (EntryCell Section footer に spec 全文と同一文言) | 文言対照 (tasks 1.9 / 3.1) | ✅ 一致 |
| 同上 (Section 構成・Cell 数・パラメータの現行維持) | base 比 diff 上、変更は spec が要求する箇所 (footer 追加・DatePicker 文言・予約日の状態分離・tracked ラップ・Theme 適用) のみ。7 Section / Cell 数 / placeholder / maxLength / items / maxSelectedNumber / min-max-step-unit / format は不変 | `git diff ef821e9 -- InputCellsDemoView.swift` で確認 | ✅ 一致 |
| **DatePicker Section の文言中立化** ／ Scenario: 見出しと footer の表示 | `InputCellsDemoView.swift:199-202` (「DatePickerCell（ホイール）」/「ホイール形式で日付を選択するデモ。」)、`:216-219` (「DatePickerCell（カレンダー）」/「カレンダー形式で日付を選択するデモ。」)。旧文言 (`.wheels` / `.calendar` / 「Toolbar に「今日」ボタン」/「iOS カレンダーアプリ風」) は削除済み | 文言対照 (tasks 1.9 / 3.1) | ✅ 一致 |
| **共通フィールド統合デモの hintText 追随** ／ Scenario: hintText の表示 | `UnifyCellCommonFieldsDemoView.swift:76` (`hintText: "推奨"` を「ダーク」に追加)、`:63` (Section header 「RadioCell — accentColor / description / icon / hintText」)。Android 側 `UnifyCellCommonFieldsDemoScreen.kt:67` と同一文言 | 文言対照 (tasks 3.1) | ✅ 一致 |
| **DSL デモの動的 Section 見出しの中立化** ／ Scenario: 見出しの表示 | `DSLDemoView.swift:57` (`Section("動的 Section（繰り返し）")`) | 文言対照 (tasks 1.9 / 3.1) | ✅ 一致 |

## 対応表: samples-android

| Requirement / Scenario | 実装 | 検証 | 状態 |
|---|---|---|---|
| **ルートメニューの一覧表示と文言一致** ／ Scenario: メニューの表示 | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MenuScreen.kt:41-67` (LazyColumn + ListItem の一覧形式。Button 列を廃止)、`:47-52` (見出し「デモ」)、`SampleScreen.kt:19-24` (文言・並び順が iOS `SampleScreen.swift:29-34` / `demos` と一致)、`SampleScreen.kt:34` (検証グループを持たない旨をコメント付きで明示) | Android ビルド成功 (本検証で `:app:assembleDebug` 再実行、exit 0)、tasks 2.7 実機確認・tasks 3.1 iOS 並置照合 | ✅ 一致 |
| **Store 方式デモの表示文言一致** ／ Scenario: 表示文言の一致 | `StoreDemoScreen.kt:48-49` (header「PoC Section」/ footer「This is a footer」)、`:51-53` (「Sample Row 1/2/3」— 旧「Sample Label N」から修正)、`:86`「項目追加」/`:96`「項目削除」、`:78`「新規 $nextIndex」(通番 4 起点は `:59`)。iOS 対応箇所は `StoreDemoView.swift:27-33,43,46,59` | tasks 2.7 / 3.1 の並置照合 | ✅ 一致 |
| **入力 Cell 5 種デモの iOS 一致** ／ Scenario: 画面構成の一致 | `InputCellsDemoScreen.kt:97-249` (7 Section・header/footer・Cell 種別/数/順序・title/placeholder が iOS `InputCellsDemoView.swift:92-229` と一致)、`:82-83,87` (直近イベント表示の様式)、`:45-78` (デモデータ: themes 3件 / notifTypes 5件 / 初期選択 `setOf(0,2)` / min10-max30-step1 / 07:30 / 1990-01-01 / 2026-06-01)、`:173` (maxSelectedNumber = 3)。rootHeader は削除 (base では `rootHeader = { Text("入力 Cell 5 種のデモ") }` が存在) | tasks 2.7 / 3.1 の並置照合 | ⚠️ deviation 記録済み (NumberPickerCell の `unit` 相当 API がなく Picker UI 側の "px" suffix が付かない点。Cell の valueText は `:192` で一致させている。deviation.md「Phase 2 — 本体公開 API の platform 差」に記録) |
| 同上 ／ Scenario: 複数選択の上限 | `InputCellsDemoScreen.kt:173` (`maxSelectedNumber = 3`)、`:265-283` (`TrackedState` の同値ガードにより上限超過時はイベント非更新) | tasks 2.7 実機 (Pixel 6a / Android 16) で 4 件目選択を操作確認 | ⚠️ deviation 記録済み (選択値は受け付けられないが、ダイアログ上のチェックボックス表示が残る本体不具合。deviation.md「Phase 2 — 本体 Android 実装の不具合」に記録) |
| **DatePickerCell の表示形式対応** ／ Scenario: 表示形式の対応 | `InputCellsDemoScreen.kt:228` (`uiStyle = DatePickerUIStyle.Spinner` — iOS `.wheels` 対応)、`:247` (`uiStyle = DatePickerUIStyle.Material` — iOS `.calendar` 対応)。Section 見出し/footer は `:212-214` / `:233-235` で iOS 中立文言と一字一句一致 | tasks 2.5 調査 + 2.7 実機確認、tasks 3.2 で deviation 網羅確認 | ⚠️ deviation 記録済み (Spinner 指定にもかかわらず実機がカレンダー表示になる本体バグ、および `todayText` 相当 API 不在による「今日」ボタン欠如。いずれも deviation.md に記録) |
| **DSL デモの動的 Section 見出しの中立化** ／ Scenario: 見出しの表示 | `DSLDemoScreen.kt:107` (`Section(header = "動的 Section（繰り返し）")`)。iOS `DSLDemoView.swift:57` と同一文言 | tasks 2.7 / 3.1 の並置照合 | ✅ 一致 |

## 追加検査

### tasks.md の完了状況と虚偽チェック

全 18 タスク (1.1-1.9 / 2.1-2.7 / 3.1-3.2) が `[x]`。対応表と突き合わせた結果、未実装のチェック済みは検出されなかった。特に構造変更を伴うタスクを個別確認:

- 1.6 (StoreDemoView 分離): `samples/ios/KsSettingsViewSample/StoreDemoView.swift` 新設、`ContentView.swift` から除去済み
- 1.2 / Decision 3 (Theme 共有化): `SampleTheme.swift` 新設、`BasicCellsDemoView.swift` / `InputCellsDemoView.swift:230` の双方が `SampleTheme.maui` を参照。Android も `SampleTheme.kt` 新設、`BasicCellsDemoScreen.kt` の private 定数群を削除して共有参照に統一
- 2.1 (1画面1ファイル分割): `MenuScreen.kt` / `StoreDemoScreen.kt` / `DSLDemoScreen.kt` 新設、`MainActivity.kt` は -312 行で NavHost と Scaffold のみに縮退

### 逆流検査 (足場アーティファクトの書き換え)

- `ef821e9..HEAD` にコミットなし (実装は未コミットの作業ツリー)
- `kasane/changes/align-sample-parity/` 配下でベース比の変更があるのは `tasks.md` のみ。差分は全 18 行の `[ ]` → `[x]` のみで、本文の書き換えはなし
- `proposal.md` / `design.md` / `specs/samples-ios/spec.md` / `specs/samples-android/spec.md` / `ui/` はベースから未変更
- 新規追加は `deviation.md` / `review-001.md` / `second-opinion-002.md` (いずれも実装期間中に生成されることが想定されている証跡)

→ **逆流なし**

### 未記録乖離の洗い出し

対応表に ❌ はなく、⚠️ はすべて deviation.md に該当記述がある。未記録の欠落・乖離は検出されなかった。

deviation.md には対応表に現れない合意済み差分も 2 件記録されている (いずれも spec 違反ではなく、spec 記述外の実装判断):

- 「通知種別」の初期選択が承認済み mock (メール, アプリ内) と実装 (`setOf(0,2)` = メール, SMS) で異なる — spec の「パラメータは現行を維持する」をオーナー判断で mock より優先
- 共通フィールド統合デモの `accentColor` / `titleColor` の共有パレット化 (`SampleTheme.demoAccent*` / `demoTitleBlue`) — デルタスペックに記述のない追加スコープ。オーナー指示で本変更に含めた旨と、iOS が dark mode の色追随を失うトレードオフが記録済み

### UI 変更の検査

- `ui/brief.md:21-25` に承認記録あり: 「mock/plan-b.html を採用 (approved.png)。2026-07-31 ユーザー承認」+ 承認後改訂 (DatePicker 文言中立化・予約日初期値) と approved.png 再取得の記録
- 合意済み妥協は deviation.md に記録済み (上記)

### ビルド / テスト

本変更は samples のみで単体テストの対象となる公開挙動を持たない (design.md Decision 4)。本検証でビルドを再実行し、いずれも成功を確認:

- iOS: `xcodebuild -project samples/ios/KsSettingsViewSample.xcodeproj -scheme KsSettingsViewSample -destination 'generic/platform=iOS Simulator' build` → `** BUILD SUCCEEDED **` (exit 0)
- Android: `samples/android/gradlew :app:assembleDebug` → exit 0

## 判定

**VALID**

- ❌ (未記録の欠落・乖離): 0 件
- ⚠️ (deviation 記録済み): 3 Scenario (すべて samples-android。本体公開 API の platform 差 2 件・本体 Android 実装の不具合 2 件に起因)
- ✅ (一致): 11 項目
- tasks.md の虚偽チェックなし / 逆流なし / 両 platform ビルド成功

## 備考 (対象外の所見)

- デルタスペックの Requirement に含まれないが並置照合の対象となる画面 (DSL 方式デモの静的 Section・rootHeader / rootFooter・操作ボタン、共通フィールド統合デモの他 Section) についても文言が一致していることをスポット確認した。判定には算入していない
- deviation.md の Android 側 4 件はいずれも「本体の公開 API パリティ課題」または「本体 Android 実装のバグ」であり、後続変更での扱いが必要。本検証の判定範囲外
