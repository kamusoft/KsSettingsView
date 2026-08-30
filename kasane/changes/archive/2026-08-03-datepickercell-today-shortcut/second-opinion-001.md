# セカンドオピニオン: datepickercell-today-shortcut (001 回目)
**相方**: codex / **日付**: 2026-08-03 / **対象**: 提案一式 (proposal / specs / tasks / ui / exploration + ADR-0010)
---
# レビュー結果: datepickercell-today-shortcut

**判定**: `NEEDS_DISCUSSION`  
**指摘件数**: Critical 0 / Major 4 / Minor 4 / Suggestion 0

## サマリー

通常ダイアログで承認モックの配置を実現できない実装指示、経路 A 失敗時の扱いの矛盾、非同期リトライと再 build のライフサイクル未定義が残っています。また、予定されているテストでは ViewHolder との結線漏れを検出できません。実装前に仕様・tasks の補強が必要です。

## 指摘事項

### [🟠 Major] `index 0` への追加では承認モックの左端配置にならない

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/tasks.md:9`、`kasane/changes/datepickercell-today-shortcut/ui/brief.md:12`、`kasane/changes/datepickercell-today-shortcut/ui/brief.md:15`

**問題点**: material 1.12.0 の `date_picker_actions` は `gravity="end"` の水平 `LinearLayout` です。今日ボタンを index 0 に挿入するだけでは `[今日][キャンセル][OK]` が右端に固まるため、承認モックの `[今日] … [キャンセル][OK]` にはなりません。

さらに、フルスクリーン時の `confirm_button` の親はヘッダ内の確定ボタン・入力モードトグルを収めるコンテナです。ここへの追加は mock 対象外かつ「実装判断」とされ、配置崩れを判定する基準もありません。

**推奨修正**: `ui/brief.md` と tasks で通常ダイアログ／フルスクリーンそれぞれの配置方法を確定してください。通常ダイアログでは weight 付き spacer 等を含め、子 index ではなく実座標で「今日が左、キャンセル/OKが右」を検証します。フルスクリーン用レイアウトを inflate／表示する契約・統合テストも追加してください。

### [🟠 Major] 経路 A 失敗時の no-op 許容が Requirement と ADR に反する

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/proposal.md:11`、`kasane/changes/datepickercell-today-shortcut/proposal.md:31`、`kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:21`、`kasane/decisions/android/0010-datepicker-today-jump-via-native-click-path.md:20`

**問題点**: proposal 前半と ADR は「経路 A を駆動できなければ必ず経路 D」と定めていますが、Impact では「フォールバック D か no-op」とされています。範囲内の今日に対する no-op は、「表示月・選択日を今日にする SHALL」および代替表示状態の SHALL を満たしません。

**推奨修正**: no-op を許容する条件を「今日が範囲外」「ダイアログが既に破棄済み」などに限定してください。表示中かつ範囲内で経路 A が失敗した場合は、ADR どおり経路 D を必須とし、グリッド未発見を意図的に発生させるフォールバックテストを追加してください。

### [🟠 Major] 非同期リトライと再 build の排他・キャンセル条件が未定義

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/tasks.md:5`、`kasane/changes/datepickercell-today-shortcut/tasks.md:16`、`kasane/changes/datepickercell-today-shortcut/tasks.md:18`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:126`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:130`

**問題点**: `post` によるリトライ中に、連打、モード切替、キャンセル、Activity の state save／破棄が起こる場合の扱いがありません。複数のリトライがそれぞれ再 build したり、ユーザーが閉じたダイアログを遅延処理が再表示する可能性があります。

現行 Colorizer は pre-draw の解除は行いますが、追加される posted task のキャンセル機構はありません。また、再 build で同じ Fragment tag を使う場合、旧 Colorizer は旧 Fragment の `onFragmentDestroyed` まで登録されたままです。

**推奨修正**: ジャンプ処理を single-flight にし、Fragment view の破棄・dismiss 時にリトライを無効化する世代番号またはキャンセルトークンを設けてください。再表示は旧 Fragment の破棄後に行い、tag の世代分離も定めます。「リトライ中に dismiss」「今日操作の連打」で、再表示・重複ボタン・複数 callback が発生しないテストを追加してください。

### [🟠 Major] 予定された統合テストでは ViewHolder との結線漏れを検出できない

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/tasks.md:22`、`kasane/changes/datepickercell-today-shortcut/tasks.md:23`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerDialogIntegrationTest.kt:66`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:61`

**問題点**: 現行 `DatePickerDialogIntegrationTest.showPicker()` は `MaterialDatePicker` と `DatePickerColorizer` を直接構築しており、`DatePickerCellViewHolder.bind`、`todayText`／`todayProvider` の受け渡し、constraints、positive callback の実配線を通りません。Colorizer 単体テストがすべて成功しても、ViewHolder が新しい引数を渡し忘れた状態を見逃せます。

**推奨修正**: 少なくとも代表 Scenario 1件は、固定 `todayProvider` を設定した ViewHolder を bind → 行をタップ → 実 MaterialDatePicker の今日操作 → OK、という公開利用経路に近い統合テストにしてください。通常経路と再 build 経路の双方で callback 回数も検証します。

### [🟡 Minor] アクセシビリティ契約に対応する Scenario とテストがない

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:7`、`kasane/changes/datepickercell-today-shortcut/tasks.md:22`

**問題点**: Requirement はラベルをアクセシビリティサービスへ公開することを明記していますが、Scenario、brief の検証条件、tasks のいずれにも検証がありません。

**推奨修正**: 操作の accessibility node が `todayText`、クリック可能状態、適切なボタン role/class を公開する Scenario とテストを追加してください。

### [🟡 Minor] ヘッダと確定操作の追随が受け入れ基準から落ちている

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:21`、`kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:26`、`kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:55`

**問題点**: Requirement 本文は選択日の表示と確定操作の状態も追随するとしていますが、Scenario は表示月・選択日・callback しか判定していません。特に不完全／不正なテキスト入力で無効化された OK が、今日への切替後に有効へ戻ることを検証できません。

**推奨修正**: テキスト入力に不完全値があり OK が無効な状態を GIVEN とし、今日操作後に日グリッド表示、ヘッダの日付、選択マル、OK 有効状態がすべて追随する Scenario を追加してください。

### [🟡 Minor] 境界日 Scenario が `maxDate` 側しか検証していない

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/specs/settings-view-android-ui/spec.md:38`、`kasane/changes/datepickercell-today-shortcut/tasks.md:23`

**問題点**: Scenario 名は「min/max 当日は有効」ですが、GIVEN は `maxDate = 今日` だけです。`minDate` 比較を誤って排他的に実装しても合格します。

**推奨修正**: `minDate = 今日` と、可能なら `minDate = maxDate = 今日` の境界 Scenario／テストを追加してください。

### [🟡 Minor] `domain: android` の変更に iOS ファイル編集が混在している

**該当箇所**: `kasane/changes/datepickercell-today-shortcut/proposal.md:14`、`kasane/changes/datepickercell-today-shortcut/proposal.md:37`、`kasane/changes/datepickercell-today-shortcut/tasks.md:27`

**問題点**: proposal は Android domain と宣言していますが、iOS ソースコメントの編集もタスクに含みます。Kasane の domain-axis 規約では複数ドメインを触る変更は `cross` です。

**推奨修正**: iOS コメント修正を別の局所変更へ分離するか、本変更を `cross` として扱うかを確定してください。

## アクションプラン

1. 通常／フルスクリーンの配置契約と実装方法を確定する。
2. 経路 A 失敗時の no-op を除外し、経路 D の発動条件を一本化する。
3. リトライ・dismiss・再 build の状態遷移と排他を仕様化する。
4. ViewHolder 経由の統合テストを必須化する。
5. アクセシビリティ、ヘッダ／OK 状態、min 境界の Scenario を補完する。
6. iOS コメント修正の domain を整理する。

依頼どおり、ビルド・テスト実行およびファイル書き込みは行っていません。


## 突き合わせ結果 (2026-08-03、ホスト自己レビュー2周との照合)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| Major 1 | index 0 挿入では左端配置にならない (end gravity) | **採用** | brief.md に配置の実装注意 (スペーサ+実座標判定・フルスクリーンの照合基準) を追記、tasks 2.1 更新 |
| Major 2 | 経路 A 失敗時の no-op 許容が Requirement/ADR と矛盾 | **採用** | proposal Impact の文言を修正 (no-op は範囲外と完了前 dismiss のみ)、tasks 3.4 に明記 |
| Major 3 | リトライと再 build の排他・キャンセル未定義 | **採用** | spec に冪等性・完了前 dismiss の Scenario を追加、tasks 3.5 (single-flight/世代分離) を新設 |
| Major 4 | 統合テストが ViewHolder 結線を通らない | **採用** | tasks 4.5 (公開経路の統合テスト、通常+再build 双方) を新設 |
| Minor 1 | a11y の Scenario 不足 | **採用** | spec にアクセシビリティ公開 Scenario を追加 |
| Minor 2 | ヘッダ/OK 状態追随の検証不足 | **採用** | spec に「不完全入力で無効化された確定操作が有効に戻る」Scenario を追加 |
| Minor 3 | 境界日が maxDate 側のみ | **採用** | Scenario を minDate = maxDate = 今日 に拡張 |
| Minor 4 | domain: android に iOS ファイル編集が混在 | **確定 (android 維持)** | iOS はコメントのみで蒸留知識を生まないため android 維持 — 2026-08-03 オーナー確定 (proposal に注記) |

採用 7 / 降格 0 / 未解決 0 (Minor 4 は 2026-08-03 オーナー判断で android 維持に確定)。
