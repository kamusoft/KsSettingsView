---
type: reference
title: CustomCell
description: 事前登録なしで任意の宣言 UI を1行にする CustomCell の公開契約とカスタムセル3層の使い分け
tags: [cells, public-api, custom, declarative-ui]
timestamp: 2026-08-16
---

# CustomCell

この文書は、任意の宣言 UI (iOS: SwiftUI / Android: Compose) をその場で設定行にする `CustomCell` の公開契約を説明する。読むと、content 値 + builder という構造、等価性と再バインドの規則、挙動プロパティ (`onTap` / `showArrow` / `isEnabled` / `isVisible`)、高さの扱い、そしてカスタムセル3層 (①インライン / ②ラップ関数 / ③UserDefinedCell) の使い分けが分かる。[基本 Cell](basic-cells.md) の共通契約を先に読むと分かりやすい。

## 目的

プリセット外の UI (スライダー等) を1行だけ差し込みたいとき、自前 Cell 型 + Renderer の定義と `register` を強いずに、DSL へ直接書けるようにする。CustomCell は**利用者定義の content 値 (データ)** と **content から View を生成する builder クロージャ**を併せ持つ ([core/ADR-0014](../../../decisions/core/0014-customcell-content-value-with-builder.md))。Renderer / ViewHolder は基本 Cell・入力 Cell と同じ**標準登録集合** (Host 初期化時に既定で登録される Renderer 群) に含まれ、利用者による [Cell Renderer Registry](../architecture/cell-renderer-registry.md) の操作は不要 (未登録 Cell を例外にする Registry の strictMode でも例外にならない)。

データを持たない固定表示向けに、content を省略して builder (引数なし) だけで生成する省略形もある。この形の等価性は content を除く参加要素 (`id` / `style` / `showArrow` / `isEnabled` / `isVisible`) で決まる。

行は accessory (矢印) 領域を除く **full-bleed** で、共通行レイアウト (title / description / icon 等のスロット) を持たない ([core/ADR-0022](../../../decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md) の適用除外)。行内のレイアウトは利用者が builder 内で SwiftUI / Compose の合成により組む。ネイティブ View を使いたい場合は専用の口ではなく `UIViewRepresentable` / `AndroidView { }` の公式 interop で builder 内に埋め込む。

## 等価性と再バインド

**再バインド**とは、差分検出が Cell を「変わった」と判定したとき builder を再実行して行の表示を作り直すこと。等価と判定されれば builder は再実行されず、表示は据え置かれる。再バインドの要否は Cell の値等価で決まる。参加するのは `id` / `style` / `content` / `showArrow` / `isEnabled` / `isVisible` で、**関数値 (`builder` / `onTap`) は除外**される。DSL 再評価のたびに新規クロージャが生成されても再バインドは暴発しない。

裏返しの契約として、**見た目や動作を変える値は関数のキャプチャではなく content に含める**。関数値だけを差し替えても再バインドは発生しない。content は値等価 (iOS: `Hashable`、Android: `equals` / `hashCode`) を持つ non-null の型であること (Android は `Content : Any` で型強制、iOS は契約)。

内部表現はプラットフォームで異なる (iOS: init 時点で型消去した非ジェネリック struct、Android: ジェネリック class。[core/ADR-0016](../../../decisions/core/0016-customcell-type-erasure-vs-generic-representation.md))。iOS は `AnyHashable` の数値正規化 (`Int(1)` と `Double(1.0)` が等価) を補うため content の実体型も等価性に参加しており、値が同じでも型が変われば再バインドされる。

## 挙動プロパティ

- **`onTap`** (既定 nil = 行タップ非対応): 非 nil かつ有効時、行のタップで発火する。content 内の操作可能要素がタップを消費した場合は発火しない (二重発火は起きない)。nil のときは content 内部の操作 (ボタン・スライダー等) を妨げない。
- **`showArrow`** (既定 false): true で [基本 Cell](basic-cells.md) の CommandCell と同一の Disclosure Indicator (chevron) を trailing に表示し、content の占有領域は indicator 領域を除いた範囲になる。`onTap` と独立に指定できる。
- **`isEnabled`** (既定 true): false のとき行タップと content 内部の操作の両方を抑止し、**content 全体を淡色化する** (alpha 0.38)。標準 Cell は text 色を無効時用の色 (`Theme.disabledTextColor`) へ差し替えて無効を表すが、任意ビューにはその差し替え先が特定できないための代替表現 ([Cell の視覚状態](../styling/cell-visual-states.md) の例外)。追加の描き分けは利用者の自由。無効時、Android は content が TalkBack の読み上げ対象から外れる (iOS の VoiceOver は読み上げが残る。意図的な非対称 — [core/ADR-0017](../../../decisions/core/0017-customcell-disabled-suppression-over-a11y-symmetry.md))。
- **`isVisible`** (既定 true): false のとき、表示対象として列挙される Cell 集合 (visible projection — [設定ツリー](../core-model/settings-tree.md) で定義) から除外され、行として出力されない。Cell 値は model に保持されたまま残る。

## 高さと style

- 行高さは content の self-sizing に全面委任し、content のサイズが実行時に変化しても行高さが追従する (専用の再計測 API は無い)。
- `style: CellStyle` は保持するが、効くのは**行レベルの項目 (背景色・cellHeight) のみ**。テキスト色・フォント等のコンテンツ内装項目は builder の出力に影響しない。
- cellHeight の意味は既存の高さ解決契約 ([Cell 共通行のレイアウト](../styling/cell-row-layout.md)) に従う: `Theme.hasUnevenRows == true` なら最低高として働き内容に応じて伸び、`false` のときだけ固定される。
- content が行の高さに収まらないとき (固定高等) の縦位置は、両プラットフォームとも「収まるときは縦中央、収まらないときは上端揃え」。

## DSL による配置

Android は `DSLSectionScope` の拡張関数 (content あり / なしの 2 形)、iOS は SectionBuilder への struct 直書き。戻り値・準拠により `.cellHeight(...)` / `.cellID(...)` 等の既存 modifier チェーンが機能する。**icon modifier は型として非対応** (アイコン領域が存在しない)。

## カスタムセル3層の使い分け

| 層 | 呼び名 | 選ぶ場面 |
|---|---|---|
| ① | CustomCell (インライン利用) | その場構築・その場専用。DSL に直書き |
| ② | CustomCell (ラップ関数による再利用) | 固定 builder + content 型を与えた CustomCell を返す関数 (例: `SliderCell(...)`)。①があれば登録不要で自動的に手に入る |
| ③ | UserDefinedCell (利用者定義 Cell) | 自前 Cell 型 + Renderer + `register` の一級市民セル ([Cell Renderer Registry](../architecture/cell-renderer-registry.md)) |

再利用したいだけなら②で足りる。③を選ぶのは、独自の型 identity・スタイル解決への参加・描画の完全制御 (共通行レイアウトへの参加を含む) が必要な場合に限られる。`UserDefinedCell` はコード上の型名ではなく概念上の呼び名。

## 保証すること

- Registry 未操作で CustomCell が builder の出力で描画される (標準登録集合)。
- builder / onTap の関数値だけが異なるインスタンスは等価で、再バインドは発生しない。content および `showArrow` / `isEnabled` / `isVisible` / `style` の変更は再バインドを発生させる。
- スクロール等で行のビューが別の Cell に使い回される (リサイクルされる) とき、前の content に由来する表示・listener・購読を残さない。
- `isEnabled = false` で行タップ・content 内部の操作の両方を抑止する。
- 可変高さ構成で content のサイズ変化に行高さが追従する。

## してはいけないこと

- 見た目や動作を左右する値を builder のキャプチャだけに置かない (再バインドされず画面が古いまま残る)。
- content に値等価を持たない型 (参照同一性のみのクラス等) や Optional を使わない。
- テキスト系 CellStyle 項目が content に効くと仮定しない。
- 行が画面外へ出て戻る再表示をまたいで、content 内部の一時状態 (`remember` / `@State` 等) が保持されると仮定しない。逆に「必ず初期化される」とも仮定しない — Android はリサイクル機構の都合で維持されることがあり ([android/ADR-0015](../../../decisions/android/0015-customcell-pool-aware-composition-disposal.md))、iOS はリサイクル毎に hosting 階層を再生成する ([ios/ADR-0002](../../../decisions/ios/0002-customcell-hosting-recreation-accepted.md))。再表示後も残したい状態は content 値へ持ち上げる。
- 無効時の Android で content が TalkBack に読み上げられると仮定しない。
- Section / Root の Header・Footer 装飾領域用の型消去ラッパ `KsAnyView` を Cell 本体の代替として使わない (`KsAnyView` は意図的に等価性へ参加しないため、ツリー再構築のたびに「変更あり」と判定され再バインドが無駄打ちされる)。

## 利用例

表示値は content 経由 (builder の引数 `v`) で渡し、状態への書き込みだけをキャプチャで行う — 表示に効く値をキャプチャに置かない契約と両立する形。

```swift
KsSettingsView {
    Section("音量") {
        CustomCell(content: volume, showArrow: false) { v in
            HStack {
                Image(systemName: "speaker.wave.2")
                Slider(value: $volume, in: 0...100)
                Text("\(Int(v))")
            }
        }
    }
}
```

```kotlin
KsSettingsView {
    Section(header = "音量") {
        CustomCell(content = volume) { v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Slider(value = v, onValueChange = { volume = it })
            }
        }.cellHeight(72)
    }
}
```

②のラップ関数再利用は、これらをそのまま関数に包むだけでよい (例: `func SliderCell(value:) -> CustomCell`)。

## 関連

- [基本 Cell](basic-cells.md)
- [Cell Renderer Registry](../architecture/cell-renderer-registry.md)
- [Cell 共通行のレイアウト](../styling/cell-row-layout.md)
- [Cell の視覚状態](../styling/cell-visual-states.md)
- [core/ADR-0014](../../../decisions/core/0014-customcell-content-value-with-builder.md) — content 値 + builder という表現の決定
- [core/ADR-0022](../../../decisions/core/0022-customcell-lifecycle-delegated-to-platform-adr.md) — 共通行レイアウト統一の適用除外と宣言 UI lifecycle の platform ADR への委譲 (core/ADR-0015 を supersede)
- [core/ADR-0016](../../../decisions/core/0016-customcell-type-erasure-vs-generic-representation.md) — プラットフォーム別内部表現の決定
- [core/ADR-0017](../../../decisions/core/0017-customcell-disabled-suppression-over-a11y-symmetry.md) — 無効時の操作抑止優先と読み上げ非対称の受け入れ
