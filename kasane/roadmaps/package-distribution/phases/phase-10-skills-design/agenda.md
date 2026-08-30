# phase-10-skills-design

利用者向けドキュメントを `docs/` から `skills/` (利用者がコピーして使う Agent Skills、英語 / 日本語の 2 版) へ置き換えるための設計と、旧 `docs/` の棚卸しを行う。

## 論点


## 決定事項

- **Skill の一覧と分割軸 (2026-08-25)**: platform 別 4 Skill 構成 — iOS ガイド / Android ガイド / MAUI ガイド (新設。concepts/maui + Sample から書き起こし) / AiForms 移行ガイド (新規書き起こし。旧 API → 新 API 対応表、主読者は MAUI 乗り換え組)。分割基準は「利用者の状況 (platform × 新規/移行) ごとに 1 Skill、`SKILL.md` は発火判断と全体像に絞り、詳細リファレンスは `references/` へ」。各 Skill は en/ja の 2 版。共通概念の変更が 3 Skill に波及する弱点は manifest の影響マップで機械的に追従する前提。ADR は論点の supersede ADR (cross/ADR-0014 の後継) に含める
- **ディレクトリ規約と 2 言語配置 (2026-08-25)**: `skills/{en,ja}/<skill-name>/` — 言語をトップで分け、各言語配下に自己完結した Skill ディレクトリ (`SKILL.md` + `references/`) を置く。利用者がコピーする単位は「1 言語 1 Skill の 1 ディレクトリ」。`SKILL_ja.md` 同居案は日本語版が標準外ファイルになり実行系から見えないため却下。ADR は supersede ADR に含める
- **frontmatter 標準 (2026-08-25)**: Agent Skills 標準 6 フィールドの範囲内のみ使用 — `name` / `description` (必須) + `license` + `metadata` (language: en|ja、source)。実行系固有の拡張フィールドは使わない (claude.ai 系でハードエラーになるため)。name は配信識別子に揃えた ASCII kebab-case `kssettingsview-{ios,android,maui,aiforms-migration}` で、**en/ja 同名** (言語はパスと `metadata.language` で表現、利用者は片言語のみコピーする前提)。ja 版 description は日本語本文 + 英語キーワード併記。利用者への案内は `.agents/skills/` を共通コピー先の第一候補、Claude Code は `.claude/skills/` (本文は phase-9/12)
- **Skill の内容設計原則と骨格 (2026-08-25)**: 旧 docs/ の構成・章立ては引き継がず、真っ白から利用者向けに設計する (旧 docs はコード例の素材庫としてのみ利用し、全ファイル廃止)。アーキテクチャ解説など利用に関係しない読み物は入れない。設計原理は 4 観点 — ①できること一覧 (`SKILL.md` の能力マップ表) で全景即把握、②「やりたいこと見出し + 完動コード」のレシピ形式、③全公開 API がどれかのレシピに対応することを manifest で機械検査、④段階開示 (description 発火 → SKILL.md で全景 → 必要な references 1 本のみ)。人間可読性も重要だが優先度はエージェント > 人間 — 各レシピに 1〜2 行の平易なリード文、見出しは「やりたいこと」の自然言語、人間専用の補助はエージェントの読む量を害さない範囲で残す。骨格: `SKILL.md` (発火 + 能力マップ + 導入 + 最小動作コード + 振り分け) + `references/` 4 本 (cells / updates / styling / custom-cells)。AiForms 移行 Skill のみ `SKILL.md` + `references/api-mapping.md`
- **legacy-aiforms-reference.md の行き先 (2026-08-25)**: kasane/concepts/ の cross ドメイン配下へ移して開発資料 (歴史資料) として温存する。残りの移植作業 (maui-support pending フェーズ) と AiForms 移行 Skill の書き起こし材料の両方から参照できる。`cross/conventions/aiforms-origin-reference.md` と相互リンクし index に登載。正確なファイル名・カテゴリは phase-12 実装時に確定
- **manifest (`skills/.manifest.json`) v3 設計 (2026-08-25)**: v2 の concepts ハッシュスナップショットを維持しつつ、`targets` (Skill ファイル → 源泉 concepts の依存列挙) と `excluded` (利用者向けでない concepts の理由つき除外リスト) を新設。concept 変更時は targets の逆引きで更新対象を特定し、「どの Skill にも現れず excluded にもない concept」の機械検出で網羅を検査する (新 concept 追加時に検査が落ちて配置判断を強制)。targets のパスは言語を含まない Skill 相対パス — en/ja は常に同一構成・同時更新 (翻訳ロックステップ) の規約とし、マップの倍増と片言語更新事故を構造的に防ぐ。源泉は concepts のみ (コード・テストへの追従は蒸留・drift の責務で、manifest はその下流)。README 群は `readmes` として更新対象の列挙のみの粗い管理
- **索引と導線の形 (2026-08-25)**: `skills/README.md` (英語) + `skills/README_ja.md` (日本語) の 2 枚構成 — ルート README の「英語 + `README_ja`」運用 (原典 AiForms 踏襲) に揃える。中身は ① Skill 一覧表 (name / 対象 / 1 行説明 / en・ja リンク)、② コピー手順 (`.agents/skills/` 第一候補、Claude Code は `.claude/skills/`)、③ 片言語のみコピーする前提の明記、の 3 要素のみ。2 枚とも manifest の `readmes` に登載し docs-refresh の追従対象。ルート README には Skills への導線の節を置く (本文は phase-9)
- **supersede ADR の起票 (2026-08-25)**: 本フェーズの決定一式を [cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md) (proposed、supersedes 0014) として起票済み。accepted への昇格と ADR-0014 の superseded 化は phase-12 実装後の蒸留時

## 調査結果 (2026-08-25 完了)

利用者向けドキュメントの Skills 化の設計を確定した。骨子: platform 別 4 Skill (`kssettingsview-{ios,android,maui,aiforms-migration}`) × en/ja 2 版を `skills/{en,ja}/<name>/` に置き、frontmatter は Agent Skills 標準 6 フィールド内・en/ja 同名。内容は旧 docs の構成を引き継がずエージェント優先 (能力マップ + レシピ形式 + 段階開示) で新規設計。manifest v3 (targets 逆引き + excluded + 翻訳ロックステップ) で網羅を機械検査。索引は `skills/README.md` + `README_ja.md`。legacy-aiforms-reference.md は concepts cross 配下へ温存。決定一式は [cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md) (proposed、supersedes 0014) に起票済みで、accepted 昇格は phase-12 実装後の蒸留時。詳細は上記「決定事項」と [history.md](history.md)。

後続への影響: phase-11 (manifest v3 と対象切り替えの実装仕様が確定)、phase-12 (生成対象 8 部・索引 2 枚・legacy 移送・docs/ 廃止)、phase-9 (移行ガイドの置き場 = 移行 Skill、README 導線の形)。各 agenda に申し送りを追記済み。

## TODO

- [x] 論点の解消
- [x] 調査結果のまとめ (決定事項 + history.md に集約)
- [x] ksn-roadmap で research 完了をマーク (2026-08-25)
