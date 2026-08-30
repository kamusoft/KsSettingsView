## Context

前 change `refine-basic-cells-style`（既にアーカイブ済）で基本 Cell 7 種の実装と仕様整備が完了したが、実機レビュー（オーナー確認）により以下のギャップが判明した：

- **サンプル**: BasicCellsDemo が業務的な分類（Account / Storage / Preferences / Type / Items / Action / Help）になっており、各 Cell タイプの動作確認用途には合っていない。iOS / Android のテキストも揃っていない（実は一致しているが、構成自体が不透明）。
- **Android UI**: アイコン未表示、SwitchCell の Thumb / Track が同色（区別がつかない）、CheckboxCell の右端が他のセルと揃わない。
- **iOS UI**: Section Footer が画面下に sticky 固定されてしまうデグレ、`Theme.viewBackgroundColor` がセクション間の背景に反映されない、Section Header / Footer の不要な余白、罫線インセット規則（セクション境界=端から端、Cell 間=Title リーディング位置）が未実装。
- **アイコン解決**: Android で `KsImage.systemName` の解決手段がなく、現状アイコンが描画できない。前 change で「将来 change で対応」と保留したが、本 change で確定する。

Android 開発者の慣習を調査した結果、アイコン指定 API は `@DrawableRes Int` がデファクトスタンダード（Material Components / AndroidX Preference 等）であり、文字列 → リソース ID 解決（`Resources.getIdentifier`）はアンチパターン（低速・ProGuard 不整合・型検査なし）と判明した。

オリジナルの AiForms.Maui.SettingsView は MAUI の `ImageSource` を使い、内部で `FileImageSource` / `StreamImageSource` / `UriImageSource` / `FontImageSource` を sealed-like に分岐していた。Flutter `ImageProvider` も同様。

## Goals / Non-Goals

**Goals:**

- BasicCellsDemo を Cell タイプ別セクション構成に再編し、iOS / Android で一字一句揃える。
- `KsImage` 型を sealed interface / enum 化し、各プラットフォーム固有のアイコン表現を派生として持つ構造に変更する。Android では `@DrawableRes Int` を主軸とする。
- Android UI の Switch Thumb / Track 色分離、CheckboxCell 右端整列、アイコン解決を実装する。
- iOS UI の Sticky Footer デグレ修正、viewBackgroundColor のセクション間反映、Section Header / Footer の余白制御、罫線インセット規則を実装する。
- AiForms.Maui.SettingsView の `Section.HeaderHeight` 仕様に準拠し、Core に `Section.headerHeight: Double`（既定 `-1`）を追加する。

**Non-Goals:**

- 新規 Cell タイプの追加は本 change に含めない。
- アイコンキャッシュ・非同期ロード（Glide / Coil 風の URL ローディング）は対象外。
- カスタム Header / Footer View（任意 View 差し込み）は本 change に含めない（テキストヘッダ / フッタのみ対象）。
- Compose 側のサンプル整備は対象外（既存のまま継続）。

## Decisions

### Decision 1: `KsImage` を sealed 化（破壊変更、定義責務は `cell-types-basic` capability に集約）

**選択**: `KsImage` を sealed interface（Kotlin）/ enum with associated value（Swift）に変更し、プラットフォーム固有派生を持たせる。定義責務（型構造・派生・等価性契約）はすべて `cell-types-basic` capability の Requirement「KsImage 値型」に集約する（`settings-view-core` 側では `KsImage` の詳細を定義しない）。

```kotlin
// Android
sealed interface KsImage {
    data class Resource(@DrawableRes val resId: Int) : KsImage
    data class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage
    data class SystemName(val name: String) : KsImage  // iOS との対称性のため残置、Android では解決不可
}
```

```swift
// iOS
public enum KsImage: Hashable {
    case systemName(String)        // SF Symbols 名
    case uiImage(UIImage)          // 任意 UIImage
}
```

**理由**:

- Android の主流 API（`@DrawableRes Int`）を一級市民として提供できる（Material Components / AndroidX Preference と同じ感覚）。
- `Drawable` 直渡しのオーバーロードを派生として併設でき、`VectorDrawableCompat` や動的生成にも対応可能。
- `Resources.getIdentifier` アンチパターンを回避でき、ProGuard / R8 のリソース縮小と整合する。
- MAUI `ImageSource` / Flutter `ImageProvider` と同じ抽象構造で、将来 `Uri` / `Font` 派生の追加が非破壊的に可能。
- iOS の `systemName` も sealed 派生として並ぶため、「`KsImage` は各プラットフォーム固有のアイコン表現を運ぶ共通コンテナ」というセマンティクスで対称性を確保。

**代替案**:

- **案 A: `KsImage(resourceId: Int)` 単独**: Android 慣用 ◎ だが、`Drawable` 直渡しや SF Symbols との対称性が崩れる。
- **案 B: `KsImage(name: String)` で `Resources.getIdentifier` 解決**: 文字列で iOS / Android 対称化できるが、Android ではアンチパターン採用となり ProGuard 不整合・実行時遅延・型検査喪失が発生する。
- **案 D: `@DrawableRes Int` のみ**: 最小だが、将来の Font Icon / Drawable 動的生成への拡張余地がない。

**API 対称性の方針**: 引数型ではなく「`KsImage` という共通コンテナで各プラットフォーム固有のアイコン表現を運ぶ」というセマンティクスで対称性を確保する。MAUI / Flutter と同じアプローチ。

### Decision 2: Android UI の `KsImage` 解決ロジック

**選択**: Android UI 層に以下の優先順位で解決ロジックを追加する：

1. `KsImage.Drawable` → そのまま `setImageDrawable(drawable)`
2. `KsImage.Resource(resId)` → `ContextCompat.getDrawable(context, resId)` で取得して `setImageDrawable`
3. `KsImage.SystemName(_)` → 解決不可。**アイコン非表示**（icon ImageView を `View.GONE` にする）でフォールバック
4. `null` → アイコン非表示

**理由**:

- `SystemName` は SF Symbols 由来であり Android 側に対応リソースが無いため、無視するのが安全。エラーログ等は出さず、視覚的にもアイコン無しとして自然に振る舞う。
- `Drawable` 派生を `Resource` より優先するのは、明示的にロード済みのインスタンス指定の方が利用者意図が強いため。

**代替案**:

- `SystemName` を SF Symbols 名から Material Symbols 名へ自動変換: マッピング表のメンテナンス負荷大、命名規則の差異も多いため不採用。
- `SystemName` 受け取り時にエラー throw: 利用者が iOS / Android 共通の DSL を書く際にハンドリング煩雑になるため不採用。

### Decision 3: Android `SwitchCell` の Thumb / Track 色分離

**選択**: `MaterialSwitch` に対して以下のテーマ設定を適用する：

- `trackTintList`: 実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）の `ColorStateList`
- `thumbTintList`: checked 状態に応じて以下：
  - `state_checked = true` → `Material colorOnPrimary` 相当（白系。Material3 既定では `Color.WHITE` 相当）
  - `state_checked = false` → `Material colorOutline` 相当（中間グレー）

**理由**:

- Material3 の `Switch` 既定スタイルでも Thumb と Track は別色（Track が accent、Thumb が onPrimary）。本 change はその既定動作を、本ライブラリの実効 accent 色に合わせて再現する。
- 同色塗りは Material3 仕様から外れており、Material Components のデザインガイドにも反する。

**代替案**:

- `MaterialSwitch` の既定（XML スタイル）に任せる: 既定スタイルは `colorPrimary` を使うため、`CellStyle.accentColor` が反映されない。本ライブラリでは accent 色のカスタマイズが要件なので不採用。
- `setUseMaterialThemeColors(true)` を呼ぶ: テーマ依存が増え、`CellStyle.accentColor` 個別指定が反映されないため不採用。

### Decision 4: iOS Section Footer の `pinToVisibleBounds` 強制 OFF

**選択**: `KsSettingsViewController` の `boundarySupplementaryItems` map で、Header / Footer 両方とも `pinToVisibleBounds = false` を強制する。

**理由**:

- 前 change で Header のみ強制 OFF にしていたが Footer は漏れていた。`UICollectionLayoutListConfiguration.plain` の既定では Footer も sticky になるケースがあり、その既定値に巻き込まれた結果デグレが発生した。
- 方針は「Sticky は採用しない」（オーナー確認済）であり、Header / Footer ともに通常スクロールに従う仕様で揃える。

**代替案**:

- `appearance = .insetGrouped` などへの変更: 影響範囲が大きく、罫線・Cell 形状など多くの仕様変更が連鎖するため不採用。

### Decision 5: iOS `viewBackgroundColor` のセクション間反映

**選択**: `UICollectionLayoutListConfiguration.backgroundColor = .clear` を設定し、`UICollectionView.backgroundColor` だけが実効背景になるようにする。

**理由**:

- `UICollectionLayoutListConfiguration` の既定 `backgroundColor` は `.systemBackground` 相当の不透明色で、セクション間にもこの色が描画される。これが `UICollectionView.backgroundColor` を覆い隠す。
- `.clear` にすることで Cell 自身は `cellBackgroundColor`、セクション間（supplementary や inset 領域）は `viewBackgroundColor` という二層構造になり、Android 側の挙動と揃う。
- Cell 自身の背景は `UIListContentConfiguration` の `backgroundConfiguration` 経由で `cellBackgroundColor` を維持する。

**代替案**:

- セクション間に明示的な `decoration item` を挟む: 実装複雑度が上がる。`backgroundColor = .clear` で同等の見た目が得られるため不採用。

### Decision 6: `Section.headerHeight` を Core に追加（AiForms 準拠）

**選択**: `settings-view-core` の `Section` 型に `headerHeight: Double`（既定 `-1`）を追加する。意味は AiForms.Maui.SettingsView の `Section.HeaderHeight` と同等：

- `-1` → 自動高さ（Header テキスト有: テキスト寸法に合わせ自動、Header テキスト空かつ高さ未指定: supplementary 自体を生成しない）
- 正値 → 固定高さ（その値で固定）

Footer 側は AiForms に揃え、`Section.footerHeight` は追加しない（Footer テキスト空なら supplementary 非生成のみ）。

**理由**:

- AiForms.Maui.SettingsView の元仕様に揃えることで、MAUI 版利用者の移植性を確保する。
- Footer 用プロパティは AiForms にも無く、Section レベルでの個別制御は不要（SettingsView レベルの fontSize と、テキストの有無で十分）。

**代替案**:

- Footer 側にも `footerHeight` を追加: AiForms に存在せず、本 change のスコープを広げるため不採用。

### Decision 7: iOS 罫線インセット規則

**選択**: `UIListSeparatorConfiguration` をカスタマイズし、Cell ごとに以下を設定する：

- **セクション最初の Cell の上罫線（top separator）** → `topSeparatorVisibility = .visible`、`topSeparatorInsets.leading = 0`（端から端へ）
- **セクション最後の Cell の下罫線（bottom separator）** → `bottomSeparatorVisibility = .visible`、`bottomSeparatorInsets.leading = 0`（端から端へ）
- **セクション内 Cell 間の罫線** → `bottomSeparatorVisibility = .visible`、`bottomSeparatorInsets.leading = titleLeadingPosition`（Title のリーディング位置以降）

`titleLeadingPosition` は：

- アイコン無し時 → 標準左マージン（16pt）
- アイコン有り時 → アイコン枠右端 + アイコンとテキスト間の標準マージン（≒ アイコン枠右端 + 12pt）

**理由**:

- AiForms.Maui.SettingsView および iOS 標準 Settings.app の見た目に近づける。
- セクション境界は「端から端」、Cell 間は「Title 揃え」の二段階規則は Image #3（オーナー提示の参考画像）の挙動に一致。

**代替案**:

- 一律 `inset = 16pt`: セクション境界の罫線も短くなり、参考画像と異なる見た目になるため不採用。

### Decision 8: サンプル構成の再編（Cell タイプ別）

**選択**: BasicCellsDemo のセクション構成を以下に再編する（iOS / Android 共通、一字一句揃え）：

```
セクション 1: CommandCell
  - Cell 1: フル構成（icon + title + description + valueText + Disclosure）
  - Cell 2: シンプル（title のみ + Disclosure）
  - Cell 3: 中間（title + valueText）

セクション 2: LabelCell
  - Cell 1: フル構成（icon + title + description + valueText）
  - Cell 2: シンプル（title + valueText のみ）

セクション 3: SwitchCell
  - Cell 1: title + isOn

セクション 4: CheckboxCell
  - Cell 1: title + isChecked

セクション 5: RadioCell（最低 2 必須）
  - Cell 1: TypeA
  - Cell 2: TypeB
  - footer: 説明文

セクション 6: SimpleCheckCell
  - Cell 1〜3: Item 1 / Item 2 / Item 3

セクション 7: ButtonCell
  - Cell 1: ログアウト
```

**理由**:

- 各 Cell タイプの表示パターンを 1 ヶ所で確認できる。
- セクション名 = Cell タイプ名でデモアプリの目的が明示される。
- `CommandCell` は `LabelCell` のレイアウトに Disclosure を足したもので、最も表現の幅があるため複数バリエーション配置する（オーナー方針）。

**代替案**:

- 既存の業務分類維持: 「実用アプリっぽくしなくて良い」というオーナー方針に反するため不採用。

### Decision 9: Android Sample アイコンリソース戦略

**選択**: `samples/android/app/src/main/res/drawable/` に Material Symbols 由来の VectorDrawable を数個追加（少なくとも CommandCell / LabelCell で利用するアイコン分）。`BasicCellsDemoScreen.kt` から `KsImage.Resource(R.drawable.xxx)` で参照する。

iOS Sample は `KsImage.systemName("...")` のまま SF Symbols を利用する。

**理由**:

- VectorDrawable は単一 XML で全密度対応でき、Material Symbols は Google 公式が無料配布しているため利用権の問題なし。
- `androidx.core` の標準ベクター系は数が限定的で、本サンプルで使いたいアイコン（person / storage / notifications 等）を網羅できない。

**代替案**:

- アイコン無しで構成: Cell タイプの表示パターン確認が不十分になるため不採用。
- Material Symbols Compose ライブラリ依存追加: View ベースの ks-settingsview-ui に Compose 依存を持ち込むのは設計上不適切なため不採用。

### Decision 10: Android CheckboxCell の右端整列実装

**選択**: `MaterialCheckBox` を accessoryHolder に追加する際、明示的に `LayoutParams(24dp, 24dp)` を設定し、`setPadding(0, 0, 0, 0)` / `minimumWidth = 0` / `minimumHeight = 0` は維持する。さらに必要に応じて `marginEnd` を微調整する（実機 ±1px 以内に追い込むため）。

**理由**:

- `MaterialCheckBox` の本体描画領域は内部的に 18dp 角だが、外接矩形は実装依存。明示サイズなしだと wrap_content の自然サイズが効き、他 Cell の `KsSimpleCheckView(30dp 角)` や `MaterialSwitch(width 32dp 程度)` の右端と揃わない。
- 24dp は Material Symbols の標準サイズと揃い、視覚的にも自然な大きさになる。

**代替案**:

- 全アクセサリを同一サイズ（例: 30dp）に統一: `MaterialCheckBox` の描画が小さくなりすぎるため不採用。

## Risks / Trade-offs

### [Risk 1] `KsImage` API の破壊変更により既存利用箇所すべてに修正が必要

**Mitigation**:
- 本リポジトリ内（Sample + テスト）のすべての `KsImage(systemName:)` 呼び出しを一括書き換え。リポジトリ外利用者はまだ存在しないため影響は本 change のスコープ内で吸収可能。
- iOS / Android の両ビルド（`swift test` / `gradle build`）で網羅性を検証する。

### [Risk 2] iOS の `UICollectionLayoutListConfiguration.backgroundColor = .clear` が他の見た目を壊す可能性

**Mitigation**:
- セルの `backgroundConfiguration` は変更せず維持し、Cell 自身の背景描画が消えないことを保証する。
- 既存 SwiftUI Preview / Sample で目視確認後にコミットする。

### [Risk 3] Footer 空時の supplementary 非生成が既存テストの期待と食い違う可能性

**Mitigation**:
- 影響を受けるテストを洗い出し、テスト側を「Footer / Header 空なら supplementary なし」の期待値に更新する。
- もし「supplementary は常に存在し高さ 0」を期待しているテストがあれば、Decision 6 に合わせて修正する。

### [Risk 4] Android Material Symbols の VectorDrawable 追加でビルドサイズが増加

**Mitigation**:
- 追加するのは Sample アプリ側のみ（ライブラリ `ks-settingsview-ui` には入れない）。
- 数個（5〜10 個）にとどめ、本体配布物への影響なし。

### [Trade-off 1] `KsImage` の派生名・実引数型がプラットフォーム間で非対称

**Trade-off**:
- iOS は `systemName(String)` / `uiImage(UIImage)`、Android は `Resource(@DrawableRes Int)` / `Drawable(android.graphics.drawable.Drawable)` / `SystemName(String)` と、派生名と実引数型がプラットフォームで異なる。
- 結果として「`KsImage` は型を共有するが、実引数の指定方法はプラットフォームで異なる」というセマンティクスになる。
- MAUI / Flutter と同様のアプローチで、利用者が DSL を書く際に platform-specific な分岐を明示することを許容する。

### [Trade-off 2] iOS の罫線インセット規則を Cell 個別の境界判定で実装する

**Trade-off**:
- 各 Cell が「自セクション内で最初か最後か」を知る必要があり、DataSource 側からの情報注入またはセクションマップを保持する必要がある。
- 実装は `UICollectionViewDiffableDataSource.snapshot()` から `indexPath` を逆引きするロジックで対応する。

## Phase 15 追加 Decision（オーナー二次実機目視 Image #8〜#11 対応）

### Decision 15-1: Section Header は下揃え、Footer は上揃え（AiForms オリジナル `TextHeaderView` 準拠）

**選択**: Section Header テキストは boundary 領域の **下端揃え（bottom alignment）**、Section Footer テキストは **上端揃え（top alignment）** で描画する。

**理由**:

- AiForms.Maui.SettingsView オリジナル `Platforms/iOS/TextHeaderView.cs` の `SetVerticalAlignment(LayoutAlignment.End)` が既定値であり、`SettingsView.HeaderTextVerticalAlign` の `BindableProperty` 既定が `LayoutAlignment.End` に固定されている（`SettingsView.DefineProperites.cs:198`）。これは「Header テキストは次の Cell の上端にぴったり接する」という視覚効果を意図したもの。
- `TextFooterView.cs` 側は明示的な alignment API を持たず、`Label.TopAnchor` への制約のみが活きるため、Footer テキストは結果として **上端揃え**で描画される（前 Cell の下端にぴったり接する）。
- これにより、Header テキストと Footer テキストが「自セクションの Cell 側に張り付く」見た目になり、視覚的にどの Section に属するかが直感的に分かる。

**実装**:

- iOS: `applyAccessoryToListCell` で Header / Footer のとき `UIListContentConfiguration` ではなく **UIView + UILabel + AutoLayout 制約** を用いる。Header は `label.bottomAnchor == contentView.bottomAnchor`、Footer は `label.topAnchor == contentView.topAnchor` で制約を設定し、左右は標準左マージン 16pt にインセット。
- Android: `SectionTextAccessoryViewHolder.bind()` で Header の `TextView.gravity = Gravity.BOTTOM or Gravity.START`、Footer の `TextView.gravity = Gravity.TOP or Gravity.START` を設定する。

**代替案**:

- 中央揃え（既定）のまま: AiForms オリジナルと挙動が異なり、Header / Footer が宙ぶらりんに見える。不採用。

### Decision 15-2: Android の `Section.headerHeight` は `CellListItem.SectionHeader` 経由で伝搬する

**選択**: `CellListItem.SectionHeader` データクラスに `headerHeight: Double` フィールドを追加し、`KsSettingsView.flatten()` から `section.headerHeight` を伝搬する。`SectionTextAccessoryViewHolder.bind()` が受け取って `itemView.layoutParams.height` に反映する。

**理由**:

- 現状 `CellListItem.SectionHeader` は `sectionId` と `accessory` のみ保持し、`headerHeight` は Core 側にしかない。UI 層に伝わらないため Sample の `headerHeight = 60.0` 指定が実機で無視されていた（Image #10 で確認）。
- データクラスへのフィールド追加は `flatten()` 内の `out.add(SectionHeader(...))` 呼び出しと `bind()` 引数の追加で完結し、影響範囲は限定的。
- DiffUtil の `areItemsTheSame` は `sectionId` のみ比較するため、`headerHeight` 値の変化はアイテム同一性に影響しない（既存挙動と整合）。

**代替案**:

- `KsSettingsView` 側で section index → headerHeight のマップを保持し ViewHolder bind 時に解決: グローバル状態が増えるため不採用。

### Decision 15-3: Android 基本 Cell の垂直パディングを 4dp に縮める（AiForms `cellbaseview.axml` 準拠）

**選択**: 基本 Cell 7 種のコンテナ View の `paddingTop` / `paddingBottom` を **4dp**（density 乗算 px）に変更する。横方向は標準 16dp を維持する。

**理由**:

- AiForms.Maui.SettingsView オリジナル `Platforms/Android/Resources/layout/cellbaseview.axml` が `paddingTop="4dp"` / `paddingBottom="4dp"` を採用している。
- 従来 16dp ずつだったため、実機で iOS（Image #8）や AiForms オリジナル Android（Image #11）よりも明らかに上下に余白がある状態だった（Image #10）。
- `minimumHeight` で 44dp が保証されるため、視覚的な行高さは大きく変わらない。テキスト周辺の上下余白だけが詰まる。

**代替案**:

- 8dp / 6dp など中間値: AiForms オリジナルとの一致性が下がるため不採用。AiForms に揃える方が一貫性が高い。

### Decision 15-4: Section Header / Footer 内部 TextView の上下 padding は 0（AiForms オリジナル準拠）

**選択**: `SectionAccessoryViewHolders.kt` の `createSectionTextView` で TextView 内側の上下 padding を **0** にする。横方向 padding は標準左マージン 16dp 相当を維持する。

**理由**:

- AiForms.Maui.SettingsView オリジナル `Platforms/Android/Resources/layout/headercell.axml` / `footercell.axml` の `TextView` は **上下 padding を明示指定しておらず**、Android 既定値（0）で動作している。
- iOS 側 `Native/iOS/TextHeaderView.cs:38-39, 79-85` の `Label` の `TopAnchor` / `BottomAnchor` は `ContentView` に対して **0pt インセット**で結ばれており、`Label` 自身は `PaddingLabel` だが Padding 既定は `(0,0,0,0)`（ユーザーが `HeaderPadding` を明示指定した場合のみ反映される）。
- KsSettingsView 側で `pad / 2`（≒ 8dp 相当）の上下 padding を残すと、`Section.headerHeight = 60` を指定したケースで TextView の描画領域が `60dp - 16dp = 44dp` まで縮み、`gravity = Gravity.BOTTOM` の "下端" が `contentView 下端から 8dp 上` を指すため、AiForms オリジナルや iOS と比較して **約 8dp 浮いて見える**（review-result_005.md Suggestion-2）。
- 上下 padding を 0 にすれば、`layoutParams.height` で指定された高さ全体がテキストの配置領域となり、`Section.headerHeight` の指定値どおりの見た目に揃う。Header = bottom / Footer = top の垂直配置（Decision 15-1）は `bind()` 側の `gravity` 設定で引き続き担保される。

**実装**:

- `createSectionTextView` の `setPadding(pad, pad / 2, pad, pad / 2)` を `setPadding(pad, 0, pad, 0)` に変更する（`pad = 16dp * density`）。
- `headerHeight` 未指定（既定 = `WRAP_CONTENT`）でも、テキスト 1 行分の高さで自然に描画される（TextView の `lineHeight` で決まり、`padding=0` でも詰まりすぎる視覚的問題は生じない）。AiForms オリジナル `headercell.axml` も `wrap_content` + padding 指定なしで動作しており、本実装と同等。
- `SectionAccessoryRenderingTest` に「Header bind で `paddingTop == 0`、`paddingBottom == 0`、横方向 padding は `(16 * density).toInt()`」を検証するテスト（`Phase 15_10 Header bind で TextView の上下 padding は 0 になる` / `Phase 15_10 Footer bind でも TextView の上下 padding は 0 になる`）を追加。

**代替案**:

- 条件分岐（`layoutParams.height > 0` のときだけ上下 padding を 0 にする）: AiForms オリジナルは `WRAP_CONTENT` / 固定高さの双方で常に padding=0 のため不採用。条件分岐を入れると意図が分散し保守性も下がる。
- `pad / 4`（4dp 相当）などへ縮小: AiForms と一致しないため不採用。

## Phase 16 追加 Decision（オーナー三次実機目視 Image #12 / #13 対応）

### Decision 16-1: iOS の Header `heightDimension` 選択ロジック（`.absolute` vs `.estimated`）

**選択**: `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` を **`internal static`** 関数として切り出し、`section.headerHeight > 0` のとき `.absolute(CGFloat(section.headerHeight))` を `NSCollectionLayoutBoundarySupplementaryItem.layoutSize.heightDimension` に設定する経路を保証する。`section.headerHeight == -1` かつ `section.header` 非空のときのみ `.estimated(20)` を使う（Phase 14.2 の密度詰め値）。

**疑似コード**:

```
// 修正前（Phase 14.2 で導入。private メソッドのため単体テスト不可）
private func makeHeaderBoundaryItem(for section, original) -> Item? {
    if section.headerHeight > 0 {
        return Item(layoutSize: ..., heightDimension: .absolute(section.headerHeight))
    }
    if section.header == nil { return nil }
    return Item(layoutSize: ..., heightDimension: .estimated(20))
}

// 修正後（Phase 16: internal static で純粋ロジック化）
internal static func makeHeaderBoundaryItem(for section, original) -> Item? {
    if section.headerHeight > 0 {
        return Item(layoutSize: ..., heightDimension: .absolute(section.headerHeight))  // ← `.absolute` を必ず適用
    }
    if section.header == nil { return nil }
    return Item(layoutSize: ..., heightDimension: .estimated(20))
}
```

**理由**:

- Phase 14.2 で Header / Footer 余白詰めのため `.list(using:)` の既定 `.estimated(44)` を `.estimated(20)` に縮める実装を入れた際、ロジック上は `section.headerHeight > 0` のとき `.absolute(headerHeight)` を返す経路があったが、`private` 関数のため単体テストで直接検証する経路が無く、回帰時に気付きにくい状態だった。
- オーナーの三次実機目視（Image #12）で「`headerHeight = 60` を指定しても iOS の CommandCell セクションのヘッダ高さが他セクション（自動高さ）と変わらない」事象が報告された。一方 Android（Image #13）は同 `headerHeight` 値で明確に反映されていた。
- 仮に `.estimated(20)` が常に使われていれば、SupplementaryView の自動高さ算出が UILabel の intrinsic 高さ（≒ 20pt）に支配され、`Section.headerHeight` の指定値は完全に打ち消される。`.absolute(headerHeight)` を **確実に**適用する経路を、純粋ロジックテストで担保する必要がある。
- `internal static` 化により、`KsSettingsViewControllerTests` から `Section.headerHeight = 80` の Section を渡したときに `item.layoutSize.heightDimension.isAbsolute == true` かつ `.dimension == 80` を直接検証できる。これにより将来 Phase 14.2 / Phase 15.1 のような余白詰め系の変更が入っても、`.absolute(headerHeight)` 経路が回帰しないことを CI で保証できる。
- Phase 15.1 で導入した AutoLayout 制約（UILabel `bottomAnchor == contentView.bottomAnchor`、priority 999）は、`.absolute(headerHeight)` で確定する supplementary 領域の **中に納まる前提**で設計されており、`headerHeight = 80` 指定時はラベルが下端に張り付いた状態で 80pt の領域内に描画される。Phase 16 でこの両立性も Requirement に追記する。

**代替案**:

- `.estimated(headerHeight)` を使う: `.estimated` は UIKit が intrinsic content size を計算して **自動高さ算出**するため、UILabel の intrinsic（≒ 20pt）に支配されて指定値が打ち消される。固定高さの意味論を満たさないため不採用。
- `NSCollectionLayoutSection.contentInsets` で代替する: contentInsets は section コンテンツ全体の inset であり、Header 単独の高さは制御できない。`Section.headerHeight` の意味論（Header 領域の固定高さ）と一致しないため不採用。
- private のまま留め、UI 統合テストでカバーする: UI 統合テストは UICollectionView のレイアウトパスを完全には走らせない（テスト環境）。`.absolute` の適用は純粋ロジックレベルで検証可能で、かつ早期に回帰検出できるため `internal static` 公開を採用する。

### Decision 16-2: テキスト accessory の supplementary view を `KsAccessoryReusableView` に切り替える（Phase 16 追加対応） — **Phase 18 で revert 済み**

> **Phase 18 で revert**: 本 Decision はオーナー指示により revert された（後述 Decision 18-1 を参照）。Phase 16 はオーナーの本来の指摘である「個別 Cell の `cellHeight` 反映不具合」を「`Section.headerHeight` 反映不具合」と読み違えた結果の誤実装であり、Phase 17 で本来の指摘が正しく解決されたため、本 Decision の機構（`KsAccessoryReusableView` 導入）は不要となった。以下の記述は経緯記録として残す。


**選択**: Section / Root の Header / Footer 描画（テキスト accessory または accessory 未指定）で使用する supplementary view クラスを、`UICollectionViewListCell`（row cell 用）から **`KsAccessoryReusableView`**（`UICollectionReusableView` 直系のサブクラス）に切り替える。`accessoryView` 経路（任意 UIView / SwiftUI View の埋め込み）は `UIListContentConfiguration` / `UIHostingConfiguration` が必要なため引き続き `UICollectionViewListCell` を使用する。

**背景**:

- Phase 16 の `internal static` 化 + `.absolute(headerHeight)` ロジック単体テスト追加で純粋ロジック上の `.absolute(80)` 経路は保証された（Decision 16-1）。しかしオーナー三次実機目視で Image #12 の事象が依然解消されない可能性を残していた。
- オーナー指摘「AiForms オリジナルは工夫している」を受け、`AiForms.Maui.SettingsView/Platforms/iOS/` 配下のソースを再確認した結果、以下が判明した：
  - `TextHeaderView.cs` は **`UITableViewHeaderFooterView`**（UITableView の supplementary 専用 class）を継承している。
  - `SettingsTableSource.cs` の `GetHeightForHeader`（lines 143-167）は **CGFloat を直接返し**、UITableView がその値を rect 計算に直接反映する構造になっている（`.estimated` / `.absolute` の概念は存在しない）。
  - 制約は **priority 999** で張り、UITableView 由来の Required priority の rect 制約と衝突しないように設計されている（`TextHeaderView.cs` lines 38-46）。

**問題**: `UICollectionViewListCell` は本来 row cell 用 class であり、内部に `selfSizingInvalidation` 機構を持つ。`boundarySupplementaryItem.layoutSize.heightDimension = .absolute(80)` を指定しても、Cell が自身の content（UILabel）の intrinsic content size をもとに `preferredLayoutAttributesFitting` / `systemLayoutSizeFitting` で再計算した値で実際の frame.height が上書きされるケースがある。

**疑似コード**:

```swift
// 修正前: UICollectionViewListCell を supplementary に使用
collectionView.register(
    UICollectionViewListCell.self,
    forSupplementaryViewOfKind: kind,
    withReuseIdentifier: identifier
)
// → row cell 用の self-sizing が .absolute(80) を上書きするケースがある

// 修正後（Phase 16 追加対応）: UICollectionReusableView 直系の KsAccessoryReusableView
collectionView.register(
    KsAccessoryReusableView.self,
    forSupplementaryViewOfKind: kind,
    withReuseIdentifier: identifier
)
// → boundarySupplementaryItem.layoutSize.heightDimension が描画 frame に直接反映される
```

**AiForms オリジナルからの工夫の反映**:

| AiForms オリジナル要素                              | 本実装での対応                                                  |
| --------------------------------------------------- | --------------------------------------------------------------- |
| `TextHeaderView : UITableViewHeaderFooterView`      | `KsAccessoryReusableView : UICollectionReusableView`            |
| `TextHeaderView.cs` lines 38-46: 制約 priority 999  | `KsAccessoryReusableView.setVerticalAlignment(_:)` で priority 999 |
| `TextHeaderView.cs` line 49: `BackgroundView = ...` | 必要時 `backgroundColor` を明示設定                             |
| `SetVerticalAlignment(LayoutAlignment.End)`         | `setVerticalAlignment(.bottom)` (Header 下端揃え)               |
| `TextFooterView` の TopAnchor 制約                  | `setVerticalAlignment(.top)` (Footer 上端揃え)                  |
| `SettingsTableSource.GetHeightForHeader` の CGFloat | `boundarySupplementaryItem` の `.absolute(headerHeight)`        |

**理由**:

- AiForms オリジナルが `UITableViewHeaderFooterView` を選んでいる事実は、「supplementary view は row cell ではなく専用 class を使うべき」という UIKit 設計上の意図を反映している。`UICollectionView` でも同じ意図を尊重し、`UICollectionReusableView` 直系のサブクラスを使うのが妥当。
- 視覚的高さ検証テスト（`test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる`）で、`layoutIfNeeded()` 後の `supplementary.frame.height` が 80pt（許容 ±0.5pt）に確実に収まることを実測で確認済み。
- accessoryView 経路（任意 UIView / SwiftUI View）は依然として `UIHostingConfiguration` が必要であり、こちらは self-sizing と `.absolute(headerHeight)` のいずれかを選ぶことになる。SwiftUI / 任意 View の埋め込み機能を残すため、accessoryView 経路だけは `UICollectionViewListCell` を維持する。

**代替案**:

- 案 A（applyAccessoryLabel に height 制約を Required priority で追加）: `UICollectionViewListCell.contentView` に `heightAnchor.constraint(equalToConstant: 80)` を Required priority で張る方法。だが Required を張ると AiForms と異なり priority 999 の方針から外れ、UIKit 内部制約と衝突する Warning が出やすい。さらに `headerHeight = -1`（自動）経路と分岐が複雑化する。
- 案 B（boundarySupplementaryItem の contentInsets を再検討）: `contentInsets` は supplementary 内部のレイアウトに作用するが、`.absolute(80)` の領域は inset とは独立に確保される。Cell の self-sizing 問題自体は解決しない。
- **案 C（本決定）**: `UICollectionReusableView` 直系の新クラスに切り替える。AiForms オリジナルの設計意図にも合致し、Required priority の追加なしで `.absolute(headerHeight)` が確実に描画 frame に反映される。視覚的高さ検証テストで動作実証済み。

### Decision 16-3: Phase 16 経路切替で発生した Phase 15.1 由来のデッドコードを削除する（review-result_006.md Minor-1 対応） — **Phase 18 で revert 済み**

> **Phase 18 で revert**: 本 Decision はオーナー指示により revert された（後述 Decision 18-1 を参照）。Phase 16 機構（Decision 16-2）が revert されたことで、Phase 15.1 由来の `applyAccessoryLabel` / `applyAccessoryToListCell` テキスト分岐は再びテキスト accessory の正規経路となり、デッドコードではなくなったため、本 Decision で削除した実装は Phase 18 で復活させた。以下の記述は経緯記録として残す。


**選択**: Phase 16 で `KsSettingsViewController.makeAccessoryListCell` の分岐が
`accessoryText != nil || accessoryView == nil` → `KsAccessoryReusableView` 経路に切り替わった結果、
Phase 15.1 で導入した以下のテキスト accessory 用の実装が **到達不能なデッドコード**となった。
本決定では、これらを **完全に削除** してコード衛生を保つ（`// removed` コメントや後方互換 shim は残さない）。

**削除対象**:

| 対象                                                                  | 削除理由                                                                                                                                       |
| --------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `applyAccessoryToListCell` 内の `if let text = accessoryText { ... }` 分岐 | Phase 16 以降、テキスト accessory は `KsAccessoryReusableView` 経路に流れるため呼び出し側で `accessoryText == nil` が保証される。                  |
| `applyAccessoryToListCell` の `accessoryText: String?` パラメータ      | 上記分岐削除に伴い未使用化。                                                                                                                   |
| `applyAccessoryToListCell` の `verticalAlignment: AccessoryVerticalAlignment` パラメータ | テキスト分岐削除に伴い `accessoryView` 経路では未使用化。                                                                                       |
| `applyAccessoryToListCell` の `textColor: UIColor` パラメータ          | accessoryView 経路（SwiftUI View / UIKit View）では Cell 自身が配色するため未使用化。                                                           |
| `applyAccessoryLabel` 関数全体                                        | Phase 15.1 で導入したが、`if let text = accessoryText` 分岐からのみ呼ばれていたため、上記削除と同時に到達不能となる。同等の挙動は `KsAccessoryReusableView.setVerticalAlignment(_:)` が再現済み。 |

**残置対象**:

- `AccessoryVerticalAlignment` enum は、`makeAccessoryListCell` → `makeAccessoryReusableView` および `refreshRootSupplementary` のテキスト accessory 経路で引き続き使用されるため残置する。
  - `mapVerticalAlignment` 経由で `KsAccessoryReusableView.VerticalAlignment` に変換され、内部表現と KsAccessoryReusableView 側 API の責務分離が保たれる。
  - 共有 enum 化（`KsAccessoryReusableView.VerticalAlignment` 直接利用）も検討したが、外部公開クラスの enum に内部用ロジックを依存させると将来の API 変更時の影響範囲が広がるため、本決定ではコントローラ内部の `AccessoryVerticalAlignment` を維持しつつ `mapVerticalAlignment` で橋渡しする現方針を継続する。

**呼び出し側の修正**:

- `makeAccessoryListCell` の accessoryView 経路: `applyAccessoryToListCell(listCell, accessoryView: accessoryView)` に簡素化。
- `refreshRootSupplementary` の listCell 経路: 同上。
- テスト `test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する` の 3 回の呼び出しを新シグネチャに合わせて更新。

**理由**:

- CLAUDE.md「半端な実装や未使用コードを残さない」「使われなくなったものは削除する」方針を遵守。
- レビュー指摘（review-result_006.md Minor-1）で「読み手は『`applyAccessoryToListCell` がテキスト経路をサポートしている』と誤読しやすい」と指摘されており、可読性向上のため到達不能経路を残す合理的理由はない。
- Phase 16 の `KsAccessoryReusableView` への切替が安定動作することは Phase 16.13 の視覚的ヘッダ高さ検証テストで実証済みであり、テキスト accessory 経路を「listCell 経路にもフォールバックできる」状態に保つ必要はない。

**代替案**:

- 案 A（`assert(accessoryText == nil)` を `applyAccessoryToListCell` 冒頭に追加するだけにとどめる）: 想定外パスの混入を即時検出できるメリットはあるが、デッドコード自体は残るため可読性問題が解消しない。レビュー Minor-1 の本質的な指摘（「`applyAccessoryLabel` の存在意義が薄れ、`KsAccessoryReusableView.setVerticalAlignment(_:)` と二重実装」）にも応えられない。不採用。
- **案 B（本決定）**: デッドコードを完全削除する。Phase 16 経路の単一化により責務が明確になり、`KsAccessoryReusableView` と `applyAccessoryToListCell` の役割分担（テキスト accessory vs accessoryView 経路）が一目で理解できる。

## Phase 17 追加 Decision（オーナー三次実機目視 Image #12 / #13 正式対応）

オーナー三次実機目視（Image #12 / #13）の **本来の指摘** は「個別 Cell の `CellStyle.cellHeight = 80` が iOS で反映されていない」だった。Phase 16 では `Section.headerHeight` を誤って対象として扱い、`KsAccessoryReusableView` 切替などの修正を行ったが、これはオーナーの指摘とは別の問題への対応であった。Phase 16 で行った機構自体は正しい改善（オーナーから NG を受けていない）として維持しつつ、Phase 17 で **本来の指摘** である Cell.cellHeight 反映に正式対応する。

### Decision 17-1: iOS Cell.cellHeight 反映ロジック（`preferredLayoutAttributesFitting` override + 共通基底クラス）

**決定**: iOS の全 Cell View（7 種）を共通基底クラス `KsListCellBase: UICollectionViewListCell` を継承させ、`preferredLayoutAttributesFitting(_:)` を override して proposed attributes の `size.height` を補正することで `CellStyle.cellHeight` を実際の描画 `frame.height` に反映する。

**問題と AiForms オリジナル**:

AiForms.Maui.SettingsView オリジナル `Native/iOS/SettingsTableSource.cs` の `GetHeightForRow`（lines 113-135）は次の構造で個別 Cell 高さを実現している：

```csharp
public override NFloat GetHeightForRow(UITableView tableView, NSIndexPath indexPath)
{
    var cell = _settingsView.Model.GetCell(indexPath.Section, indexPath.Row);
    if (!cell.IsVisible) return NFloat.Epsilon;
    if (!_settingsView.HasUnevenRows) return tableView.EstimatedRowHeight;
    var h = cell.Height;
    if (h == -1) return tableView.RowHeight;  // automatic height
    return (NFloat)h;                          // individual height
}
```

`UITableView` の `heightForRowAt` は CGFloat を直接返せるため `cell.Height` の値がそのまま rect 計算に反映される。一方、本実装は `UICollectionView` + `UICollectionLayoutListConfiguration` を使用しており、`heightForRowAt` 相当の経路がない。代わりに各セルが `UIListContentConfiguration` の intrinsic 高さで self-sizing される。

Phase 17 着手前は `KsCellViewSupport.applyEffectiveHeight(_:effective:)` が `contentView.heightAnchor.constraint(greaterThanOrEqualToConstant: effectiveCellHeight)` を priority 999 で設定していたが、`UIListContentConfiguration` の intrinsic 高さ（priority 1000）が優先されるため、`cellHeight = 80` 指定時でも実際の `frame.height` は intrinsic（≒67pt）となり指定値が反映されていなかった（Phase 17.6 の `test_視覚的セル高さ_cellHeight80指定時...` で実測確認）。

**解決策**:

`UICollectionViewLayoutAttributes` の補正経路である `preferredLayoutAttributesFitting(_:)` で、自セルが保持する `lastHeight` を参照して proposed `size.height` を補正する。これにより `UICollectionViewCompositionalLayout` の self-sizing 経路と矛盾せず、`UITableView.heightForRowAt` 相当の振る舞いを実現する。

**実装方針**:

- `KsListCellBase: UICollectionViewListCell` を新規作成し、以下を担う：
  1. `init(frame:)` で `KsCellViewSupport.installSelectedColorHandler(self)` を呼ぶ（従来各 Cell View に重複していた処理）
  2. `preferredLayoutAttributesFitting(_:)` を override し、`KsCellViewSupport.adjustedLayoutAttributes(self, proposed:)` 経由で `cellHeight` を反映する
- 7 種の Cell View（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）の継承元を `UICollectionViewListCell` から `KsListCellBase` に変更する。各 Cell View 固有の `init` 処理（`ButtonCellView` の `titleLabel` 配置、`SwitchCellView` の `UISwitch.addTarget` 等）は保持する。

`KsCellViewSupport.adjustedLayoutAttributes(_:proposed:)` の補正規則：

- `lastHeight` / `lastIsFixedHeight` 未記録（`applyEffectiveHeight` 未呼び出し）→ `proposed` をそのまま返す。
- `lastIsFixedHeight == true`（`Theme.hasUnevenRows == false`、固定高さモード）→ `proposed.size.height` を無視して **厳密に** `lastHeight` に揃える。
- `lastIsFixedHeight == false`（`hasUnevenRows == true`、可変高さモード）→ `lastHeight` を **下限** として `max(proposed.size.height, lastHeight)` を採用する。

**理由**:

1. **AiForms オリジナルと意味論的に等価**: `UITableView.heightForRowAt` の「個別 Cell ごとに CGFloat を直接返す」設計を、`UICollectionView` + Compositional Layout の世界で実現する公式 API が `preferredLayoutAttributesFitting(_:)` である。
2. **既存 `contentView.heightAnchor` 制約だけでは不十分**: priority 999 で `UIListContentConfiguration` の intrinsic（priority 1000）に負けることが実測で確認された。`preferredLayoutAttributesFitting(_:)` は proposed attributes を直接書き換えるため、priority 競合の影響を受けない。
3. **共通基底クラスで保守性確保**: 7 種の Cell View に `preferredLayoutAttributesFitting` 個別 override を書くと変更点が分散するため、共通基底に集約する。同時に従来の `installSelectedColorHandler` 呼び出しも基底クラスに集約され、各具象 View の `init` が簡潔になる。

**Phase 16 との関係**:

- Phase 16 で導入した `KsAccessoryReusableView`（`Section.headerHeight` 反映）は本決定とは別経路（supplementary view 用）であり、Phase 17 でも維持する。
- Phase 16 の `Section.headerHeight = 60 → 80` 増量は Phase 17 でも維持する（`Section.headerHeight` 反映機構自体は正しく機能している）。
- 本決定で対象とするのは「個別 Cell の `frame.height`」のみであり、Section Header / Footer の `frame.height` は引き続き `KsAccessoryReusableView` 経路で担保される。

**実機検証**:

- Phase 17.6 の `test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる` / `test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる` で `UICollectionView.layoutIfNeeded()` 後の `cellForItem(at:)?.frame.height` が指定値以上であることを検証する。
- 実機目視では iOS Sample（`BasicCellsDemoView.swift`）の CommandCell セクション 1 番目「Tanaka Taro」セル（`CellStyle(cellHeight: 80)`）が他セル（自動高さ）より明確に高く描画され、Android Sample（Image #13 で観測された見た目）と一致することを確認する。

**代替案**:

- **案 A（`contentView.heightAnchor` の priority を `.required`（1000）に上げる）**: `UIListContentConfiguration` の intrinsic と priority が同列になり競合する。Auto Layout が unsatisfiable とログ警告を出すリスクが高く、不採用。
- **案 B（`KsListCellBase` を導入せず各 Cell View に `preferredLayoutAttributesFitting` を個別実装）**: 7 箇所に同じ 4-5 行の override を書くことになり、変更点の分散と将来の保守コストが大きい。基底クラス導入のオーバーヘッドは init 1 メソッドのみで、コード量はトータルで減る。不採用。
- **案 C（独自 `UICollectionViewLayout` を実装し、`UITableView.heightForRowAt` 相当の API を提供する）**: 実装コストが過大であり、`UICollectionLayoutListConfiguration` が提供する separator / accessory 等の機能を自前で再実装する必要が出る。本 change のスコープ外。不採用。
- **案 D（本決定）**: `preferredLayoutAttributesFitting(_:)` override + 共通基底クラス。実装コストが最小で、AiForms オリジナルと意味論的に等価、テストで実測検証可能。採用。

**Phase 17 と Phase 16 機構の関係（Phase 18 で再評価）**:

- Phase 17 で本来の指摘（個別 Cell の `cellHeight` 反映）が `KsListCellBase` + `preferredLayoutAttributesFitting` で解決されたことを受け、Phase 18 で Phase 16 の `KsAccessoryReusableView` / Sample 値 80 増量は revert された。詳細は次節「Phase 18 追加 Decision」を参照。
- 「Phase 17 でも Phase 16 機構は維持する」と Phase 17 タスクで記載されていた方針は、Phase 18 のオーナー指示（「間違ってしなくて良い修正を入れたなら戻して欲しい」）により撤回されている。

## Phase 18 追加 Decision（オーナー指示による Phase 16 機構の revert）

### Decision 18-1: Phase 16 機構を revert し、副次改善のみ維持する（B 案）

**選択**: Phase 16 で導入した以下の機構を **revert**（削除して Phase 15.1 / Phase 15.2 の状態に戻す）する：

| revert 対象                                                          | revert 後の状態                                                                  |
| -------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift`         | 完全削除（`trash` 経由）                                                          |
| `KsSettingsViewController.makeAccessoryListCell` の分岐ロジック     | テキスト / SwiftUI / UIKit すべて `UICollectionViewListCell` 経路に統一           |
| `KsSettingsViewController.makeAccessoryReusableView` / `mapVerticalAlignment` | 削除                                                                              |
| `KsSettingsViewController.refreshRootSupplementary` の `KsAccessoryReusableView` 分岐 | 削除（`UICollectionViewListCell` 経路のみ）                                       |
| `KsSettingsViewController.applyAccessoryToListCell` のシグネチャ    | Phase 15.1 の `applyAccessoryToListCell(_:accessoryText:accessoryView:textColor:verticalAlignment:)` に戻す |
| `KsSettingsViewController.applyAccessoryLabel`                      | Phase 15.1 の実装を復活させる（UILabel + AutoLayout priority 999、Header = 下端 / Footer = 上端） |
| `SectionAccessoryRenderingTests.swift` の `is KsAccessoryReusableView` 検証 | `is UICollectionViewListCell` 検証に戻し、UILabel は `listCell.contentView.subviews` から取得 |
| `KsSettingsViewControllerTests.swift` の視覚的ヘッダ高さ検証 2 件 / `measuredSectionHeaderHeight` ヘルパ | 削除                                                                              |
| Sample `headerHeight = 80` / `headerHeight = 80.0`（iOS / Android） | Phase 15.2 / 15.5 の `60` / `60.0` に戻す                                          |
| `specs/settings-view-ios-ui/spec.md` の「テキスト accessory 用 supplementary view クラスの選択」Requirement | 削除                                                                              |
| `specs/settings-view-ios-ui/spec.md` の「headerHeight 正値が描画 frame に反映される」Scenario | 削除                                                                              |
| `specs/samples-ios/spec.md` / `specs/samples-android/spec.md` の `headerHeight = 80` 記述 | `60` に戻す                                                                       |

**維持する Phase 16 副次改善（B 案）**:

| 維持対象                                                              | 維持理由                                                                              |
| --------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` の `internal static` 化 | 純粋関数として正しく、`.absolute` / `.estimated` 切替を単体テストから検証できる。     |
| `test_makeHeaderBoundaryItem_*` 純粋ロジックテスト 4 件               | Phase 14.2 / Phase 15.1 のような余白詰め系変更が入っても、`.absolute(headerHeight)` 経路が回帰しないことを CI で保証する。 |
| Decision 16-1（`.absolute` vs `.estimated` 選択ロジック）            | `makeHeaderBoundaryItem` の純粋ロジックとして正しく、上記副次改善で生きている。       |

**revert する Phase 16 Decision**:

- Decision 16-2（`KsAccessoryReusableView` 採用）→ revert（Decision 本体に「Phase 18 で revert 済み」と注記）。
- Decision 16-3（Phase 15.1 由来のデッドコード削除）→ revert（Phase 16 機構が revert された結果、Phase 15.1 由来コードはデッドコードではなくなったため、削除した実装を復活させる）。

**理由**:

1. **オーナーから明確な指示**: 「Phase 16 で間違ってしなくて良い修正を入れたなら戻して欲しい」との直接指示があり、B 案（機構は戻すが、副次的な改善は残す）で進めることが合意された。
2. **Phase 16 はオーナーの本来の指摘の誤読**: オーナー三次実機目視（Image #12 / #13）の本来の指摘は「個別 Cell の `cellHeight` 反映不具合」だった。Phase 16 ではこれを `Section.headerHeight` 反映不具合と読み違え、`KsAccessoryReusableView` 導入で対応した。本来の指摘は Phase 17 で `KsListCellBase` + `preferredLayoutAttributesFitting` により正しく解決されている（`test_視覚的セル高さ_*` で実測保証済み）。
3. **Phase 16 機構は本来不要**: Phase 17 が成立した時点で、Phase 16 で導入した `KsAccessoryReusableView` は「動作する正しい改善」だが「本来不要だった追加機構」となった。コード衛生・保守性の観点から、不要な追加機構は残さないのが本プロジェクトの方針（CLAUDE.md「半端な実装や未使用コードを残さない」）。
4. **副次改善は独立した価値を持つ**: `makeHeaderBoundaryItem` の `internal static` 化 + 純粋ロジックテスト 4 件は Phase 16 機構とは独立した「CI 保証の改善」であり、`.absolute` / `.estimated` 切替の回帰防止に有用。これだけは残す（B 案）。

**代替案**:

- **案 A（完全 revert）**: `makeHeaderBoundaryItem` の `internal static` 化と純粋ロジックテストも含めて Phase 16 を完全に revert する。Phase 16 機構が誤実装だった事実を強調できるが、副次改善まで失うと将来の `.absolute` / `.estimated` 切替の回帰防止が弱まる。不採用。
- **案 B（本決定、オーナー合意）**: 機構（`KsAccessoryReusableView` / 視覚的高さ検証テスト / Sample 値 80）は revert し、副次改善（`makeHeaderBoundaryItem` の `internal static` 化 + 純粋ロジックテスト）は残す。Phase 16 で得られた CI 保証の改善を活かしつつ、本来不要だった追加機構を排除する。
- **案 C（Phase 16 をそのまま維持）**: Phase 16 機構を残し、Phase 17 と並存させる。「動作する正しい改善」として理屈は通るが、オーナーから NG（「間違ってしなくて良い修正」）の判断が下っており、コード衛生上も不要な機構を残す合理性がない。不採用。

**Phase 17 機構との関係**:

- Phase 17 で導入した `KsListCellBase` + `preferredLayoutAttributesFitting` は Phase 18 でも維持する（オーナーの本来の指摘を解決した正しい修正）。
- Phase 18 の revert は Phase 17 とは独立しており、`test_視覚的セル高さ_cellHeight80指定時...` / `test_視覚的セル高さ_cellHeight120指定時...` は Phase 18 後も引き続き PASS する。

**実機検証**:

- iOS / Android Sample の CommandCell セクションは `headerHeight = 60` で描画され、他セクション（自動高さ）と視覚的に区別できる。
- iOS の Tanaka Taro CommandCell は Phase 17 機構により `cellHeight = 80` が反映され、他セル（自動高さ）より明確に高く描画される（Phase 18 で機構維持）。

