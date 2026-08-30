# handbook: cross

リポジトリ横断の規約と手順。担当範囲 (触るファイル・行う作業) が「適用のきっかけ」に当たる文書を本文まで読む。

| 文書 | 適用のきっかけ | 種別 |
|---|---|---|
| [comment-policy.md](comment-policy.md) | **常時** (コメント構文を持つ全ソースコード。テスト・Sample を含む) | rule |
| [test-execution.md](test-execution.md) | テストを実行するとき・テスト結果を報告するとき | rule |
| [runtime-behavior-verification.md](runtime-behavior-verification.md) | 実行時挙動 (IME・フォーカス・アニメーション・タイミング) が絡む不具合を調査・修正し、完了を判定するとき | rule |
| [sample-parity.md](sample-parity.md) | `samples/` のデモ画面・文言・デモデータを追加・変更するとき | rule |
| [public-identifiers.md](public-identifiers.md) | 公開識別子・namespace・application ID・配布座標を決めるとき (`**/build.gradle.kts` / `ios/Package.swift` / `**/*.csproj` を触るとき) | rule |
| [aiforms-origin-reference.md](aiforms-origin-reference.md) | 未移植機能を実装するとき・移植元との挙動差や不具合を調査するとき (移植完了までの時限規約) | rule |
| [user-skill-api-listing.md](user-skill-api-listing.md) | `skills/` を触るとき・docs-refresh の API 名網羅検査 (3e) の報告を仕分けるとき | rule |
| [local-development-setup.md](local-development-setup.md) | 環境構築・Sample の起動・本体のビルド / lint・本体 source へのステップインが要るとき | guide |
