# セカンドオピニオン: add-spm-distribution (code-001)
**相方**: codex / **label**: so-code-add-spm-distribution / **日付**: 2026-09-01 / **対象**: 未コミット working tree のうち本 change の 13 ファイル (ios/Package.swift、samples/ios・ios/binding の xcodeproj、build-xcframework.sh、scripts/spm-snapshot/ 新設 3 点、.github/workflows/verify-ios.yml、MAUI binding csproj、handbook 3 点)
---
# レビュー結果: add-spm-distribution

**判定**: **CHANGES_REQUESTED**

## サマリー

umbrella product 化、Xcode project の参照更新、CI scheme、MAUI の増分入力は仕様と整合しています。一方、README がデルタスペックの「monorepo への誘導のみ」を満たしておらず、修正必須です。

指摘件数: Critical 0 / Major 1 / Minor 2 / Suggestion 0

## 照合した規約

- ソースコメント規約（always）
- テスト実行規約
- 公開識別子と配布座標
- MAUI 検証ホストの実行規約
- cross/ADR-0018・0019・0020・0025・0026
- MAUI binding の Native artifact 統合

Swift 6 言語モード確認は `ios/Sources/**` が未変更、Sample parity はデモ画面・文言・データが未変更のため適用外と判断しました。

## 指摘事項

### [🟠 Major] 配信 README が「monorepo への誘導のみ」という契約を満たしていない

**該当箇所**: `scripts/spm-snapshot/README.template.md:10`、`specs/spm-distribution/spec.md:78`、`tasks.md:16`

**問題点**: デルタスペックと task 3.2 は、README を monorepoおよびIssue窓口への誘導だけに限定しています。しかし実装は `.package(... from: "0.1.0")` と product 指定を直接掲載しています。また、グループ4完了後は検証tagを削除するため、正式リリースまでこの例をコピーしても解決できません。task 3.2 の完了チェックも現状では実態と一致しません。

**推奨修正**: `Adding the dependency` のコード例とproduct指定を削除し、インストール手順はmonorepoのREADMEを参照する案内へ置き換えてください。修正完了まではtask 3.2を未完了に戻してください。

### [🟡 Minor] filesystem rootを祖先として拒否できない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot.sh:101`

**問題点**: `DESTINATION=/` の場合、生成されるcaseパターンが `//*` となり、`MONOREPO_ROOT` に一致しません。通常はgit top-level・origin検証で拒否されますが、rootが条件を満たす環境では破壊的処理へ到達し、明示契約である「monorepoのすべての祖先を拒否」を破ります。

**推奨修正**: `/` を明示的に拒否するか、文字列パターンではなく正規化済みパスの包含判定へ置き換えてください。実rootを操作せず判定関数だけを検証できる回帰テストも追加してください。

### [🟡 Minor] コピー内容のテストがSources・Tests・LICENSEを検証していない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:73`

**問題点**: 直下の名前と種別、Package.swift、READMEは検証していますが、LICENSEの内容とSources・Testsの再帰的な内容は比較していません。誤ったディレクトリや空の内容をコピーする退行でも、配置・冪等性テストが成功し得ます。

**推奨修正**: LICENSEの内容一致、およびSources・Testsそれぞれの相対パス一覧とファイル内容のハッシュ一致を検証してください。

## アクションプラン

1. READMEを誘導専用に修正し、task 3.2の状態を整合させる。
2. rootを含む祖先判定を堅牢化する。
3. コピー内容の回帰テストを補強する。
4. 同期スクリプトのテストを再実行する。

指定に従い、レビュー結果ファイルへの書き込みやビルド・テストの再実行は行っていません。

## 突き合わせ結果 (ホスト review-001 との採否)

| 指摘 | 出典 | 採否 |
|---|---|---|
| 配信 README が「誘導のみ」(SHALL) を超える | 双方 (相方 Major / ホスト Minor 1) | **確定 (Major)** — 高い方を採用。誘導のみに修正 |
| 検証 4 の検出力 (テスト未到達・冗長 case・`/` 指定の穴) | 双方 (ホスト Minor 2 / 相方 Minor) | **確定 (Minor)** — 同根の指摘として統合。判定堅牢化+テスト組み替え |
| コピー内容テストが Sources/Tests/LICENSE の内容を未検証 | 相方のみ | **採用 (Minor)** — 該当箇所特定・退行シナリオ明確で根拠強 |
| 新設シェル資産の CI 未接続 | ホストのみ Minor 3 | 採用 — ci.yml lint job へ追加 |
| SwiftUI 同梱の実測提供 | ホストのみ Minor 4 | オーナー判断 A (受容) で決着。コメント追記のみ反映 |
| 未コミット変更の無警告破棄 / readonly の失敗握りつぶし | ホスト Suggestion | 採用 (数行) |
| .gitignore 同梱 / roadmap 旧 scheme 名 | ホスト Suggestion | 見送り (spec 定義の 5 点集合を維持 / 日付つき決定記録のため) |

未解決 (両者矛盾) はなし。
