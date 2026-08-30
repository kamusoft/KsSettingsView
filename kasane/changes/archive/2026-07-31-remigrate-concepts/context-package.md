# remigrate-concepts 実行用コンテキストパッケージ

この change を実行するエージェント (codex 等) は、このファイルを起点に作業する。

## 1. 読むべきスキル (規律の正)

| 役割 | パス |
|---|---|
| 指揮 (バッチ計画・統合・確定) | `~/.agents/skills/ksn-migrate/SKILL.md` |
| 抽出ワーカー規律 (capability 単位) | `~/.agents/skills/ksn-migrate-extract/SKILL.md` |
| 規約ライブラリ (価値 lint・アンカー規約・用語規約・可読性規約・テンプレート) | `~/.agents/skills/ksn-core/SKILL.md` |

進め方は ksn-migrate の Step 5 (concepts 一括起こし) を本 change に読み替えて適用する。進捗の SSoT は [tasks.md](tasks.md)、スコープは [proposal.md](proposal.md)。

## 2. プロジェクト記述規約 (kasane/config.yaml の context 転記。ksn-core の既定に優先)

> アーティファクトは日本語で記述する。技術用語および仕様構文キーワードは英語のまま使用する。
> 本プロジェクトは UI コントロールライブラリであり、公開 API の契約と利用コード例は製品知識として価値を持つため、concepts の記述対象とする。
> 内部実装フローや単なるファイル一覧は、コードから再導出できる情報として concepts へ記載しない。
> docs/ の内容は concepts へ吸収する方針であり、concepts の記述水準と用語は docs/ を標準とする。

## 3. 材料の場所と読む順序

**先に読む (一次資料)**: 実装コードとテスト

- iOS: `ios/Sources/KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` + `ios/Tests/`
- Android: `android/ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` の `src/`
- Samples: `samples/ios/` `samples/android/`

**後から照合する (二次資料。コードを読み終えるまで開かない)**:

1. 旧 spec: `openspec/specs/<capability>/spec.md` (凍結。Purpose 節から「なぜ存在するか」を回収)
2. 旧 concepts: `reference/old-concepts/` (初回移行の成果物。取捨の判断はスキルの規約に従う)
3. `docs/` (人間向け解説文書 8 本。proposal のとおり内容は concepts へ吸収する)

## 4. 品質基準

品質基準は **ksn-core の concepts 規約と上記 2 の context が正**。このパッケージでは追加の基準を与えない。各バッチの成果物は初見可読性レビューとオーナーレビューで検証される (手順は ksn-migrate Step 5d)。

## 5. 禁止事項 (ksn-migrate 準拠 + 本 change 固有)

- kasane/ と AGENTS.md 以外のリポジトリ資産 (docs/・README・samples・コード・テスト・openspec/) への書き込み。**すべて読み取りのみ**
- decisions/ (ADR)・config.yaml・rules.md の変更。新 ADR 候補は candidates の「ADR 候補」節で報告のみ
- spec の先読み (コード・テストより先に二次資料を開くこと)
- Requirements / Scenario の丸写し
- tasks.md を更新せずに進める (中断＝再開不能になる)
- 統合を経ずに candidates から concepts/ へ直行
- drift 所見 (コード・spec・docs・旧 concepts 間の矛盾) の解消方向を独断で決める — 記録してオーナーへ

## 6. 出力先

- 抽出ワーカーの成果: `candidates/<capability>.md` (ksn-migrate-extract の出力形式)
- 確定書き込み: `kasane/concepts/<category>/<concept>.md` (カテゴリは `concepts/rules.md` が正) + `index.md` / `log.md` 更新
- 前回移行の未解消 drift・deferred 事項は `kasane/concepts/log.md` の 2026-07-17〜18 節を参照し、Batch E で引き継ぎ一覧に含める
