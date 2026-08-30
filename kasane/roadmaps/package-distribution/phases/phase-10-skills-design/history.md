# phase-10-skills-design 議論履歴

## 2026-08-25: Skill の一覧と粒度 (分割軸)

ksn-scout の調査を材料に議論。旧 docs/ は 9 ファイル 3,255 行で、core-model.md / cells.md は concepts の焼き直し度が高く付加価値はコード例に集約、iOS/Android guide は節構成がほぼ 1:1、MAUI ガイドと利用者向け AiForms 移行ガイドは存在しない (legacy-aiforms-reference.md は移植作業者向け歴史資料で読者が違う) と判明。

選択肢は A: platform 別 4 Skill / B: トピック別 5〜6 Skill / C: 混成 (platform 別導入 + 共通トピック)。判断軸はエージェントが読む量・コピー単位の完結性・追従更新の手間・MAUI 空白の埋め方。

**採用: A (platform 別 4 Skill)** — iOS / Android / MAUI (新設) / AiForms 移行 (新規書き起こし)。利用者のプロジェクトは通常 1 platform であり、自分の platform の Skill だけが発火・コピーされる形がコンテキスト効率とコピー単位で最良。トピックは各 Skill 内の references/ に分割し、SKILL.md 本体は発火判断と全体像に絞る。弱点 (共通概念の変更が 3 Skill に波及) は manifest の影響マップで追従する前提で許容。ADR は cross/ADR-0014 を supersede する新 ADR (論点残) に含める。

## 2026-08-25: ディレクトリ規約と 2 言語の配置

選択肢は 案1: `skills/{en,ja}/<name>/` (言語トップ分離・各言語が自己完結 Skill) / 案2: `SKILL.md` + `SKILL_ja.md` 同居 / 案3: `skills/<name>/{en,ja}/`。判断軸は「どの実行系でも日本語版が正規に発火するか」「コピーしたときのディレクトリ名」「manifest の concept → Skill × 言語対応の素直さ」。

**採用: 案1** — Agent Skills 標準では Skill = 直下に SKILL.md を持つディレクトリであり、SKILL_ja.md は標準外で実行系から見えない (案2 却下)。案3 はコピーしたディレクトリ名が en/ja になりリネームが必要 (却下)。案1 はコピー単位 (1 言語 1 Skill の 1 ディレクトリ) が完結し、パスに言語が現れて manifest の影響マップも素直になる。

## 2026-08-25: frontmatter 標準と実行系互換

ksn-scout の Web 裏取り (agentskills.io spec / Claude Code / Codex CLI / Gemini CLI / Cursor / Copilot 各ドキュメント) を材料に議論。判明事項: 最大公約数は name + description で、標準 6 フィールド (+ license / compatibility / metadata / allowed-tools) 以外を書くと claude.ai アップロード・Skills API でハードエラー。name は ASCII kebab-case でディレクトリ名一致必須。`.agents/skills/` が Codex/Gemini/Cursor/Copilot 共通のコピー先。多言語 Skill の公式慣行は標準に存在しない。

**採用**: 標準 6 フィールド範囲内のみ (name / description / license / metadata に language・source を格納)。name は `kssettingsview-{ios,android,maui,aiforms-migration}`。ja 版 name は「-ja 付き」案 (併存衝突なし) と「同名」案 (パス対称) を比較し、**同名を採用** — 言語はパスと metadata.language で表現し、利用者は片言語のみコピーする前提を規約に明記する。ja 版 description は日本語本文 + 英語キーワード併記。

## 2026-08-25: Skill の内容設計原則と骨格 (旧 docs 棚卸しの方針転換)

当初は旧 docs/ 9 ファイルの吸収/廃棄の棚卸し表 (行き先マッピング) を提示したが、オーナーの指摘で方針転換: **旧 docs に引っぱられず、真っ白から純粋に利用者向けの構成を設計する**。旧 docs は構成の参照元にせず、コード例の素材庫としてのみ使い、全ファイル廃止。アーキテクチャなど利用に関係ない読み物は入れない。

オーナー提示の 4 観点を設計原理に採用: ①パッと何ができるか把握できる (SKILL.md の能力マップ表)、②スマートに効率よく使い方を伝える (レシピ形式: やりたいこと見出し + 完動コード)、③使い方を網羅する (全公開 API ↔ レシピの対応を manifest で機械検査)、④エージェントが目的を達成しやすい (段階開示で無関係なものを読ませない)。人間可読性も重要だが、ぶつかったときの優先度はエージェント > 人間 (レシピに平易なリード文、自然言語見出し、人間専用補助は害のない範囲で残す)。

骨格は 3 案比較 (A: 能力マップ + クイックスタート入り SKILL.md + references 4 本 / B: SKILL.md を索引だけに絞る / C: 単一 SKILL.md) で **A を採用** — 発火直後の 1 枚で全景把握と最小実装が完了し、深掘りだけ references に降りる。references は cells / updates / styling / custom-cells の 4 本、移行 Skill のみ SKILL.md + api-mapping.md。

残課題として legacy-aiforms-reference.md (移植作業者向け) の行き先だけ論点に残す。

## 2026-08-25: legacy-aiforms-reference.md の行き先

選択肢は A: kasane 配下へ温存 / B: 移行 Skill に吸収して原本廃棄 / C: 完全廃棄 (原典参照)。読者が移植作業者 (開発エージェント) であり、maui-support 側に pending の移植フェーズが残っていて参照が生きていること、移行 Skill 書き起こしの材料になることから **A を採用**。配置はオーナー指定で kasane/concepts/ のどこか — cross ドメイン配下に歴史資料として置き、cross/conventions/aiforms-origin-reference.md と相互リンクする。ファイル名・カテゴリの確定は phase-12 実装時。

## 2026-08-25: manifest (skills/.manifest.json) v3 の影響マップ設計

現行 v2 (docs/.manifest.json) を確認: concepts 全体のハッシュ一覧 + docs ファイル列挙のみで、concept → doc の対応を持たない粗い構造だった。

**採用 (v3)**: ① `targets` = Skill ファイル → 源泉 concepts の依存列挙 (ビルド依存と同じ向き)。逆引きで差分更新対象を特定し、未参照かつ未除外の concept の機械検出で網羅検査を実現。② `excluded` = 利用者向けでない concepts (開発規約等) の理由つき除外リスト。③ targets のパスは言語抜きの Skill 相対パスとし、en/ja は同一構成・同時更新の翻訳ロックステップ規約 (代替の「en/ja 明示列挙」はマップ倍増と構成ずれの見逃しがあり却下)。④ 源泉は concepts のみ (コード追従は蒸留・drift の責務)。⑤ README 群は `readmes` 列挙のみの粗い管理を維持。

## 2026-08-25: 索引と導線の形

`skills/README.md` (英語) + `skills/README_ja.md` (日本語) の 2 枚構成を採用 — ルート README の「英語 + README_ja」運用 (原典 AiForms 踏襲) との一貫性を優先し、1 枚バイリンガル案は非対称になるため却下。中身は Skill 一覧表 / コピー手順 (.agents/skills/ 第一候補、Claude Code は .claude/skills/) / 片言語コピーの前提、の 3 要素のみ。2 枚とも manifest の readmes に登載。ルート README の導線の本文は README 改訂フェーズの責務。

## 2026-08-25: supersede ADR の起票

本フェーズの決定一式 (skills/ への置き換え・4 Skill 構成・2 言語配置・frontmatter 標準・内容設計原則・manifest v3・索引・legacy 温存) を [cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md) として proposed で起票し、cross/index.md を更新した。supersedes: 0014。accepted への昇格と ADR-0014 の superseded 化は skills/ 生成の実装完了後の蒸留時に行う。

これで論点はすべて解消。残 TODO は「ksn-roadmap で research 完了をマーク」のみ。
