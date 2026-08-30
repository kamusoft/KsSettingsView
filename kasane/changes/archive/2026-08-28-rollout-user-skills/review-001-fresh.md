# 初見レビュー (タスク 5.2): rollout-user-skills

concepts もコードも読んでいない新鮮なエージェント 2 体 (ja 担当 / en 担当) に Skill 本文 (skills/ 配下 18 ファイルずつ) だけを渡して実施。実装との突合は行っていない (それは review-001-* の担当)。以下は両者の指摘の統合要約。原文はオーケストレーターのセッション記録にあり、修正サイクルの委譲パッケージに反映済み。

## 両言語共通の最重要指摘

1. **ライブラリ自体の導入手順が 4 Skill すべてに無い** — 「導入」節は Skill (ドキュメント) のコピー手順への参照のみで、依存宣言の実コード片 (SwiftPM / Gradle 座標 / csproj 参照) が無い。初見利用者はコード例に到達する前に止まる
2. **索引のコピーコマンドが実行不能** — `cp -R skills/<lang>/<skill-name> .agents/skills/` は clone 手順が無く、コピー元・コピー先の作業ディレクトリ前提が両立しない (相方レビュー・共通レビューとも一致)
3. **コピー後に切れるリンク** — 各 SKILL.md の `../../README.md` と移行 Skill の `../kssettingsview-maui/SKILL.md` は単体コピーで必ず dangling。索引の「自己完結」宣言と矛盾
4. **`.agents/skills/` 第一候補と Claude Code `.claude/skills/` の 2 文の関係が読めない** (分岐条件の明示が無い)
5. **Cell 数の数え方不統一** — iOS/Android「12 種 + CustomCell」vs MAUI「13 種 (CustomCell 込み)」が索引に横並び

## 未定義語・宙に浮いた参照 (定義補完が必要)

- iOS/Android `KsCellID` (updates) — cell の `id` との関係が未説明
- iOS/Android 「4 つの accessory 位置」— 列挙が無い。`AccessoryTarget` / `SettingsAccessory` / `SectionAccessory` の 3 型が同時初出 (Android)
- Android 「Handle」(styling) — Cell 関数の戻り値の説明が無い
- iOS `registerBasicCells()` / `registerInputCells()` の内訳 (どの Cell がどちらか) が無い
- iOS `KsCell` / Android `Cell` の要求メンバ (id / style の要否) が確定できない。Android は同一ファイル内で `ProgressCell` の 2 定義 (style 有無) が矛盾して見える
- iOS `DSLStyleModifiable` / `DSLIconModifiable` の要求メソッド名が無い (Android は明示)
- iOS の列挙型名が leading-dot 記法のみ (`.classic` / `.wheels` / `.calendar` 等) で型名を検索できない
- iOS/Android `Theme` / `CellStyle` の全フィールド一覧が無い (MAUI にはある)
- iOS styling 「meaning-specific value」に例示が無い (MAUI のみ例あり)
- MAUI `picker.DisplayFormatter` の `picker` 取得手段 (x:Name) が無い / `Tapped` 購読例が無い
- 移行 Skill: before/after コードブロックにラベルが無い / 「起票する」の宛先が無い

## 文書間矛盾 (実装確認のうえ正しい側へ寄せる)

- maui SKILL「プロジェクト (またはパッケージ) を参照」vs 移行 Skill「NuGet パッケージは無い」
- MAUI styling `RowHeight="-1"` = 自動 vs api-mapping `RowHeight` (`int?`) null = 自動
- iOS cells「例外は ButtonCell の description だけ」vs MAUI「ButtonCell と CustomCell は Description / HintText を運ばない」(review-001-maui は「ButtonCell は HintText を運ぶ」と実装確認)
- iOS custom-cells「独立 Registry 注入で自動登録が切れる」vs 直後の例が `autoRegisterBasicCells: false` を明示する矛盾
- Android cells 「`disabled(...)` modifier は no-op」— 当該 modifier がどこにも紹介されていない

## en 版固有 (英文品質)

- 不自然・破綻した英文: "animation-free position" (ios updates:60) / "lose their redraw" (android updates:67) / "keeps a known Android focus-loss path" (migration SKILL:88) / "which is what CELL_VIEW_TYPE_MIN stands for" (android custom-cells:147) / "answers the measure" (maui styling:160) / maui cells:83 の自己矛盾文 ("has no ValueText; ValueText is the edited string")
- 英米綴りの混在: api-mapping で colour/color・behaviour・customising が米綴り基調の他ファイルと不統一 (iOS/Android custom-cells の "honour" も)
- android updates:3 の 1 文が長すぎ構文が追えない

## 評価が高かった点 (保持すること)

- レシピの粒度・落とし穴の明示 (Android の Material3 / FragmentActivity 前提、MAUI の配置制約と「効かないプロパティは黙って無視」、EntryCell のフォーカス挙動、iOS の ksSection 衝突回避説明)
- api-mapping.md は 4 Skill 中最も完成度が高い (凡例先出し・22 共有プロパティの行数一致・リーク回避説明)
- 総評 (両者一致): 導入手順の欠落と用語定義の補完で、初見利用者に十分成立する水準
