## Context

KsSettingsView の `Theme` / `CellStyle` は、`archive/purify-core-extract-style-to-ui-layer` により UI 層（`KsSettingsViewUI` / `ks-settingsview-ui`）に隔離された値型である。オリジナル `AiForms.Maui.SettingsView` の `SettingsView` クラスは 40+ の BindableProperty をフラット構造で持っていたが、KsSettingsView は「全体既定 = `Theme`」「Cell 個別 = `CellStyle`」の 2 層に再構成されている。

しかしこの再構成では「`Theme` への昇格は `titleColor` / `titleFont` のみ」に留まっており、オリジナルが持っていた `CellValueText*` / `CellDescription*` / `CellHint*` / `CellIconSize` / `CellIconRadius` などの「Cell 全体既定」が **`Theme` 側に存在しない**。結果として、`CellStyle` には個別フィールドがあるのに、`Theme` でまとめて指定する手段がない非対称な状態にある。

加えて：

- **命名揺れ**: `Theme.viewBackgroundColor` / `Theme.titleColor` はオリジナル `BackgroundColor` / `CellTitleColor` と整合しない。利用者がまだ少ない基礎段階での破壊的 rename が許容される。
- **fontFamily 課題**: Compose `TextStyle.fontFamily` の `equals` が `FontFamily` 参照比較に依存するケースが Theme.kt のコメントに記載されている。`Theme` 等価判定とのからみで実害が出ないことを e2e テストで保証したい。

詳細根拠: `openspec/drafts/05-port-gap-change-plan-roadmap.md §1`、`openspec/drafts/04-original-property-port-gap-survey.md §3`。

## Goals / Non-Goals

**Goals:**

- オリジナル `SettingsView` の「Cell 全体既定」プロパティ群を `Theme` に網羅追加し、`Theme` ↔ `CellStyle` を 2 段重ねで運用できる状態にする。
- `Theme.viewBackgroundColor` / `Theme.titleColor` をオリジナル整合の `backgroundColor` / `cellTitleColor` に **互換シムなしで** リネームする。
- `EffectiveStyle`（iOS / Android）で全 Cell プロパティの解決順序「`CellStyle.X` → `Theme.cellX` → 既定」を Requirement として明文化し、実装する。
- Compose 側の `fontFamily` 指定が確実にレイアウトに反映されることを e2e テストで保証する（同一インスタンス再利用での等価安定性、size 反映）。
- iOS / Android 両プラットフォームで等価のフィールド構成にする（型は各 Native）。

**Non-Goals:**

- 全 Cell（Switch / Checkbox / Radio / SimpleCheck / Button）の `description` / `icon` / `hintText` 共通化（Change 2 で扱う）。
- `Section.isVisible` / `Cell.isVisible` の追加（Change 3 で扱う）。
- `RadioCell.accentColor` / `SimpleCheckCell.accentColor` 移植漏れ（Change 2 で共通フィールド化と同時に対応）。
- 互換シム（旧名 deprecated 残し）の提供。
- ヘッダ / フッタの `Padding` / `VerticalAlign`（draft 04 で「カスタム View で対応」と決定済）。
- `ScrollToTop` / `ScrollToBottom` 等の命令系 API（draft 04 で別 API として後で検討）。
- DragSort 機能（後に刷新実装予定）。

## Decisions

### Decision 1: `Theme.backgroundColor` / `Theme.cellTitleColor` への rename（互換シムなし）

**選択**: 旧 API（`viewBackgroundColor` / `titleColor`）を **削除** し、新名にコンパイル時点で書き換えさせる。

**理由**:
- 利用者がまだ少ない基礎段階。互換シムを長期残すコストの方が高い。
- 一度に揃えることで Theme 構造の整合性を取り戻す。
- サンプル（`samples/ios` / `samples/android`）は本 change 内ですべて更新できる。

**代替案**:
- (A) deprecated 警告で旧名残し → 「いつ消すか」議論が永続化し、新名 spec への移行が完了しない。本基礎段階では採用しない。
- (B) 旧名を維持し新名を別フィールドで追加 → 二重定義で `Equatable` / `data class` に矛盾が出る上、利用者が混乱する。却下。

### Decision 2: `Theme` 追加フィールドの命名と既定値

**選択**:
- Header/Footer 系: `headerFontFamily` / `headerFontAttributes` / `footerFontFamily` / `footerFontAttributes` / `headerHeight: Double`（既定 `-1.0` = 自動）
- Cell 全体既定: `cellTitleFontSize: Double`（既定 `-1.0`）、`cellValueTextColor`、`cellValueTextFont`、`cellDescriptionColor`、`cellDescriptionFont`、`cellHintTextColor`、`cellHintFont`、`cellIconSize`、`cellIconRadius`（いずれも Optional / nullable、既定 `nil` / `null`）

色・フォント系は既存 `Theme.titleFont`（`UIFont?` / `TextStyle?`）と同様に **単一 `Font` 型に集約**。`fontSize` / `fontFamily` / `fontAttributes` を独立フィールドにせず、`Font` 型のメンバとして表現する。例外として、オリジナル `SettingsView.CellTitleFontSize` が独立 BindableProperty だったことを尊重し `cellTitleFontSize: Double` のみ並立で持つ（`titleFont` と `cellTitleFontSize` が両方非 nil の場合は `cellTitleFontSize` を size として優先）。

ヘッダ/フッタの `headerFontFamily` / `headerFontAttributes` は **既存 `headerFontSize` / `footerFontSize`**（`Double`）と並立。`headerFont` / `footerFont` を `UIFont?` / `TextStyle?` の単一 Optional フィールドとして追加し、`fontSize` + `font` が両方非 nil なら `font.size` を優先する（既存 `titleFont` のパターン踏襲）。

**理由**:
- オリジナル `CellXxxColor` / `CellXxxFontSize` / `CellXxxFontFamily` / `CellXxxFontAttributes` をすべて独立フィールドにすると、`Theme` のフィールド数が 70+ に膨らみ「ファイル全体スタイル」の俯瞰性が壊れる。
- 既存 `titleColor` / `titleFont` が「色 1 つ + フォント 1 つ」で運用されており、`description` / `valueText` / `hint` も同パターンを踏襲することで Theme 内の対称性が保たれる。
- `cellTitleFontSize` のみ独立にすることでオリジナル運用（`SettingsView.CellTitleFontSize` だけを変える）と互換性のある書き方を残す。

**代替案**:
- (A) オリジナル準拠で fontSize / fontFamily / fontAttributes をすべて独立フィールドにする → フィールド数が爆発し、`equals` / `data class` の差分管理コストが激増。却下。
- (B) `Font` 型に完全集約し `cellTitleFontSize` も廃止 → オリジナル運用「`CellTitleFontSize` だけ変える」が `Font` 全体の再構築になり API としてうるさい。`cellTitleFontSize` だけ並立で残すのが現実解。

### Decision 2.5: `valueText` / `description` / `hintText` の Theme フォールバック先を専用フィールドに切り替える

**選択**: `CellStyle.valueTextColor` / `CellStyle.valueTextFont` が `nil` のとき、これまで `Theme.descriptionColor` / `Theme.descriptionFont` にフォールバックしていた挙動を、本 change で追加する **`Theme.cellValueTextColor` / `Theme.cellValueTextFont` を最優先のフォールバック先** に切り替える。同様に `descriptionXxx` は `Theme.cellDescriptionXxx`、`hintTextXxx` は `Theme.cellHintXxx` を最優先のフォールバック先とする（その先のフォールバックは spec 各 Requirement で個別に定義）。

**理由**:
- 既存 spec の「`valueTextColor` 未指定時は `Theme.descriptionColor` で補完」は、`Theme` に `valueText` 専用フィールドが存在しなかった時代の **苦肉の代替** にすぎない。本 change でオリジナル整合の `Theme.cellValueTextColor` 等を新設するのに、フォールバック先だけ古い設計を残すと、利用者は「`Theme.cellValueTextColor` を入れたのに `Theme.descriptionColor` が勝つ」誤動作を疑うことになる。
- オリジナル `AiForms.Maui.SettingsView` でも `CellValueTextColor` は `CellDescriptionColor` とは別 BindableProperty として独立しており、KsSettingsView もこれに揃える。
- `description` / `hintText` も同パターン。Theme に専用フィールドを新設した以上、CellStyle 未指定時の **第一フォールバック先は Theme の同種フィールド** とする方が一貫している。

**代替案**:
- (A) 既存挙動（`valueText` 未指定時に `Theme.descriptionColor` フォールバック）を維持し、`Theme.cellValueTextColor` は二次フォールバック先とする → 「Theme に専用フィールドがあるのに、別フィールドが優先される」非直感的な状態が固定化する。却下。
- (B) `valueText` 未指定時はそのまま UI 層既定（`UIColor.label` / `Color.onSurface` 相当）にフォールバックする → Theme の `cellValueTextColor` を導入する意味がなくなる。却下。

### Decision 3: EffectiveStyle の解決順序

**選択**: 全 Cell プロパティを以下の 3 段で解決：

```
最終値 = CellStyle.X        if X != nil
       else Theme.cellX     if cellX != nil
       else プラットフォーム既定
```

**例外**: `ButtonCell.titleColor` は既存 4 段（`ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → 既定`）を維持。

`EffectiveStyle.swift` / `EffectiveStyle.kt` に各プロパティのアクセサ関数を追加：
- `effectiveTitleColor(cellStyle, theme) -> UIColor` / `Color`
- `effectiveTitleFont(cellStyle, theme) -> UIFont` / `TextStyle`
- `effectiveTitleFontSize(cellStyle, theme) -> Double`
- `effectiveValueTextColor(cellStyle, theme) -> UIColor` / `Color`
- ... 全プロパティ網羅

**理由**:
- オリジナル 2 段重ね運用と完全互換。
- 各 Cell View / ViewHolder の bind 処理を「`EffectiveStyle.effectiveXXX(...)` を呼ぶだけ」に統一でき、解決ロジックを 1 箇所に集約できる。

**代替案**:
- (A) Cell ごとに inline で `cellStyle.X ?? theme.cellX ?? default` を書く → コード散在し、テストカバレッジも分散。却下。
- (B) `Theme` 側に「effective 値」を計算するメソッドを生やす → `Theme` が `CellStyle` に依存することになり、責務分離が崩れる。却下。

### Decision 4: fontFamily 課題への対応

**選択**: Compose 側で以下のテストを追加し、`fontFamily` 指定がレイアウトに確実に反映されることを保証する：

1. 同一 `FontFamily` インスタンスを使う 2 つの `TextStyle` が `==` で等価になること。
2. `fontFamily` を指定した `Theme` でセルを描画したとき、`TextView` / `Text` Composable の `fontFamily` 状態に同じインスタンスが流れていること。
3. `fontSize` 指定が `Layout` の measured height / width に反映されること（e2e）。

iOS は `UIFont.isEqual(_:)` ベースで既に動作実績があるため、確認のみのテストを追加（同一 `UIFont` インスタンスでの等価性、Theme.titleFont の反映）。

**理由**:
- Theme.kt のコメントに「`FontFamily` の equals に注意」と書かれており、本 change で `cellTitleFontSize` / `cellDescriptionFont` 等を新規追加するにあたり、retest して問題なし or 修正必要かを明らかにすべき。
- 単に Theme に追加して終わりだと、利用者が「`fontFamily` を変えても効かない」報告で戻ってくるリスクがある。

**代替案**:
- (A) テストなし、実装のみ → fontFamily 課題の解決を放置し、将来同じ問題が再発。却下。
- (B) Compose 内部の `equals` 挙動を改造 → 範囲外。Theme 等価判定の方を `fontFamily` の内容比較に切り替える方が安全だが、本 change ではテスト追加に留め、必要に応じて等価判定の見直しは別 change で対応する余地を残す。

### Decision 5: spec への落とし込み（MODIFIED vs ADDED）

**選択**: 既存 Requirement 「Theme 型 (UI 層)」「CellStyle 型 (UI 層)」を **MODIFIED** として全文書き直す（フィールド一覧を新リストに置換）。

**理由**:
- フィールドリネーム（`viewBackgroundColor` → `backgroundColor`、`titleColor` → `cellTitleColor`）は既存 Requirement の **本文**を変える行為。
- フィールド追加（`cellValueTextColor` 等）も同じ Requirement に対する **本文**変更。
- OpenSpec 規約上、Requirement 本文を変えるときは MODIFIED で全文を copy-edit する。partial で済ませると archive 時に旧本文が残る。

EffectiveStyle の解決順序は新規概念のため、新 Requirement「Theme と CellStyle の解決順序」を **ADDED** として追加する。

**代替案**:
- (A) REMOVED + ADDED で旧 Requirement を消して書き直す → Requirement 名を維持できず archive 整合性が落ちる。却下。

## Risks / Trade-offs

### [Risk] 利用者コードのコンパイルエラー
**Mitigation**: 本 change で `samples/ios` / `samples/android` をすべて更新する。CHANGELOG に rename 一覧を明記。外部利用者向けには README に移行ガイド（旧名 → 新名）を追加する。

### [Risk] `Theme` フィールドが増えることで `equals` / `data class` の差分計算コストが上がる
**Mitigation**: 追加フィールドはすべて Optional / nullable で既定 `nil` / `null`。未使用フィールドは `equals` で「両方 nil」のショートサーキットがほぼ最速で済む。Cell の bind 頻度（数 ms に 1 回程度）ではプロファイル上有意な差は出ない想定。

### [Risk] `cellTitleFontSize` と `titleFont` の優先順位ロジックが利用者に分かりづらい
**Mitigation**: spec の Requirement に明文化（「`cellTitleFontSize` が非 nil なら、`titleFont` の size を `cellTitleFontSize` で上書きする」）。Theme コメントにも同記述を入れる。

### [Risk] Compose `FontFamily` の等価が破綻していて Theme の差分検出が誤動作する
**Mitigation**: fontFamily テストを Change 内で追加（Decision 4）。テストが失敗する場合は本 change 内で `Theme.equals` を `fontFamily` 内容比較に切り替えるサブタスクを追加する。

### [Risk] iOS / Android で API 名が微妙にずれる
**Mitigation**: spec を 1 つの change 内で両方同時に MODIFIED 化し、フィールド名リストを左右一致させる。iOS は camelCase、Android も Kotlin camelCase で命名は揃う。Native 型の違いは Native の慣習通り（`UIColor` vs `Color`、`UIFont` vs `TextStyle`、`CGFloat` vs `Dp`）。`iconSize` はオリジナル `Size` 型ではなく **一辺スカラー（`CGFloat` / `Dp`）に簡素化** 済（Decision 2 / 各 spec Requirement で明文化）で、2D 型は採用しない。

### [Trade-off] `Font` 集約 vs `fontSize` / `fontFamily` / `fontAttributes` 独立
集約を選んだことで「`fontFamily` だけ変えたい」利用者は `Theme.titleFont = currentFont.withFontFamily(...)` 相当の書き方が必要になる。代わりに Theme フィールドが半分以下に抑えられ、見通しが効く。Decision 2 の通り、`cellTitleFontSize` のみ並立を残してオリジナル運用との橋渡しをする。

## Migration Plan

本 change はアプリ内部の値型変更で、永続化形式やリモート API には影響しない。

**実装フェーズの順序**（tasks.md で細分化）:

1. design.md ベースで spec deltas を確定（本 change の specs フェーズで完了）。
2. iOS 側を先に実装（`Theme.swift` / `CellStyle.swift` / `EffectiveStyle.swift`）。
3. iOS Cell View（`LabelCellView` / `CommandCellView` 等）の bind 経路を `EffectiveStyle.effectiveXXX(...)` 経由に書き換え。
4. iOS テスト追加（解決順序、UIFont equals、rename 反映）。
5. Android 側を同様に実装。
6. Android Cell ViewHolder の bind 経路を書き換え。
7. Android テスト追加（解決順序、TextStyle equals、fontFamily 反映、rename 反映）。
8. `samples/ios` / `samples/android` を新 API に移行。
9. README / Theme コメント / CHANGELOG 更新。

**ロールバック戦略**: 本 change は単一 PR に収まる想定。実装フェーズ完了前に致命的問題が発覚した場合は PR を破棄し、proposal を保留して別アプローチを設計する。Archive 後の rollback は `git revert` でフィールドリネーム & 追加フィールドを巻き戻す（互換シムがないため、巻き戻し時には利用者も逆書き換えが必要になる点を CHANGELOG に明記）。

## Open Questions

- **`Theme` 等価判定の `fontFamily` 内容比較への切り替え**: Decision 4 のテストが失敗した場合、本 change で対応するか別 change に切り出すか。実装着手時に判断。失敗しなければ現状の `data class` 自動 equals のままで OK。

## Resolved Questions

- **`cellTitleFontSize` の単位**: 各 Native の慣習通りに解釈する方針で確定。iOS は `Double` → `CGFloat` (pt)、Android は `Double` → `sp` として扱い、**Native 層ではプラットフォーム間で見た目を揃える努力はしない**。Native 単体での利用者は各プラットフォームの慣習に従って数値を指定する責務を持つ。spec 上は型を `Double`（論理単位）と表現するに留め、Native 側の単位解釈はそれぞれの Native 慣習に委ねる。
- **MAUI 層での単位調整**: MAUI ラッパは本 change のスコープ外（in-progress な `add-maui-*` 系 change で別途扱う）。MAUI 経由で利用するとき「同じ数値で iOS / Android の見た目を揃えたい」要件は、MAUI Bridge 側で iOS pt と Android sp / dp の換算を行って吸収する想定（density 換算や pt ≒ sp の補正は Bridge 層の責務）。Native 層は **Native 慣習のスカラーをそのまま受け取る** ことに徹し、クロスプラットフォーム整合は Bridge 層に閉じ込める。`cellIconSize` / `cellIconRadius` の `CGFloat` ↔ `Dp` 変換も同様に Bridge 層の責務。
