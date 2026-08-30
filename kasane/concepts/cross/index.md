# cross 目次

リポジトリ横断のメタ事項。カテゴリ定義と配置基準は [../rules.md](../rules.md) を参照。

## architecture/

- [architecture/repository-boundaries.md](architecture/repository-boundaries.md) — 横断変更をまとめる monorepo と、独立した platform build・Sample の責務分担

## conventions/

- [conventions/aiforms-origin-reference.md](conventions/aiforms-origin-reference.md) — 移植元 AiForms リポジトリの在り処と参照ルール (移植完了までの時限規約)

- [conventions/aiforms-spec-summary.md](conventions/aiforms-spec-summary.md) — 移植元 AiForms の公開 API・構造・実装パターンの要約 (凍結資料。最終的な正は移植元コード)

- [conventions/comment-policy.md](conventions/comment-policy.md) — 全言語共通のソースコメントの許容参照と禁止する記述類型・書き換え時の判断基準

- [conventions/local-development-setup.md](conventions/local-development-setup.md) — iOS・Android・MAUI のローカル環境設定、Sample の起動、本体モジュールのビルド / lint、本体 source へのステップイン手順
- [conventions/public-identifiers.md](conventions/public-identifiers.md) — 所有主体・製品・成果物の役割を ecosystem ごとの識別子へ写像する規約
- [conventions/runtime-behavior-verification.md](conventions/runtime-behavior-verification.md) — 実行時挙動が絡む不具合修正の完了判定 (実環境での再現確立と修正後の解消確認)、および iOS 基本 Cell Sample の目視確認項目
- [conventions/sample-parity.md](conventions/sample-parity.md) — Sample アプリを全 platform で同一文言・同一画面構成にするプラットフォーム間検証規約
- [conventions/test-execution.md](conventions/test-execution.md) — iOS / Android / MAUI のテストの正しい実行コマンドと、黙って検証にならない範囲 (iOS の `swift test` / Android Robolectric の描画検証と非同期反映の待機 / MAUI facade が触らない platform TFM)
- [conventions/user-skill-api-listing.md](conventions/user-skill-api-listing.md) — skills/ に公開 API をどこまで載せるか (「簡潔でも網羅」の方針と意図的な掲載除外の基準・現行除外リスト)
