---
id: 0009
title: MAUI facade は素の net10.0 TFM を持ち Bridge 呼び出しを internal gateway 抽象で隔離する
status: accepted
date: 2026-08-08
---

## Context

`KsSettingsView.Maui` (facade 層) の本丸は、コレクション購読 → Bridge 操作の変換経路 (対応表・dirty set flush・ItemsSource 器) である。この純ロジックを高速に網羅テストしたいが、Bridge を運ぶ Binding assembly は platform TFM (net10.0-ios / net10.0-android) でしか参照できず、素の `dotnet test` から到達できない。後続の全 Cell フェーズが同じ変換経路に DTO 変換を足していくため、ここで決めるテスト戦略とプロジェクト構成は以後のフェーズを制約する。

## Decision

`KsSettingsView.Maui` の TargetFrameworks を `net10.0;net10.0-ios;net10.0-android` とする。変換経路・対応表・dirty set・ItemsSource 器などの純ロジックは platform 非依存コードに置き、Bridge 呼び出しは internal な gateway インターフェース (Store 操作 1:1 + `updateAccessory` + lifecycle) 越しに行う。platform TFM のみが Binding 参照・gateway 実装・Handler の platform 部分を持つ。ユニットテストは素の net10.0 で fake gateway (呼び出し記録 + ID 採番 + Bridge の no-op/null 契約の再現) を注入して検証する。

## Alternatives Considered

- **platform TFM のみでデバイス/シミュレータテストに寄せる** — 変換経路の網羅検証が遅く、回帰のたびに実機系ビルドが要る。却下
- **純ロジックを別アセンブリに分離** — 利用者から見えるアセンブリが2つに割れ公開面の管理が複雑化する。internal seam で同じ効果が得られる。却下

## Consequences

- 正: 変換経路の回帰が `dotnet test -f net10.0` で高速に回る (phase-2 実績: 115件)。fake gateway が Bridge の interop 契約 (未知 ID no-op / ID 採番) をテスト内で再現できる
- 正: 後続 Cell フェーズは gateway に DTO 変換メソッドを足すだけで同じテスト戦略に乗れる
- 負: net10.0 はアプリ実行に使われない「テスト用の顔」であり、NuGet パッケージング時に TFM 構成の再検討が必要 (実装結果より)
- 負: platform 固有経路 (iOS の UIKit containment 実呼び出し等) は net10.0 テストから到達不能で、検証ホストの E2E に依存する (実装結果より)

出典: kasane/changes/archive/2026-08-08-add-maui-core/design.md (Decision 1)
