# セカンドオピニオン: add-sample-dark-mode-toggle (spec-001)
**相方**: codex / **label**: so-spec-add-sample-dark-mode-toggle / **日付**: 2026-09-05 / **対象**: 提案一式 (proposal.md / exploration.md / specs/ / tasks.md / ui/brief.md / ui/mock/plan-a.html)
---
# レビュー結果: add-sample-dark-mode-toggle（spec-review）

**日付**: 2026-09-05  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 6 / Minor 1 / Suggestion 0

## サマリー

Android の「システム」選択の実現方法が公開 API 契約上確定しておらず、承認済みカレンダーモックも Android Native / MAUI Android では現行 Material3 `DatePicker` の描画構造上再現できません。さらに dark 配色の定義漏れ、MAUI chrome、検証マトリクスにも仕様上の穴があります。

read-only 制約に従い、ビルド・テスト・ファイル作成は行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/sample-parity.md`（Sample の構成・文言・データ変更）
- `kasane/handbook/cross/runtime-behavior-verification.md`（OS 外観変更の実行時検証）
- `kasane/handbook/cross/test-execution.md`（検証計画）
- `kasane/decisions/android/0020-bundled-theme-always-wrap-host-independent.md`
- `kasane/concepts/core/styling/style-resolution.md`
- `kasane/concepts/core/cells/date-picker-selection-surface.md`
- `kasane/lessons/spec-review.md`

## 指摘事項

### [🟠 Major] Android の「システム」選択を `UiModeManager` へどう写像するか未確定

**該当箇所**: [specs/samples-android/spec.md:7](specs/samples-android/spec.md:7)、`proposal.md:13-15`、`tasks.md:18`

**問題点**: `UiModeManager.setApplicationNightMode` が受け取る公開値は `MODE_NIGHT_NO / YES / AUTO / CUSTOM` であり、`MODE_NIGHT_FOLLOW_SYSTEM` は `AppCompatDelegate` 側の値です。`MODE_NIGHT_AUTO` も公開契約上は位置・センサー等による自動切替で、「端末設定への継続追随」と同義ではありません。[Android API リファレンス](https://developer.android.com/reference/android/app/UiModeManager.html)

したがって現在の仕様では、「システム」を選んだ後に端末設定を変更した場合まで追随できるのか、どの値を渡すのかを実装者が推測することになります。現行 Scenario も「ダークから端末が既にライトの状態へ戻す」だけで、選択後の端末外観変更を検証していません。

**推奨修正**: Android の「システム」の具体的な写像と対応 OS 範囲を先に実証して確定してください。併せて「システム選択中に端末外観を変更すると、実行中または再開時に追随する」Scenario を3プラットフォームへ追加してください。公開契約だけで保証できない場合は、表示名を「自動」に変える案、AppCompat 経路を採る案、Android のみ仕様差として明示する案からオーナー判断が必要です。

### [🟠 Major] Android chrome の DayNight 化が ADR-0020 の検証装置を失わせ得る

**該当箇所**: [proposal.md:15](proposal.md:15)、`exploration.md:31-33,39-40`、`tasks.md:19`、`samples/android/app/src/main/AndroidManifest.xml:10-22`

**問題点**: タスクは「Manifest テーマの DayNight 化」としか定めていません。Android の標準的な DayNight 手順は `Theme.AppCompat.DayNight` または `Theme.MaterialComponents.DayNight` ですが、それを採ると、現在の「非 AppCompat・非 Material3 XML テーマでもライブラリが動く」という ADR-0020 の検証条件が失われます。[Android dark theme ガイド](https://developer.android.com/develop/ui/views/theming/darktheme)

`ComponentActivity` を維持するだけでは、非 Material ホストテーマの証明にはなりません。

**推奨修正**: 次のいずれかを design/tasks で確定してください。

- `values/` と `values-night/` の同名 style で framework の light/dark テーマを切り替え、非 AppCompat・非 MaterialComponents の条件を維持する。
- Sample から ADR-0020 の検証責務を外し、代替検証ホスト・テストへ移すことを明記する。

### [🟠 Major] 固定範囲と承認モックは Android のカレンダーで成立しない

**該当箇所**: [ui/brief.md:9](ui/brief.md:9)、`ui/mock/plan-a.html:74-79`、`specs/samples-ios/spec.md:40-45`、`specs/samples-android/spec.md:56-61`、`specs/samples-maui/spec.md:40-45`

**問題点**: 範囲を月初 `2026/06/01` から月末 `2026/06/30` にすると、6月内に disabled になる日は一つもありません。承認モックは前月・翌月の日を同じグリッドに表示していますが、Compose Material3 `DatePicker` は月外セルを日付ではなく空の `Spacer` として描画します。このため Android Native と同じ Native 実装を通る MAUI Android では、モックおよび「同じ月の表示内で範囲外 disabled が見える」という目的を満たせません。[AndroidX DatePicker 実装](https://android.googlesource.com/platform/frameworks/support/+/40820f60236e9b7b3703a480e95a06b882361843/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/DatePicker.kt)

**推奨修正**: 3面共通の範囲を月途中（例: `06/05`〜`06/25`）にし、初期値も範囲内へ置いてください。これなら同月の日付が全実装で disabled 表示になります。月初〜月末を維持するなら、「前月・翌月へ移動して disabled を確認する」契約へ変更し、承認モックも実際の Android 表示に合わせる必要があります。

### [🟠 Major] dark プリセットの色ロールが承認モックと一致していない

**該当箇所**: [ui/mock/plan-a.html:81](ui/mock/plan-a.html:81)、`ui/brief.md:19-27`、`tasks.md:7`

**問題点**: モックは value text を `#B8B2A6` で描画していますが、色ロール対応表には `cellValueTextColor` がありません。さらに基本 Cell デモには description が存在するのに、`cellDescriptionColor` も定義されていません。

現行 Android の未指定 description 色は固定 `#6D6D72` であり、夜間モードに自動追随しません（`android/kssettingsview/.../Theme.kt:105-109,165-166`）。表どおりのフィールドだけを実装すると、モックとの差異や暗色背景上の低コントラストが残っても、どちらが正しいか判定できません。

**推奨修正**: 承認対象画面に現れる少なくとも次のロールを対応表へ追加し、3面同一 RGBA を確定してください。

- `cellValueTextColor`
- `cellDescriptionColor`
- 必要なら `cellHintTextColor`

モックの装飾用 CSS と実装へ渡す Theme 値も一対一に対応させてください。

### [🟠 Major] MAUI の「アプリ全体の外観」と固定ナビゲーションバーが矛盾する

**該当箇所**: [specs/samples-maui/spec.md:7](specs/samples-maui/spec.md:7)、`tasks.md:25-26`、`samples/maui/KsSettingsView.Sample.Maui/App.cs:14-20`

**問題点**: 現行 `NavigationPage` は `BarBackgroundColor = #2C3E50`、`BarTextColor = White` を直接指定しています。`Application.UserAppTheme` を変更しても、この固定値は light/dark に切り替わりません。したがって「アプリ全体の外観へ即時反映」は満たせません。

MAUI で自動追随させるには `AppThemeBinding` / `SetAppThemeColor` 等を明示的に適用する必要があります。[Microsoft Learn](https://learn.microsoft.com/en-us/dotnet/maui/user-interface/system-theme-changes?view=net-maui-10.0)

**推奨修正**: MAUI chrome も追随対象なら、専用 Requirement・Scenario と `App.cs` の変更タスクを追加してください。固定バーを意図的に維持するなら、「アプリ全体」という契約を狭め、chrome は例外であることを spec / brief に明記してください。

### [🟠 Major] 検証計画が Scenario と4つの実行面を覆っていない

**該当箇所**: [tasks.md:29](tasks.md:29)

**問題点**:

- 実行面は iOS Native / Android Native / MAUI iOS / MAUI Android の4つですが、5.2・5.3は「3面」とだけ書かれ、MAUI 両 target の描画・カレンダー確認を保証していません。
- 5.4 は API 30 実機経路を `Build.VERSION.SDK_INT` 分岐の単体確認で代替可能としています。しかし Scenario が要求するのは、実際の項目群が disabled で、操作しても選択・外観が変わらないことです。分岐の単体確認だけでは UI の配線逆転やクリック抑止漏れを検出できません。
- Sample のビルド、既存回帰テスト、comment-policy 等の静的検査がタスクにありません。現在のタスクだけならコンパイル不能でも全項目を完了扱いにできます。

**推奨修正**:

- 各検証を4実行面の表にし、Theme 明示画面・既定 Theme 画面・予約日をすべて両 MAUI target で確認する。
- API 30 は実 Emulator で確認するか、実際の `MenuScreen` を描画してクリックまで行う Compose UI テストにする。
- iOS Sample、Android Sample、MAUI 両 TFM のビルド、関連既存テスト、標準 lint の実行と件数確認を追加する。

### [🟡 Minor] M 級判定が複数 capability 横断基準と整合していない

**該当箇所**: [proposal.md:18](proposal.md:18)、`proposal.md:35-37`、`exploration.md:60-65`

**問題点**: proposal 自身が `samples-ios / samples-android / samples-maui` の3能力を列挙していますが、Kasane の基準では複数能力横断は L 級です。オーナー確定済みでありレビューゲートも M/L 共通ですが、M とする理由が「サンプル限定・可逆」だけで、L の明示条件をどう扱ったか説明されていません。実際、Android の方式選択など design 相当の未確定事項が残っています。

**推奨修正**: L へ再分類して `design.md` にプラットフォーム別の状態・永続化・外観伝播を記述するか、M とするオーナー承認済み例外であることと、その理由を明記してください。

## アクションプラン

1. Android の「システム」モードと非 Material host の維持方法をオーナー判断で確定する。
2. カレンダー範囲を全 target で可視な値へ変更し、モックを更新する。
3. dark Theme の不足ロールと MAUI chrome の扱いを仕様化する。
4. 4実行面の検証マトリクスとビルド・lint・回帰確認を tasks に追加する。
5. 変更級を再確認してから再レビューする。

## 総合判定

**NEEDS_DISCUSSION**

Android の「システム」意味論と ADR-0020 の検証責務は実装者判断で埋めるべきではなく、オーナーによる設計選択が必要です。


## 突き合わせ結果 (2026-09-05)

ホスト側自己レビュー (整合性チェックリスト・UI lint・L-002 の 3 軸) は指摘 0 件で、相方の 7 件はすべて相方のみの指摘。根拠で判定した:

| # | 指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| 1 | Android「システム」の `UiModeManager` への写像が未確定 | **採用** (ホスト側の見逃し、設計変更) | AOSP Javadoc で裏取り: AUTO は位置・センサー、CUSTOM は時刻で、端末追随の値が無い。オーナー再判断で手段 4 (Activity の Configuration 上書き + recreate) へ変更。specs/samples-android・proposal・tasks 3.0/3.1・exploration を改訂。端末外観変更への追随 Scenario を 3 面に追加 |
| 2 | chrome の DayNight 化が ADR-0020 の検証条件を崩す | **採用** | tasks 3.2 を `values/` + `values-night/` の framework テーマ切替に限定 (AppCompat / MaterialComponents の XML テーマは使わない) |
| 3 | 月初〜月末の範囲では同月内に disabled が出ず、モックの月外日は Compose では描画されない | **採用** | 範囲を 06/01〜06/20 に変更 (初期値は維持)。specs 3 面・proposal・brief・モックと approved.png を改訂 |
| 4 | dark プリセットに valueText / description の色ロールが無い | **採用** | Android の description 既定 (#6D6D72 固定) が夜間に追随しないことをコードで確認。dark 側に `cellValueTextColor` / `cellDescriptionColor` を追加 (specs 3 面・brief・モック対応表) |
| 5 | MAUI のナビゲーションバー固定色と「アプリ全体」が矛盾 | **採用** (契約を狭める側) | バーは両外観で判読できる固定色のため変えず、spec / proposal / brief で対象外を明記 |
| 6 | 検証計画が 4 実行面と Scenario を覆っていない | **採用** | tasks 5 を 4 実行面の表形式に改訂、端末外観変更の追随・選択面のダーク提示・ビルド / lint を追加。API 30 の項目は手段 4 で API 分岐自体が消えたため不要 |
| 7 | M 級判定が複数能力横断基準と整合しない (Minor) | **降格** | オーナー確定済みで、同形の前例 (align-timepicker-hour-cycle-across-platforms) に倣う M 運用。proposal の級の理由に前例と確定日を追記した |

未解決: なし。判定は採用 6 / 降格 1 / 未解決 0。
