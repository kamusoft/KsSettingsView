# Design: implement-modern-style

## Context

Modern の現状は iOS が `.insetGrouped` 任せ、Android が `ModernSectionDecoration` のハードコード描画。ios/ADR-0003 で「iOS は `.insetGrouped` を廃し compositional layout 上の自前装飾で実現する」ことが決定済み。本書はその具体化と、Theme 4属性の API 形状、Android 側の Theme 駆動化を扱う。

前提となる既存構造:

- iOS: `makeLayout(for:)` が `UICollectionViewCompositionalLayout` + sectionProvider で Section ごとに `NSCollectionLayoutSection.list(using:)` を生成。separator は `itemSeparatorHandler`、Classic では `.plain` の header pin を手動解除済み
- Android: RecyclerView + ConcatAdapter は style 非依存で、`applyDecoration()` が style に応じて ItemDecoration を差し替え
- Theme: 両 OS とも「nil / null = 未指定 → フォールバック」の文法 (`cellIconSize` 等)

## Goals / Non-Goals

proposal.md のとおり (Non-Goals: MAUI・Section 単位上書き・内外トグル・icon 連動 inset・既定値の platform 間統一)。

## Decisions

### Decision 1: Theme 4属性は optional で追加し、未指定は style 別 platform 既定へ解決する

**採用案:**

| 属性 | iOS | Android |
|---|---|---|
| `sectionMargin` | `NSDirectionalEdgeInsets?` | `PaddingValues?` |
| `sectionCornerRadius` | `CGFloat?` | `Dp?` |
| `sectionBorderWidth` | `CGFloat?` | `Dp?` |
| `sectionBorderColor` | `UIColor?` | `Color?` |

nil / null は「未指定 → style 別の platform 既定」: Modern は各 platform が所有する既定寸法 (iOS はモック承認で確定する iOS 風の値、Android は現行値を既定として維持)、Classic は margin 上下 0 (現行外観不変)。borderWidth の実効既定は 0、borderColor は透明 (Modern の既定にボーダーはない)。

`sectionMargin` の意味論は **Section 単位 (Header・Cell 箱・Footer を一体とした表示単位) の外側余白**とする (second-opinion-spec-001 M2 の採用)。承認モックの余白の置き方がこれで、iOS でも `NSCollectionLayoutSection.contentInsets` が supplementary を含む section frame に効くため実装と自然に一致する。水平成分は leading / trailing 基準 (M5)。負値は描画時に 0 へ、radius は幾何的に許される値へ clamp する (M6)。Modern は新たな色既定を導入せず、箱と下地の色は既存の `cellBackgroundColor` / `backgroundColor` の対比に委ねる (M1 — モックの下地はサンプル SampleTheme のデモ値)。

**理由:** Theme の既存文法 (`cellIconSize: CGFloat?` = nil で platform 既定) と完全に同型で、利用者の学習コストがない。style 別の既定を「未指定」の解決先に押し込めるのは、この文法だけが自然に表現できる。

**代替案:**
- **A: sentinel 方式 (`-1` = 未指定、`rowHeight` / `headerHeight` と同型)** — 数値系はまだしも `NSDirectionalEdgeInsets` / `PaddingValues` に sentinel が定義できず、4属性内で文法が割れる。sentinel は AiForms 運用互換のための歴史的形式であり新規 API に広げない。却下
- **B: 非 optional + Modern 既定値を直接埋める** — 「未指定」と「明示的に既定値と同じ値を指定」が区別できず、将来既定値を改訂したとき利用者の未指定値が追従しない。却下

### Decision 2: iOS の箱は background decoration + layout subclass の frame 補正で描く

**採用案:** `UICollectionViewCompositionalLayout` のサブクラスを新設し、Section 背景の decoration (`NSCollectionLayoutDecorationItem.background` 相当) の layoutAttributes を「その Section の **Cell 行だけ**を覆う frame」に補正する。装飾値 (radius / border / 背景色) はカスタム `UICollectionViewLayoutAttributes` サブクラスで decoration view へ輸送し、decoration view が角丸背景とボーダーを描画する。margin は sectionProvider で `section.contentInsets` に反映する。

**理由:** decoration item は compositional layout の公式な Section 背景機構でスクロール同期・再利用を layout が保証する。ただし素の decoration は Header / Footer supplementary を含む Section 全域を覆うため、「箱は Cell 群のみ・Header / Footer は外」(探索決定) には frame 補正が必須。layoutAttributes サブクラスは decoration view へ値を渡す UIKit の標準手法で、Theme 変更時は invalidateLayout で再評価に乗る。

**代替案:**
- **A: 先頭 / 末尾 Cell の corner mask 方式 (Cell 背景の合成で箱を表現)** — ボーダーが行単位に分断されて連続した枠線を描けない。押下ハイライト (`selectedColor`) と Cell 背景の関係にも干渉する。却下
- **B: RecyclerView 方式の模倣 (collection view の背後に自前 overlay view を置き座標計算で描く)** — スクロール・insert / delete アニメーションとの同期を全部自前で負うことになり、decoration item が無償で提供する保証を捨てる理由がない。却下

### Decision 3: iOS の separator は既存 `itemSeparatorHandler` の style 分岐で実現する

**採用案:** `separatorConfiguration(for:base:)` に Modern 分岐を追加: Section 先頭 Cell の top / 末尾 Cell の bottom は非表示、中間は leading 側を Classic と同じ inset 規則 (箱の内側 leading 端基準)、trailing 側にも同量の inset を取る (箱の分断見えを避ける。2026-08-20 ユーザー指示)。

**理由:** separator は list configuration の既存機構が担っており、Classic の規則 (位置別の出し分け) と同じ場所に分岐が増えるだけ。描画機構を増やさない。

**代替案:**
- **A: decoration view に separator も自前描画** — Cell の self-sizing・アニメーション中の行位置追従を自前で再実装することになる。却下

### Decision 4: Android は `ModernSectionDecoration` を Theme 駆動化し、単一クラスで背景・ボーダー・separator を描く

**採用案:** ハードコード定数 (12dp / 16dp / 12dp) を廃し、`theme` の4属性 (未指定は現行値と同じ既定へ解決) から毎描画時に解決する。箱の範囲判定を「Section 内の **Cell 行のみ**」に変更し、Section Header / Footer 行を `getItemOffsets` の inset 対象と箱の上下端計算から除外する。背景とボーダーは `onDraw` (Cell の下)、セクション内の中間 separator は `onDrawOver` (Cell の上) で描く。

**理由:** 箱の上下端座標は背景・ボーダー・separator の出し分けすべてが共有する情報で、単一 decoration に置くのが最も単純。separator を `onDrawOver` にするのは、Cell が `setBackgroundColor` で背景を塗る場合に `onDraw` の罫線が上書きされて消える既知の教訓への対処。

**代替案:**
- **A: 背景用と separator 用に decoration を分割** — Section 端の判定ロジック (前後の sectionId 比較・Header / Footer 除外) が2クラスに重複する。却下

### Decision 5: Classic の `sectionMargin` は上下成分のみ適用する

**採用案:** iOS は Classic の sectionProvider でも `section.contentInsets` の top / bottom に `sectionMargin` の上下成分を反映 (左右は 0 固定)。Android は `ClassicSectionDecoration.getItemOffsets` に Section 先頭 / 末尾行への上下 offset を追加 (左右は 0 固定)。左右成分の無視は両 OS 共通の契約として明記する。

**理由:** 探索での決定 (X2)。左右を効かせると「Classic の Section 境界は全幅」契約と衝突する。既定 0 なので現行外観は不変。

**代替案:** (探索で却下済み) Modern 専用化 — flat リストのセクション間隔の要望に応えられない。4方向適用 — 全幅契約と衝突。

### Decision 6: 既定値の所有

**採用案:** Modern の既定寸法は各 platform が所有する。Android は現行実装値を既定として維持 (視覚非破壊)。iOS は `.insetGrouped` 廃止に伴い iOS 設定画面風の値をライブラリが定義する — 具体値はモック承認 (ui/) で確定し、実装はモックを正とする。

確定既定値 (2026-08-20 モック案 A 承認。モックの CSS px を論理単位へ 1:1 で写す):

| 属性 | iOS (pt) | Android (dp、現行実装値の維持) |
|---|---|---|
| sectionMargin | top 22 / leading 16 / trailing 16 / bottom 0 | top 12 / bottom 12 / start 16 / end 16 |
| sectionCornerRadius | 26 | 12 |
| sectionBorderWidth / Color | 0 / 透明 | 0 / 透明 |

**理由:** list-appearance.md の「platform 実装を同じ生の margin・radius 値へ統一しない」の意図 (既定値の platform native 維持) を継続する。

**代替案:**
- **A: 両 OS の既定を同一値に統一** — 上記 concepts の意図に反する。却下

### Decision 7: 箱と Cell 背景の合成契約 (second-opinion M3 の採用)

**採用案:** ボーダーは Cell 背景・押下 / 選択背景より前面 (最前) に描く。Section 先頭 / 末尾 Cell の背景と押下背景は箱の角丸形状で clip する (iOS は decoration / cell 側の corner mask、Android は canvas の clipPath 等 — 手段は実装に委ねるが観察結果は spec の合成契約に従う)。`CellStyle.backgroundColor` は Cell 行領域を箱背景より前面で塗る。

**理由:** 現行 Cell は自身を不透明背景で塗るため (CellBaseLayout)、合成順を契約化しないとボーダー被覆・角のはみ出しがプラットフォーム毎に異なる結果になる。

**代替案:**
- **A: Cell 側の corner mask だけで箱を表現 (ボーダーも Cell 分担)** — ボーダーが行単位に分断され連続した枠線を保証できない。却下
- **B: Modern では Cell 自身の背景描画を全面禁止** — `CellStyle.backgroundColor` の既存契約 (単一 Cell の背景指定) を Modern で失う。却下

### Decision 8: Android の箱端はオフスクリーンの Section 端を考慮して描く (second-opinion M8 の採用)

**採用案:** 可視 child の集計に加えて「Section の先頭 / 末尾 item が viewport 内に実在するか」を判定し、実在しない側の箱端は viewport 外へ延長して角丸・端ボーダーを描かない。

**理由:** 現行 `ModernSectionDecoration.onDraw` は画面内の child だけから箱の top / bottom を求めるため、viewport より長い Section のスクロール中に偽の角丸端が現れる。この欠陥を継承しないことを契約 (spec) とテストで固定する。

**代替案:**
- **A: 現行の可視 child のみ集計を維持** — スクロール中の偽の箱端がそのまま残る。却下

## Risks / Trade-offs

- **iOS layout 置換の glitch**: 既存コードに `setCollectionViewLayout` 同期差し替えの glitch 注意コメントがあり、style 切替は既存の `rebuildLayout()` 経路を維持する。decoration 追加が snapshot 適用と干渉しないかは実装時にサンプルで目視確認する
- **Cell 背景と箱の角の干渉**: 合成契約 (Decision 7) として実装前に確定済み。実装手段の選択は残るが、観察結果は spec の Scenario で検証する
- **decoration frame 補正の未知数**: Decision 2 の frame 補正 (supplementary 除外) は カスタム layout の layoutAttributes 加工が必要で、self-sizing との相互作用は実装スパイクで早期検証する (tasks 先頭に配置)
- **Android の視覚変化**: Header / Footer が箱の外へ出るのは意図した挙動変更 (探索決定)。Modern は未使用の骨組み段階のため実害は限定的

## Migration Plan

公開 API は追加のみでソース互換。iOS の `appearance(for:)` (internal) は削除または Classic 専用化するが、`KsSettingsViewStyle` の公開形状は不変。既存テスト `appearance(for: .modern) == .insetGrouped` は新契約のテストへ置換する。

## Open Questions

- ~~iOS Modern 既定値の具体数値~~ → 解消 (2026-08-20 モック案 A 承認、Decision 6 の表が確定値)
- decoration frame 補正の実装詳細 (self-sizing・アニメーション時の追従) — tasks 1.1 のスパイクで検証

## ADR 候補

- なし。方式決定 (自前装飾化) は ios/ADR-0003 で起票済み。Decision 1〜6 はその具体化と局所 API 詳細であり、コード+テストと concepts 追随 (蒸留時の list-appearance.md 改訂) で足りる
