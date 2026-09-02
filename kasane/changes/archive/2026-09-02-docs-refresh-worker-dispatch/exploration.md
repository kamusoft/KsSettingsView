# Exploration: docs-refresh-worker-dispatch

## 課題 / 動機

docs-refresh (`.agents/skills/docs-refresh/SKILL.md`) は高レベルモデルでメインを回すと膨大なトークンを消費する。原因は 2 つ:

1. **器の未指定**: Step 5 は Skill 単位 / README 単位でサブエージェントへ委譲するが、起動する器 (subagent_type) を指定していない。器の指定がない Task はメインのモデルを継承するため、concepts 全文と en/ja 一式を読み書きする最も重い作業がそのまま高レベルモデルで走る
2. **SKILL.md の肥大化**: 56KB あり、起動のたびにメインが丸ごと読む。内訳は Step 6 整合性チェック 19KB (インライン Python 7 本)、Step 3 差分検出 10KB (同 3 本)、5a/5b プロンプトテンプレート 7KB。半分以上が「メインが読んでも使わない」中身 (スクリプトは実行するだけ、テンプレートはサブエージェントへ渡すだけ)

## 検討した選択肢 (却下案と理由を含む)

### 器の決め方

| 案 | 内容 | 評価 |
|---|---|---|
| A: impl 役割キー経由 | docs-refresh が ksn-core `references/worker-dispatch.md` と config の `workers:` 節を読み、役割キー impl で器と backend を解決する | 編成変更に自動追従できるが、毎回の起動でメインの読み込みが増え (数千トークン)、host/counterpart の分岐とブリッジ起動の記述も必要。追従の利点はめったに発生しない事象への保険で、運用コストに見合わない。却下 |
| B: ksn-implementer 名指し固定 (採用) | ksn-core を読まず、docs-refresh 内に器名を直書きする | 毎回の読み込み増分ゼロ、記述は 1 行 + テンプレートのパッケージ節のみ。保守は器名が変わったときに 1 行直すだけ。未配置時は deploy.sh 案内で停止するため事故にならない |
| C: docs 専用の器を Kasane に新設 | 役割キー docs / 器 ksn-docs-writer を追加する | 別リポジトリ (Kasane) の変更を伴い、目的 (トークン節約) には過剰。却下 |

### 肥大化の扱い

- 別 change に逃がす案は却下 (隣接課題は同じ change で直す方針。どちらも「高レベルモデルでのトークン消費」という同一の課題)

## 決定事項

- 器は **ksn-implementer 名指し固定** (案 B)。ksn-core・config は読まない (docs-refresh は Kasane 非依存のまま)
- 運用コスト (毎回の起動コスト + 保守量) が安いほうを選ぶ、が判断基準 (ユーザー決定)
- SKILL.md の肥大化解消を同じ change に同梱する
- ADR は起票しない (覆すコストが低く、Step 5 の記述を戻せば済むため)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

なし

## 実装の見当 (S 級のため tasks の代わり)

### (1) 器の固定

編集対象は `.agents/skills/docs-refresh/SKILL.md`:

1. **コンテキスト節約方針**: サブエージェントは常に器 ksn-implementer で起動する (メインのモデルを継承させない) 旨を追記
2. **Step 1**: 「委譲可否の判定」を「ksn-implementer という器が環境に存在するか」に具体化。未配置なら Kasane の deploy.sh 実行を案内して停止 (現行の停止規律と整合)
3. **Step 5**: Task の subagent_type を ksn-implementer に固定する旨を明記
4. **5a / 5b テンプレート**: 先頭にコンテキストパッケージ節を追加 (読むべきスキル: なし — 規約は本文に内包 / change-id: なし / 担当範囲: 対象ファイル / 制約: git 操作禁止・進捗リスト不更新・drift はテキスト報告)。ksn-implementer の器定義は「パッケージなしで起動されたら作業しない」ため必須
5. **Guardrails**: 器の指定なしで Task を起動しない、を追加

### (2) 肥大化の解消

1. Step 3 / Step 6 のインライン Python (計 10 本) を `.agents/skills/docs-refresh/scripts/<検査名>.py` へ切り出す。SKILL.md には 1 行の起動コマンド (`DOCS_REFRESH_MANIFEST=... python3 .agents/skills/docs-refresh/scripts/xxx.py`) と「何を検査するか・結果の読み方」の説明だけ残す。環境変数のインライン渡し規律と空ガードの注記は残す
2. 5a / 5b テンプレートを `.agents/skills/docs-refresh/references/prompt-skill.md` / `prompt-readme.md` へ移し、Step 5 は「このファイルを読んで埋める」の指示だけにする
3. 検証: 切り出し前後で同じ入力 (現行 manifest / skills/) に対するスクリプト出力が一致することを確認する。SKILL.md は 20KB 前後を目標
4. `.claude/skills/docs-refresh` が symlink である前提を崩さない (scripts/・references/ は symlink 先の実体側に置く)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (理由)

単一スキル (SKILL.md + 切り出しスクリプト・テンプレート) の再構成のみ。コード・公開 API・UI に触れず、切り出しは機械的でスクリプト出力の一致という客観的な確認手段があり、可逆。既存 change `docs-refresh-3e-hardening` (3e 検査のノイズ削減) とはテーマが異なるため別立てとする。
