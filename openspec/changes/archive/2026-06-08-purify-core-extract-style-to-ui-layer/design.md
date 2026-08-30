## Context

### 現状

`KsSettingsViewCore` (iOS Swift Package + Android Library モジュール) は、`SettingsRoot` / `Section` / `Cell` 抽象 / `SettingsRootDiff` といった **構造的ドメインモデル** と、`Theme` / `CellStyle` / `KsColor` / `KsFont` / `KsImage` といった **スタイル系の値型** の両方を保持している。建前としては「プラットフォーム非依存の論理表現のみを保持する」契約があり、`KsColor` は 0.0–1.0 の Double 4 成分で RGBA を表現する独自値型として定義されている。

この契約に従うため、利用者は次のような冗長な記述を強いられる：

```swift
// iOS Sample
private static let mauiViewBackground = KsColor(red: 0xF2/255.0, green: 0xEF/255.0, blue: 0xE6/255.0, alpha: 1.0)
```

```kotlin
// Android Sample
private val MAUI_VIEW_BACKGROUND = KsColor(0xF2 / 255.0, 0xEF / 255.0, 0xE6 / 255.0, 1.0)
```

### 矛盾と現実

しかし「Core はプラットフォーム非依存」という契約は **既に破綻している**：

- iOS Core の `KsImage` は `case uiImage(UIImage)` ケースを公開しており、UIKit 型 `UIImage` を直接保持する。
- Android Core の `KsImage` は `class Drawable(val drawable: android.graphics.drawable.Drawable)` を公開しており、Android Framework 型 `Drawable` を直接保持する。
- Android Core はそもそも `com.android.library` プラグインを採用し、`AndroidManifest.xml` を持ち、`androidx.compose.runtime` と `androidx.annotation` に依存している。
- iOS Core と Android Core は同名概念（`SettingsRoot` / `Theme` 等）を持つが、コード共有メカニズム（KMP の `commonMain` など）は存在せず、**物理的にも論理的にも別モジュール**である。spec ファイル (`settings-view-core/spec.md`) が 1 本で両プラットフォームの Requirement を表現しているのみ。

### 観察された問題

- `KsColor` / `KsFont` の論理表現は、結局 UI 層で `UIColor` / `UIFont` / `@ColorInt` / `Typeface` / Compose `Color` に変換される。Core で論理表現を保持する利益は「Core テストが JVM のみで実行できる」と「将来 KMP 共通化の余地」しかない。
- 既に `KsImage` でプラットフォーム型を直接持つことが許容されているのに、`KsColor` / `KsFont` だけ独自抽象を維持している整合性のなさ。
- KMP の通例では UI 層は各プラットフォーム Native (Compose Multiplatform 採用時を除く) なので、Theme / Color / Font を `commonMain` に置く需要は実質ない。
- 利用者目線では「自分の知っている `UIColor.white` / `Color.White` をそのまま渡せない」のは API 体験を著しく損なう。

### 制約

- 後続 active 提案 (`add-maui-bridge` / `add-maui-core` / `add-maui-cells` / `add-samples-maui` / `add-cell-types-input` / `add-cell-types-custom`) はまだ実装されていない。本変更を **実装段階に入る前** に行うことで、後続提案を整合の取れた設計で進められる。
- 既存 archived 提案 (`refactor-display-state-sync` / `refine-basic-cells-sample-layout` / `fix-ios-basic-cells` / `refactor-accessory-and-root-hf` / `add-partial-update-*`) の archive は維持し、本変更で参照する場合は履歴として明示する。
- ビルド・テストは Phase ごとに通る状態に保つことが理想だが、Theme / CellStyle / SettingsRoot / Cell 抽象の同時改修は Phase 内で一時的にビルドが通らない区間が発生する。Phase 内の細粒度コミット規律は確保しつつ、各 Phase 完了時には全テストが通る状態を保証する。

## Goals / Non-Goals

**Goals:**

- Core を「純粋な構造的ドメインモデルのみ」(SettingsRoot / Section / Cell 抽象 / Accessory / KsAnyView / SettingsRootDiff / CellTitleAlignment / DSLCellIdentity) に純化する。
- Theme / CellStyle / KsImage を UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に再配置し、フィールド型として Native 型 (`UIColor` / `UIFont` / `UIImage` / Compose `Color` / `TextStyle` / `KsImage`) を直接保持する。
- 利用者が Native の慣れた色・フォント API (`UIColor(red:green:blue:alpha:)` / `Color.White` / `Color(0xFF...)`) でそのまま Theme / Cell を構築できるようにする。
- 後続未実装 MAUI 提案 (`add-maui-bridge` ほか) の Bridge DTO 設計と Native API を整合させ、MAUI 利用者からも独自抽象を見えない設計を維持する。
- iOS / Android Core モジュールの責務を明確化し、spec から「プラットフォーム非依存契約」の二枚舌を解消する。

**Non-Goals:**

- KMP (Kotlin Multiplatform) の commonMain 共有化は対象外。将来 commonMain 共有が必要になった場合も、UI 層は Native のままで構造ドメインのみ共有する想定とする (Compose Multiplatform 不採用前提)。
- iOS / Android の Theme フィールドの完全な API シグネチャ統一は目指さない (それぞれの Native 型に最適化する)。
- 既存 archive 済み提案の遡及修正は行わない。本変更で `settings-view-core` 等を MODIFIED する形で表現する。
- `KsAnyView` の再設計は対象外。`KsAnyView` は装飾領域用の型消去ラッパとして Core に残置する (Hashable に参加しないため Theme / CellStyle と異なる責務)。
- 既存 sample 以外の追加デモ画面は対象外。
- Theme 構築のためのプリセット (Material 既定 / iOS 既定 / Classic 既定) を UI 層に追加することは本提案の範囲外（後続提案で別途検討）。

## Decisions

### Decision 1: `KsColor` / `KsFont` の Core 削除

**選択**: `KsColor` / `KsFont` / `KsFontWeight` を `KsSettingsViewCore` / `ks-settingsview-core` から完全に削除する。UI 層にも再配置せず、Theme / CellStyle のフィールド型として Native 型を直接採用する。

**理由**:
- 既に `KsImage` がプラットフォーム型 (`UIImage` / `Drawable`) を直接保持しており、「Core はプラットフォーム非依存」契約は破綻している。`KsColor` / `KsFont` だけ独自抽象を維持する整合性のある理由がない。
- UI 層で必ず `UIColor` / `UIFont` / Compose `Color` / `TextStyle` に変換されるため、論理表現を中間に挟む工程は純粋なオーバーヘッド。
- 利用者の API 体験を著しく改善 (`KsColor(red: 0xF2/255.0, ...)` → `UIColor(red: 0xF2/255, ...)`)。
- KMP の通例 (UI は各 plat、commonMain にロジック) と矛盾しない。

**代替案**:
- 案 A: `KsColor` / `KsFont` を Core から UI 層に降ろし、Theme フィールド型として残す → 結局利用者は `KsColor` を書くことになり改善されない。却下。
- 案 B: `KsColor` を `internal` に下げ、入口だけ Native 型を受ける convenience init を追加 → `theme.separatorColor` を読み出すと依然 `KsColor` が露出する。中途半端。却下。
- 案 C: `KsColor(hex: 0xF2EFE6)` のような hex literal init を追加するのみ → 改善幅が小さい。本質的解決にならない。却下。

### Decision 2: `Theme` / `CellStyle` の UI 層への完全移動

**選択**: `Theme` (Swift `struct` / Kotlin `data class`) と `CellStyle` を Core から UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に完全移動する。フィールド型は Native 型 (iOS: `UIColor` / `UIFont`、Android: Compose `Color` / `TextStyle`) を直接保持する。

**理由**:
- スタイル系の値は UI 層の関心事。Core ロジックは Theme / CellStyle を参照しない (SettingsRootDiff.updateTheme と SettingsRoot.theme のフィールド参照を除く → 本提案でこれらも除去する)。
- UI 層に置けば、フィールド型として `UIColor` を直接持つことが自然 (UIKit 依存は UI 層では前提)。
- iOS / Android で Theme の API シグネチャは独立で構わない。両者は同名同概念のだけで、コード共有はない。

**代替案**:
- 案 A: Theme / CellStyle を Core に残し、フィールド型だけ Native 化 → Core が UIKit / Compose に依存することになり、Core の責務範囲を曖昧にする。spec の「プラットフォーム非依存契約」破綻が更に悪化。
- 案 B: Theme を `Any` 相当の型で受け、UI 層で型キャスト → 型安全性を失う。

### Decision 3: `SettingsRoot.theme` の削除と View 側引数化

**選択**: `SettingsRoot` から `theme: Theme` フィールドを削除する。Theme は View 側の引数 / modifier として渡す経路に一本化する (iOS: `KsSettingsView { ... }.theme(_:)`、Android: `KsSettingsView(theme = ...)` 引数)。

**理由**:
- 既に Android Compose 層は `KsSettingsView(root, theme = ...)` の形で root と theme を別引数化している実装パターンが確立。
- iOS SwiftUI 層も `.theme(_:)` modifier として既存パターンあり。
- Theme は UI 描画時のスタイル情報であり、ドメイン状態 (Section / Cell の構造) とは関心事が異なる。
- `SettingsRoot` が「データ構造のみ」になることで、KMP 共通化や他 UI 実装 (例: MAUI) からの再利用が容易になる。

**代替案**:
- 案 A: `SettingsRoot.theme` を `Any?` で残す → 型安全性低下。
- 案 B: Container 型 (`SettingsViewModel<Theme>`) を新設して theme と root を 1 つに抱える → 既存パターンと整合せず、コード経路が増える。

### Decision 4: `KsCell` / `Cell` 抽象から `style: CellStyle` プロパティ要求を削除

**選択**: Core の `KsCell` プロトコル (Swift) / `Cell` インターフェース (Kotlin) から `var style: CellStyle { get }` 要求を削除する。各具象 Cell が個別に `style` プロパティを (必要なら) 持つ。

**理由**:
- `CellStyle` を UI 層に移すと、Core から `CellStyle` を参照できなくなるため必然的修正。
- 「全 Cell に style がある」という前提を共通契約から外すことで、Cell 抽象がより小さく明快になる (`id` のみ要求)。
- 各具象 Cell (`LabelCell` / `SwitchCell` 等) は UI 層に既に定義されているため、そこで `style: CellStyle` を持たせる形に変える。
- カスタム Cell 実装者にとっても「`style` を必ず持たなければならない」契約から解放される。

**代替案**:
- 案 A: Cell 抽象をジェネリック化 `Cell<S>` → 既存コードへの影響が大きく、Swift `any KsCell` / Kotlin `Cell` の existential / interface としての扱いが複雑化。
- 案 B: `style: Any?` で弱型化 → 型安全性低下、UI 層でキャスト多発。

### Decision 5: `SettingsRootDiff.updateTheme(Theme)` の Diff からの除外

**選択**: `SettingsRootDiff` から `updateTheme(Theme)` ケースを削除し、Theme 更新は UI 層の独立 API (`SettingsRootStore.applyTheme(_:)` 相当) に分離する。

**理由**:
- Theme が Core から消えるため、Diff の payload に Theme 型を持てなくなる。
- Theme 更新は「構造差分の一種」ではなく、「描画スタイルの再適用」という別の関心事。SettingsRootDiff を「構造の差分」に純化する方が責務が明確。
- UI 層では既に Store パターンが存在 (iOS: `SettingsRootStore`、Android: `SettingsRootStore`)。`applyTheme(_:)` を Store に追加するのは自然な拡張。

**代替案**:
- 案 A: `SettingsRootDiff<TTheme>` のようにジェネリック化 → Swift `enum` と Kotlin `sealed interface` のジェネリック化は ergonomically つらく、`switch` / `when` 網羅性が損なわれる場合がある。
- 案 B: Theme 更新のためだけに `UpdateThemeDiff` 別型を Core に残す → Core が Theme 型を知る必要が残るため目的と矛盾。

### Decision 6: `KsImage` を Core から UI 層へ移動 (型名・構造維持)

**選択**: `KsImage` (Swift `enum` / Kotlin `sealed interface`) を Core から UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に移動する。型名・サブケース構造はそのまま (iOS: `systemName(String)` / `uiImage(UIImage)`、Android: `Resource(@DrawableRes Int)` / `Drawable(android.graphics.drawable.Drawable)` / `SystemName(String)`)。

**理由**:
- 既に `UIImage` / `Drawable` を直接保持する設計で、Core 内では誰からも参照されておらず、UI 層のみで使われている。
- 移動コストが最小 (Core 内被参照ゼロ)。
- 複数ソース (リソース ID / Drawable / SF Symbol 名) を 1 つの型で表現できる利点は維持。
- 利用者の `LabelCell(icon: KsImage.systemName("bell"))` / `LabelCell(icon = KsImage.Resource(R.drawable.ic_x))` API 体験を維持。

**代替案**:
- 案 A: `KsImage` 廃止、`LabelCell(icon: UIImage?)` / `LabelCell(icon: Drawable?)` に直接化 → SF Symbol 名で渡したい場合に呼び出し側で `UIImage(systemName:)` を書く必要があり、Android 側で「複数ソースの sealed 型」の重宝さが失われる。

### Decision 7: 各 Cell の Color / Font パラメータ型の Native 化

**選択**: `SwitchCell.accentColor` / `CheckboxCell.accentColor` / `ButtonCell.titleColor`、`CellStyle.titleColor` 等の型を、iOS では `UIColor?`、Android では Compose `Color?` に変更する。Font 系も同様 (`UIFont?` / `TextStyle?`)。

**理由**:
- Theme / CellStyle のフィールド型が Native 化されることに合わせて Cell パラメータも統一。
- 二重表現を避け、API の一貫性を保つ。
- 利用者から見て「色を渡す箇所はすべて Native 型でよい」という直感的なルールが成立する。

**代替案**:
- 案 A: Theme / CellStyle のみ Native 化し、Cell API は KsColor のまま残す → API の一貫性なし。

### Decision 8: iOS で受け付ける色型は `UIColor` 1 本

**選択**: iOS UI 層の Theme / CellStyle / Cell の Color 型は `UIColor` に統一する。SwiftUI `Color` は受け付けない (利用者は `UIColor(Color.white)` 経由で変換)。

**理由**:
- 二系統サポートすると API が膨張し、合成順序や fallback 仕様が複雑化。
- `UIColor` は UIKit / SwiftUI 両方の根底型として広く扱える。SwiftUI `Color → UIColor` 変換は iOS 17+ で `UIColor(_ color: Color)` 一発で済む (iOS 16 サポート期間中も `UIColor(Color)` initializer は利用可)。
- iOS UI 層は元々 UIKit ベース (`UICollectionViewListCell` 等) なので、UIColor 採用は自然。
- 既存 `KsSettingsViewSwiftUI` は内部で UIKit Cell を組み立てているため、UIColor で揃えると一貫性が増す。

**代替案**:
- 案 A: SwiftUI `Color` も受け付ける convenience init を追加 → 当面 SwiftUI Color 用 init を作るのは利用者ニーズが固まってからでよい。本提案範囲では UIColor 一本でシンプルに保つ。

### Decision 9: Android で受け付ける色型は Compose `Color` 1 本

**選択**: Android UI 層の Theme / CellStyle / Cell の Color 型は `androidx.compose.ui.graphics.Color` に統一する。`@ColorInt` (Int) や `android.graphics.Color` は受け付けない (利用者は Compose `Color(intValue)` 経由で変換)。

**理由**:
- Compose `Color` は `@JvmInline value class Color(val value: ULong)` であり、`data class Theme` のフィールドとして自然に振る舞う (`equals` / `hashCode` 自動)。
- 将来的に Compose Multiplatform 移行や Compose 中心の Android UI へシフトしやすい。
- View 系から使う場合も `color.toArgb()` (Compose 標準拡張) で `@ColorInt` に容易に変換可能。
- `ks-settingsview-ui` 層は内部に RecyclerView ベースの ViewHolder を持つが、Compose Color → `@ColorInt` の変換は ViewHolder bind 時に内部で行う。

**代替案**:
- 案 A: `@ColorInt` (Int) を採用 → Compose 派の利用者にとって `Color(0xFF...).toArgb()` を毎回呼ぶことになり煩雑。
- 案 B: 両方サポート (overload) → API 膨張、合成順序の解釈が複雑化。

### Decision 10: 既存 `theme-bridge` capability の縮小と継続

**選択**: `settings-view-{ios,android}-theme-bridge` capability は維持する。ただし「`KsColor` → `UIColor` / `@ColorInt` 変換」の Requirement は削除し、「実効スタイル合成 (CellStyle → Theme → プラットフォーム fallback の 3 段階)」「タッチフィードバック」「`isEnabled` 描画」「`ButtonCell` の baseColor 解決」「`KsImage` 派生のアイコン解決」の責務に縮小する。

**理由**:
- Theme / CellStyle が Native 型を保持するようになっても、「3 段階の優先順位による合成」「タッチフィードバック時の selectedColor 反映」等のロジックは依然必要。
- bridge capability を消すと既存仕様の Requirement 整合性が崩れる。縮小して維持する方が変更影響を局所化できる。

**代替案**:
- 案 A: theme-bridge capability を `settings-view-{ios,android}-style` に統合 → spec 整理量が増え、本提案の影響範囲が更に拡大。
- 案 B: 完全に削除 → 実効スタイル合成ロジックの spec 上の置き場所が失われる。

### Decision 11: MAUI Bridge active 提案との整合は「依頼ベース」で実施

**選択**: 後続未実装 `add-maui-bridge` / `add-maui-cells` / `add-maui-core` / `add-samples-maui` のアーティファクト（proposal / design / spec / tasks）は、本提案の implementer は **直接変更しない**。本提案完了時に各担当者への修正依頼として整合事項を記録し、各提案側で対応する形を取る。本提案では読み取り確認のみ実施する。

**理由**:
- OpenSpec 規約により、本変更提案の implementer が他の active 変更提案のアーティファクトを直接変更することは禁止されている（変更提案間の整合性管理を破壊する）。
- MAUI 提案はまだ実装されていない (active 状態) ため、各担当者が依頼に応じて spec 修正で済む。実装手戻りは発生しない。
- MAUI 提案で `KsColor` 直接言及はなく、修正は `KsSettingsRootDiffUpdateThemeDTO` を Diff 階層から外す件と `KsThemeDTO` payload を Native 新型に整合させる件のみで、依頼内容は局所的かつ明確。

**代替案**:
- 案 A: 本提案で他の active 提案アーティファクトを直接変更 → OpenSpec 規約違反、変更提案間の整合性管理を破壊。却下。
- 案 B: 別 change `align-maui-bridge-with-purified-core` として分割 → 規約遵守の観点では正当だが、本提案との一時不整合期間が発生。各 active 提案担当者への依頼ベースで十分対応可能。
- 案 C: MAUI 提案実装着手後に各提案側で修正 → 本提案で依頼事項を明確化しておけば、各提案担当者の判断で対応可能。Bridge 実装着手前に修正するのが望ましいが、依頼として記録すれば十分。

## Risks / Trade-offs

- **[Risk] 影響範囲広大によるビルド一時破綻**: Core / UI / SwiftUI / Compose / Sample / テストの同時改修が必要で、Phase 内で一時的にビルドが通らない区間が発生する → **Mitigation**: Phase をプラットフォーム別 (Core 純化 → iOS UI 層整備 → Android UI 層整備 → MAUI spec 整合 → サンプル) に分割し、各 Phase 完了時にはビルド・テストが通る状態を保証する。Phase 内コミット粒度は実装者裁量とするが Phase 跨ぎでは必ず緑。

- **[Risk] iOS / Android で Theme シグネチャが分岐**: UIColor vs Compose Color、UIFont vs TextStyle で完全に別 API になる → **Mitigation**: spec / Sample で「数値ベタ書きで揃えるガイド」(`0xRRGGBB` hex literal を両 plat で同じ値で書く) を README / Sample コメントに明記。両プラットフォームの Theme 構築が「テキスト的に類似」になる規約を維持。

- **[Risk] `SettingsRoot.theme` 削除によるテスト書き換え多数**: 既存 `SettingsRootStoreTest` / `ApplyDiffTest` / `SettingsRootTest` 等の theme 関連テストが影響を受ける → **Mitigation**: テスト書き換え手順を tasks.md に明記。Theme 切替テストは「Store.applyTheme + View 側の theme 引数」の 2 段階テストに分割。

- **[Risk] MAUI Bridge spec 修正と active 提案間の整合**: 4 つの MAUI 提案で spec を整合させる必要がある → **Mitigation**: 各 MAUI 提案 spec の修正タスクを tasks.md に列挙。各提案 spec の修正後、`openspec validate` で整合性を確認。

- **[Trade-off] KMP commonMain 共有の可能性放棄**: Theme / Color / Font を Core から消すと将来 KMP 共通化 (Compose Multiplatform 採用時) で commonMain に置けなくなる → **Mitigation**: KMP は通常 UI 層を各 Native に置く設計が定石。Compose Multiplatform 採用時は別途設計判断とする。本提案では構造ドメイン (SettingsRoot / Section / Cell 抽象 / Diff) のみを KMP 候補と位置付ける。

- **[Trade-off] カスタム Cell 実装者の API 変化**: `Cell` 抽象から `style: CellStyle` 要求が消えるため、外部利用者のカスタム Cell が `style` プロパティを持たなくてもコンパイル可能になる (=「style を持つ Cell」と「持たない Cell」が共存し得る) → **Mitigation**: 提案範囲では本体提供 Cell (LabelCell / CommandCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell) すべてに `style` を持たせる。スタイル合成ロジックは UI 層の `EffectiveStyle` 経路で「`cell as? CellStyleProviding` でキャスト」する形に変更（または各具象 Cell へのキャスト網羅）。

- **[Trade-off] iOS で SwiftUI `Color` 直接サポート不採用**: SwiftUI 派利用者は `UIColor(Color.red)` 経由になる → **Mitigation**: 将来要望が高ければ後続提案で SwiftUI Color 用 convenience init を追加可能。

## Migration Plan

### 実装フェーズ順序

1. **Phase 1: Core 純化準備**
   - `KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` の Core 内 public API を `internal` / `private` 化はせず、新ファイルを UI 層に作成する準備のみ (テスト不変)。
   - spec の MODIFIED 計画を確定。

2. **Phase 2: UI 層に Theme / CellStyle / KsImage を新規追加 (iOS / Android 並行)**
   - iOS: `KsSettingsViewUI` に `Theme.swift` / `CellStyle.swift` / `KsImage.swift` を追加。フィールド型は UIColor / UIFont / UIImage。
   - Android: `ks-settingsview-ui` に `Theme.kt` / `CellStyle.kt` / `KsImage.kt` を追加。フィールド型は Compose Color / TextStyle / KsImage。
   - この段階では Core 側の旧型と新型が共存。

3. **Phase 3: 各 Cell View / ViewHolder / SwiftUI / Compose 層の参照を新型に切替**
   - 各 Cell の `accentColor` / `titleColor` / `icon` パラメータ型を新型に変更。
   - `EffectiveStyle` / `applyDiff` / `CellModifiers` / `DSLHandles` / `BasicCellDsl` / `DSLNodes` / `DSLDiffCalculator` / `KsSettingsViewComposable` の参照を新型に切替。
   - SwiftUI 側 `KsSettingsView.theme(_:)` modifier を新 Theme 受付に。
   - Compose 側 `KsSettingsView(theme = ...)` 引数を新 Theme 受付に。

4. **Phase 4: Core の旧型削除と SettingsRoot / Cell / Diff 修正**
   - `SettingsRoot` から `theme` 削除。
   - `KsCell` / `Cell` から `style` 要求削除。
   - `SettingsRootDiff` から `updateTheme` ケース削除。
   - `KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` を Core から削除。
   - 旧 `UIColor.init(ksColor:)` / `KsColor.toColorInt()` を削除。
   - Theme 部分更新 API (`SettingsRootStore.applyTheme(_:)`) を UI 層に追加。

5. **Phase 5: テスト書き換え**
   - Core テスト (`ThemeTests` / `CellStyleTests` / `KsImageTests`) を UI 層テストに移動 (新型に対するテスト)。
   - `ApplyDiffTests` / `SettingsRootStoreTests` / `BasicCellsTests` の theme / style 関連 Scenario を新 API に書き換え。
   - `EffectiveStyleTests` (UI 層) の入力型を Native 型に変更。

6. **Phase 6: Sample 更新**
   - iOS Sample (`BasicCellsDemoView.swift`) を `UIColor` ベースに書き換え。
   - Android Sample (`BasicCellsDemoScreen.kt`) を Compose `Color` ベースに書き換え。

7. **Phase 7: MAUI Bridge / Cells / Core / Samples 各 active 提案との整合（依頼ベース、本提案では read-only 確認のみ）**
   - `add-maui-bridge/specs/maui-bridge/spec.md` を読み取り確認し、`KsSettingsRootDiffUpdateThemeDTO` を Diff 階層から外す必要があることを依頼一覧に記録。
   - `KsThemeDTO` の payload 定義を Native 新 Theme 構造に合わせる依頼を一覧に記録。
   - `add-maui-cells` の `setTheme` / `addButtonCell(titleColor:)` 等の Color 経路を新方針に整合させる依頼を一覧に記録。
   - `add-maui-core` / `add-samples-maui` の Theme / Color 言及箇所を確認（read-only）。
   - 上記依頼一覧を本提案完了時のサマリ（PR 説明等）に含め、各 active 提案の担当者に修正を依頼する。

8. **Phase 8: 旧 spec の MODIFIED / REMOVED を delta spec に記述**
   - `settings-view-core` から KsColor / KsFont / KsImage / Theme / CellStyle 関連 Requirement を REMOVED として削除。
   - `SettingsRoot` / `KsCell` / `SettingsRootDiff` の Requirement を MODIFIED で更新 (theme / style / updateTheme 削除を反映)。
   - `settings-view-{ios,android}-style` に Theme / CellStyle / KsImage 関連 Requirement を ADDED として追加。
   - `settings-view-{ios,android}-theme-bridge` の Requirement から変換 Scenario を削除 (MODIFIED)。
   - `settings-view-{ios,android}-host` / `settings-view-ios-swiftui` / `settings-view-android-compose` の Theme 受け渡し経路を MODIFIED。
   - `cell-types-basic` / `samples-{ios,android}` を MODIFIED。

### ロールバック戦略

- 本変更は破壊的変更を多数含むため、ロールバックは Git revert のみ。Migration を段階適用するアプローチは不採用 (一気に切り替える)。
- 各 Phase 完了時のコミット粒度でブランチを切り、レビューしやすくする。
