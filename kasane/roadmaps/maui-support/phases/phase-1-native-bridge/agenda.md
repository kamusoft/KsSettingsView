# phase-1-native-bridge

Native (iOS Swift / Android Kotlin) の薄い Bridge 層と MAUI Binding csproj を新設し、C# から Native SettingsView を LabelCell 1種で疎通させる。

原案: `openspec/changes/add-maui-bridge` (凍結・参照のみ)

## 論点


## 決定事項

- **Bridge の位置づけ (2026-08-04)**: MAUI Bridge は Native 側に内部所有 `SettingsRootStore` を持つ **DSL 方式の類型**として位置づける。Bridge 公開 API は内部で Store の公開操作へ変換し、既存の `SettingsRootStore → Native Host` 収束経路に乗せる。旧案の直接 `applyDiff` (Store 迂回) は却下。Store handle の C# 公開 (Store 方式) は将来拡張として保留 → [maui/ADR-0001](../../../../decisions/maui/0001-maui-bridge-dsl-variant-internal-store.md)
- **Bridge API の形 (2026-08-04)**: union DTO (`KsSettingsRootDiffDTO`) をやめ、**Store 公開操作と 1:1 の12メソッド** (setRoot / 構造9種 / replaceCells / setTheme) にする。`replaceCells` は Bridge 内ループで誤魔化さず **iOS Store 本体へ公開操作として追加**し Android と対称化する (additive・非破壊)。iOS 側の実装+テストを本フェーズのスコープに含める → [maui/ADR-0002](../../../../decisions/maui/0002-bridge-api-per-store-operation.md)
- **ユーザー操作通知 (2026-08-04)**: 単一 `KsCellInteractionDelegate` / `Listener` 集約 (旧案踏襲)。Cell 種別はメソッド名で識別、C# 側は cellId → CellBase Dictionary で配送。Cell 追加時は delegate へ additive にメソッド追加 → [maui/ADR-0003](../../../../decisions/maui/0003-single-interaction-delegate.md)
- **EntryCell 高頻度更新 (2026-08-04)**: 旧案の `updateCellValue` 直行パス + 200ms debounce は**作らない**。updateCellValue は Store の公開操作に無く ADR-0002 (Store 操作 1:1) の例外経路になること、入力は人間のタイプ速度で interop コストが無視できること、debounce は 200ms の取りこぼし窓を作り UX を壊すことが理由。Native 由来のエコーは MAUI 層 (CellBase) の同値チェックで抑止する (実装は phase-2 の論点へ引き継ぎ)。実測で問題が出たら Store 方式 (ADR-0001 で保留) と合わせて再検討
- **Theme / CellStyle の型 (2026-08-04)**: `setTheme` は Store `applyTheme` へ素通し (同値スキップ・Diff 不発行は Store 保証)。C# 公開 API は **MAUI 慣例型** (`Microsoft.Maui.Graphics.Color` 等)、interop DTO は非公開の輸送表現として ARGB 等へ marshalling。platform 固有項目は接頭辞付き nullable プロパティで持ち対象外 OS では無視 → [maui/ADR-0004](../../../../decisions/maui/0004-maui-idiomatic-types-for-styling.md)
- **Binding 形式と .NET 10 検証 (2026-08-04)**: Binding は旧案どおり Native Library Interop (`XcodeProject` / `AndroidGradleProject`) を踏襲。net10.0 での toolchain 動作確認は議論では結論が出ない性質のため、phase-1 実装の**先頭 spike タスク** (最小スケルトンのビルド疎通) とする。spike で問題が出たらこの agenda に戻す
- **疎通範囲 (2026-08-04)**: LabelCell 1種のみ実装で Bridge→Store→Host を縦に疎通 (旧案踏襲)。ただし旧案の「残り Cell のインターフェース先行定義」は**やめる** — CustomCell (phase-5 で全面再検討) や Picker 系 (phase-4 の選択面論点でシグネチャが動く) の推測定義は腐るだけで、ADR-0002/0003 の additive リズムとも一貫しないため。phase-1 の Bridge 表面は「12メソッド + `addLabelCell` + delegate の LabelCell ぶん」のミニマル構成

## TODO

- [x] 論点の解消 (2026-08-04 全7論点を決定事項へ昇格)
- [ ] spec 化の際、binding テンプレートの net10.0 ビルド疎通 spike をタスク先頭に置く
- [x] ksn-propose で変更提案を起こす (2026-08-04 [add-maui-native-bridge](../../../../changes/archive/2026-08-05-add-maui-native-bridge/proposal.md)。spec-review 2ラウンド + maui/ADR-0005 起票済み)

## 実装結果 (2026-08-05 反映)

- [add-maui-native-bridge](../../../../changes/archive/2026-08-05-add-maui-native-bridge/proposal.md) として実装完了 (verify-001 VALID / review-003 APPROVED)。spike の成功ゲート4点 (native artifact / binding assembly / C# compile / 最小アプリ起動) は両 OS とも通過し、agenda への差し戻しなし
- deviation 1件: Android の Binding csproj は `AndroidGradleProject` ではなく **gradlew Exec 方式** (SDK init script の buildDirectory 束ねを実測確認、オーナー承認済み → [maui/ADR-0006](../../../../decisions/maui/0006-android-binding-gradlew-exec.md))。iOS は「SDK 制約」主張が並走調査で反証され標準 `XcodeProject` 方式へ復帰
- 契約の追加確定: `replaceSection` / `replaceCell` は置換後の有効 ID を nullable で返す (渡した DTO 自身の ID は破棄)。「返された ID だけを使う」規則を API 形状で担保する形に review サイクルで確定
- delegate (ユーザー操作通知) は決定どおり未実装のまま phase-4 (最初の対話型 Cell) へ
- 実装中の派生起票 5件: 既存バグ2件 ([fix-replace-section-header-refresh](../../../../changes/fix-replace-section-header-refresh/exploration.md) / [fix-android-accessory-header-refresh](../../../../changes/fix-android-accessory-header-refresh/exploration.md) — ロードマップ外の独立 S。修正後に Bridge 契約表の「header text 不変」回避を戻す followup を含む)、契約論点3件 (phase-2 / phase-4 の agenda へ転記済み)
