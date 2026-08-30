# レビュー結果: align-sample-parity (001 回目)

**日付**: 2026-08-01
**判定**: CHANGES_REQUESTED

## サマリー

実装コード自体の品質は高い。両 platform ともビルドが通り (iOS `** BUILD SUCCEEDED **` / Android `:app:assembleDebug` 成功・Kotlin 警告 0)、全7画面の表示文字列をレビュー側で機械的に突き合わせた結果、表示文言・パラメータ・初期値の不一致は 1 件も検出されなかった。design.md Decision 2 (表示名の一元化) / Decision 3 (Theme 共有) も両 platform で意図どおり構造化されている。

一方で **deviation.md の Phase 2 記録に事実誤認と分類の誤りがある**。この記録はこの後 ksn-distill で本体側の後続課題として長命層へ引き継がれるため、誤った原因分析がそのまま流れると本体の修正方針を誤誘導する。加えて tasks.md グループ 3 (両 Phase 完了後の突き合わせ検証) が未了、iOS 側に新規の Swift concurrency 警告が 2 件ある。いずれも実装のやり直しではなく記録の是正と残作業の消化で閉じられる。

## 指摘事項

### [🟡 Minor / 優先度高] deviation.md「Spinner が calendar 表示になる」の原因分析が誤っており、「samples 側で解消不能」の判断も未検証

**該当箇所**: `kasane/changes/align-sample-parity/deviation.md:11`
（関連する本体コード: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:117-135`）

**問題点**:

deviation.md は理由をこう記録している。

> 本体 `DatePickerCellViewHolder.showSpinnerDatePicker` が `DatePicker(ctx)` + `calendarViewShown = false` で spinner モードを狙っているが、Material3 テーマ配下の `android.widget.DatePicker` は calendar モードが既定で `calendarViewShown` が無効化されており、Sample 側からは spinner モードを強制できない

3 点、記録として不正確。

1. **原因の帰属先が違う。** `android.widget.DatePicker` のモードは `android:datePickerMode` スタイル属性でしか決まらず、`setCalendarViewShown(false)` は「spinner モード時に CalendarView を隠す」フラグであってモード切替の手段ではない (calendar モードでは no-op)。つまり本体は最初から spinner モードを指定していない。本体コードのコメント (DatePickerCellViewHolder.kt:120-130) が「programmatic に作る方が確実な spinner モードを強制するアプローチ」「spec MUST を満たす唯一の手段」と書いているのが、そもそも誤った前提。
2. **Material3 テーマのせいではない。** platform 既定が API 21 以降 calendar モードであり、Material3 は `android:datePickerStyle` を設定しない。Material3 を外しても直らない。
3. **「Sample 側からは強制できない」が未検証。** app テーマに `android:datePickerMode="spinner"` を持つ `android:datePickerStyle` を差し込めば、本体が `views.root.context` (= Activity テーマ由来) から `DatePicker` を生成している以上、表示形式は変えられる可能性が高い。少なくともこの経路を試した形跡がない。

なお、**deviation に残して本体側で直すという結論自体は妥当**と考える (Sample の app テーマ hack は本体の欠陥を隠すことになり、規約 sample-parity.md の「一致が不可能な箇所を黙認しない」の趣旨に沿わない)。是正すべきは記録の内容であって判断ではない。

**推奨修正**: deviation.md の当該項目の理由を、「本体 Android 実装が `DatePickerUIStyle.Spinner` に対して spinner モードの指定手段 (spinner スタイルの `ContextThemeWrapper` 等) を実装していない **バグ**。`calendarViewShown` はモード切替の手段ではない」という趣旨に書き換える。あわせて後続変更の課題として本体バグを明示的に起票する (下記「後続変更の課題」(b))。

---

### [🟡 Minor / 優先度高] deviation.md Phase 2 の見出しが「本体公開 API の platform 差」となっているが、4 件中 2 件は公開 API 差ではなくバグ

**該当箇所**: `kasane/changes/align-sample-parity/deviation.md:7-14`

**問題点**: 見出しは「本体公開 API の platform 差に由来する未一致」だが、実際には性質の異なる 2 種類が混在している。

- 公開 API 差 (Android に対応 API がない): `todayText` (12 行目) / `unit` (13 行目) — 見出しどおり
- **本体 Android 実装のバグ**: Spinner が spinner にならない (11 行目) / 上限超過時に選択ダイアログのチェックが視覚的に戻らない (14 行目) — 公開 API は両 platform に存在し、Android 側の挙動が仕様どおりでない

後続で「API を揃えるか」を検討すべき前者と、「バグを直す」だけの後者では、扱いも優先度も違う。まとめて「統一課題」と記録すると後者が API 設計議論に埋もれる。

**推奨修正**: Phase 2 のセクションを「本体公開 API の platform 差」と「本体 Android 実装の不具合」の 2 つに分け、後者は後続の bug fix 変更として扱う旨を明記する。

---

### [🟡 Minor] iOS: `SampleScreen.destination` で新規の Swift concurrency 警告が 2 件発生している

**該当箇所**: `samples/ios/KsSettingsViewSample/SampleScreen.swift:49` / `:52`

```
SampleScreen.swift:49:22: warning: call to main actor-isolated initializer 'init()' in a synchronous nonisolated context
SampleScreen.swift:52:27: warning: call to main actor-isolated initializer 'init()' in a synchronous nonisolated context
```

**問題点**: 新規ファイルが持ち込んだ警告。`destination` が nonisolated なため、MainActor 分離された `StoreDemoView.init()` / `InputCellsDemoView.init()` の呼び出しが警告になる。プロジェクトは `SWIFT_VERSION = 6.0` を指定しており (project.pbxproj:344 / 375)、この種の警告は将来のツールチェーン更新でエラー化しうる。他ファイルの既存警告 (`main actor-isolated property ... Sendable closure` 系、計 33 件) は本変更以前からのパターンなので区別している。

**推奨修正**: `var destination` に `@MainActor` を付与する (または `enum SampleScreen` 自体を `@MainActor` にする)。ContentView 側の呼び出しは既に MainActor 文脈なので影響はない。

---

### [🟡 Minor] tasks.md グループ 3 (両 Phase 完了後の突き合わせ検証) が未了のまま

**該当箇所**: `kasane/changes/align-sample-parity/tasks.md:29-30`

**問題点**: 3.1 (sample-parity.md のチェック項目での対照確認) と 3.2 (deviation 網羅確認) が未チェック。本変更の受け入れ根拠そのものであり、これが閉じるまでは「収束した」と言えない。

補足として、3.1 の静的部分はレビュー側で代行実施済み (下記「確認した観点」参照) で、不一致は検出されていない。3.2 は上記 deviation の是正後に確定させる必要がある。

**推奨修正**: deviation.md 是正後に 3.1 / 3.2 を実施してチェックする。3.1 で新たに検出された差異があれば追記する。

---

### [🔵 Suggestion] Android: `androidx.fragment:fragment-ktx` ではなく `androidx.fragment:fragment` で足り、バージョンも本体と食い違う

**該当箇所**: `samples/android/app/build.gradle.kts:79-82`

**問題点**: 使用しているのは `FragmentActivity` のみで、`fragment-ktx` の KTX 拡張 (`commit { }` / `viewModels()` 等) は使っていない。また本体 `android/ks-settingsview-ui/build.gradle.kts:93` は `1.8.4` を指定しており、Sample が `1.8.5` を宣言することで解決バージョンが Sample ビルドだけ上がる。実害はないが、意図的でないなら揃えるほうが素直。

**推奨修正**: `implementation("androidx.fragment:fragment:1.8.4")` にする。上げる意図があるならコメントに理由を残す。

---

### [🔵 Suggestion] Android: `StoreDemoScreen` の `collectAsState()` を `collectAsStateWithLifecycle()` へ

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/StoreDemoScreen.kt:61`

**問題点**: jetpack-compose-impl-skill の規約は「Flow の収集は必ず `collectAsStateWithLifecycle()` を使う」。MainActivity.kt から移設された既存コードで本変更が持ち込んだものではないが、1 画面 1 ファイル化で触れた箇所ではある。Sample はライブラリ利用例として読まれるため、推奨形にしておく価値がある。

**推奨修正**: `androidx.lifecycle:lifecycle-runtime-compose` を追加して `collectAsStateWithLifecycle()` に置き換える。本変更のスコープに入れたくなければ後続で可。

---

### [🟡 Minor / 情報] NumberPickerCell の footer が Android 上で虚偽になる件 — deviation 記録済みだが design Decision 1 の原則と衝突している

**該当箇所**: `samples/android/.../InputCellsDemoScreen.kt:391` (`footer = "Picker UI と Cell の valueText に \"px\" suffix が付く。"`) / `deviation.md:13`

**問題点**: deviation.md に記録済みのため**本変更の仕様違反としては扱わない**。ただし design.md Decision 1 は代替案 A を「Sample が Android 上で虚偽の説明を表示することになり、検証装置としての信頼を損なう」として明示的に却下しており、DatePicker Section に適用したその原則が NumberPickerCell の footer にはそのまま適用されていない。samples-ios spec の「上記の変更および DatePicker Section の文言中立化を除き、表示文言は現行を維持する」という要求と Decision 1 の原則が競合しており、実装側では解けない (足場凍結)。

**推奨修正**: 本変更では現状のままでよい。後続で spec 論点として扱い、DatePicker と同様に両 platform とも中立文言 (例: 「Cell の valueText に単位 suffix を付けるデモ。」) へ揃えることを検討する。判断は spec / mock の改訂を伴うためオーナー確認が要る。

---

## 後続変更の課題 (本体ライブラリ — 今回の変更対象外)

本変更のレビュー中に本体側で見つけたもの。今回のコード変更の指摘ではない。

- **(a) Android UI 層が host Activity に `FragmentActivity` を要求するが、その契約がどこにも記録されていない。**
  `TimePickerCellViewHolder.kt:158-165` の `findFragmentManager()` は解決できないと `null` を返し、呼び出し側 (`TimePickerCellViewHolder.kt:59`、`DatePickerCellViewHolder` の Material 経路) は `?: return` で**無言で何も起きない**。KDoc は「利用側はホスト Activity が FragmentActivity を継承していることを保証する必要がある」と書いているが、`kasane/concepts/` にも `docs/` にも記述が 0 件 (grep 済み)。ライブラリの公開契約として `concepts/android/` に記録し、満たさない場合は例外送出かログ出力にする改修を検討したい。今回 MainActivity を `ComponentActivity` → `FragmentActivity` に変えなければ TimePickerCell / Material DatePicker が無反応のままだった (= Sample がこの前提を体現していなかった) 点が、この課題の実害を示している。
- **(b) `DatePickerUIStyle.Spinner` が実機で spinner にならない。** 上記 Minor の 1 件目。本体コードのコメント (`DatePickerCellViewHolder.kt:120-130`) が「spec MUST を満たす唯一の手段」と主張している実装が、実際には MUST を満たしていない。
- **(c) `PickerCell` の `maxSelectedNumber` 超過時、選択ダイアログのチェックボックスが視覚的に戻らない** (deviation.md:14 記録済み。`PickerCellViewHolder.kt:98` 付近の `setItemChecked(which, false)` が反映されない)。
- **(d) Android の `NumberPickerCell` に `unit`、`DatePickerCell` に `todayText` 相当の公開 API がない** (deviation.md:12-13 記録済み)。iOS との公開 API パリティ課題として、揃えるか揃えないかの判断が要る。

## 確認した観点 (問題なし)

- **ビルド**: iOS `xcodebuild -scheme KsSettingsViewSample -configuration Debug` → `** BUILD SUCCEEDED **`。Android `./gradlew :app:assembleDebug` → 成功、`compileDebugKotlin --rerun-tasks` で Kotlin 警告 0。テストは本変更の対象外 (tasks.md 冒頭の但し書きどおり samples のみで単体テスト対象の公開挙動を持たない)。
- **全7画面の表示文言 一字一句照合 (spec の中核要求)**: iOS 各 View と Android 各 Screen の文字列リテラルを機械的に突き合わせた。表示文言の差分は **0 件**。検出された差は (1) 文字列補間構文 (`\(x)` vs `$x`)、(2) SF Symbols 名 vs Android アイコン、(3) Cell/Section の内部 ID、(4) spec 参照コメント、(5) iOS のみの `"今日"` (todayText — deviation 記録済み) のみ。対象: 入力 Cell 5 種 / Store 方式 / DSL 方式 / 共通フィールド統合 / 基本 Cell 7 種 / isVisible。
- **入力 Cell 5 種デモのパラメータ一致**: Section 数 7・並び順・header/footer・Cell 種別と数・初期値 (Tanaka Taro / tanaka.taro@example.com / 090-0000-0000 / secret123 / themeIndex 0 / notifSelection [0,2] / volume 30 / 07:30 / 1990-01-01 / 2026-06-01)・`maxSelectedNumber = 3`・`min 10 / max 30 / step 1`・pageTitle / pickerTitle が全一致。rootHeader は Android から削除済み。
- **mock/approved.png との照合**: ルートメニュー (「デモ」6 項目 + 「検証」1 項目、順序も一致) と入力 Cell 5 種デモ (直近イベント 1 行 + 7 Section、MAUI 互換 Theme) が mock と一致。相違は「通知種別」の初期選択のみで、これは deviation.md:5 に記録済みの合意事項。
- **design.md Decision 2 (表示名の一元化)**: iOS `SampleScreen.title` を ContentView のメニュー項目と全7画面の `navigationTitle` が参照 (MinimalDiffable 含む)。Android `SampleScreen.title` を MenuScreen の一覧と NavHost の `DemoScaffold(title = screen.title)` が参照。両 platform とも文言の二重手書きは残っていない。
- **design.md Decision 3 (Theme 共有)**: iOS `SampleTheme.swift` / Android `SampleTheme.kt` に色定数と Theme を一元化し、基本 Cell 7 種デモと入力 Cell 5 種デモの双方が参照。色値 (F2EFE6 / FFFFFF / E6DAB9 / 50FFBF00 / FFBF00 / CC9900 / 999999) は両 platform で一致。Android 側は `object` の `val` になり `remember { buildMauiCompatibleTheme() }` が不要になっている点も適切。
- **MainActivity の `FragmentActivity` 化 (依頼された評価点)**: **妥当**と判断する。(1) Non-Goals「本体ライブラリの変更なし」を守っている (変更は samples 配下のみ)。(2) 本体 `findFragmentManager()` の KDoc が明示する前提条件を満たすための最小手段で、これなしでは TimePickerCell と `DatePickerUIStyle.Material` の picker が無言で開かず、samples-android spec Scenario「表示形式の対応」を検証できない。(3) Sample の代表性はむしろ向上する — 利用者が満たすべき前提を Sample が体現する形になる。ただし前提そのものが未文書化である点は上記 (a) として残す。`setContent` は `ComponentActivity` 拡張であり `FragmentActivity` はその派生なので互換性の問題もない。
- **足場アーティファクトの凍結**: proposal.md / design.md / specs/ / ui/ はいずれも未変更 (git status で確認)。tasks.md の差分はチェックボックスのみ。虚偽チェック (未実装なのに `[x]`) は検出されなかった。
- **Compose 規約**: `LazyColumn` の `items` に `key = { it.route }` 指定あり、`Icon` の `contentDescription = null` に装飾扱いの理由コメントあり、色・タイポグラフィは `MaterialTheme` 参照、Material 3 のみ使用。`TrackedState` は `remember` された source を包む薄いラッパーで、毎 recomposition の再生成もライブラリ側 DSL (`text.value` 読み + `onTextChanged` ラムダ) の使い方に照らして問題なし。
- **iOS / Android の直近イベント記録ロジックの等価性**: 双方とも「新値 == 現在値なら何もしない」ガードを持ち、spec Scenario「受け付けられない操作では更新されない」を同じ方法で満たしている。

## アクションプラン

優先度順。

1. **deviation.md の Phase 2 記録を是正する** — (a) Spinner 項目の理由を「本体 Android 実装のバグ (spinner モードの指定手段が未実装。`calendarViewShown` はモード切替の手段ではない)」に書き換える。(b) Phase 2 を「公開 API の platform 差」(todayText / unit) と「Android 実装の不具合」(Spinner / チェック残留) に分割する。
2. **後続変更の課題を起票する** — 上記「後続変更の課題」(a)〜(d)。特に (a) FragmentActivity 契約の未文書化と (b) Spinner バグ。
3. **`SampleScreen.destination` に `@MainActor` を付与**して新規の concurrency 警告 2 件を解消する。
4. **tasks.md 3.1 / 3.2 を実施してチェックする** (1 と 2 の完了後)。
5. (任意) `androidx.fragment:fragment-ktx` → `androidx.fragment:fragment:1.8.4`、`collectAsState()` → `collectAsStateWithLifecycle()`。
6. (任意・要オーナー判断) NumberPickerCell footer の中立文言化を後続の spec 論点として残す。
