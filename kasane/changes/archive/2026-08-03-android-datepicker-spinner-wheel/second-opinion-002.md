# セカンドオピニオン: android-datepicker-spinner-wheel (002 — 実装レビュー並走)

**相方**: codex (gpt-5.6-sol / effort high、器: ksn-reviewer) / **日付**: 2026-08-02 / **対象**: worktree 未コミット変更全体 (DateSelectionSheet 新設 + KsWheelView 拡張 + todayText パリティ実装)

---

# レビュー結果: android-datepicker-spinner-wheel

**判定**: CHANGES_REQUESTED
**件数**: Critical 0 / Major 2 / Minor 2 / Suggestion 0

## サマリー

基本的な候補計算、範囲丸め、確定契約、アクセシビリティ操作、公開 API の伝播は概ね仕様どおりです。一方、Locale 固定順と慣性移動中のプログラム選択に、実利用で仕様違反となる問題があります。

## 指摘事項

### [🟠 Major] RTL Locale で年・月・日の表示順が反転する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:465`

**問題点**: 3列を通常の水平 `LinearLayout` に年→月→日の順で追加していますが、`LinearLayout` は RTL 環境では子の視覚配置を反転します。そのため Arabic 等では日→月→年となり、仕様の「系列の並び順は Locale によらず年→月→日で固定」を満たしません。現在のテストも `en-rUS` で `indexOfChild` を確認するだけで、RTL における実際の左右位置を検証できていません。

**推奨修正**: ホイールコンテナの `layoutDirection` を明示的に LTR に固定してください。併せて `ar-rEG-ldrtl` 等でレイアウトし、`yearWheel.left < monthWheel.left < dayWheel.left` を検証する回帰テストを追加してください。

### [🟠 Major] 慣性移動中の「今日」ジャンプが後続 fling に上書きされる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:283`、同 `:302`

**問題点**: `setCandidates`、`setSelectedIndex`、アクセシビリティ移動が `LinearLayoutManager.scrollToPositionWithOffset` を直接呼んでいます。この API は位置を予約して再レイアウトするだけで、`RecyclerView` の実行中の fling を停止しません。ホイールを fling した直後に「今日」を押すと、一度今日へ更新された後も旧 `ViewFlinger` が進行し、静止時の通知で別の日付へ上書きされ得ます。候補差し替え中も同様に旧スクロールが新候補へ持ち越されます。

**推奨修正**: プログラム移動・候補差し替えの前に `listView.stopScroll()` で既存スクロールを停止してください。停止時の `IDLE` 通知が中間値を発火しないよう、通知抑止フラグも設ける必要があります。「fling開始→静止前に今日を押す→Looperをidleまで進めても今日のまま」という回帰テストを追加してください。

### [🟡 Minor] Android Sample の文言が iOS Sample と不一致

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:207`

**問題点**: Android の footer を変更していますが、対応する iOS Sample は `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:201` の「ホイール形式で日付を選択するデモ。」のままです。`sample-parity` 規約は対応画面の文言完全一致を要求しており、片側先行を追跡する tasks / deviation もありません。

**推奨修正**: Android の footer を既存の共通文言へ戻すか、別途追跡を明示したうえで両プラットフォームを同じ文言へ揃えてください。

### [🟡 Minor] 新規コメントが履歴記述を含んでいる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:123`

**問題点**: 「旧 `AlertDialog` 実装の〜引き継ぎ」は過去実装を説明する履歴記述で、`comment-policy` の禁止類型に該当します。

**推奨修正**: 「確定・取消操作色は `androidButtonColor` を最優先する」のように、現在の契約だけで自己完結する記述へ変更してください。

## アクションプラン

1. プログラム選択前に進行中のスクロールを安全に停止する。
2. 3列の視覚配置を LTR 固定し、RTL 回帰テストを追加する。
3. Sample の footer 文言を iOS と一致させる。
4. 履歴依存コメントを書き換える。
5. 既存テストに加えて fling 中ジャンプと RTL 配置のテストを実施する。

指定に従い、レビュー結果ファイルの作成およびビルド・テストの再実行は行っていません。既知の到達不能 Scenario は指摘対象から除外しています。

---

## 突き合わせ結果 (ホスト review-001 との採否)

| # | 指摘 | 出典 | 採否 | 確定重要度 |
|---|---|---|---|---|
| 1 | プログラム的スクロールが進行中の fling を止めない (「今日」ジャンプ・候補差し替えの上書き) | 双方一致 (host: Minor / 相方: Major) | **確定** | **Major** (相方の実害シナリオ — spec「3系列の選択中がデバイスの現在日付になる」の競合下違反 — により高い方を採用) |
| 2 | comment-policy 違反 — host: DateSelectionSheet.kt 6箇所 (「承認モックの N dp」参照)、相方: DatePickerCellViewHolder.kt:123 (履歴記述) | 双方 (箇所は相補) | **確定** (両箇所とも修正) | **Major** (host 判定基準) |
| 3 | RTL Locale で列順が日→月→年に反転 (spec「列順は Locale によらず年→月→日で固定」違反) | 相方のみ | **採用** (該当箇所特定 + 実害シナリオあり。ホスト側の見逃し) | **Major** |
| 4 | Android sample footer の片側先行変更が sample-parity 規約 (ADR-0016) の追跡条件を満たさない | 相方のみ | **採用** (ADR-0016 と iOS 側現物で根拠確認済み) | **Minor** |
| 5 | 非 ISO 暦 Locale (th/fa/ar) で DateTimeFormatter が ISO 暦・ASCII 数字のまま | host のみ (Suggestion) | 記録のみ (修正サイクルに回さない) | Suggestion |
| 6 | シート固定高が横向きで下端切れの可能性 (NumberSelectionSheet と共通の既存パターン) | host のみ (Suggestion) | 記録のみ (既存パターンのため本 change のスコープ外) | Suggestion |

未解決 (矛盾) の論点: なし

**修正サイクル対象**: #1〜#4 (Major 3 件相当 + Minor 1 件)
