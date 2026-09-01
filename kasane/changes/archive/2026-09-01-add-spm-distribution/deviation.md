# Deviation: add-spm-distribution

## 実測所見 (proposal の見込みとの差)

- Impact リスク欄: proposal では「Bridge は SwiftUI の記号を参照しないためリンカが落とす見込み」→ 実測では落ちない。archive は `MACH_O_TYPE = staticlib` のため `KsSettingsViewSwiftUI.o` が静的ライブラリにそのまま入り、`nm` で SwiftUI 由来シンボル 642 件を検出 (framework バイナリ 3,174,696 bytes)。spec の受け入れ条件 (ビルド成功・xcframework 生成完了) は満たす。理由: staticlib archive はリンカの dead-strip が働かない。**オーナー合意済み: 受容して進める** (除外が必要になったら将来の change で検討) (2026-09-01)
- スナップショット同期スクリプトの削除操作: 全体ルールでは削除は `trash` → スクリプト内は `rm -rf` を採用。理由: phase-8 で CI から呼ぶ前提のためローカル依存 (`trash`) を避ける。既存 `ios/binding/build-xcframework.sh` と同型で、発火は 4 段の事前検証を通過した配信リポジトリ作業コピーに限る (2026-09-01)

## 付随修正

- [付随修正] `.github/workflows/verify-ios.yml`: scheme 名を `KsSettingsView-Package` → `KsSettingsView` に更新。product 一本化の副作用で Xcode 生成 scheme が package 名と同名の 1 本になり、旧名のままでは検証 CI が全 PR で失敗するため (2026-09-01)
- [付随修正] `kasane/handbook/cross/test-execution.md` / `kasane/handbook/ios/swift6-language-mode-check.md`: 規範が参照する scheme 識別子を実態 (`KsSettingsView`) へ追随。加えてレビューで判明した誤読経路 (バンドル複数時、出力末尾の `Executed` 行は最後のバンドルの値しか映らない) への注意として、バンドル集計行の合算で全体件数を確認する手順を test-execution.md に追記 (2026-09-01)
- [付随修正] `.github/workflows/ci.yml`: lint job に `scripts/spm-snapshot/sync-snapshot-test.sh` を実行する step を追加。破壊的操作を持つ同期スクリプトの安全弁テストを CI で回すため (レビュー採用指摘への対応。spec / tasks の範囲外のため付随修正として記録) (2026-09-01)
- [付随修正] `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj`: `_XcbInputs` に `Sources/KsSettingsViewSwiftUI` を追加し、対応コメントを 3 → 4 ターゲットへ更新。同ファイルのコメントが「package product の依存を増やしたらここも足す」と明示しており、欠くと SwiftUI 変更時に native が再ビルドされないため (2026-09-01)
- [付随修正] `ios/Package.swift` ヘッダコメント: 「後続の変更提案で products / targets を順次追加していく」の進捗記述を削除 (comment-policy 違反の解消) (2026-09-01)
- [付随修正] `ios/binding/build-xcframework.sh` ヘッダコメント: 静的ライブラリに束ねる対象の記述を umbrella product の実態へ更新 (2026-09-01)
