# phase-12-skills-rollout 議論履歴

## 2026-08-26: skills/ 初回生成の進め方とレビュー方法

論点のうち依存が最上流 (change の tasks.md の骨格を決める) として最初に議論。

生成の進め方は、当初エージェントが「英語版を先に確定 → 日本語版を翻訳で派生 (2 段階)」を推奨したが、オーナーの指摘で phase-11 の決定 (2026-08-25「言語ペア 1 組 = 1 タスク」— 正本 concepts が日本語のため英語先行は日→英→日の往復翻訳になり却下、同一文脈生成でロックステップが構造的に破れない) と衝突することが判明し撤回。初回生成にも同決定を適用し、**Skill 単位 fan-out (4 体並列)・各ワーカーが en/ja ペアを同一文脈で同時生成**で確定。manifest 初期版の書き出しも同 change に含める。

レビューは 3 層 + 検収で確定: ①機械検査 (byte 一致・構成一致・manifest 網羅)、②独立レビュー ksn-review (concepts 整合・規約遵守・en/ja 等価)、③初見レビュー (オーナー提案で追加。ksn-core の初見可読性レビューを利用者視点に読み替え、ソース未読の新鮮なエージェントに Skill 本文だけを渡す)、④オーナー目視検収。機械で取れるものはレビュアーに読ませない分担。

ADR 化は見送り (単一 change の実施手順で可逆・局所的。生成順の原則自体は phase-11 history が、ロックステップ規約は ADR-0022 が既に保持)。

## 2026-08-26: legacy-aiforms-reference.md の移送先

phase-10 決定 (cross 配下へ歴史資料として温存) の実施詳細を確定。移行 Skill の生成材料パスと manifest の中身に響くため、生成着手前に議論した。

選択肢は「cross/conventions/ へ同居 (相互リンク相手 aiforms-origin-reference.md が同カテゴリに type: reference で存在する前例あり)」と「cross に history/ 等の新カテゴリ新設」。新カテゴリは rules.md の条件 (3 概念以上の蓄積 + ユーザー合意) を 1 ファイルでは満たさず却下。**cross/conventions/aiforms-spec-summary.md へ改名して移送**で採用 — 「在り処とルール」(origin-reference) と「仕様の要約」(spec-summary) の対になる名前。「legacy」は docs 時代の呼び名のため改名。

付随決定: manifest では excluded ではなく **AiForms 移行 Skill の targets の源泉に登載** (api-mapping の旧 API 側の源泉が本資料。凍結資料のため追従はほぼ発火しない)。frontmatter は type: reference + 凍結注記、origin-reference の docs/ 直リンク差し替え、cross index 登載を change の実装タスクへ。

ADR 化は見送り (移送自体は ADR-0022 が保持。本決定はその実施詳細で可逆・局所的)。

## 2026-08-26: 索引とルート README への導線

skills/README.md 索引の中身は phase-10 決定 (Skill 一覧表 / コピー手順 / 片言語コピーの前提の 3 要素) で確定済みのため、残る意思決定を「既存ルート README への導線 1 行の置き場と文言」に絞って議論。選択肢は「冒頭 (主な特徴の直後) の導線 1 文 + モノレポ構成表への 1 行追加」と「構成表への 1 行のみ」。利用者とそのエージェントが上から読んだ直後に使い方へ誘導される段階開示の効きを理由に**前者を採用**。文言案 (使い方ガイド 1 文 + 構成表の skills/ 行) も併せて採用、実装時の微調整可。途中「これは skills/README.md とルート README のどちらの話か」の確認があり、いずれもルート README への追記であることを明確化した。ADR 化は対象外 (文言レベルの可逆な決定)。

## 2026-08-26: docs/ の廃止 (trash) の進め方と config 残記述整理

吸収確認の要否を実物で検証: docs/ の 8 ファイルは docs-refresh の実行コミットでしか変更されておらず、.manifest.json (v2) が源泉 concepts のハッシュを持つ純派生物であることを確認。手書きの legacy-aiforms-reference.md は移送決定済み。よって「残す記述の吸収」は追加作業なしで完了扱いと決定。trash docs/ は change の最終タスク (skills 8 部レビュー通過後) — 旧 docs はコード例素材として生成完了まで必要なため。config.yaml は同タスクで lint.exclude の docs/ 除外 (コメント込み) と identity.scope の docs を除去。git 履歴に残るため実質復元可能で、ADR 化は対象外 (廃止自体は ADR-0022 が保持)。

## 2026-08-26: concepts の追随 — どこを・いつ更新するか

concepts 全体を grep して docs/ 体制前提の記述を洗い出し。core/maui/android のヒットは別ドメインの ADR-0014 (android/maui/core の各 0014) で無関係と確認し、対象は cross/conventions の 3 箇所 (comment-policy 対象外リスト・test-execution の docs 節・aiforms-origin-reference の直リンク) で全部と特定。agenda 旧記述は「蒸留時に更新」だったが、「構造変化への追随 (パス差し替え)」と「決定の反映 (ADR 昇格)」を分け、**機械的差し替えは change の最終タスク (trash と同タスク) へ前倒し**で採用 — trash 直後に concepts 内へ壊れた参照が残るコミットを挟まないため。蒸留時は ADR-0022 accepted 昇格・ADR-0014 superseded 化と conventions 整合確認のみ。ADR 化は対象外 (実施順の整理)。

## 2026-08-26: phase-9 への申し送り

phase-10 申し送りで既に届いている項目 (導線の形・移行ガイドの置き場) との重複を避け、phase-12 の議論で新たに確定した分と、phase-12 完了後に phase-9 agenda の記述が古くなる箇所の読み替えに絞って 4 点を確定し、phase-9 agenda に「phase-12 からの申し送り」節として追記した: ①ルート README 導線の英訳維持と索引 2 枚の言語対応リンク、②docs/ 前提記述 (冒頭注記の ADR-0014・「docs へ移すもの」) の skills/ADR-0022 読み替えと Skill 構成変更の承認要件、③phase-1 由来 TODO のうち docs/overview.md 追随の対象消滅 (README.md / android/README.md 分は存続)、④docs-refresh の「--readme-only × コード正チェック交差は報告のみ」運用前提の引き継ぎ (初運用は phase-12 が実施済みの前提)。

これで全 6 論点が解消し、残 TODO は提案化のみ。次のステップは ksn-propose (フェーズ由来入力)。

## 2026-08-26: 提案化と相方スペックレビュー (rollout-user-skills)

ksn-propose (フェーズ由来入力) で change rollout-user-skills を作成 (級 M、proposal + specs/user-skills + tasks)。自己レビュー 2 周は指摘なしで通過したが、config 規定の相方スペックレビュー (codex、second-opinion-spec-001.md) が Critical 2 / Major 5 / Minor 1 を指摘。裏取りの結果 Critical 2 件 (README 変更制限と docs/ 参照ゼロ条件の両立不能・legacy 移送対象のローカル絶対パス残存) と Major 3 件 (manifest の concept 集合/ハッシュ完全性未定義・targets ファイル単位とワーカー報告の不整合・manifest 書き出し後の concepts 変更によるハッシュ陳腐化) は実在する欠陥で採用、Major 2 件と Minor は部分採用、いずれもアーティファクトへ反映した。

Critical 1 件目はオーナー判断: 決定事項「ルート README は追記 2 箇所のみ」を改訂し、README 群 (ルート / android / samples) の docs/ リンク差し替え・除去を本 change に含める案 A を採用 (代替の「docs/ trash を phase-9 後へ延期」はロードマップ順序の改訂を要するため不採用)。tasks は「manifest 草案 (2.5) → docs/ 廃止 (6) → manifest 確定と最終検査 (7)」の順序に改めた。
