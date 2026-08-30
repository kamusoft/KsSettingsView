# レビュー結果 - unify-cell-common-fields-via-shared-row-layout

**レビュー日時**: 2026年06月09日
**レビュワー**: sdd-reviewer
**変更提案ID**: unify-cell-common-fields-via-shared-row-layout

## サマリー

本変更提案は「全 Cell に共通フィールド（description/valueText/icon/hintText/accentColor）を追加し、
共通行レイアウト関数（iOS: `ksCellRow`, Android: `KsCellRow` Composable）を 1 つに集約することで
重複を排除する」という二本柱で構成されている。

実装状況の総括は以下の通り。

| 領域 | 状態 | 評価 |
|------|------|------|
| iOS モデル拡張 (Phase 2) | 完了 | spec 通り |
| iOS 共通行レイアウト関数 `ksCellRow` (Phase 1) | 完了 | spec 通り、accessory 順序も準拠 |
| iOS 各 Cell View の `ksCellRow` 経由化 (Phase 3) | 完了 | spec の MUST を満たす |
| iOS DSL 拡張 (Phase 4) | 完了 | spec 通り |
| iOS ユニットテスト (Phase 5) | 完了 | 83 件すべて成功 |
| Android モデル拡張 (Phase 7) | 完了 | spec 通り |
| Android 共通 Composable `KsCellRow` (Phase 6) | 実装はされている | **誰からも呼ばれていないデッドコード** |
| Android 各 ViewHolder の `KsCellRow` 経由化 (Phase 8) | **未実施** (8.1〜8.8 すべて未チェック) | **spec の MUST 違反** |
| Android ButtonCellViewHolder の aux フィールド描画 | **未実施** | **spec の MUST 違反** |
| Android DSL 拡張 (Phase 9) | 完了 | spec 通り |
| Android ユニットテスト (Phase 10) | 一部完了 (10.2 / 10.6 未実施) | KsCellRow 経由の描画検証なし、右端整列の回帰テストなし |
| サンプルアプリ (Phase 11) | 完了 | iOS / Android 双方 |
| ビルド・テスト (Phase 12) | `swift test` / `:ks-settingsview-ui:test` / `:ks-settingsview-compose:test` すべて成功 | 動作確認 (12.4/12.5) は実機未実施 |

### ビルド・テスト確認結果
- `swift build`: 成功
- `swift test`: 83 tests passed, 0 failures
- `./gradlew :ks-settingsview-ui:assembleDebug`: BUILD SUCCESSFUL
- `./gradlew :ks-settingsview-ui:test`: BUILD SUCCESSFUL（UnifyCellCommonFieldsTest=13、全 240+ tests pass、failures/errors=0）
- `./gradlew :ks-settingsview-compose:test`: BUILD SUCCESSFUL

### 判定

**`CHANGES_REQUESTED`**

理由:

1. Android 側で **本 change の中核 spec（settings-view-android-compose）の MUST 要件が満たされていない**。
   - spec ("共通行レイアウト Composable KsCellRow" Requirement) は **「各 CellViewHolder は `bind(cell, theme)` 内で `ComposeView.setContent { KsCellRow(...) { ... } }` を呼び出して描画しなければならない (MUST)」** と定めているが、実装は既存 View ベース（`LabelCellViews` + `applyLabelCellContents`）のままで `KsCellRow` を一切呼んでいない。
   - `KsCellRow` Composable は内部関数として実装されているが、**プロダクションコードからの呼び出し箇所が 0**。実質デッドコードであり、Phase 6 の実装は spec の「重複排除」目的を達成していない。
2. Android `ButtonCellViewHolder` が `cell.icon` / `cell.valueText` / `cell.hintText` を **完全に無視している**。spec (cell-types-basic「ButtonCell が icon / valueText / hintText を持てる」/「icon / valueText / hintText を指定したときの titleAlignment の挙動」Scenario) の MUST に違反。
3. 上記が「scope 外」として許容されると、後続 change が `KsCellRow` 経由前提で書かれた場合に Android 側で破綻するリスクが高い（design.md Decision 4 / 8 に反する）。

「scope を理由に MUST 違反を許容する」のは仕様駆動開発の原則から見て不可。
**spec 自体に「Android Phase 8 を本 change ではスキップする」と書いてあれば許容できるが、本 change の spec には明確に MUST と書かれている**。したがって、本 change のままアーカイブしてはならない。

採りうる対処は次のいずれか:

- **(A)** Android Phase 8 / ButtonCellViewHolder aux 対応を本 change で完遂する（推奨）。
- **(B)** spec を改訂し、Android Compose 化を別 change（例: `migrate-android-cells-to-kscellrow`）に切り出す。その際は本 change の `settings-view-android-compose` spec から「`ComposeView.setContent { KsCellRow(...) }` で呼び出す MUST」を削除（または「将来 change で実施する」と明文化）。Android `ButtonCell` の aux フィールドについても cell-types-basic spec を改訂し「Android は本 change ではモデル値のみ保持。描画は後続 change」と例外明記する。

(B) を採る場合でも、デッドコードとなった `KsCellRow` は **(B1)** プレースホルダとしてコメント明記して残す、または **(B2)** 一旦削除する、いずれかを選択すべき。

## 指摘事項

### 🔴 Critical

#### [Critical-1] Android: 共通 Composable `KsCellRow` がプロダクションから 1 度も呼ばれていない

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt:53`（定義のみ）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{LabelCellViewHolder,CommandCellViewHolder,SwitchCellViewHolder,CheckboxCellViewHolder,RadioCellViewHolder,SimpleCheckCellViewHolder,ButtonCellViewHolder}.kt`

**問題点**:

`settings-view-android-compose` spec の "共通行レイアウト Composable KsCellRow" Requirement は以下を MUST と定めている:

> 各 CellViewHolder（…）は、`bind(cell, theme)` 内で `ComposeView.setContent { KsCellRow(...) { /* accessory */ } }` を呼び出して描画しなければならない (MUST)。`title` / `description` / `valueText` / `icon` / `hintText` の Compose レイアウト組み立てロジックを各 ViewHolder 内に重複して実装してはならない (MUST NOT)。

しかし `grep -rn KsCellRow android/ks-settingsview-ui/src/main` の結果、`KsCellRow` を呼んでいる箇所は皆無で、全 ViewHolder が引き続き従来の `LabelCellViews` + `applyLabelCellContents`（純粋な View ヒエラルキ）を使用している。

これは spec の「重複実装してはならない (MUST NOT)」と「`KsCellRow` 経由でなければならない (MUST)」の両方に違反している。tasks.md の 8.1〜8.8 が未チェックなのも整合している。

Phase 6 で実装した `KsCellRow` は呼び出し側を伴わないため、現状は完全な dead code であり、

- メンテナンス対象が増えるだけ（実 UI からの fb なし）
- AndroidView 経由の Drawable 表示パスや icon 領域非表示分岐などにバグが入っても気づけない（テスト自体が単体構築テストにとどまる）

**推奨修正**:

(A) 本 change で Android Phase 8 を完遂する場合: 各 ViewHolder を以下のような構造に変換する（例: SwitchCellViewHolder）。

```kotlin
internal class SwitchCellViewHolder(
    private val composeView: ComposeView,
) : CellViewHolder<SwitchCell>(composeView) {

    override fun bind(cell: SwitchCell, theme: Theme) {
        val effective = EffectiveStyle.from(composeView.context, theme, cell.style)
        composeView.setContent {
            KsCellRow(
                title = cell.title,
                description = cell.description,
                valueText = cell.valueText,
                icon = cell.icon,
                hintText = cell.hintText,
                effective = effective,
                isEnabled = cell.isEnabled,
            ) {
                Switch(
                    checked = cell.isOn,
                    onCheckedChange = { cell.onValueChanged?.invoke(it) },
                    enabled = cell.isEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = cell.accentColor ?: Color(effective.accentColor),
                    ),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        // height / clickable は KsCellRow 内部 or wrapper 側で吸収
    }
    ...
}
```

`MaterialCheckBox` などの View ベース部品を残したい場合は `accessory` slot 内で `AndroidView` 経由で配置することは spec も許容している（「`MaterialCheckBox` … `setPadding(0, 0, 0, 0)` / `minimumWidth = 0` / `minimumHeight = 0` 設定を維持」）。

(B) Android Compose 化を本 change から外す場合: `settings-view-android-compose/spec.md` の "共通行レイアウト Composable KsCellRow" Requirement と "各 ViewHolder が共通 Composable を経由する" Scenario を後続 change へ移動するか、Requirement 本文に「**本 change では実装せず、後続 change `XXX` で実施する**」と明文化する必要がある。spec 自体を改訂しない限り MUST 違反のままアーカイブはできない。

加えて `KsCellRow.kt` の存在意義として、Phase 6 完了の証跡を維持するなら以下のような注意書きを追記する:

```kotlin
/**
 * NOTE: 本 Composable は Phase 8 で各 ViewHolder から呼ばれる予定だが、本 change では
 * 既存 View ベース ViewHolder を維持しているため、現時点では未使用。利用は後続 change
 * （TODO: 後続 change ID を明記）で開始する。
 */
@Composable
internal fun KsCellRow(...)
```

---

#### [Critical-2] Android: ButtonCellViewHolder が `icon` / `valueText` / `hintText` を完全に無視している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:32-65`

**問題点**:

`ButtonCellViewHolder.bind()` は `cell.title` / `cell.titleAlignment` / `cell.titleColor` / `cell.isEnabled` のみ反映し、`cell.icon` / `cell.valueText` / `cell.hintText` を読み取っていない。

`cell-types-basic` spec の以下 Scenario は MUST 相当で iOS / Android 双方に適用される:

- "ButtonCell が icon / valueText / hintText を持てる" Scenario: 「左端にアイコン、タイトル「登録」（青系、左寄せ）、右側に valueText「送信」と hintText「推奨」が表示される。`titleAlignment = .start` は title のみに適用され、icon / valueText / hintText の配置は他 Cell と同じ規約に従う」
- "icon / valueText / hintText を指定したときの titleAlignment の挙動" Scenario（MODIFIED Requirement ButtonCell 内）: 「`titleAlignment = .center` は title 列の中での揃え位置のみを制御する」

実装は単一 `TextView` 上に `cell.title` のみを描画しているため、`ButtonCell(title="登録", icon=...)` を渡してもアイコンは出ない。`UnifyCellCommonFieldsTest.kt` には ButtonCell の aux 描画テストがなく、未検出のまま通っている。

iOS の `ButtonCellView.swift` は `hasAuxField = icon || valueText || hintText` 分岐で `ksCellRow` 経由に切り替える正しい実装で、Android 側だけ取り残された状態。

**推奨修正**:

`ButtonCellViewHolder` を `KsCellRow` 経由に書き換えるか、または現在の `TextView` 単独描画と View ベース LabelCell 風レイアウトの 2 系統を `hasAux` 判定でスイッチする。

```kotlin
override fun bind(cell: ButtonCell, theme: Theme) {
    val hasAux = cell.icon != null || cell.valueText != null || cell.hintText != null
    if (hasAux) {
        // KsCellRow 経由（または LabelCellViews + applyLabelCellContents で title 列内 alignment 反映）
        // 通常レイアウト + title 列内 titleAlignment 反映
        bindRegularLayout(cell, theme)
    } else {
        // 既存ボタンスタイル (TextView 単独 + 全体 titleAlignment 反映)
        bindButtonStyle(cell, theme)
    }
}
```

これに合わせて `ButtonCellViewHolderTest`（既存）に aux 指定時のテストを追加する。

---

### 🟠 Major

#### [Major-1] Phase 10.2 / 10.6 が未実施: `KsCellRow` の描画検証と右端整列回帰テストが欠落

**該当箇所**: `openspec/changes/unify-cell-common-fields-via-shared-row-layout/tasks.md:86-90`

**問題点**:

- 10.2 「`KsCellRow` 経由の描画テスト（Compose Test）：`description` / `valueText` / `icon` / `hintText` が指定されたとき対応する Text / Image が SemanticsNode として存在し、`null` のとき存在しないことを確認」
- 10.6 「右端アクセサリ X 座標整列の回帰テスト（SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell を縦に並べたときの右端 X 座標一致、±1px 以内）」

10.2 は Critical-1 と同根（ViewHolder が `KsCellRow` を呼んでいないため、Compose Semantics 経由のテストが書けない）。10.6 は `cell-types-basic` spec の "右端アクセサリ位置の整列（Android）" Scenario の回帰検知点で、これがないと将来の DP 変更や padding 変更で容易に揃いが崩れる。

**推奨修正**:

Critical-1 と一緒に解決する。`KsCellRow` 経由になれば 10.2 は `createComposeRule()` + `onNodeWithText` で容易に書ける。10.6 はそのまま既存 View ベース ViewHolder のままでもユニットテスト（Robolectric で `measure` → `getRight()` 比較）で書けるため、本 change 内で追加可能。

---

#### [Major-2] `KsCellRow.kt` の `hintText` 色解決が spec と食い違う（`descriptionColor` を使用）

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt:68`

```kotlin
val hintColor = if (isEnabled) Color(effective.descriptionColor) else Color(effective.disabledTextColor)
```

**問題点**:

`cell-types-basic` spec の「`hintText` の色: `CellStyle.hintTextColor → Theme.cellHintTextColor → Theme.cellAccentColor`」と定められているが、`KsCellRow` 実装は **`hintColor` に `effective.descriptionColor` を使用している**（`hintTextColor` ではない）。iOS の `ksCellRow` は正しく `effective.hintTextColor` を使っているため、両プラットフォームで挙動がずれる。

なお Critical-1 のとおり `KsCellRow` は誰からも呼ばれていないので**現状ユーザ影響はゼロ**だが、後続で呼び出し側を実装した瞬間に hintText の色が仕様と異なる挙動になる。

**推奨修正**:

```kotlin
val hintColor = if (isEnabled) Color(effective.hintTextColor) else Color(effective.disabledTextColor)
```

`EffectiveStyle` に `hintTextColor` が無い場合は、Change 1 で追加されたフィールドを確認のうえ追加する（spec の前提条件「Phase 1 で `EffectiveStyle.hintTextColor` が両プラットフォームで利用可能」がそもそも満たされていない可能性がある）。

加えて `hintStyle` のフォントも spec は `CellStyle.hintTextFont → Theme.cellHintFont → preferredFont(.footnote)` と定めるが、現状 `KsCellRowLayout.kt:85-89` は `descriptionSizeSp` を流用している。`FontFamily.Default` のハードコードも含め、`effective.hintTextFont` 系を反映するよう修正する必要がある。

---

#### [Major-3] `KsCellRow.kt` がタイトル等のフォントを `FontFamily.Default` ハードコードしている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt:70-89`

**問題点**:

`titleStyle` / `descriptionStyle` / `valueStyle` / `hintStyle` ですべて `fontFamily = FontFamily.Default` をハードコードしており、`effective.titleTypeface` 等のフォント情報が反映されていない。spec の「色・フォント・サイズは Change 1 (`port-theme-and-cellstyle-missing-fields`) で確立された解決順序 (`CellStyle → Theme → 既定`) に従わなければならない (MUST)」に違反する。

これも Critical-1 のため現状ユーザ影響はゼロだが、放置すると同根の品質低下を引き起こす。

**推奨修正**:

`EffectiveStyle` に既に `titleTypeface` / `descriptionTypeface` / `valueTextTypeface` が存在する（`LabelCellViewHolder.applyLabelCellContents` で使われている）。`android.graphics.Typeface` から Compose `FontFamily` への変換ヘルパを追加して反映する:

```kotlin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.androidx.compose.ui.text.font.toFontFamily // 注: 直接変換 API は無いため remember + Typeface ベース wrapper

private fun Typeface.toComposeFontFamily(): FontFamily = FontFamily(this) // androidx.compose.ui.text.font.Typeface ラッパ
```

または既存の `LabelCellViews` パスを踏襲して Compose 内で `AndroidView` でラップする代替もある。いずれにしても spec MUST を満たすよう修正が必要。

---

#### [Major-4] Phase 12.4 / 12.5 が未実施: 実機目視確認をしていない

**該当箇所**: `openspec/changes/unify-cell-common-fields-via-shared-row-layout/tasks.md:105-106`

**問題点**:

iOS / Android サンプルアプリでの目視検証が完了していない。design.md Risk セクション「`UIListContentConfiguration` ベースのレイアウトでは『ヘッダ右上の hintText』『accessory の組み合わせ順』等の微妙な見え方が現状 `LabelCellView` のロジックに依存している」へのミティゲーションは「ユニットテスト＋サンプルアプリで実機確認」と明記されており、目視確認が未実施では Risk が打ち消されていない。

**推奨修正**:

iOS Simulator / Android Emulator で `UnifyCellCommonFieldsDemo*` ページを起動し、

- 各 Cell で icon / valueText / hintText / description が想定位置に出る
- Switch / Checkbox / Radio / SimpleCheck の右端 X 座標が揃う
- RadioCell の `accentColor` 個別指定が反映される

の 3 点について最低限スクリーンショットを残す（または手動チェックリストとして本 change にコメントで報告する）。

---

### 🟡 Minor

#### [Minor-1] `KsCellRow.kt` の icon 表示が `KsImage.SystemName` の場合に「アイコン領域そのものを非表示」と一致しているが空文字コメントが不十分

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt:99-125`

**問題点**:

spec の「`SystemName` 派生はフォールバックでアイコン領域非表示」「`icon == null` の場合はアイコン領域そのものを描画せず、`title` を左端から開始させる」は満たしているが、`KsImage.Drawable` の `AndroidView` 経由表示について `rememberDrawablePainter` を使えない理由のコメントしかない。

`androidx.compose.ui.viewinterop.AndroidView` を `Image` 領域代替に使う場合、`size(24.dp).padding(end=8.dp)` を `Modifier` 並びで指定しているが、`size(24.dp)` の前に `padding` を置く順序の方が明示的に意図が見える（現状でも動くが、将来の挙動依存を避けたい）。

**推奨修正**:

オプション的修正。Critical-1 を解決する際にあわせて `Image(painter = remember(icon.drawable) { BitmapPainter(icon.drawable.toBitmap().asImageBitmap()) })` のようなパターンに置き換えるとパフォーマンスも良くなる（recomposition で AndroidView 再生成を避ける）。

---

#### [Minor-2] `KsCellRow.kt` が `padding(horizontal = 16.dp, vertical = 4.dp)` をハードコード

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt:95`

**問題点**:

水平 16dp / 垂直 4dp が直書きされており、`LabelCellViewHolder.buildLabelCellViews` 内のロジック（4dp / 16dp）と二重管理になっている。`refine-basic-cells-style` などで内部値が変わると差異が生じる。

**推奨修正**:

`KsCellRow` から呼ぶ場合の padding は `EffectiveStyle` 経由（または共通定数）に寄せる。

---

#### [Minor-3] サンプルアプリのテキスト （特に「ButtonCell（aux 全部指定 → 通常レイアウト）」）が iOS 側にしかない可能性

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/UnifyCellCommonFieldsDemoScreen.kt`

**問題点**:

iOS 側のサンプル (`UnifyCellCommonFieldsDemoView.swift`) は本 change の変更点を視覚的に網羅しているが、Android 側のサンプルで `ButtonCell` に icon/valueText/hintText を指定したケースを並べていても、Critical-2 のため画面に何も出ない（モデル値だけ）。

**推奨修正**:

Critical-2 解決と合わせて Android サンプルにも ButtonCell aux 指定パターンを追加する。

---

### 🔵 Suggestion

#### [Suggestion-1] `KsCellRow` の引数を 1 つの data class にまとめて Stable 化する

引数が 8 個と多いため、`@Immutable` data class に集約することで Strong skipping mode の効果が高まる。`@Stable` または `@Immutable` を活用する案。

#### [Suggestion-2] `ButtonCellViewHolder` を `LabelCellViews` + `applyLabelCellContents` 経路で実装し直す（Critical-2 の最小コスト解）

Compose 化が scope outside であれば、Critical-2 だけは既存 View ベースで実装可能。`LabelCellViews` を再利用し、`hasAux` 時のみ title 列の `TextView.gravity` で `titleAlignment` を反映する形にする。

---

## アクションプラン

優先度順:

1. **(必須) Critical-1 / Critical-2 の解決**: Android Phase 8 を本 change で完遂するか、spec を改訂して後続 change に分離するかを決定する。ユーザー判断が必要だが、デフォルトは spec 厳格遵守の (A) を推奨。
2. **(必須) Major-2 / Major-3 の修正**: `KsCellRow.kt` の hintText 色 / フォント反映を spec 仕様（`hintTextColor`、各種 `*Font` または `*Typeface`）に揃える。Critical-1 を後続 change に分離する場合でも、`KsCellRow` 本体は本 change で正しく実装する。
3. **(必須) Major-1 の対応**: 10.2 / 10.6 のテストを実装する。少なくとも 10.6 は既存実装でも書けるため即実施可能。
4. **(必須) Major-4 の対応**: 12.4 / 12.5 を実機で実行し、結果を本 change のコメントに残す。
5. **(推奨) Minor-1 / Minor-2 / Minor-3 の対応**: 上記対応中に合わせて修正。
6. **(任意) Suggestion-1 / Suggestion-2**: 余力があれば対応。

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

**理由**: Android 側で本 change の中核 spec MUST が満たされていない（共通 Composable が誰からも呼ばれないデッドコード、ButtonCell aux 描画未実装）。修正後の再レビューが必須。

代替として、Android Compose 化を本 change から外す方針（design / spec 改訂）を取る場合は、`NEEDS_DISCUSSION` 相当のユーザー判断が必要だが、その場合でも Major-2 / Major-3 / Major-4 / Critical-2 の解決は本 change 内で行う必要がある（spec を改訂しても ButtonCell の Android 描画は MUST 違反のまま）。
