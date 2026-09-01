# セカンドオピニオン: fix-ios-tapnotifyingrenderer-actor-isolation (spec-001)
**相方**: codex / **label**: so-spec-fix-ios-tapnotifyingrenderer-actor-isolation / **日付**: 2026-09-01 / **対象**: 提案一式 (proposal.md / specs/ios-host/spec.md / tasks.md)
---
# レビュー結果: fix-ios-tapnotifyingrenderer-actor-isolation

**日付**: 2026-09-01  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 4 / Minor 2 / Suggestion 0

## サマリー

`TapNotifyingRenderer` と `configurationUpdateHandler` の方針は探索でコンパイル確認されており、方向性は妥当です。一方、Swift 6 対応範囲の矛盾、継続的な検証手段の欠落、`deinit` 設計の未決着が残っているため、この状態で実装フェーズへ進むべきではありません。

静的レビューのため、ビルド・テストは実行していません。また、指定どおりレビュー結果ファイルは作成していません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always。`deinit` 周辺コメント更新に適用）
- `kasane/handbook/cross/test-execution.md`（tasks 4.2 の Simulator 全件検証に適用）
- `kasane/handbook/cross/runtime-behavior-verification.md`（タッチフィードバックの回帰確認に適用。ただし元症状はコンパイル診断のため、修正前の実行時不具合再現は非該当）
- `swift-ui-impl-skill/references/swift-language.md`（Swift Concurrency / actor isolation）
- 関連概念: `ios-native-host.md`、`cell-renderer-registry.md`

## 指摘事項

### [🟠 Major] 「Swift 6 へ切替可能」とテストターゲット除外が矛盾している

**該当箇所**: `proposal.md:19`、`proposal.md:20`、`specs/ios-host/spec.md:7`

**問題点**: proposal は「切り替えてもエラーゼロ」の状態を完成条件とする一方、直後にテストターゲットの Swift 6 適合を除外しています。Scenario の `Package.swift` への言語モード設定はパッケージ全体へ影響しますが、`xcodebuild build` では source 4 ターゲットしか検証せず、将来の恒久切替後に `xcodebuild test` が成功する保証はありません。proposal の完成条件とデルタスペックの source-only 契約が一致していません。

**推奨修正**: 次のどちらを契約とするか決定してください。

- source ターゲットだけの事前適合とし、「パッケージを切替可能」という表現を全成果物で狭める。
- テストターゲットも対象へ含め、Swift 6 言語モードで Simulator 全件テストのコンパイル・実行まで検証する。

### [🟠 Major] 維持契約を将来検出するゲートがない

**該当箇所**: `specs/ios-host/spec.md:7`、`tasks.md:20`

**問題点**: Requirement は「切替可能な状態を保つ」と継続的な契約を定めていますが、検証後に `Package.swift` を戻すため、通常のビルドや CI は Swift 5 モードのままです。後続変更が Swift 6 エラーを再導入しても検出されず、Scenario は今回限りの手動受け入れ確認にしかなりません。

**推奨修正**: Swift 6 互換ビルドを再現可能なスクリプトまたは CI ゲートとして残してください。継続検査を行わない方針なら、Requirement を「本 change 完了時に source targets の適合を確認する」という一回限りの受け入れ条件へ変更し、「保つ」という契約を外してください。

### [🟠 Major] `deinit` の設計判断が実装タスクへ先送りされている

**該当箇所**: `proposal.md:15`、`tasks.md:14`、`tasks.md:15`

**問題点**: 明示解放を全削除するのか、別の lifecycle 境界へ移すのか、`isolated deinit` 等を使うのかが決まっていません。探索でも「提案フェーズで詰める」とされた論点です。

既存 `MemoryLeakTests` が観測するのは Controller の weak 参照が `nil` になることと、その後の Store 操作がクラッシュしないことだけです。全処理を一度に削除してテストが通っても、各 cancel／delegate 解除／dataSource・CollectionView 関連資源の解放が個別に不要だとは判定できません。また、Store 操作後の状態や通知結果を assert していないため、「正常に使い続けられる」の検証も弱い状態です。

**推奨修正**: 実験は探索段階で完了させ、実装開始前に次を確定してください。

- 採用する `deinit`／cleanup 方針と、deployment target 上での成立条件
- 各購読・UIKit オブジェクトの所有関係と、自動解放に任せられる根拠
- Controller 以外の内部資源までリーク非発生を保証するか
- 解放後の Store 操作について、期待する状態変更・通知を観測できる受け入れ条件

### [🟠 Major] ハイライト解除側の Scenario を既存テストが検証していない

**該当箇所**: `specs/ios-host/spec.md:25`、`tasks.md:21`

**問題点**: Scenario は「押下中に選択色」「離すと平常背景へ戻る」の二つを要求します。しかし既存の `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:985` は `isHighlighted = true` 後の選択色しか確認しておらず、解除後の復元を検証していません。今回の変更は handler 本体を書き換えるため、解除分岐を壊しても既存テストが通ります。

**推奨修正**: `isHighlighted` を `true → false` と遷移させ、選択色と平常時の実効背景色を順に assert する回帰テストを tasks に明記してください。

### [🟡 Minor] 行タップ Scenario の対象 Cell とテスト範囲が曖昧

**該当箇所**: `specs/ios-host/spec.md:19`

**問題点**: 「Command / Picker 等」では、11 種すべてを要求するのか代表例だけでよいのか判定できません。Controller の `didSelectItemAt → TapNotifyingRenderer` 実経路を既存テストで通しているのは主に CustomCell で、基本 Cell の多くは `tapHandler` を直接呼んでいます。個別 extension を誤って外しても source build は成功します。

**推奨修正**: 対象となる11種を列挙するか、「基本操作系・モーダル入力系・Entry・Custom」の代表カテゴリを定義し、どのテストが各カテゴリを担保するか tasks に対応付けてください。

### [🟡 Minor] 既知の合意済み作業を「付随修正」として扱っている

**該当箇所**: `tasks.md:10`、`proposal.md:21`

**問題点**: downcast 修正は探索時点で発見され、proposal に明記された合意済みスコープです。実装中に見つかったスコープ外修正を表す Kasane の `[付随修正]`／`deviation.md` の扱いとは一致しません。

**推奨修正**: 通常のスコープ内タスクとして記載してください。付随修正として扱うなら、実装時に `deviation.md` へ記録する運用を明示してください。

## アクションプラン

1. Swift 6 対応範囲を source-only とするか、テストターゲットまで含めるか決定する。
2. `deinit` の実験と設計判断を提案承認前に完了させる。
3. Swift 6 適合を継続的に検査するゲートの有無を決定する。
4. ハイライト解除、Store 解放後の操作、行タップ対象の受け入れテストを具体化する。
5. `[付随修正]` の分類を整理したうえで再レビューする。


## 突き合わせ結果

| 指摘 | 採否 | 対応 |
|---|---|---|
| Major: 「切替可能」とテストターゲット除外の矛盾 | **採用** | proposal の到達点表現を「source 4 ターゲットのエラーゼロ」に狭めた (spec は当初から source-only で整合) |
| Major: 維持契約を将来検出するゲートがない | **採用 (ユーザー裁定: handbook 規約化)** | CI ゲートではなく「iOS ソースを触る変更は完了判定前に Swift 6 一時設定ビルドで確認する」を handbook 規約として追加する (蒸留への申し送り — proposal.md What Changes (4)) |
| Major: deinit の設計判断の先送り | **採用** | 提案フェーズ中に実測で決着: deinit を一時削除して MemoryLeakTests 2 件 pass、全 closure が `[weak self]` 捕捉でサイクル不在を確認。方針「deinit 全削除」を proposal / tasks に確定記載 |
| Major: ハイライト解除側の Scenario が未検証 | **採用** | tasks 2.3 (true→false 遷移の復元テスト追加) を新設 |
| Minor: 行タップ Scenario の対象が曖昧 | **採用** | spec に準拠 11 種を列挙し、準拠網羅テスト (tasks 1.2) を新設 |
| Minor: 合意済み作業を「付随修正」扱い | **採用** | downcast 修正を通常のスコープ内タスクに変更 (tasks 2.2)、proposal Non-Goals の記述も整理 |
