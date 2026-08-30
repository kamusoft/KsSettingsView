## Context

`KsSettingsView` は Native（iOS / Android）モジュールと MAUI バインディングから構成される。現行 `settings-view-core` の `SectionAccessory` は `text(String)` / `custom(AnyCell)` の 2 ケースを持ち、Section ヘッダ／フッタの装飾領域に `AnyCell`（Cell 抽象の型消去ラッパ）を流用している。これは「Cell（タップ・選択・編集する行）」概念を装飾領域に持ち込んだ設計上の混入であり、`add-cell-types-custom` 提案でも H/F 描画機構が `CustomCell` 機構の流用として混在している原因となっている。

並行して、`AiForms.Maui.NativeCollectionView` 互換の「リスト全体のヘッダ／フッタ」機能が要望されており、MAUI 側 XAML から `<settings:KsSettingsView.HeaderView>...</>` で任意 `View` を差し込めることがマスト要件である。

本提案はこの 2 つの課題を一括で扱い、Cell 概念を装飾領域から排除しつつ Root H/F 機能を追加する。

## Goals / Non-Goals

**Goals:**

- `SectionAccessory` から Cell 概念を排除する（`.custom(AnyCell)` → `.view(KsAnyView)`）。
- `KsAnyView` 型消去ラッパを `settings-view-core` に追加する（iOS: SwiftUI / UIView、Android: Compose / Android View）。
- `SettingsRoot` に Root H/F（`header` / `footer`）を追加する。
- iOS は `boundarySupplementaryItem` で、Android は `ConcatAdapter` で Root H/F を描画する。
- MAUI は `KsSettingsView.HeaderView` / `FooterView`（`BindableProperty`、`View?`）を公開する。
- Section H/F の `.view(KsAnyView)` ケースも本提案で本実装する（Phase 1 のプレースホルダから昇格）。

**Non-Goals:**

- `KsAnyView` に差分検出（`Hashable` / `equals`）を実装すること — 中身の更新は描画レイヤに委ねる（AiForms 互換方針）。
- `SectionAccessory` と `RootAccessory` を共通型に統合すること — 将来の挙動分岐に備え別型として導入する。
- Root H/F のスクロール固定（`pinToVisibleBounds` / sticky）— 既定はスクロール追従、将来オプション化の余地のみ残す。
- `CustomCell`（Cell 本体）の実装 — 別提案 `add-cell-types-custom` のスコープ。本提案は H/F 描画スコープのみを切り出す。

## Decisions

### Decision 1: Section H/F の Cell 概念排除（破壊的変更）

`SectionAccessory.custom(AnyCell)` / `.Custom(Cell)` を削除し、`.view(KsAnyView)` / `.View(KsAnyView)` に置き換える。

**理由:**
- Header/Footer は本来 Cell（行）ではなく、装飾領域である。`AnyCell` 流用は概念的に誤り。
- `KsAnyView` は Cell 抽象に縛られないため、将来の supplementary 描画機構（ピン留め、テーマ継承、Insets 制御）の拡張余地を確保しやすい。
- `add-cell-types-custom` の H/F 描画が CustomCell 機構の流用となっていたが、本変更により責務分離が明確になる。

**代替案:**
- (a) `SectionAccessory.custom(AnyCell)` を維持: 概念混入を放置することになる。本提案の動機と直接矛盾するため不採用。
- (b) `SectionAccessory` を完全廃止し `AnyCell` 直入れにする: `.text(String)` の手軽さを失う。`text` は Section ヘッダの一般慣習（"一般"・"通知" 等のラベル）として価値が高く、不採用。

### Decision 2: `RootAccessory` を `SectionAccessory` と別型として新設

`RootAccessory.text(String) | .view(KsAnyView)` を新設。`SectionAccessory` と shape は同じだが別型として導入する。

**理由:**
- 将来の挙動分岐に備える（ピン留め、テーマ継承ルール、Safe Area 扱い、iOS/Android 実装層の差異）。
- Root と Section は同じ「装飾領域」概念だが、UI 上の役割（リスト全体の見出し vs. セクション境界）と実装層（`boundarySupplementaryItem` の global vs. section、`ConcatAdapter` の独立 adapter vs. ListItem 内）が異なる。
- 同じ shape の型を 2 つ持つコピー感はあるが、概念純度を優先。

**代替案:**
- (a) `Accessory` 共通型に統合: 概念は単純化されるが、将来の挙動分岐で if 分岐が増える可能性。今は分けておき、必要に応じて統合する道を残す。
- (b) `SectionAccessory` を Root にも流用: 命名が役割と乖離する。不採用。

### Decision 3: `KsAnyView` は差分検出に参加しない

`KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持たず、`SettingsRoot` / `Section` の差分判定にも含めない。

**理由:**
- SwiftUI `View` ジェネリック型・`@Composable` ラムダ・`(Context) -> View` ファクトリは値の等価性を意味のある形で比較できない。
- 利用者に identity を渡させる API（`HeaderView(id: ...)`）はユーザビリティを著しく損なう。本提案では明確に避ける方針が確定済み。
- AiForms.Maui.NativeCollectionView も Header/Footer は固定 1 要素として扱い差分検出していない。実用上の問題は確認されていない。
- 中身の更新は iOS: `UICollectionViewListCell.contentConfiguration` の再構成 / Android: `ComposeView.setContent` の再呼び出し で吸収する。これは SwiftUI / Compose のリアクティブ機構と整合する。

**代替案:**
- (a) 利用者に identity を渡させる: API のシンプルさを損なう。明確に却下。
- (b) クロージャの参照同一性で判定: XAML 等で毎フレーム新しいクロージャが渡される可能性があり、常に「違う」判定になる。不採用。
- (c) 型情報で識別: Android のラムダから型を抜けない／同じ型で違う内容を区別できない。不採用。

### Decision 4: iOS は `boundarySupplementaryItem` で Root H/F を実装

`UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に global supplementary（`alignment: .top` / `.bottom`）を 1 つずつ追加する。`elementKind` は `"ks-root-header"` / `"ks-root-footer"` を予約する。`pinToVisibleBounds = false`（デフォルト動作）でスクロール追従させる。

**理由:**
- `UICollectionView` 単一の中で完結する。外側に `UIStackView` で挟むより SwiftUI/UIKit 利用者双方にとって自然。
- スクロールに乗る挙動が AiForms.Maui.NativeCollectionView と一致。
- `UICollectionLayoutListConfiguration.list` ベースの sectionProvider と組み合わせて利用可能。
- `UICollectionViewListCell` を supplementary に流用することで、既存の `UIHostingConfiguration` ベース機構を再利用できる。

**代替案:**
- (a) 外側 `UIStackView` ラップ: スクロール追従が崩れる。不採用。
- (b) Section header/footer に擬似的に詰め込む: Section と Root の概念が混ざる。不採用。

### Decision 5: Android は `ConcatAdapter` で Root H/F を実装

`RecyclerView.adapter = ConcatAdapter(headerAdapter, mainAdapter, footerAdapter)` の構成を取り、`headerAdapter` / `footerAdapter` は `RootHeaderFooterAdapter`（`ItemCount = 0/1` 切り替え式、`getItemViewType` / `getItemId` は固定値）として実装する。

**理由:**
- AiForms.Maui.NativeCollectionView の `HeaderAdapter` / `FooterAdapter` 実装と同じパターン。実績がある。
- 既存の `mainAdapter`（`ListAdapter` / `DiffUtil`）に手を入れず、責務分離が明確。
- Section H/F は引き続き `mainAdapter` 内の `ListItem.SectionHeader` / `ListItem.SectionFooter` で扱う。Root と Section は別レイヤとして処理される。
- 中身の更新は `notifyItemChanged(0)` による再 bind で吸収する。`KsAnyView` の差分判定なしで動作する。

**代替案:**
- (a) `mainAdapter` の `ListItem` に `RootHeader` / `RootFooter` ケースを追加: DiffUtil 一元化されるが、Root と Section の概念が混ざる。`KsAnyView` の equals 不在により実質的な差分検出は効かないため、ConcatAdapter 化のメリット（責務分離）の方が大きい。不採用。

### Decision 6: MAUI 公開 API は `BindableProperty` のみ

`KsSettingsView.HeaderView` / `FooterView` の `BindableProperty`（`View?`）を公開する。Handler 内で MAUI `View.ToPlatform()` を呼び、ネイティブ `View` を `KsAnyView.uiKit(...)` / `KsAnyView.androidView(...)` として `SettingsRoot.header` / `footer` に格納する。

**理由:**
- XAML 利用がマスト要件。`BindableProperty` 化は必須。
- `MauiView.ToPlatform()` は MAUI 標準 API として既に存在し、新規変換層を用意する必要がない。
- HotReload や BindingContext 伝播は MAUI 標準機構に乗る。

**代替案:**
- (a) Builder API のみ: XAML 不可。要件不適合。
- (b) `View` だけでなく `DataTemplate` も公開: 過剰機能。`HeaderView` / `FooterView` はリスト全体に 1 つしか出現しないため `DataTemplate` の必要性が薄い。本提案では `View?` のみ。

### Decision 7: `cell-types-custom` から H/F 描画スコープを切り出す

`add-cell-types-custom` の proposal / design / tasks / 関連 delta spec から「Section ヘッダ／フッタの任意 View 描画」関連記述を削除し、`CustomCell`（Cell 本体）のみに集中させる。

**理由:**
- 本提案で H/F 描画を `KsAnyView` ベースで本実装するため、`cell-types-custom` の H/F 描画スコープは不要になる。
- 責務分離が明確になり、`cell-types-custom` のレビュー・実装範囲が縮小する。

**代替案:**
- (a) `cell-types-custom` を取り下げる: `CustomCell`（Cell 本体）の必要性は別途残るため、提案そのものは維持する。
- (b) `cell-types-custom` 内に H/F スコープを残し本提案と並列に: 重複作業が発生する。不採用。

## Risks / Trade-offs

- **[破壊的変更（Cell 概念排除）の波及]** → archive 済 `settings-view-core` spec に対し MODIFIED delta で対応する。in-progress の `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-cell-types-custom` の各アーティファクトを本提案と同期して書き換える。マージ順序として本提案を先行させる。
- **[`KsAnyView` の差分検出非対応]** → 中身の更新は描画レイヤ（`UIHostingConfiguration` / `ComposeView.setContent`）の都度再構成で吸収する。SwiftUI / Compose のリアクティブ機構は内部状態更新を担う。spec / design に明示する。
- **[MAUI `View.ToPlatform()` の挙動]** → HotReload / BindingContext 伝播は MAUI 標準機構に依存する。Handler 実装時に検証ポイント（`Mapper` の `propertyChanged` で旧 View の Disconnect / 新 View の Connect、`Container` の取り扱い）を tasks に明記する。
- **[`ConcatAdapter` の `stableIds`]** → `mainAdapter` / `headerAdapter` / `footerAdapter` の `getItemId` がスコープ内で衝突しないよう、`headerAdapter`=1, `footerAdapter`=2 などの予約値を使う。`mainAdapter` 側は section/cell の構造から派生する大きな値を使い、衝突しないことを spec / design に記述する。
- **[Root と Section H/F の挙動差]** → `Theme` の `headerTextColor` / `footerTextColor` は Section H/F の `.text` ケースに適用される。Root H/F の `.text` ケースには別途 Theme フィールドを設けるか、Section と同じ Theme を流用するかは本提案の Open Questions（後続改善で扱う）。本提案では Section と同じ Theme フィールドを参照することを暫定採用し、将来 Root 専用 Theme フィールドを追加する余地を残す。

## Migration Plan

本提案の Capability 範囲は `settings-view-core` のみである（`settings-view-ios-ui` / `settings-view-android-ui` / `maui-bindings` / `cell-types-custom` は in-progress 提案であり `openspec/specs/` に未登録のため、本提案では delta spec を作らない。当該提案ファイル群は **本提案の探索段階で既に直接書き換え済み** である）。

1. 本提案の merge 順序: `refactor-accessory-and-root-hf` を先に merge し、その後 `add-cell-types-custom` 更新版を進める。
2. `settings-view-core` の `SectionAccessory.custom(AnyCell)` を直接参照する実装コードがあれば `.view(KsAnyView)` に書き換える（archive 済 core spec の sync コミット `82da3aa` 以降の実装に対し走査）。
3. **既存提案の書き換え（本提案の探索段階で完了済み）**:
   - `add-settings-view-ios-ui` の proposal / design（Decision 5c 更新、Decision 5d 追加）/ tasks（4.3〜4.6 / 8.7〜8.9 更新）/ `specs/settings-view-ios-ui/spec.md`（Section H/F の `.view` 描画 Requirement 本実装化、Root H/F 描画 Requirement 追加）を本提案方針に合わせて書き換え済み。
   - `add-settings-view-android-ui` の proposal / design（Decision 1 注記追加、Decision 5c 更新、Decision 5d 追加）/ tasks（3.1 / 4.6 / 5.3〜5.9 / 9.7〜9.10 更新）/ `specs/settings-view-android-ui/spec.md`（Adapter 構成変更、Section H/F の `.View` 描画、Root H/F 描画 Requirement 追加）を本提案方針に合わせて書き換え済み。
   - `add-maui-bindings` の proposal / design（Decision 6d 追加）/ tasks（1.3 / 2.4 / 5.6〜5.7 / 6.1〜6.2 / 10b.4 更新）/ `specs/maui-bindings/spec.md`（`HeaderView` / `FooterView` BindableProperty Requirement 追加、Collection 同期に HeaderView/FooterView 購読を追加）を書き換え済み。
   - `add-cell-types-custom` の proposal / design / tasks から H/F 描画スコープを削除し、関連 delta spec ファイル（`specs/settings-view-ios-ui/spec.md`、`specs/settings-view-android-ui/spec.md`）を削除、`specs/cell-types-custom/spec.md` から H/F 関連 Requirement を削除済み。
4. ロールバック戦略: 本提案を revert する場合、`SectionAccessory.custom(AnyCell)` への戻しと `KsAnyView` / `RootAccessory` / `SettingsRoot.header,footer` の削除を伴う。in-progress 提案側の修正もロールバック対象。

## Open Questions

- Root H/F の `.text` ケースに対する Theme フィールド: Section と共有する暫定方針で OK か、Root 専用 Theme（`rootHeaderTextColor` 等）を導入するか。
- `pinToVisibleBounds` をオプション化する API の形（`SettingsRoot.headerPinned: Bool?` / 各プラットフォームの style 引数で切り替え）。
- `KsAnyView.uiKit` / `.androidView` factory の lifecycle: 同じスロットに同じ `KsAnyView` が留まる場合、factory を毎回呼ぶか、初回のみ呼ぶかの方針。実装時に検証する。
