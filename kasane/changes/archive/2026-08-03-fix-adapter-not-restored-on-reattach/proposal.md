# Proposal: fix-adapter-not-restored-on-reattach

## Why

Android の `KsSettingsView` は `onDetachedFromWindow()` で内部 RecyclerView の adapter を `null` にするが、`onAttachedToWindow()` に adapter を戻す処理がない。adapter がセットされるのは初期化時の 1 回のみ。

したがって **一度 detach されてから再 attach された `KsSettingsView` は、`internalRoot` にデータを保持したままリストが空で復帰する**可能性がある。ViewPager2 の offscreen ページ、Compose の `AndroidView` の付け外し、Fragment の view の detach / reattach などで到達し得る。

該当箇所 (行番号は 2026-08-03 時点):

- `android/ks-settingsview-ui/.../KsSettingsView.kt:212` — adapter のセット (初期化時の 1 回のみ)
- `KsSettingsView.kt:259` — `onDetachedFromWindow()` 内の `recyclerView.adapter = null`
- `KsSettingsView.kt:235-250` — `onAttachedToWindow()`。adapter を戻す処理がない

`adapter = null` は Android UI 基盤の初期実装 (`8d78b49`) 由来で、**メモリリーク対策として意図的に入れられたもの**。`MemoryLeakTest.kt` が「detach 後は adapter が null」を 2 件のテストで明示的に固定している (`onDetachedFromWindow で RecyclerView adapter が null になる` / `Store 経由で setRootDirect しても detach 後 adapter が null になる`)。

発見経緯: `fix-picker-dialog-recreation` (ピッカーダイアログの Activity 再生成復元) の実装中に副次的に気づいたもの。同変更の diff にこの行は含まれず、既存の挙動であることを確認済みのため、独立した変更として切り出した。

## 未確認事項 (最初に潰すこと)

**本提案は静的読解のみに基づく。実環境・テストでの再現は未実施。**

- 実際にリストが空になるのか (RecyclerView が detach / attach をまたいで adapter 参照をどう扱うかは要実測)
- 到達する実利用シナリオが本当にあるか

再現が取れなかった場合は「なぜ問題にならないか」を記録して変更をクローズする。**症状の再現確立が実装の前提**であり、再現できないまま「直った」とはしない。

## What Changes

対象能力: **settings-view-android-ui** (Android View Host)

- 再 attach 時に adapter が復帰するようにする。素直な案は `onAttachedToWindow()` で `recyclerView.adapter` を復元することだが、**`adapter = null` の元の意図 (RecyclerView からの参照断ちによるリーク防止) を壊さない形にする**
- 退行テストを追加する (detach → reattach でリスト内容が保たれること)
- 既存の `MemoryLeakTest` の 2 件は「detach 後は null」を固定しているため引き続き green を保つ。両立できない設計を採る場合は、その理由と既存テストの扱いをユーザーに確認する

## Non-Goals

- 公開 API の変更
- `adapter = null` によるリーク対策そのものの見直し (両立させる。撤回はしない)
- iOS / MAUI (Android View Host 固有)
- `fix-picker-dialog-recreation` で追加された復元走査への影響対応 — 同走査は `internalRoot` を直接読むため本件の影響を受けない (関連テスト `PickerDialogRecreationTest` の「予約分を detach 中に消化しても再 attach で復元される」がこの経路を通り green)

## Impact

- 破壊的変更なし。挙動変更は「再 attach 後に空だったリストが内容を保って復帰する」のみ
- リスク: リーク対策との両立。adapter を保持し直す実装がリークを再導入しないことを `MemoryLeakTest` で担保する必要がある

## 級: S

バグ修正 / 単一能力内 / 公開 API 変更なし / 局所的かつ可逆。

デルタスペックは作成しない (S 級のため verify は非適用)。受け入れ基準は tasks.md が持つ。独立文脈でのレビューは S 級でも必須。

domain: android
