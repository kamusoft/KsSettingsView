# 旧規約下で生成された concepts を再移行し docs/ を吸収する

## Why

初回移行 (migrate-openspec、2026-07-17〜18) で生成された concepts 18 ファイルは、改訂前の規約の下で作られ、オーナーレビューで改訂後の規約 (2026-07-18 改訂) を満たさない品質問題が確認された。欠陥は局所でなく文書全体の構造に及ぶため、修繕ではなく再抽出で作り直す。あわせて docs/ (2026-07-18 復元済み・8 文書) の内容を concepts へ吸収し、プロジェクト文書の正を concepts に一本化する。

## What

- 対象: concepts の全面再生成。旧 18 ファイルは `reference/old-concepts/` へ退避済み (照合材料)。platforms/ の空欄も解消する
- 方法: capability ごとの code-first 抽出 (ksn-migrate-extract) → バッチ統合 → 初見可読性レビュー → オーナーレビュー → 確定
- 後照合材料 (コードとテストを読んだ**後**に開く): ① `openspec/specs/` の旧 spec (凍結・13 capability) ② `reference/old-concepts/` の旧 concepts ③ `docs/` (8 文書)
- docs/ 吸収: 最終バッチで docs/ 8 文書を走査し、concepts 未回収の知識を回収する (残差スイープ)
- 進捗管理: tasks.md (バッチ × capability。再開点の SSoT)

## Non-Goals

- decisions/ (ADR 13 件・レビュー済み)、config.yaml、rules.md は変更しない。新規 ADR 候補が出たら所見として報告に留める
- docs/ の書き換え・スタブ化は行わない (concepts 確定後の**別変更**で実施する)
- kasane/ と AGENTS.md 以外のリポジトリ資産 (docs/・README・samples・コード・テスト・openspec/) には触れない。読み取りのみ
- 旧 spec の Requirements / Scenario の丸写しをしない
- 進行中の openspec changes 7 件 (add-cell-types-input 等) の移設・archive はしない。ただし**実装済みコードは spec の有無に関係なく code-first 抽出の対象** (コードが常に現実)
- 前回 log.md に記録された実装不具合候補・未解消 drift の解消はしない (記録の引き継ぎのみ)

## 変更級

`migrate` (S/M/L、実装レビュー、verify、deviation の対象外)
