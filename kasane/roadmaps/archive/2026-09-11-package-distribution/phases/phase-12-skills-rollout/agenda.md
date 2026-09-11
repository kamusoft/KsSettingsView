# phase-12-skills-rollout

`skills/` 一式と manifest 初期版 (`skills/.manifest.json` v3) を本フェーズの change として初回生成し、`skills/README.md` (索引) とルート README への導線を置き、`docs/` と `docs/.manifest.json` を廃止する。public 化 (phase-2 実施) と README 改訂 (phase-9) の前に完了させる。

## 論点

(すべて解消済み — 決定事項へ)

## phase-10 からの申し送り (2026-08-25)

設計は確定済み ([phase-10 決定事項](../phase-10-skills-design/agenda.md) / [cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md))。このフェーズで生成・整備するもの:

- `skills/{en,ja}/` の 4 Skill × 2 言語 = 8 部 (MAUI と AiForms 移行は旧 docs に原型がなく新規書き起こし。旧 docs は構成を引き継がずコード例の素材としてのみ使う)
- 索引 `skills/README.md` + `skills/README_ja.md` (Skill 一覧表 / コピー手順 (`.agents/skills/` 第一候補、Claude Code は `.claude/skills/`) / 片言語コピーの前提、の 3 要素のみ)
- 蒸留時に ADR-0022 を accepted へ昇格し、cross/ADR-0014 を superseded 化する

## phase-11 からの申し送り (2026-08-25)

docs-refresh 改修の提案 ([changes/archive/2026-08-26-retarget-docs-refresh-to-skills](../../../../changes/archive/2026-08-26-retarget-docs-refresh-to-skills/proposal.md)) で確定した分担:

- **初期生成 (skills/ 一式 + manifest 初期版) は本フェーズの change が直接行う** — 改修後の docs-refresh は manifest 不在時に停止するため、`--all` は初回生成に使えない (`--all` = manifest 前提の全再生成)。本 agenda の旧記述「docs-refresh --all で初回生成」は本決定に合わせて修正済み
- manifest v3 の規範スキーマ (キーの基準パス・不変条件) は change のデルタスペック (`specs/docs-refresh/spec.md`) が正。初期生成はこの規範に従って manifest を書き出す
- CLAUDE.md / AGENTS.md / config.yaml context の記述は phase-11 で skills/ ベースへ更新済みになる予定 (本フェーズでの作業は docs/ 廃止に伴う config 残記述の整理のみ)
- 生成時の内容規約: コード例は原則コメントレス (説明はレシピ見出しとリード文へ)、やむを得ない最小限のコメントは英語統一。en/ja はコードブロック byte 一致

### 実装完了後の追加分 (2026-08-26 蒸留時)

- **ADR-0022 を accepted へ昇格する際の確認範囲**: 蒸留時に Decision へ責務分界 (skills/ 一式と manifest の初期生成・Skill 構成の見直しは変更フローの承認を通す / docs-refresh は追従専用で manifest 不在時は停止) を追記した。昇格時はこの項も含めて内容を確認する
- **`--readme-only` × コード正チェックの交差は「報告のみ」**: ツール最低バージョンの差分が Skill 導入節へ及ぶケースは、要追従リストに載せず完了サマリで次回の通常実行へ誘導する ([deviation.md](../../../../changes/archive/2026-08-26-retarget-docs-refresh-to-skills/deviation.md))。初期生成後に docs-refresh を初運用するのは本フェーズなので、運用前提として引き継ぐ
- **初期生成が満たすべき manifest の不変条件**: `--all` は網羅検査を実行するため、生成した manifest の `targets` / `excluded` は全 concept を覆っている必要がある (未配置の concept が残ると初回の docs-refresh 実行が網羅検査で止まる)

## 決定事項

- **初回生成の進め方とレビュー方法 (2026-08-26)**: 生成は Skill 単位で fan-out (ksn-implementer 4 体並列)。各ワーカーは源泉 concepts (日本語正本) + 旧 docs のコード例素材 + ADR-0022 の内容規約を受け取り、**en/ja ペアを同一文脈で同時生成**する — phase-11 決定「言語ペア 1 組 = 1 タスク」(正本が日本語のため英語先行は往復翻訳になり却下済み) の初回生成への適用。manifest 初期版 (`targets` / `excluded`) も同 change で書き出す。レビューは 3 層 + 検収:
  1. **機械検査**: en/ja コードブロック byte 一致・構成一致、manifest 網羅 (全 concept が `targets` か `excluded` に現れる)
  2. **独立レビュー (ksn-review)**: 源泉 concepts との内容整合 / レシピ形式・能力マップ・段階開示の規約遵守 / en/ja の等価性
  3. **初見レビュー**: ksn-core の初見可読性レビュー (references/concepts.md) を利用者視点に読み替えて適用 — concepts もコードも読んでいない新鮮なエージェントに Skill 本文だけを渡し、「この文書だけで利用目的を達成できるか / 宙に浮いた参照・意味の取れない新造語がないか」を報告させる
  4. **オーナー目視検収**: 最後にオーナーが通し読み (少なくとも ja 4 部)
- **legacy-aiforms-reference.md の移送先 (2026-08-26)**: `kasane/concepts/cross/reference/aiforms-spec-summary.md` へ改名して移送する (`type: reference`、「凍結された歴史資料、最終的な正は移植元コード」の注記つき)。同居する [aiforms-origin-reference.md](../../../../handbook/cross/aiforms-origin-reference.md) と相互リンクし (同ファイル「参照先」節の docs/ 直リンクを差し替え)、cross の index.md に登載する。manifest では `excluded` ではなく **AiForms 移行 Skill の `targets` の源泉**として登載する (api-mapping の旧 API 側の源泉が本資料であり、除外扱いは依存の実態と食い違うため。凍結資料なので追従はほぼ発火しない)
- **ルート README への導線 (2026-08-26)**: skills/README.md 索引の中身は phase-10 決定 (3 要素) のとおりで追加論点なし。既存ルート README (日本語) へは「冒頭 (主な特徴の直後) に使い方ガイドへの導線 1 文 + モノレポ構成表に `skills/` の 1 行」を追記する。文言は agenda 議論時の案を採用 (実装時に微調整可)。英語化・本文改訂は phase-9 が引き継ぐ。**(2026-08-26 改訂・相方スペックレビュー指摘)** 当初の「追記 2 箇所のみ」は README 群に既存の docs/ リンク (ルート 7 箇所 + android / samples 計 6 箇所) がある事実を見落としており、docs/ trash でリンク切れになるため、**README 群の docs/ 参照の差し替え・除去も本 change に含める**よう変更範囲を緩和した
- **docs/ の廃止と config 整理 (2026-08-26)**: 吸収確認は完了扱い — docs/ は docs-refresh が concepts から生成した純派生物 (manifest v2 のハッシュと git 履歴で裏付け) で、唯一の手書き資料 legacy-aiforms-reference.md は移送決定済みのため、docs にしかない知識は存在しない。`trash docs/` (.manifest.json 含むディレクトリごと) は change の**最終タスク** (skills 8 部のレビュー通過後) に置く — 旧 docs は生成のコード例素材として使い終えるまで残す。同じタスクで `kasane/config.yaml` の `lint.exclude` から docs/ とコメントを除去し、`identity.scope` から `docs` を除去する (`skills` は phase-11 で登載済み)
- **concepts の追随の分担 (2026-08-26)**: docs/ 体制を前提にした concepts は cross/conventions の 3 箇所で全部 — ①comment-policy の対象外リスト (docs/ → skills/ へ差し替え、skills のコード例は「原則コメントレス・最小限は英語」の別規約であることを添える)、②test-execution の「README・docs の swift test との関係」節 (skills/ + README 群へ読み替え)、③aiforms-origin-reference の docs/ 直リンク (移送タスクで対応済みの決定)。**①②の機械的差し替えは蒸留を待たず change の最終タスク (docs/ trash と同タスク) で実施**し、壊れた参照のコミットを挟まない。蒸留時に行うのは cross/ADR-0022 の accepted 昇格・cross/ADR-0014 の superseded 化と、それに伴う conventions 全体の整合確認のみ (agenda 旧記述「(蒸留時)」からの前倒し)
- **phase-9 への申し送り (2026-08-26)**: 4 点 (導線の英訳維持と索引の言語対応 / docs/ 前提記述の skills 読み替えと Skill 構成変更の承認要件 / docs/overview.md ツールチェーン追随 TODO の対象消滅 / docs-refresh 運用前提の引き継ぎ) を [phase-9 agenda](../phase-9-docs/agenda.md) に追記済み

## TODO

- [x] 論点の解消 (2026-08-26 全 6 論点を決定事項へ)
- [x] ksn-propose で変更提案を起こす (2026-08-26 提案化 → 実装 → 2026-08-28 蒸留・アーカイブ完了)

## 実装結果 (2026-08-28 反映)

change [changes/archive/2026-08-28-rollout-user-skills](../../../../changes/archive/2026-08-28-rollout-user-skills/proposal.md) として実装完了 (review-002 APPROVED)。skills/ 8 部 + manifest v3 + 索引 2 枚 + README 群の docs/ リンク解消 + `docs/` 廃止 + config 整理 + aiforms-spec-summary 移送をすべて実施。蒸留で cross/ADR-0022 を accepted へ昇格し (検収確定の閉世界性・冒頭概念説明・配布座標前提を Decision に反映)、cross/ADR-0014 を superseded 化した。

スペックからの主な乖離 (詳細は [deviation.md](../../../../changes/archive/2026-08-28-rollout-user-skills/deviation.md)):

- オーナー検収指摘により SKILL 外参照の全撤去 (閉世界性)・各 SKILL.md 冒頭の概念説明追加・導入節のパッケージ前提化 (仮座標は proposed ADR 準拠) を実施
- 検収指示の同梱実装: maui `SettingsView.SectionMargin` の XAML 属性記法対応 (TypeConverter + テスト 1 件)

申し送りのルーティング (すべて受け皿確定済み):

- 検収で発覚した実装課題のうち Android テーマ/Activity 前提 → change relax-android-host-prerequisites として実装・蒸留完了 (2026-08-28 アーカイブ)
- 同 PlaceholderColor 欠落 → change add-entrycell-placeholder-color として実装・蒸留完了 (2026-08-28 アーカイブ)
- 同 PickerCell の object items 後退 → [changes/restore-pickercell-object-items](../../../../changes/restore-pickercell-object-items/exploration.md) に簡易起票済み (独立変更として追跡)
- 導入節の仮座標の実座標への追従 (iOS 配信リポジトリ名の確定を含む) → docs-refresh の責務として cross/ADR-0022 の Decision と deviation.md に恒久記録済み (公開時にユーザーが docs-refresh を明示依頼する運用)
- phase-9 への申し送り 4 点 → phase-9 agenda に追記済み (2026-08-26、決定事項の記載どおり)
