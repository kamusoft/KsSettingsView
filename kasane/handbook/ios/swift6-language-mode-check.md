---
kind: rule
applies-when:
  always: false
  paths: ["ios/Sources/**"]
  tasks: [iOS source を触る変更の完了判定]
title: iOS source の Swift 6 言語モード適合確認
description: ios/Sources/ を触る変更は、完了判定の前に Swift 6 言語モードの一時設定ビルドでエラーゼロを確認する
timestamp: 2026-09-01
---

# iOS source の Swift 6 言語モード適合確認

`ios/Sources/` 配下 (source 4 ターゲット: Core / UI / SwiftUI / Bridge) を触る変更は、完了判定の前に Swift 6 言語モードでのビルドを一時的に行い、エラーゼロを確認する。

`ios/Package.swift` は Swift 5 言語モードのままであり、Swift concurrency の分離違反は通常ビルドでは警告にしかならない。警告のまま放置すると toolchain / 言語モード更新時に一括でエラー化するため、「source ターゲットは Swift 6 言語モードでエラーゼロ」の状態をこの確認で維持する (fix-ios-tapnotifyingrenderer-actor-isolation で到達した状態の維持。CI ゲートは採らない)。

## 手順

1. `ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時追加する
2. `xcodebuild build -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` でパッケージ全体をビルドする
3. error が 0 件であることを確認する (warning はこの確認の対象外 — Swift 6 モードでも警告に留まるものは残存してよい)
4. 一時設定を戻し、`ios/Package.swift` の差分が 0 件であることを確認する

## 範囲外

- テストターゲット (`ios/Tests/`) — build アクションの対象外で本確認では検査されない。Swift 6 言語モードへの恒久切替時に露出したものを対応する
- Swift 6 言語モードへの恒久切替の時期 — toolchain 更新のタイミングで行う運用判断

到達状態: 完了報告に Swift 6 一時設定ビルドの結果 (error 0 件) が含まれ、`ios/Package.swift` に一時設定が残っていない。
