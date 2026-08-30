---
id: 0022
title: 利用者向けドキュメントは Agent Skills (skills/、en/ja 2 版) として提供し、concepts から追従させる
status: accepted
date: 2026-08-26
supersedes: 0014
---

## Context

公開リポジトリ化と公開レジストリ配信 (package-distribution ロードマップ) に合わせて、利用者向けドキュメントの提供形態を見直した。cross/ADR-0014 は `docs/` を利用者向け派生ドキュメントとして維持すると決めたが、docs は章立て型の読み物であり、AI エージェントを併用して実装する利用者 (公開後の主要読者) が必要箇所だけを読む用途に合わない。また旧 docs には MAUI の実践ガイドと、AiForms 利用者が乗り換えるための移行ガイドが存在しなかった。

設計の指針としてオーナーが 4 観点を提示した: ①パッと何ができるか把握できる、②スマートに効率よく使い方を伝える、③使い方を網羅する、④エージェントが目的を達成しやすい。人間可読性も重要だが、設計が衝突したときの優先度はエージェント > 人間とする。

## Decision

- `docs/` を廃止し、利用者向けドキュメントは `skills/` 配下の Agent Skills (`SKILL.md` + `references/`) として提供する。利用者は自分のプロジェクトへコピーして使う (`.agents/skills/` を共通コピー先の第一候補、Claude Code は `.claude/skills/`)。
- 知識の正は `kasane/concepts/` とコード・テスト。`skills/` はそこから利用者向けに翻訳した派生物で、手で直接育てず、docs-refresh (ユーザーの明示依頼で起動、自動発動禁止) が manifest 差分で追従させる — ADR-0014 の原則を対象を `docs/` から `skills/` + README 群に変えて継承する。
- 分割軸は利用者の状況 (platform × 新規/移行): platform 別 3 Skill (iOS / Android / MAUI) + AiForms 移行 1 Skill の計 4 本。name は配信識別子に揃えた `kssettingsview-{ios,android,maui,aiforms-migration}`。
- 2 言語は `skills/{en,ja}/<name>/` で言語をトップ分離し、各言語配下に自己完結した Skill ディレクトリを置く。name は en/ja 同名 (言語はパスと `metadata.language` で表現し、利用者は片言語のみコピーする前提を規約に明記)。en/ja は常に同一構成・同時更新の翻訳ロックステップとする。
- frontmatter は Agent Skills 標準の範囲内のみ使用する: `name` / `description` (必須) + `license` + `metadata` (language / source)。実行系固有の拡張フィールドは使わない。ja 版 description は日本語本文 + 英語キーワード併記。
- 内容はエージェント優先で設計する: `SKILL.md` = 冒頭のライブラリ概念説明 (KsSettingsView とは何か) + 発火情報 + できること一覧 (能力マップ表) + 導入 + 最小動作コード + references への振り分け。`references/` = 「やりたいこと見出し + 完動コード」のレシピ形式 (platform Skill は cells / updates / styling / custom-cells の 4 本、移行 Skill は api-mapping 1 本)。アーキテクチャ解説など利用に関係しない読み物は載せない。旧 docs の構成・章立ては引き継がず、コード例の素材としてのみ使う。人間可読性は各レシピの平易なリード文と自然言語見出しで保つ。
- **配布単体利用の閉世界性** (初回生成のオーナー検収で確定): Skill は 1 ディレクトリを単体コピーして使われるため、SKILL.md と references は Skill 外のファイル・URL への参照を持たない。索引や兄弟 Skill への言及はスキル名のみ (リンクなし)。見出し・用語は文書単体の文脈で成立させ、リポジトリ内部の用語 (facade 等) を漏出させない。
- 導入節はパッケージが公開レジストリに存在する前提の配布座標で書く。座標の正は配布系の決定 (android/ADR-0016・cross/ADR-0018・cross/ADR-0019・maui/ADR-0025) に従い、公開時の実座標への追従は docs-refresh の責務とする。(2026-08-29 改訂: 初版にあった「未公開注記は公開後に履歴のゴミになるため書かない」という縛りを削除した。作業中のオーナー指示が蒸留で ADR へ拾われたもので、決定として残す性質のものではなかった)
- manifest (`skills/.manifest.json`) は concepts ハッシュのスナップショットに加えて、`targets` (Skill ファイル → 源泉 concepts の依存列挙) と `excluded` (利用者向けでない concepts の理由つき除外) を持つ。concept 変更時は targets の逆引きで更新対象を特定し、「どの Skill にも現れず excluded にもない concept」の機械検出で網羅を検査する。源泉は concepts のみ (コード・テストへの追従は蒸留・drift の責務)。
- 索引は `skills/README.md` (英語) + `skills/README_ja.md` (日本語) の 2 枚 (ルート README の「英語 + `README_ja`」運用に揃える)。Skill 一覧表・コピー手順・片言語コピーの前提のみを載せ、manifest の `readmes` に登載して docs-refresh の追従対象とする。
- 旧 `docs/legacy-aiforms-reference.md` (移植作業者向けの AiForms 仕様要約) は利用者向けではないため skills/ に含めず、`kasane/concepts/` の cross ドメイン配下へ歴史資料として温存する (移植作業と移行 Skill 執筆の材料)。
- `skills/` 一式と manifest の初期生成、および Skill 構成の見直し (Skill の増減・references の再編・除外方針の変更) は変更フローの承認を通す。docs-refresh は既存構成への追従更新に限定し、manifest 不在・非対応バージョン時は全再生成へフォールバックせず停止して初期生成を案内する — 配置判断とレシピ設計は創作であり、追従の道具に委ねない。

## Alternatives Considered

- **docs/ を読み物ドキュメントとして維持する (ADR-0014 の現状)**: エージェントが必要箇所だけを読めず、公開後の主要読者 (AI エージェント併用の利用者) に合わない。MAUI・移行ガイドの欠落も残る。本 ADR で supersede する。
- **トピック別 Skill 分割 (導入 / Cell / styling / 移行など)**: 概念→Skill がほぼ 1:1 で追従は楽だが、各 Skill に 3 platform のコード例が並び、利用者のエージェントに無関係 platform を読ませる。コピーも全 Skill が必要になる。却下。
- **`SKILL.md` + `SKILL_ja.md` 同居 (1 ディレクトリ 2 言語)**: 日本語版が Agent Skills 標準外のファイルになり、どの実行系からも Skill として見えない (人間しか読めない)。references/ の言語対応も曖昧になる。却下。
- **`skills/<name>/{en,ja}/` (Skill 名の下に言語)**: 両言語とも正規の Skill になるが、コピーしたディレクトリ名が `en` / `ja` になり利用者のリネームが必要。却下。
- **ja 版 name に `-ja` 接尾辞**: 両言語を同一プロジェクトへコピーしたときの name 衝突は防げるが、そのような利用は想定されないため、パスの対称性と規約の単純さを優先して却下。
- **manifest の targets に en/ja を明示列挙**: ツールは単純になるが、マップが倍増し、en/ja の構成がずれても列挙が正となって検査で気づけない。翻訳ロックステップ規約を却下理由として採用。

## Consequences

- 正: エージェントが description で発火 → 能力マップで全景把握 → 必要な references 1 本だけ読む段階開示になり、無関係な platform・トピックを読まずに利用目的を達成できる。
- 正: 全公開 API とレシピの対応 (網羅) が manifest で機械検査でき、新 concept 追加時に配置判断 (どの Skill か / 除外か) が強制される。
- 正: MAUI 実践ガイドと AiForms 利用者向け移行ガイドの欠落が解消される。
- 負: en/ja × 4 Skill = 8 部の生成・維持コストが生じ、共通概念の変更が複数 Skill に波及する (manifest の逆引きで機械的に追従する前提)。
- 負: 人間が通しで読むブラウズ型ドキュメントは失われ、人間の可読性は README 索引と各 SKILL.md のリード文・自然言語見出しに依存する。
- 負: Agent Skills 標準は多言語慣行が未標準化であり、標準の進化に応じて 2 言語規約の見直しが必要になり得る。

---
出典: kasane/roadmaps/package-distribution/phases/phase-10-skills-design/history.md (2026-08-25 の各節) / kasane/roadmaps/package-distribution/exploration.md / kasane/roadmaps/package-distribution/phases/phase-11-docs-refresh-retarget/agenda.md (決定事項「初期生成の分担」) / kasane/changes/archive/2026-08-26-retarget-docs-refresh-to-skills/proposal.md / kasane/changes/archive/2026-08-28-rollout-user-skills/deviation.md (閉世界性・冒頭概念説明・配布座標前提のオーナー検収決定)
出典 (2026-08-29 改訂): kasane/roadmaps/package-distribution/phases/phase-9-docs/history.md (2026-08-29「未公開注記の縛りを ADR から外す」)
