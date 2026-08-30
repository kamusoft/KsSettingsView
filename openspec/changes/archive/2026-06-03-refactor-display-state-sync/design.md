## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。

- 移植元オリジナル（Android）：
  - [`SettingsViewRecyclerAdapter.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/SettingsViewRecyclerAdapter.cs) — `GetItemId(position) => position`（内容非依存）、`CellPropertyChanged` 購読 → `NotifyItemChanged` / `NotifyItemRangeChanged` による部分更新。DiffUtil の内容比較を使わない。
  - [`CellBaseView.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/CellBaseView.cs) — Cell ↔ View の TwoWay 連携基盤。
  - [`CheckboxCellView.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/CheckboxCellView.cs) — `RowSelected` で View 直接トグル、`OnCheckedChanged` でモデル書き戻し（TwoWay）。
  - [`SimpleCheck.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/SimpleCheck.cs) / [`RadioCellView.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/RadioCellView.cs) — Radio のグループ連動（自分を ON にして通知、他セルは選択値更新で解除）。
- 現状の KsSettingsView 実装（修正対象）：
  - Android: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt`（`areContentsTheSame` / `getItemId`）、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`（`ReplaceCell` 発行条件）、各 `*CellViewHolder.kt`
  - iOS: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`（`reloadItems` 呼び出し、`cellIndex`、cell provider）、DSL Diff 算出

## Context

`add-settings-view-{core,android-ui,ios-ui}` で SettingsView の基盤が、`add-declarative-dsl` で宣言的 DSL が、`add-cell-types-basic` で基本 Cell 7 種が実装された。`add-cell-types-basic` の実機レビュー（Pixel 6a, 2026-06-02〜03）で、チェック/スイッチ操作時に行全体が再描画される「ちらつき」が顕在化した。

調査の結果、根本原因は**差分検出が Cell の内容プロパティまで比較していること**だと判明した。`add-cell-types-basic` では暫定対処として `equals`/`hashCode` から内部状態（`isOn`/`isChecked`/`selectedValue`）を除外したが、これは対症療法であり、RadioCell は `selectedValue` を equals に残すなど一貫しない。本提案で「差分は構造同期、内容更新はセルで完結」という原則を確立し、両プラットフォームを根本修正する。

## Goals / Non-Goals

**Goals:**
- 表示状態同期を「構造同期（diff/snapshot = id 同一性のみ）」と「内容更新（同一セルの部分更新）」の二層に分離する原則を iOS / Android 共通で確立する。
- 内容変化時にセルを作り直さない（Android: フルリバインド回避、iOS: `reloadItems` → `reconfigureItems`）。
- チェック系 Cell の TwoWay（View 直接トグル → モデル書き戻し）と Radio グループ連動を、Replace を介さず実現する。
- 両プラットフォームを実装し、実機/シミュレータでちらつき解消とトグル動作を確認する。

**Non-Goals:**
- 追加・削除・移動の差分アニメーション自体の変更（構造同期は維持）。
- `KsAnyView`（装飾領域）の差分非参加ルールの変更（既存維持）。
- MAUI 側の対応（別提案の責務）。

## Decisions

### Decision 1: 表示状態同期の二層分離（core 原則）

**選択**: 表示状態の同期を明確に 2 層に分離する。

```
┌──────────────────────────────────────────────────────────┐
│ ① 構造同期（diff / snapshot）= id 同一性のみ              │
│    役割: Cell/Section の Add / Delete / Move / 差し替え     │
│         （= セルそのものの増減・並べ替え・id 変化）を検出  │
│    判定: id（Android: areItemsTheSame, iOS: KsCellID）のみ │
│    内容プロパティ（title/isOn/isChecked/...）は使わない     │
├──────────────────────────────────────────────────────────┤
│ ② 内容更新 = 同一セルを作り直さず部分更新                  │
│    役割: 同一 id のセルの表示内容（プロパティ）の反映       │
│    手段: Android = ViewHolder の部分更新（NotifyItemChanged │
│          相当 / 再生成しない）                             │
│          iOS = reconfigureItems（セル破棄せず再構成）       │
│    チェック系は TwoWay（View 直接トグル → モデル書き戻し）  │
└──────────────────────────────────────────────────────────┘
```

**理由**:
- diff/snapshot の本来の責務は「データソースとアダプタ（コレクションビュー）の**構造**同期」であり、id 同一性で Add/Delete/Move/差し替えを検出すれば足りる。
- 内容プロパティを diff 比較に使うと、内容変化が「セルの差し替え（ReplaceCell / reloadItems）」と解釈され、ViewHolder/CellView のフルリバインド（再生成・再 bind）を誘発して描画が乱れる。
- 移植元 AiForms はこの分離を実践している（`GetItemId => position`、`PropertyChanged → NotifyItemChanged`）。MAUI の Binding に相当する役割を Native では ViewHolder/CellView が担う。

**代替案（却下）**:
- 内容を含む等価性で diff し、アニメーションを無効化（`animatingDifferences: false` / `notifyItemChanged` 抑制）→ セル再生成自体は残りちらつきの根が消えない。
- 内部状態だけを equals から除外（`add-cell-types-basic` の暫定対処）→ title 等の他プロパティでは依然 Replace が走る、RadioCell が一貫しない、原則化されない。

### Decision 2: `equals`/`hashCode`/`Hashable` 契約は値型として維持し、diff は使わない

**選択**: Cell を含む値型の `equals`/`hashCode`（Kotlin）/ `Hashable`（Swift）契約自体は**維持**する（内容を含む通常の値等価）。ただし**差分検出（diff/snapshot の構造同期）はこの内容等価性を用いてはならない**。構造同期は id 同一性のみで判定する。

**理由**:
- 値型の等価性はテスト・一般的な比較・コレクション操作で必要であり、これを壊すと広範な影響が出る。
- 問題は「等価性の定義」ではなく「**diff 層がどの等価性を構造同期に使うか**」である。後者を id 同一性に限定すればよい。
- これにより `add-cell-types-basic` の「内部状態を equals から除外」という歪な対処を撤回し、Cell を素直な値型（全フィールドを含む equals）に戻せる。

**影響**: Android `getItemId` は `hashCode`（内容依存）から id ベースへ変更。`areContentsTheSame` は equals 委譲を廃止し、同 id なら常に `true`。

### Decision 3: Android の内容更新経路

**選択**: 内容変化は ViewHolder の**部分更新**で反映する。`DSLDiffCalculator` は内容変化では `ReplaceCell` を発行せず（構造変化＝Add/Delete/Move/id 変化のみ）、内容更新は以下のいずれかで同一 ViewHolder を更新する：

- アダプタが id → 最新 Cell を保持し、内容変化時に該当 position へ `notifyItemChanged(position)`（再生成を伴わない bind の再実行。ただし `setBackgroundColor` 等の重い処理を避ける部分 bind とする）、または
- チェック系のように TwoWay で完結する内部状態は、ViewHolder が View を直接更新し diff を一切起こさない。

**理由**:
- `notifyItemChanged` はセルを破棄せず同じ ViewHolder に `onBindViewHolder` を呼ぶ（DiffUtil の Replace と異なりアイテム差し替えではない）。`add-cell-types-basic` で確認したとおり、内容更新で diff を起こさなければ行全体のチカチカは消える。
- TwoWay（View 直接トグル）は最も軽量で、オリジナル `RowSelected` と一致する。

**Radio グループ連動**: 同一 `groupId` の選択変更は、ViewHolder/アダプタが旧選択・新選択の 2 セルのみを部分更新して反映する（`ReplaceCell` で全 Radio を再生成しない）。

### Decision 4: iOS の内容更新経路（reconfigureItems）

**選択**: snapshot のアイテム識別子は `KsCellID`（id ベース、既存維持）。内容変化では `snapshot.reloadItems`（セル破棄＆再生成）を**やめ**、`snapshot.reconfigureItems`（iOS 15+、同一セルを破棄せず `cellProvider` で再構成）を用いる。DSL Diff 算出は内容変化で `replaceCell`（reload を誘発する旧経路）を発行せず、reconfigure 経路に載せる。

**理由**:
- iOS の snapshot は既に id（`KsCellID`）ベースで、`cellIndex[KsCellID: any KsCell]` が最新 Cell を保持している。構造は正しく、問題は `reloadItems` がセルを作り直す点のみ。
- `reconfigureItems` は同一セルインスタンスに対し contentConfiguration を再適用するため、セルの破棄・生成・アニメーションのちらつきが起きない。これが iOS 版の「部分更新」に当たる。
- 最低 OS が iOS 15 未満を含む場合は、`reconfigureItems` 不可時に限り `reloadItems` へフォールバックする（実装時に Deployment Target を確認し design に追記）。

**チェック系 TwoWay**: CellView が自身の表示状態を直接更新し `onValueChanged` でモデルへ書き戻す。Radio グループ連動も該当セルの reconfigure で行う。

### Decision 5: `SettingsRootDiff.replaceCell` の意味論明確化

**選択**: `replaceCell(cellID, new:)` は「**同一 id のセルの内容更新（reconfigure / 部分更新）**」を表すと定義し、セルの破棄＆再生成（フルリバインド）を意味しないことを core 仕様に明記する。id が変わる差し替えは remove + insert（または構造差分）で表現する。

**理由**:
- 既存の `replaceCell` 名が「セルごと差し替え（reload）」と誤解され、iOS で `reloadItems`、Android で再生成的な扱いを招いていた。意味論を「内容 reconfigure」に統一する。

## Risks / Trade-offs

- **リスク**: 内容更新時にアニメーション（フェード等）が無くなる／変わる。→ 設定画面の内容更新は即時反映が自然でありむしろ望ましい。実機で確認。
- **リスク**: iOS の Deployment Target が iOS 15 未満を含むと `reconfigureItems` が使えない。→ 実装時に確認し、フォールバックを用意。
- **トレードオフ**: 「同一 id のまま内容を外部から変える」ケースは ViewHolder/CellView の部分更新経路に依存する。外部更新が確実に部分更新へ繋がるよう、アダプタ/Controller が最新 Cell を保持し、内容変化を検知して該当セルを reconfigure する経路を実装で担保する。
- **トレードオフ**: `add-cell-types-basic` の暫定対処（equals から内部状態除外）を撤回し素直な値型に戻すため、当該変更分の再修正が必要。

## Migration

- `add-cell-types-basic` で `equals`/`hashCode` から除外していた内部状態を、本提案では値型として元に戻してよい（diff が内容等価性を使わなくなるため、ちらつきは構造同期側の id 限定で解消される）。ただし「戻す／除外のまま」の最終判断は実装時に Decision 2 に従い、回帰テストで担保する。

## Open Questions

- iOS の Deployment Target は iOS 15+ か（`reconfigureItems` 可否）。→ 実装時に `Package.swift` / プロジェクト設定を確認。
