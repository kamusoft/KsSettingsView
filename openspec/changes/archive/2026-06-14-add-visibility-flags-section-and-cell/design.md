## Context

KsSettingsView は iOS / Android 両プラットフォームに `AiForms.Maui.SettingsView` をネイティブ移植したライブラリで、`SettingsRoot` / `Section` / `Cell` の宣言的ドメインモデルを Store / DSL 経由で構築し、UI 層が `SettingsRootDiff` で部分更新する設計を持つ。

オリジナルライブラリの `Section.IsVisible` / `CellBase.IsVisible`（リスト内にデータを保持しつつ UI から非表示にする機能）は未移植で、「フォーム状態に応じた段階表示」のような頻出パターンを宣言的に書けない状態にある。

本 change は「オリジナル移植漏れ対応」シリーズ（`openspec/drafts/05-port-gap-change-plan-roadmap.md`）の Change 3 として位置付けられ、Change 1（Theme/CellStyle 補完）と Change 2（共通行レイアウト + 全 Cell 共通フィールド統一）は既にアーカイブ済み。本 change は Change 2 で確立した「全 Cell 共通フィールド」（`description` / `valueText` / `icon` / `hintText` / `isEnabled`）の流れに `isVisible` を加える形になる。

既存原則：
- **「表示状態同期の二層分離」** (`settings-view-core` spec) — 構造同期は id 同一性のみ、内容更新は reconfigure / `notifyItemChanged`
- **「Core 純化」** (`purify-core-extract-style-to-ui-layer` archive) — Core の `Cell` / `KsCell` 抽象は `id` のみ要求、UI 層プロパティは具体 Cell が個別保持
- **「共通フィールド統一」** (Change 2 archive) — 全 7 Cell が `isEnabled` を含む共通フィールドを持つ

## Goals / Non-Goals

**Goals:**

- オリジナル `Section.IsVisible` / `CellBase.IsVisible` 相当を提供する
- 「データには残しつつ UI から非表示にする → 再表示で元位置復帰」を宣言的に書ける API を提供する
- `isEnabled` と並列する独立フラグとして扱い、両者の相互作用を仕様化する
- 既存「表示状態同期の二層分離」原則を破壊せず拡張する形で、可視性変化を第三カテゴリとして取り込む
- DSL 経由の可視性変化が安全に Full 経路に乗ることを仕様で保証する
- 既存呼び出しを破壊しない（破壊的変更なし）

**Non-Goals:**

- 個別 Cell の細粒度な fade in / out アニメーション API（将来検討）
- `SettingsRootDiff` に新ケース（`SetCellVisibility` / `SetSectionVisibility`）を追加すること
- `isEnabled` 自体の opt-in 抽象化（別 change で検討）
- Root Header / Root Footer の `isVisible` フラグ追加（既存の `rootHeader = nil` で代替可能）
- 実装ディテール（visible projection の具体データ構造、cache 同期タイミング、anchor 算出アルゴリズム、変数代入順序など）の固定 — これらは実装フェーズで決定する

## Decisions

### Decision 1: `Cell.isVisible` を Core 抽象 `Cell` / `KsCell` に乗せず、UI 層 7 Cell の具体型にのみ追加する

`Cell` インターフェース / `KsCell` プロトコルを変更せず、UI 層配置の 7 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）に `isVisible: Bool = true` を追加する。

**理由:**
- `purify-core-extract-style-to-ui-layer` で確立した「Core 抽象は薄く保つ」方針と整合する
- 既存 `isEnabled` も同じパターンで具体 Cell が個別保持しており、対称性が取れる
- 外部 Sample アプリ等で `Cell` インターフェースを実装する独自 Cell に契約を波及させない

**代替案:**
- A. Core 抽象に `var isVisible: Bool { get }` を要求として追加 — Core 純化方針と逆行。外部 Cell 実装にも要求が波及するため不採用。
- B. 7 型網羅 switch でホスト層フィルタする — switch 追記漏れがサイレントに「常に表示」化するバグを生む構造的負債のため不採用（Decision 2 で `VisibilityAware` opt-in に統合）。

### Decision 2: `VisibilityAware` opt-in 抽象を UI 層に導入する

UI 層に `VisibilityAware` プロトコル / interface を新設し、7 Cell が opt-in 準拠する。ホスト層は `(cell as? VisibilityAware)?.isVisible ?? true` でフィルタする。

**理由:**
- 将来の Cell 追加（`add-cell-types-input` / `add-cell-types-custom`）でも準拠宣言だけでフィルタ層に自動的に乗る
- 外部 Sample Cell や `CustomCell` も opt-in 宣言だけで `isVisible` をサポートできる
- 7 型網羅 switch を完全排除でき、追記漏れ起因のサイレントバグを防げる
- Core を汚さず UI 層配置で完結する

**代替案:**
- A. 7 型網羅 switch — Decision 1 の代替案 B と同じ理由で不採用。
- B. `Cell` 抽象に `isVisible` を要求 — Decision 1 と同じ理由で不採用。

### Decision 3: 可視性変化は DSL diff 算出ロジックで `SettingsRootDiff.Full(newRoot)` 発行に倒す

DSL（SwiftUI / Compose）の diff 算出ロジックは、宣言ツリーで可視性変化を検出したら `SettingsRootDiff.Full(newRoot)` を発行する。通常の `ReplaceCell`（reconfigure 経路）や `contentUpdates` には可視性差分を流さない。

**理由:**
- 可視性変化は「同一 id の内容更新」（reconfigure 経路）でも「構造変化」（id 集合の追加削除）でもない第三カテゴリで、両経路に流すと UI 層側の構造同期と矛盾する
- `Full(newRoot)` は UI 層が visible projection を再構築するための単純で堅牢な経路で、cells / accessory / visibility の任意の変化を一括反映できる
- 細粒度な insert / remove に翻訳する案は実装複雑性が高く、得られる視覚効果も限定的（将来の最適化として残せる）

**代替案:**
- A. 可視性変化を `ReplaceCell` に乗せる — 「内容更新は構造同期しない」既存原則と矛盾。snapshot から item が消える/現れる構造変化を reconfigure では表現できない。
- B. 新 Diff ケース `SetCellVisibility` / `SetSectionVisibility` を追加 — API 表面が増える。Store 駆動 DSL では state 更新で十分意図表明可能で、Full 経路で実害がない。将来必要になった時点で追加余地は残せる。
- C. 可視性変化を細粒度 insert/remove に翻訳 — 実装コスト高、リスク大。Full で十分。

### Decision 4: 「表示状態同期の二層分離」を「三層分離」に rename し、第三カテゴリ「可視性変化」を追加する

`settings-view-core` spec の既存 Requirement `表示状態同期の二層分離` を `表示状態同期の三層分離` に rename し、(1) 構造同期 / (2) 内容更新 に加え (3) 可視性変化 を第三カテゴリとして追加する。

**理由:**
- 可視性変化は既存の (1)(2) どちらにも該当しない独立カテゴリで、規約上の位置付けを明確にする必要がある
- 「model（hidden 含むフル状態）と visible projection（visible のみ）の分離管理」という UI 層の二重管理規約の根拠を spec に置く必要がある
- 既存 (1)(2) の規約は維持されるため、rename + 追記で済む

**代替案:**
- A. 既存原則を維持し可視性変化を `ReplaceCell` 経路に押し込める — Decision 3 の代替案 A と同じ理由で不採用。
- B. 第三カテゴリを別 Requirement として新設し、既存「二層分離」は触らない — Requirement 名と内容の整合性が失われる。原則の進化を明示的に rename + MODIFIED で示す方が読み手にとって透明。

### Decision 5: `ReplaceCell` / `ReplaceSection` での visibility 切替は MUST NOT、UI 層は防御フォールバックすべき (SHOULD)

DSL / アプリ層から `ReplaceCell` / `ReplaceSection` で visibility だけを変える操作を MUST NOT と仕様化する。同時に、UI 層は受け取った `Replace*` で可視性切替を検出した場合、Full 経路へフォールバックすべき (SHOULD) という防御挙動を仕様化する。

**理由:**
- DSL diff 算出ロジックが Decision 3 に従って可視性変化を `Full` で発行する以上、正しい経路では `Replace*` に visibility 切替が乗らない（MUST NOT を満たす）
- アプリ層が `applyDiff` を直接呼ぶ場合でも、防御フォールバックがあれば破綻しない
- 「正しい経路で書けば自然に Full」「不正経路でも UI 層が破綻を防ぐ」の二重ガードになる

**代替案:**
- A. `Replace*` での visibility 切替を許容し UI 層で対応 — DSL 経路と矛盾するし、アプリ層の意図も曖昧になる。
- B. 防御挙動を MUST 化 — UI 層実装が複雑になる。SHOULD で十分（DSL 経路が主流であり、防御は保険）。

### Decision 6: 部分 Diff の `index` 引数は model 配列基準（hidden 含む）とし、UI 層が visible projection に変換する

`InsertCell` / `MoveCell` / `Insert/MoveSection` 等の `index` 引数は、利用者から見て **model 配列上の位置**（hidden 含む）として受け取る。UI 層が hidden を除外した visible projection 上の位置に変換して snapshot / `flatten` 結果に反映する。hidden 対象を指す部分 Diff は UI 操作を no-op とする（model 更新は実行する）。

**理由:**
- 利用者は通常 hidden の存在を意識せず Section / Cell を操作する。`index` を visible projection 基準にすると、hidden の有無で同じ `index` 値の意味が変わって直感に反する
- model 配列基準なら hidden の有無に関わらず安定した意味を持つ
- hidden 対象への部分 Diff（remove / replace / move）は UI 上見えないので no-op で問題ない

**代替案:**
- A. visible projection 基準 — 上記の通り直感に反する。
- B. 利用者に visible / hidden の区別を明示要求 — API が複雑化する。

## Risks / Trade-offs

- **`Hashable` / `equals` への `isVisible` 追加** → 値型としての等価性が変わるため、Store の `distinctUntilChanged` 相当が `isVisible` 変化を捉えるようになる。これは意図した挙動だが、「構造同期は id 同一性のみ」既存原則（`Hashable` を構造同期判定に使わない）が守られていることをコードレビューで確認する必要がある。
- **「二層分離 → 三層分離」rename** → archive 済み change（`refactor-display-state-sync`）の文言と齟齬が出る。archive ファイルは触らず live spec のみ rename する運用とし、archive は「当時の文言」として保存する。実装フェーズの着手前チェックに含める。
- **`Full(newRoot)` 発行のパフォーマンス** → 数百〜千 Cell 程度では `DiffableDataSource` / `DiffUtil` の典型ベンチマーク範囲内。高頻度 toggle（1 秒間に複数回）が要件になった場合、将来 `SetCellVisibility` 等の細粒度 Diff を追加する余地は残す（Decision 3 代替案 B）。
- **`VisibilityAware` 非準拠 Cell の扱い** → safe-by-default として「`true`（常に表示）」扱いとする。外部 Sample Cell や `CustomCell` で visibility をサポートしたい場合は opt-in 準拠を宣言する。
- **DSL diff 算出ロジックの preflight 追加に伴うコスト** → 同一 id 同士の比較が追加されるため、宣言ツリーが極端に大きい場合（数千ノード）にわずかなオーバーヘッドがある。通常使用範囲では無視できる。
- **partial Section 経路の H/F mode 変化** → 既存実装（`fix-ios-basic-cells` archive）では Full 経路でのみ layout rebuild を判定していた。本 change で section visibility / accessory 変化を扱う以上、partial Section / `UpdateAccessory` 経路でも visible projection の最新状態が supplementary 表示に追従する仕組みが必要になる。実装方式は (a) `UICollectionViewLayout` 自体を作り直す、(b) `UICollectionViewCompositionalLayout(sectionProvider:)` 内で section ごとに supplementary 構成を動的に決定する、のいずれでもよい。ただし実装フェーズで、`dataSource.apply(_:animatingDifferences:)` の差分アニメーションと同じ更新サイクル内で `setCollectionViewLayout(_:animated:)` を呼ぶと、Compositional Layout 側の section 構造再構築と DiffableDataSource の section アニメが衝突し、全 Cell バウンドや描画乱れが発生することが判明した。本 change の実装では (b) の方式を採用し、`listConfig` の `headerMode` / `footerMode` を `.supplementary` 固定にしたうえで `sectionProvider` が section ごとに `boundarySupplementaryItems` を間引く構造に統一した（spec には「visibility 切替後に supplementary が追従する」「差分アニメと layout 同期差し替えを同じ更新サイクルで併用しない」という外部規約として残す）。
