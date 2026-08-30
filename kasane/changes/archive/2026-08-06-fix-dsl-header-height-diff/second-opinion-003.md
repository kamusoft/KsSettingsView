# セカンドオピニオン: fix-dsl-header-height-diff (003 — code-review 2周目)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 修正サイクル 1 周目後の実装 diff (HEAD=5f7d97e からの未コミット変更) / **セッション**: 002 と同一 (turns=2)
---
前回の Major 2件・Minor 1件は、いずれも実質的に解消されています。新たにテストのライフサイクル管理について Minor 1件があります。

## Critical

なし。

## Major

なし。

## Minor

### Controller の生存期間が明示的に保証されていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:405`

**問題点**: ヘルパーは `controller.view` を `window.addSubview` していますが、`window.rootViewController` は設定していません。また3テストとも戻り値を `let (_, cv, window)` と受けており、controller 自体を保持していません。

Controller の Store 購読は `[weak self]` で、購読の `AnyCancellable` は Controller 自身が所有しています。このため、UIView の保持だけに依存すると Controller の生存期間が言語・UIKit上で明示的に保証されず、最適化や実行環境によっては早期解放されて購読が解除される可能性があります。現在の602件成功から現テスト環境では生存していると判断できますが、テストの安定性に偶発的な寿命延長が混ざっています。

**推奨修正**: ヘルパー内で次のように UIKit の正式な所有関係を作ってください。

```swift
let window = UIWindow(frame: rootView.frame)
window.rootViewController = controller
window.makeKeyAndVisible()
```

または、各テストで controller を変数へ受け、`withExtendedLifetime(controller)` まで明示してください。

## Suggestion

なし。

## 前回指摘の解消確認

- Major-1: 解消済みです。headerHeight 分岐が可視性分岐より先に評価され、`.full → .replaceCell` が維持されています。Section追加・削除は `.full` の新ツリーに内包され、三重併発も回帰テストで固定されています。
- 非表示除外: 妥当です。新 snapshot に存在する可視 Section／Cellだけを再構成対象としており、非表示要素の最新モデルは `.full` が保持します。本来必要な可視 Cellの更新は落としていません。
- Major-2: Store APIから Publisher、Controllerの `applyDiff` まで実際の購読経路を通っています。ただし上記のController寿命だけ明示化が望まれます。
- Minor: verification READMEから実機シリアルとSimulator UUIDが削除され、機種・OS情報だけになっています。
- Androidリファクタリング: `compute` と `contentUpdates` が同じ `requiresFullRefresh` を参照しており、条件の不一致や振る舞いの変更はありません。

## 総合判定

**APPROVED**

Critical／Majorはなく、残るMinorはテストコードの所有関係を明確化する改善であり、実装の正しさを否定するものではありません。
