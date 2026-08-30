# セカンドオピニオン: relax-android-host-prerequisites (spec-001)
**相方**: codex / **label**: so-spec-relax-android-host-prerequisites / **日付**: 2026-08-27 / **対象**: 提案一式 (proposal / design / specs 3 capability / tasks / ui)
---
# レビュー結果: relax-android-host-prerequisites

**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 5 / Minor 3 / Suggestion 0

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。ADR-0018 / 0019 / 0020 の決定自体は再審していません。

## 指摘事項

### [🟠 Major] 回転復元の保証範囲と成立方式が未確定

**該当箇所**: `design.md:62`、`design.md:85`、`design.md:99`、`specs/android-datepicker/spec.md:65`、`tasks.md:5`

**問題点**:  
デルタスペックは「View インスタンス状態の保存・復元を提供する構成」で復元するとしていますが、その構成が何を満たせばよいか定義されていません。一方、design はスパイク失敗時にホスト単位で「回転で閉じる」へ縮退可能としており、実装結果によって契約が変わります。

さらに現行の `KsSettingsView` と Bridge / Compose 経路は View に安定 ID を設定せず生成しています。

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:54`
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:107`
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:147`

このまま `onSaveInstanceState` だけを実装しても、通常の View hierarchy state に保存されないホストが生じ得ます。また MAUI の MainActivity は orientation を `ConfigurationChanges` で処理するため、通常の回転では Activity 再生成が起きず、`tasks.md:6` の確認条件自体が成立しません。

**推奨修正**:  
実装前に次を確定してください。

- View 直置き / Compose `AndroidView` / MAUI ごとの状態保存経路と必要条件
- View の安定 identity をライブラリとホストのどちらが保証するか
- 回転をその場で処理するホストと Activity 再生成されるホストの期待挙動
- 復元非対応ホストへの縮退を正式契約として許容するか

Scenario の GIVEN を具体化し、強制 `Activity.recreate()`、プロセス再生成、MAUI の in-place 構成変更を分けて検証すべきです。

### [🟠 Major] テーマラップの所有境界とキャッシュ寿命が未定義

**該当箇所**: `design.md:22`、`specs/android-theming/spec.md:7`、`specs/android-theming/spec.md:23`

**問題点**:  
「全 Cell」「ホストテーマ属性はライブラリ UI に影響しない」とありますが、利用者定義 Cell、`CustomCell`、`KsAnyView.AndroidView` / Compose content をラップ対象に含めるか決まっていません。現行 Registry は利用者ファクトリへ `parent` を渡し、利用者 View はその Context から生成されます。ラップすれば利用者所有 UI のテーマまで変わり、除外すれば「全 Cell」の字義を満たしません。

また「Context 単位でラップ結果をキャッシュ」には、弱参照・破棄・configuration 更新時の無効化がありません。強参照キャッシュなら Activity leak、同一 Activity が `uiMode` 等を処理する構成では古い DayNight theme の残留リスクがあります。

**推奨修正**:  
「ライブラリ所有の標準 Cell/chrome」と「利用者所有 content」の境界を明文化してください。キャッシュは Activity を保持しない方式とし、configuration 変更時の再生成または rebase 条件も design とテストへ追加してください。

### [🟠 Major] `format` の `a` 判定が DateTimeFormatter 契約と一致しない

**該当箇所**: `design.md:42`、`specs/android-timepicker/spec.md:23`

**問題点**:  
`TimePickerCell.format` は `DateTimeFormatter.ofPattern` 互換ですが、単純な大文字小文字無視の部分文字列検索ではパターン文字を判定できません。例えば `HH:mm 'at'` は表示上 AM/PM を含まないのに、引用符内の `a` に反応して12時間制になります。大文字 `A` は有効な AM/PM パターン文字でもありません。

そのため `design.md:46` の「行表示と選択面の時制が常に一致する」は成立しません。

**推奨修正**:  
引用符を考慮して未引用の小文字 `a` を検出するか、あえて生文字列検索を公開契約にするなら、その制約と引用符ケースを明記してください。加えて 00:xx / 12:xx の AM/PM 境界 Scenario を追加してください。

### [🟠 Major] Compose DatePicker の状態変換契約が不足

**該当箇所**: `design.md:52`、`specs/android-datepicker/spec.md:39`

**問題点**:  
次の挙動が決まっていません。

- `cell.date` が `minDate..maxDate` 外の場合の初期選択
- min/max の片側または両方が未指定の場合の `yearRange`
- `LocalDate` と Compose DatePicker の epoch millis 間のタイムゾーン変換

仕様は「提示・確定する日付は常に範囲内」としますが、範囲外の初期値を clamp、提示拒否、未選択のどれにするか不明です。Spinner は既存契約で 1900–2100 の既定と最近傍への丸めを明示しているため、Material 側との意図的な差か揃えるのかも判定できません。

現行実装は日付を UTC midnight で変換しています。新設 Compose 経路でローカル midnight を使うと、タイムゾーンによって前日・翌日へずれる危険があります。

**推奨修正**:  
effective range、範囲外初期値、片側境界、UTC 変換を design と Requirement に明記し、境界日・範囲外初期値・UTC±のタイムゾーンを Scenario 化してください。端末タイムゾーンを使うのは「今日」の算出だけ、と分離すると明確です。

### [🟠 Major] DatePicker の4色ロールが検証可能な写像になっていない

**該当箇所**: `design.md:54`、`specs/android-datepicker/spec.md:83`、`tasks.md:25`、`ui/brief.md:10`

**問題点**:  
「4色ロールを `DatePickerColors` へ写像する」としかなく、カレンダー、年選択、テキスト入力、無効日、今日、操作行の各色をどのロールへ割り当てるか決まっていません。Compose の既定値を残す範囲によって、ホスト非依存・既存4色契約の維持という結果が変わります。

承認 mock も通常のカレンダー表示だけで、テキスト入力・年選択・無効状態・dark mode を判定できません。`tasks.md:28` にも色ロール固有の検証がありません。

**推奨修正**:  
design に `DatePickerColors` の各状態と4色ロールの対応表を置き、少なくともカレンダー、テキスト入力、年選択、範囲外 disabled、light/dark の視覚検証対象を定めてください。

### [🟡 Minor] DatePicker の無効状態とタイトル解決がテスト計画から漏れている

**該当箇所**: `specs/android-datepicker/spec.md:7`、`tasks.md:28`

**問題点**:  
Requirement は有効 Cell と `pickerTitle ?: title` を契約に含めていますが、TimePicker と異なり DatePicker のタスクには無効 Cell・タイトル fallback のテストがありません。

**推奨修正**:  
`isEnabled = false` で非提示、`pickerTitle` 指定時と未指定時のタイトル解決を `tasks.md:28` へ追加してください。

### [🟡 Minor] 総合的なビルド・全件テストの完了条件がない

**該当箇所**: `tasks.md:37`、`tasks.md:49`

**問題点**:  
旧テスト群と依存を大きく入れ替える変更ですが、全 Android テスト、Sample build、MAUI binding / host build の完了タスクがありません。`tasks.md:40` の「ビルド確認」だけでは対象と実行件数が判定不能です。

**推奨修正**:  
少なくとも Android の全件テストと実行件数確認、Android Sample、binding、MAUI Sample / test host の build を総合検証へ列挙してください。

### [🟡 Minor] 動画証跡の保存は Kasane 規約に反する

**該当箇所**: `tasks.md:35`

**問題点**:  
「回転の録画証跡を取得」とありますが、ksn-core の媒体規約は動画を撮影・保存せず、動きは連続静止画で代替すると定めています。

**推奨修正**:  
「回転前・遷移中・回転後の連続静止画を `evidence/` に保存」へ変更してください。

## アクションプラン

1. 回転復元のホスト別保証と状態保存方式を先に決定する。
2. テーマラップの所有境界・キャッシュ寿命を確定する。
3. TimePicker の format 判定、DatePicker の range / UTC / 色写像を仕様化する。
4. 不足 Scenario と全件検証タスクを追加する。
5. 動画証跡タスクを連続静止画へ修正する。

## 突き合わせ結果 (2026-08-27)

ホスト側自己レビュー (2周・指摘1件) との突き合わせ。8件すべて相方のみの指摘であり、根拠 (該当コードの特定・実害シナリオ) の強さで判定した。

| 指摘 | 採否 | 反映先 |
|---|---|---|
| Major: 回転復元の保証範囲と成立方式が未確定 | **採用** | design Decision 5 にホスト形態別の保証と成立条件を明記、spec (android-datepicker) の復元 Requirement を「構成変更をまたぐ表示継続」としてホスト形態別に再構成 (in-place 生存 / 再生成復元 / 縮退契約)、tasks 1.2/1.3/8.1 を具体化 |
| Major: テーマラップの所有境界とキャッシュ寿命が未定義 | **採用** | design Decision 1 に所有境界 (利用者所有コンテンツは非ラップ) と保持寿命を明記、spec (android-theming) 隔離 Requirement に利用者コンテンツ対象外の SHALL と Scenario を追加 |
| Major: format の `a` 判定が DateTimeFormatter 契約と不一致 | **採用** | 判定を「引用符外の小文字 `a`」に変更 (現行実装の誤検出は引き継がない)。design Decision 3・spec (android-timepicker) を改訂、引用リテラル/深夜・正午境界の Scenario を追加 |
| Major: Compose DatePicker の状態変換契約の不足 | **採用** | design Decision 4 に範囲外初期値の丸め (Spinner 同一)・年範囲既定 (1900/2100)・UTC 日単位往復を明記、spec に Requirement「タイムゾーンに依存しない日付の往復」と丸め Scenario を追加 |
| Major: 4色ロールが検証可能な写像になっていない | **採用** | design Decision 4 に色ロール対応表を追加、tasks 4.5 に状態×light/dark の検証対象を列挙、brief.md に mock 適用範囲の注記 |
| Minor: DatePicker の無効状態とタイトル解決のテスト漏れ | **採用** | tasks 4.4 に追加 |
| Minor: 総合ビルド・全件テストの完了条件がない | **採用** | tasks 8.3 を新設 (android 全件テスト+件数確認 / samples / binding / MAUI ホスト群のビルド) |
| Minor: 動画証跡は媒体規約違反 | **採用** | ksn-core ui-artifacts の「動画は撮らない・連番静止画で代替」を確認のうえ tasks 5.3 を修正 |

採用 8 / 降格 0 / 未解決 0。相方判定 NEEDS_DISCUSSION の主因 (回転復元の契約不確定) は、ホスト形態別の契約明文化と「不成立形態の縮退契約化 + スパイク時のオーナー確認」で解消した (オーナーへの残提示事項: 縮退契約の許容と format 判定変更の2点)。
