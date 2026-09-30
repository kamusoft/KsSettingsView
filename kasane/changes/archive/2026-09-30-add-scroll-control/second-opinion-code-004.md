# セカンドオピニオン: add-scroll-control (code-004)
**相方**: codex / **label**: so-code-add-scroll-control-ext / **日付**: 2026-09-30 / **対象**: オーナー指示の追加スコープ 5 件 (deviation.md「スコープの拡張」) の実装と既存実装への影響 (コミット 38a6c74 からの作業ツリーの差分)
---
# レビュー結果: add-scroll-control 追加スコープ

**判定: CHANGES_REQUESTED** — Major 1 件。追加スコープ (1)〜(5) と既存のスクロール制御への影響を静的に確認しました。ビルド・テストは実行せず、提示された成功結果を前提としています。

## 指摘事項

### 🟠 Major: 再レイアウト後に `center` / `end` の位置合わせが失われる

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:201`

**問題点**: レイアウト中に実行した命令の後、祖先の再レイアウトで表示領域の高さが変わっても、補正は上端アンカーを保つだけです。例えば高さが 500 から 552 に伸びると、`.center` で合わせた Cell は新しい中央から 26pt ずれます。`.end` も同様にずれます。これは `kasane/changes/add-scroll-control/specs/settings-view-ios-ui/spec.md:73` に反します。追加された回帰テストはアンカーの復元だけを確認しており、この命令経路を検出しません。

**推奨修正**: 再レイアウト時には、最後に実行した命令の対象と指定位置を新しい表示領域で解き直してください。`restoreScrollAnchor` の場合だけアンカーを保ちます。既存の「親の領域が伸びる」テスト構成で、`.center` と `.end` の最終位置も確認してください。

## 照合した規約

`cross/comment-policy.md`、`cross/test-execution.md`、`cross/runtime-behavior-verification.md`、`cross/sample-parity.md`、`cross/public-identifiers.md`、`cross/diagnostic-message-language.md`、`ios/swift6-language-mode-check.md`、`maui/integration-host-verification.md`。deviation.md の合意済み差分と蒸留・docs-refresh への申し送りは指摘対象から除外しました。

## 突き合わせ結果

ホスト側: review-004.md (APPROVED、Minor 1 / Suggestion 3)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| iOS の再レイアウト後に `center` / `end` の位置合わせが失われる | 双方一致 (相方 Major / ホスト Suggestion。ホストは `.end` は影響なしと判定) | 確定 (Major) | 双方が同じ箇所を指摘。重要度は高い方を採る。`.end` の影響は判定が割れているため、修正時に `.center` / `.end` の両方をテストで確かめる |
| `scheduleAncestorRelayout()` の「戻し直さない」2 条件を守るテストが無い | ホストのみ (Minor) | 確定 | ホスト側判定どおり |
| iOS Sample の `MainActor.assumeIsolated` が本体の公開 doc に無い「通知はメインスレッドで呼ばれる」前提に頼る | ホストのみ (Suggestion) | 採用 | 本 change の (5) の書き方の根拠。前提を確かめたうえで公開 doc に明記する (型は変えない) |
| `ComposeRootAccessoryTextTest` の両方渡しのテストが行の配置を待たずに不在を確かめる | ホストのみ (Suggestion) | 採用 | 本 change で足したテストで数行で閉じる |

未解決: なし。skills/ の例と Sample の書き方の食い違いは docs-refresh への申し送り
