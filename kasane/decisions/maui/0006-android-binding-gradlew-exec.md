---
id: 0006
title: Android Binding は AndroidGradleProject ではなく gradlew 直接実行方式を採る
status: accepted
date: 2026-08-05
---

## Context

.NET の Native Library Interop では、binding csproj からネイティブビルドを駆動する標準手段として `XcodeProject` / `AndroidGradleProject` ビルドアイテムが提供されており、maui-support ロードマップと phase-1 の proposal もこの形式を前提としていた。

phase-1 実装は当初、両 OS とも「SDK 制約」を理由にスクリプト/Exec 方式で実装されたが、これは無記録の逸脱として指摘され (second-opinion-003)、オーナー指示の ksn-dual-research (相方 codex + ホスト側実験) で主張の裏取りを行った。結果:

- **iOS の「SDK 制約」主張は誤り** — 標準 `XcodeProject` 方式は成立する (同一引数での archive 実験と参考プロジェクト AdMobMediation.Maui の実績で確認。当初失敗の真因は scheme 衝突の可能性が高い)。iOS は標準方式へ復帰した
- **Android の制約は実在** (確度 98-99%) — dotnet/android SDK の init script が `rootProject.allprojects` の buildDirectory を単一パスへ束ねるため、project 依存を持つ現行の複数モジュール構成 (`:ks-settingsview-ui` / `:ks-settingsview-bridge` 等) では aar glob 以前に Gradle validation エラーでビルド自体が失敗することを実測確認。`ModuleName` 指定でも依存連鎖がある限り回避できない

## Decision

Android の Binding csproj は `AndroidGradleProject` アイテムを使わず、`android/gradlew` を Exec タスクで直接呼んで aar を生成し、公式アイテム `AndroidLibrary` (Bind=true) で束縛する方式を採る。pack 経路は公式アイテム経由で標準方式と共通であり、`dotnet pack` の成立は実測済み。

iOS は標準 `XcodeProject` アイテム (+ `CreateNativeReference=false` + 手動 `NativeReference`) を使う。

SDK 側が複数モジュール構成に対応した時点でこの決定は見直す。再検証の入口は `maui/README.md` の「SDK 更新時に再検証する箇所」の表と対で維持する。

## Alternatives Considered

- **A: `AndroidGradleProject` 標準アイテム** — 実測でビルド不能 (init script の buildDirectory 束ねにより Gradle validation エラー)。参考プロジェクトの成立例はすべて単一モジュール構成で反例にならない。却下
- **B: Gradle 側の再構成で標準アイテムに載せる** (binding 専用 wrapper project・fat-aar 統合・モジュール統合等) — いずれも未検証で、native 側のビルド構成に .NET ビルドツールの都合を持ち込む費用対効果が見合わないためオーナー判断で不採用。SDK 側の対応があれば不要になる

## Consequences

- 正: 現行の Gradle 複数モジュール構成を変えずに binding が成立する。pack 経路が標準方式と共通のため、NuGet パッケージングへの影響もない
- 負: SDK のアンダースコア付き内部ターゲット (`_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports` 等) への依存が残り、workload / SDK 更新時に最初に壊れやすい
- 負: SDK 更新のたびに upstream (dotnet/android) の複数モジュール対応状況を再確認する運用が必要

出典: kasane/changes/archive/2026-08-05-add-maui-native-bridge/deviation.md / exploration.md (ksn-dual-research 記録) / second-opinion-003.md (オーナー裁定)
