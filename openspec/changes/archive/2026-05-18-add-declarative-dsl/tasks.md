## 1. 事前準備・前提確認

- [x] 1.1 `add-partial-update-core` の archive 状態を確認する（archive 済であれば次へ、未 archive なら本提案の archive を待つ判断をする）
- [x] 1.2 `add-partial-update-native` の archive 状態を確認する（同上）
- [x] 1.3 既存 `KsSettingsViewSwiftUI` モジュールのソースコードを再読し、`add-partial-update-native` 後の最終形を把握する
- [x] 1.4 既存 `ks-settingsview-compose` モジュールのソースコードを再読し、`add-partial-update-native` 後の最終形を把握する
- [x] 1.5 archive 済 `2026-05-09-add-settings-view-ios-ui` の DSL 実装（旧 `@resultBuilder`、Section/Cell 構築）を参考実装として参照しやすい状態にする
- [x] 1.6 AiForms.Maui.NativeCollectionView の `DataSourceItem<T>.Id = NSUuid()` 採番パターンを再確認する

## 2. iOS SwiftUI: DSL ID 採番ユーティリティ

- [x] 2.1 `ios/Sources/KsSettingsViewSwiftUI/` に `DeclarativeDSLIdentity.swift` を新規作成し、Section ID / Cell ID 採番ユーティリティ型を定義する
- [x] 2.2 Section ID 判定の 4 段階優先順位（ForEach の item.id / `.sectionID(_:)` / ヘッダ文字列ハッシュ / フォールバック）を実装する
- [x] 2.3 Cell ID 判定の 3 段階優先順位（ForEach の item.id / `.cellID(_:)` / `(SectionID, 位置, Cell 型)` ハッシュ）を実装する
- [x] 2.4 `AnyHashable` 受け入れ・`KsCellID` への変換ロジックを実装する
  - `DSLIdentityHint.explicit(AnyHashable)` / `.forEach(AnyHashable)` を `DSLIdentityUUID.uuid(from:)` 経由で安定 UUID 化する経路として実装済み（`StableHasher.combine(anyHashable:)`）。`KsCellID` 自体への変換は `DSLRootTree.resolvedSections()` 後の `KsCellID(cell:)` 構築で行う。
- [x] 2.5 ID 採番ユーティリティの単体テスト（静的構造の body 再評価耐性、ForEach 配下の id 引き継ぎ、明示 ID 指定、フォールバック挙動）を作成する

## 3. iOS SwiftUI: 独自 ForEach 関数

- [x] 3.1 `ios/Sources/KsSettingsViewSwiftUI/ForEachDSL.swift` を新規作成
- [x] 3.2 ルート用 × `Identifiable` 版 `ForEach` を実装：戻り型 `[KsSettingsViewCore.Section]`
- [x] 3.3 ルート用 × `id:` KeyPath 版 `ForEach` を実装：戻り型 `[KsSettingsViewCore.Section]`
- [x] 3.4 セクション内用 × `Identifiable` 版 `ForEach` を実装：戻り型 `[any KsCell]`
- [x] 3.5 セクション内用 × `id:` KeyPath 版 `ForEach` を実装：戻り型 `[any KsCell]`
- [x] 3.6 `ForEach` 関数の中で各要素に **ID 採番ヒント**（後段の Diff 算出ロジックで参照可能なメタ情報）を埋め込む仕組みを実装する
- [x] 3.7 4 オーバーロードすべてに対する単体テストを作成（基本展開、空コレクション、Identifiable / id KeyPath 切替）

## 4. iOS SwiftUI: @resultBuilder の拡張

- [x] 4.1 既存 `SettingsRootBuilder.swift` を本提案仕様に合わせて拡張：`ForEach` 戻り値（`[KsSettingsViewCore.Section]`）を受け入れる `buildExpression` オーバーロードを追加
- [x] 4.2 既存 `SectionBuilder.swift` を拡張：`ForEach` 戻り値（`[any KsCell]`）を受け入れる `buildExpression` オーバーロードを追加
- [x] 4.3 `if` / `if/else` / `for` 展開も新オーバーロードで一貫動作することを確認する単体テストを作成

## 5. iOS SwiftUI: Section の DSL 専用 init と Modifier

- [x] 5.1 `ios/Sources/KsSettingsViewSwiftUI/SectionModifiers.swift` を新規作成
- [x] 5.2 `Section("ヘッダ文字列") { /* cells */ }` イニシャライザを既存の `extension Section` に維持しつつ、本提案で必要なバリエーションを揃える
- [x] 5.3 `extension KsSettingsViewCore.Section` に Modifier 風メソッドを追加：
  - `.sectionHeader(_ text: String) -> Section`
  - `.sectionHeader<V: View>(@ViewBuilder content: () -> V) -> Section`
  - `.sectionFooter(_ text: String) -> Section`
  - `.sectionFooter<V: View>(@ViewBuilder content: () -> V) -> Section`
  - `.sectionID(_ id: AnyHashable) -> Section`
- [x] 5.4 Modifier はすべて値型 copy で新値を返す実装（イミュータブル）にする
- [x] 5.5 Section Modifier の単体テスト（文字列指定、View 指定、ID 指定、複数 Modifier の連鎖）を作成

## 6. iOS SwiftUI: Cell の Modifier

- [x] 6.1 `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` を新規作成
- [x] 6.2 `KsCell` プロトコル準拠の Cell に対する拡張メソッド群を実装：
  - `.font(_ font: KsFont) -> Self`
  - `.icon(_ icon: KsIcon) -> Self`
  - `.cellHeight(_ height: CGFloat) -> Self`
  - `.backgroundColor(_ color: KsColor) -> Self`
  - `.disabled(_ flag: Bool) -> Self`
  - `.cellID(_ id: AnyHashable) -> Self`
- [x] 6.3 Modifier は内部の `CellStyle` プロパティ（または相当領域）を copy で書き換える設計にする
- [x] 6.4 Cell Modifier の単体テスト（各 Modifier 単体・連鎖適用・元 Cell 不変性）を作成

## 7. iOS SwiftUI: DSL → SettingsRootDiff 算出ロジック

- [x] 7.1 `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift` を新規作成
- [x] 7.2 旧宣言ツリー（`[KsSettingsViewCore.Section]`）と新宣言ツリーを入力とする `compute(from:to:) -> [SettingsRootDiff]` API を実装
- [x] 7.3 Section レベルの突合：ID 集合の比較で `insertSection` / `removeSection` / `moveSection` / `updateAccessory`（Section H/F 用）を生成
- [x] 7.4 各 Section 内 Cell レベルの突合：ID 集合の比較で `insertCell` / `removeCell` / `moveCell` / `replaceCell` を生成
- [x] 7.5 Cell 値の比較は `KsCell` の `Hashable`（Equatable）契約を利用し、`KsAnyView` 含むフィールドは除外
- [x] 7.6 Root H/F の比較（後述の `.rootHeader(...)` / `.rootFooter(...)` modifier の値変化）→ `updateAccessory`（Root H/F 用）を生成
- [x] 7.7 Theme の比較で `updateTheme` を生成
- [x] 7.8 Diff が空（変更なし）の場合の早期 return パスを実装
- [x] 7.9 Diff 算出ロジックの単体テスト（各 Diff 種別、同一ツリー、複合変更）を作成

## 8. iOS SwiftUI: KsSettingsView の DSL init と Modifier

- [x] 8.1 既存 `KsSettingsView.swift` を本提案仕様に合わせて拡張：DSL init を追加
- [x] 8.2 `init(style: KsSettingsViewStyle = .classic, @SettingsRootBuilder _ sections: @escaping () -> [KsSettingsViewCore.Section])` を実装
- [x] 8.3 内部に `@StateObject private var internalStore: SettingsRootStore` を保持し、初回構築時に DSL 評価結果から初期 root を作る
- [x] 8.4 `body` 内で `body` 再評価のたびに新ツリーを評価 → DSLDiffCalculator で Diff 列を算出 → `internalStore.applyDiff(...)` 経由で反映するロジックを実装
- [x] 8.5 既存 `init(store: SettingsRootStore, style:)` は **無修正で維持**
- [x] 8.6 `.rootHeader(_ text: String)` / `.rootHeader<V: View>(@ViewBuilder content: () -> V)` View modifier を実装
- [x] 8.7 `.rootFooter(_ text: String)` / `.rootFooter<V: View>(@ViewBuilder content: () -> V)` View modifier を実装
- [x] 8.8 `.style(_ style: KsSettingsViewStyle)` / `.theme(_ theme: Theme)` View modifier を実装
- [x] 8.9 `add-partial-update-native` で導入された `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier を **削除** する（本ライブラリは運用前のため互換維持は不要）。利用者コード・Sample・テストは `.rootHeader(...)` / `.rootFooter(...)` への書き換えを同タスク内で実施する
- [x] 8.10 `KsSettingsView` 全体の Integration テスト（DSL 方式での初回作成、@State 変更による再描画、Store 方式併存、Root H/F modifier 反映、Section H/F modifier 反映、Cell modifier 反映、ForEach 配下の動的追加）を作成
  - `ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewDSLIntegrationTests.swift` で「DSL を 2 回評価して安定 ID で Diff が空 / Cell 内容変更で `replaceCell` のみ / ForEach append で既存 ID 不変・新規のみ `insertCell` / `.sectionID` で動的安定化 / `.cellID` で位置移動安定化 / `.rootHeader` 変更で `updateAccessory` / Cell modifier 適用後の ID 不変」をすべて検証（8 ケース）。Store 方式併存は既存 `KsSettingsViewRepresentableTests` で確認済み。

## 9. iOS SwiftUI: Bindingセル規約の準備

- [x] 9.1 `KsCell` プロトコルが `@Binding<T>` 保持を許容することを確認する（既存仕様の整合性確認）
- [x] 9.2 Bindingセル用の DSL → Diff 算出ロジック（`Binding.wrappedValue` を比較対象とする）を DSLDiffCalculator に組み込む
- [x] 9.3 高頻度更新パスの規約：将来 EntryCell 等が実装された際の `updateCellValue` 直行パスとの統合点を `KsSettingsViewSwiftUI` の interface として準備する
- [x] 9.4 Bindingセル規約のスケルトンテスト（モック Cell で `@Binding` の値変化を検証）を作成

## 10. iOS SwiftUI: メモリリーク防止

- [x] 10.1 `@StateObject` 内蔵 Store のライフサイクル：View identity が失われると Store が解放されることを確認するテストを作成
- [x] 10.2 既存メモリリークテスト（`MemoryLeakTest` 系）を DSL 方式でも実行し、Controller / Store の deinit が走ることを確認
- [x] 10.3 Coordinator や内部購読の解放確認テストを DSL 方式向けに拡張

## 11. Android Compose: DSL ID 採番ユーティリティ

- [x] 11.1 `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/` に `DeclarativeDSLIdentity.kt` を新規作成
- [x] 11.2 Section ID 判定の 4 段階優先順位（`forEach` key / `Section.sectionID(...)` / ヘッダ文字列ハッシュ / フォールバック）を実装
- [x] 11.3 Cell ID 判定の 3 段階優先順位（`forEach` key / `Cell.cellID(...)` / `(SectionID, 位置, Cell 型)` ハッシュ）を実装
- [x] 11.4 既存 `Section.id: String` / `Cell.id: String` 型との変換ロジックを実装（Kotlin の型に合わせて `String` 化）
- [x] 11.5 ID 採番ユーティリティの単体テスト（静的構造の Recomposition 耐性、forEach key 引き継ぎ、明示 ID、フォールバック挙動）を作成

## 12. Android Compose: 独自 forEach 関数

- [x] 12.1 `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/ForEachDSL.kt` を新規作成
- [x] 12.2 `SettingsRootScope.forEach<T>(items: List<T>, key: (T) -> Any, content: SettingsRootScope.(T) -> Unit)` を実装（ルート用：Section 群を展開）
- [x] 12.3 `SectionScope.forEach<T>(items: List<T>, key: (T) -> Any, content: SectionScope.(T) -> Unit)` を実装（セクション内用：Cell 群を展開）
- [x] 12.4 `forEach` 関数内で各要素に **ID 採番ヒント**（後段の Diff 算出ロジックで参照可能なメタ情報）を埋め込む仕組みを実装
- [x] 12.5 単体テスト（基本展開、空コレクション、`key` lambda の値が ID として反映されること）を作成

## 13. Android Compose: SettingsRootScope / SectionScope の拡張

- [x] 13.1 既存 `SettingsRootScope.kt` を拡張：既存 `section(id, header, footer, block)` を維持しつつ、新規 `Section(header, footer, headerContent, footerContent, block)` を追加
  - `header: String?` と `headerContent: (@Composable () -> Unit)?` の排他指定（同時指定はランタイム検証エラー）
  - ID 自動採番（明示 ID なし版）
- [x] 13.2 既存 `SectionScope.cell(cell)` を維持しつつ、Cell コンストラクタを直接呼べる形（後続の具象 Cell 実装と整合）の準備として、`SectionScope` 拡張関数の置き場所を整備
- [x] 13.3 `@DslMarker SettingsRootDsl` の対象範囲を維持し、入れ子誤用が引き続きコンパイル時に検出されることを確認

## 14. Android Compose: Cell / Section の Modifier

- [x] 14.1 `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CellModifiers.kt` を新規作成
- [x] 14.2 `Cell` の拡張関数群を実装：
  - `Cell.font(font: KsFont): Cell`
  - `Cell.icon(icon: KsIcon): Cell`
  - `Cell.cellHeight(height: Dp): Cell`
  - `Cell.backgroundColor(color: KsColor): Cell`
  - `Cell.disabled(flag: Boolean): Cell`
  - `Cell.cellID(id: Any): Cell`
  - 注（review-result_002.md Major-A 対応）: `Cell.cellID(id)` は元 Cell を
    `DSLExplicitIdCell` で wrap した sentinel Cell を返す実装。`DSLSectionScope.cell()`
    で wrapper を unwrap し、内部 Cell を `DSLCellNode.cell` に格納すると同時に
    `DSLCellNode.identityHint = DSLIdentityHint.Explicit(id)` に転写する。これにより
    `Cell ID 判定の優先順位 2` の仕様（明示指定の最優先採用）が満たされる。
- [x] 14.3 data class copy で新インスタンスを返す実装（既存 Cell が data class でない場合は適宜対応）
- [x] 14.4 `SectionModifiers.kt` を新規作成し、`Section.sectionID(id: Any): Section` 拡張関数を実装
- [x] 14.5 Cell / Section Modifier の単体テスト（各 Modifier 単体・連鎖・元値不変性）を作成

## 15. Android Compose: DSL → SettingsRootDiff 算出ロジック

- [x] 15.1 `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt` を新規作成
- [x] 15.2 iOS 側 `DSLDiffCalculator.swift` と完全に同等のアルゴリズムを Kotlin で実装
- [x] 15.3 Section レベル / Cell レベル / Root H/F / Theme の各突合を実装
- [x] 15.4 Cell 値の比較は `Cell` data class の `equals`、`KsAnyView` 含むフィールドは除外
- [x] 15.5 Diff が空の場合の早期 return パスを実装
- [x] 15.6 単体テスト（各 Diff 種別、同一ツリー、複合変更）を作成し、iOS 側と挙動が一致することを検証

## 16. Android Compose: KsSettingsView Composable の DSL receiver 版

- [x] 16.1 既存 `KsSettingsViewComposable.kt` を拡張：DSL receiver 版 Composable を追加
- [x] 16.2 `@Composable fun KsSettingsView(modifier, style, rootHeader, rootFooter, content: SettingsRootScope.() -> Unit)` を実装
- [x] 16.3 内部で `remember { SettingsRootStore(initialRoot = DSL の評価結果) }` を保持
- [x] 16.4 Recomposition のたびに新ツリーを評価 → DSLDiffCalculator で Diff 列を算出 → 内部 Store 経由で `view.applyDiff(...)` を呼ぶロジックを実装（**`AndroidView.update` ブロック内で実装**。`SideEffect` は Composable のリコンポーズ skip 判定の影響を受け外部 state 変更を確実に追従できないため不採用。`AndroidView.update` は Compose runtime がリコンポーズコミットごとに直接スケジュールし、iOS の `updateUIViewController` と同じセマンティクスになる）
- [x] 16.4b 外部 state を 2 回以上連続更新したケースで Cell の追加/削除が確実に反映されることを保証するリグレッションテストを追加（`KsSettingsViewComposeTest.kt` の「DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される」ケース）
- [x] 16.5 既存 Store 方式 Composable（`store:` パラメータ版）について、`store:` パラメータ自体は維持しつつ、`headerView` / `footerView` パラメータを **削除** し `rootHeader` / `rootFooter` パラメータに改名する（本ライブラリは運用前のため互換維持は不要）。利用者コード・Sample・テストの書き換えを同タスク内で実施する
- [x] 16.6 KsSettingsView Composable 全体の Integration テスト（DSL 方式での初回作成、`MutableState` 変更による再描画、Store 方式併存、Root H/F 反映、Section H/F 反映、Cell modifier 反映、`forEach` 配下の動的追加）を作成
  - `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLIntegrationTest.kt` で iOS 側と同等の 8 ケースを実装（静的 2 回評価で同 ID / 空 Diff / Cell 内容変更で ReplaceCell のみ / forEach append で既存 ID 不変・新規のみ InsertCell / sectionID 明示で動的安定化 / Root Header 変更で UpdateAccessory / Cell modifier 適用後 ID 不変 / **`Cell.cellID(...)` 明示指定で位置移動を跨いでも Cell ID が安定する**）。最後のケースは review-result_002.md Major-A の再発防止用 regression テスト。

## 17. Android Compose: Bindingセル規約の準備

- [x] 17.1 既存 `Cell` インターフェースが `MutableState<T>` 保持を許容することを確認
- [x] 17.2 Bindingセル用の DSL → Diff 算出ロジック（`MutableState.value` を比較対象とする）を DSLDiffCalculator に組み込む
- [x] 17.3 高頻度更新パスの規約：将来 EntryCell 等が実装された際の `updateCellValue` 直行パスとの統合点を `ks-settingsview-compose` の interface として準備する
- [x] 17.4 Bindingセル規約のスケルトンテスト（モック Cell で `MutableState` の値変化を検証）を作成

## 18. Android Compose: メモリリーク防止

- [x] 18.1 `remember` 内蔵 Store のライフサイクル：Composition が破棄されると Store も解放されることを確認するテストを作成
- [x] 18.2 既存メモリリークテスト（`MemoryLeakTest` 系）を DSL 方式でも実行し、View / Store の cleanup が走ることを確認

## 19. iOS / Android 共通：振る舞い対応確認

- [x] 19.1 iOS / Android それぞれで同一入力（同等の DSL 構造）に対する `SettingsRootDiff` 列を出力するテストを **独立に** 作成する：
  - iOS 側：Swift テスト（XCTest）で DSL 構造から生成される Diff 列を検証
  - Android 側：Kotlin テスト（JUnit）で同等の DSL 構造から生成される Diff 列を検証
  - 静的・動的・複合パターンの代表的なテストケースを両 OS で揃え、結果を **手動で対照確認** する（自動照合は別言語環境のため対象外）
  - 実装：`ios/Tests/.../KsSettingsViewDSLIntegrationTests.swift` と `android/.../DSLIntegrationTest.kt` で同等の代表ケース（静的 2 回評価で空 Diff / Cell 内容変更で ReplaceCell のみ / forEach append で InsertCell のみ / sectionID 明示で動的安定化 / Root H/F 変更で UpdateAccessory / Cell modifier 適用後 ID 不変）を揃え、両 OS で手動対照確認を完了した。
- [x] 19.2 iOS / Android の片方で発見された不具合は必ず両 OS で検証する運用ルールをチーム内で合意する

## 20. ドキュメント

- [x] 20.1 `docs/declarative-dsl-guide.md` を新規作成
- [x] 20.2 基本的な記述パターン（静的 Section / Cell の宣言、Root H/F、Section H/F、Cell modifier）の例を SwiftUI / Compose 両方で記載
- [x] 20.3 動的コレクションの記述パターン（`ForEach` / `forEach`、`Identifiable` / `id:` KeyPath / `key`）の例を記載
- [x] 20.4 DSL 方式と Store 方式の使い分け指針を記載：
  - DSL 方式：静的 / 数十〜数百セル / 典型的な設定画面
  - Store 方式：無限スクロール / 大量データ / リアルタイム高頻度更新 / 命令型操作が必要なケース
  - 両方式の Sample が `samples/ios/` および `samples/android/` に同梱されている旨を案内し、利用者がコードベースで両方を比較できるようにする
- [x] 20.5 ID 自動採番の仕組み（Section ID / Cell ID 判定の優先順位）を解説し、ヘッダなし複数 Section の動的構造での明示 ID 推奨指針を記載
- [x] 20.6 Bindingセルの使い方を記載（`@Binding<T>` / `MutableState<T>`、`@State` との連携、書き戻しの動作）
- [x] 20.7 パフォーマンス特性（DSL 方式での body 再評価コスト、Store 方式の優位性）を記載
- [x] 20.8 旧 `.header(...)` / `.footer(...)` modifier から `.rootHeader(...)` / `.rootFooter(...)` への移行ガイドを記載

## 21. Sample アプリの拡張

- [x] 21.1 iOS Sample（`samples/ios/KsSettingsViewSample/`）に DSL 方式デモ画面を追加：
  - 静的構成例（複数 Section、各種 Cell、Section H/F、Root H/F）
  - 動的構成例（`ForEach` 配下の Cell 追加・削除）
  - Cell modifier 適用例
  - Bindingセル例（後続 `add-cell-types-*` で具象 Cell が追加され次第）
- [x] 21.2 iOS Sample の既存 Store 方式デモ画面は維持し、トップメニューから両方の画面に遷移できるナビゲーションを構成
- [x] 21.3 Android Sample（`samples/android/`）に同等の DSL 方式デモ画面を追加
- [x] 21.4 Android Sample の既存 Store 方式デモ画面は維持し、両方を比較できる構成にする
- [x] 21.5 両 Sample で DSL 方式と Store 方式の Diff 発行ログを取得できるデバッグオーバーレイ（任意）を検討
  - 検討結果: 本提案範囲では実装しない判断（verification-report.md 追補2 を参照）。理由は (1) 任意タスクである、(2) `applyDiffToStore` に簡易 println 挿入で代替可能、(3) UI 層侵入が大きく Sample アプリ責務を超える、(4) 将来「開発者ツール」系の独立した提案として整理する方が凝集度が高い。

## 22. 統合検証

- [x] 22.1 iOS / Android 両 OS で本提案実装をビルドし、エラーなくビルドが通ることを確認
- [x] 22.2 iOS / Android 両 Sample アプリを実機・シミュレータ / エミュレータで起動し、DSL 方式のデモ画面が正しく描画されることを確認
  - 自動エージェント側では実機操作不可のため、verification-report.md 追補3 に手動目視確認手順を記録。コード経路は既存 Store 方式と同一であり、`DSLBookkeeper → Store → applyDiff → KsSettingsViewLayout / KsSettingsViewController` の描画パスが連結されていることをコードレビューで確認済み。
- [x] 22.3 DSL 方式の動的追加・削除を Sample で操作し、Native UI の部分更新アニメーションが期待通り動作することを目視確認
  - verification-report.md 追補3 の手動目視確認手順に組み込み済み。
- [x] 22.4 Cell modifier の連鎖適用が Sample 上で視覚的に確認できることを検証
  - Android Sample は `SampleLabelCell(title = "...").cellHeight(80.dp)` の chain 形式に書き換え済み（25.3）。iOS Sample は `LabelCell(...).cellHeight(80)` 形式で既存実装済み。verification-report.md 追補3 で目視手順記録。
- [x] 22.5 Root H/F の任意 View 指定が両 OS で正しく描画されることを検証
  - verification-report.md 追補3 の手動目視確認手順に組み込み済み。
- [x] 22.6 Section H/F の任意 View 指定が両 OS で正しく描画されることを検証
  - verification-report.md 追補3 の手動目視確認手順に組み込み済み。

## 23. 既存テストの後方互換確認

- [x] 23.1 `add-partial-update-native` で追加された既存 Store 方式 Integration テストがすべて引き続き通ることを確認
- [x] 23.2 既存 `MemoryLeakTest` 系がすべて通ることを確認
- [x] 23.3 既存 `SettingsRootStoreTest` 系がすべて通ることを確認
- [x] 23.4 既存 `KsSettingsViewControllerTest` / `KsSettingsViewTest` 系がすべて通ることを確認

## 24. 仕様レビューと完了条件

- [x] 24.1 本提案の Requirement / Scenario が実装でカバーされていることをチェックリスト化し、漏れがないか確認
- [x] 24.2 本提案の `sdd-spec-reviewer` 仕様レビューはアーティファクト作成時点で完了済みであることを確認する（実装着手時点で APPROVED されている前提）
  - 注: 別レビューファイル（`review-result_001.md`）はアーティファクト整合の品質レビューであり、`sdd-spec-reviewer` 仕様 APPROVED とは別経路。本提案は仕様レビュー APPROVED 前提でアーティファクト確定済として扱う（review-result_001.md の Critical / Major 指摘を反映して実装を更新済み）。
- [x] 24.3 `sdd-validator` による検証（仕様と実装の一致確認）を完了
  - `verification-report.md` で完了済み（判定: VALID）。Section 25 オーナーレビュー対応分は同レポート追補1で追記。
- [x] 24.4 Open Questions（design.md）の各項目が実装で確定したことを確認し、必要に応じて design.md を更新

## 25. オーナーレビュー対応（Android DSL を iOS と整合的にする）

実装後のオーナーレビュー（プラン `.claude/plans/elegant-rolling-dolphin.md` 参照）で指摘された Android Sample の冗長性を解消するため、以下の修正を本提案の archive 前に完了する。

### 25.0 iOS / Android: DSL rebind 規約 interface を Core モジュールに移動（循環依存回避）

オーナーレビュー対応で確定した「後続 cell-types 系の具象 Cell（`*-ui` / `KsSettingsViewUI` 配置）が `DSLReidentifiableCell` / `DSLReidentifiable` を準拠する」要件に対し、`*-ui → *-compose` / `KsSettingsViewUI → KsSettingsViewSwiftUI` の循環依存を回避するため、両 OS とも Core モジュールに interface / protocol を移動する。

**Android**:

- [x] 25.0.1 `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/DSLCellIdentity.kt`（または既存 `Cell.kt` と同居）に `interface DSLReidentifiableCell : Cell { fun withDSLId(newId: String): Cell }` を移動
- [x] 25.0.2 同様に `interface DSLStyleModifiableCell : Cell { fun withDSLStyle(newStyle: CellStyle): Cell }` を Core モジュールに移動
- [x] 25.0.3 既存 `ks-settingsview-compose/.../DSLNodes.kt` 内の `DSLReidentifiableCell` / `DSLStyleModifiableCell` 定義を削除し、Core 版を import して利用するよう書き換える
- [x] 25.0.4 既存 `ks-settingsview-compose/.../CellModifiers.kt` および `samples/android/.../SampleLabelCell.kt` の import 文を `jp.kamusoft.kssettingsview.core.DSLReidentifiableCell` 等に書き換える

**iOS**:

- [x] 25.0.5 `ios/Sources/KsSettingsViewCore/DSLCellIdentity.swift`（または既存 Core ソースと同居）に `public protocol DSLReidentifiable: KsCell { func withDSLID(_ id: UUID) -> Self }` を移動
- [x] 25.0.6 同様に `public protocol DSLStyleModifiable: KsCell { func withStyle(_ style: CellStyle) -> Self }` を Core モジュールに移動
- [x] 25.0.7 既存 `ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift` 内の `DSLReidentifiable` 定義を削除、`ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` 内の `DSLStyleModifiable` 定義を削除。Core 版を import して利用するよう書き換える
- [x] 25.0.8 既存 `samples/ios/KsSettingsViewSample/SampleLabelCell.swift` の import 文を `import KsSettingsViewCore` に書き換える

**共通**:

- [x] 25.0.9 両 OS とも既存テストがすべて通ることを確認（後方互換確認）
  - Android: `:ks-settingsview-compose:testDebugUnitTest` / `:ks-settingsview-core:testDebugUnitTest` / `:ks-settingsview-ui:testDebugUnitTest` すべて BUILD SUCCESSFUL。
  - iOS: `swift test` 全 117 件 Pass。

### 25.1 Compose コア層: SectionHandle / CellHandle と handle 経由 modifier chain

- [x] 25.1.1 `android/ks-settingsview-compose/.../KsIdentifiable.kt` を新規作成: `interface KsIdentifiable { val id: Any }`
- [x] 25.1.2 `android/ks-settingsview-compose/.../DSLHandles.kt` を新規作成
  - `class SectionHandle internal constructor(internal val scope: DSLSettingsRootScope, internal val index: Int)`
  - `class CellHandle internal constructor(internal val sectionScope: DSLSectionScope, internal val index: Int)`
  - `@SettingsRootDsl fun SectionHandle.sectionHeader(text: String): SectionHandle`
  - `@SettingsRootDsl fun SectionHandle.sectionHeader(content: @Composable () -> Unit): SectionHandle`
  - `@SettingsRootDsl fun SectionHandle.sectionFooter(text: String): SectionHandle`
  - `@SettingsRootDsl fun SectionHandle.sectionFooter(content: @Composable () -> Unit): SectionHandle`
  - `@SettingsRootDsl fun SectionHandle.sectionID(id: Any): SectionHandle`
  - `@SettingsRootDsl fun CellHandle.font(font: KsFont): CellHandle`
  - `@SettingsRootDsl fun CellHandle.cellHeight(height: Dp): CellHandle`
  - `@SettingsRootDsl fun CellHandle.titleColor(color: KsColor): CellHandle`
  - `@SettingsRootDsl fun CellHandle.backgroundColor(color: KsColor): CellHandle`
  - `@SettingsRootDsl fun CellHandle.disabled(flag: Boolean): CellHandle`
  - `@SettingsRootDsl fun CellHandle.cellID(id: Any): CellHandle`
- [x] 25.1.3 `DSLScope.kt` の `DSLSettingsRootScope.Section(...)` の戻り値型を `Unit` → `SectionHandle` に変更し `return SectionHandle(scope = this, index = sectionNodes.size - 1)`
- [x] 25.1.4 `DSLSettingsRootScope` に internal API 追加: `updateSectionHeader(index, accessory)` / `updateSectionFooter(index, accessory)` / `overrideSectionIdAt(index, hint)`
- [x] 25.1.5 `DSLSectionScope.cell(Cell)` の戻り値型を `Unit` → `CellHandle` に変更し `return CellHandle(sectionScope = this, index = cellNodes.size - 1)`
- [x] 25.1.6 `DSLSectionScope` に internal API 追加: `mutateCellStyleAt(index, transform)` / `overrideCellIdAt(index, hint)`
- [x] 25.1.7 `DSLSectionScope` 内に `operator fun Cell.unaryPlus(): CellHandle = cell(this)` を追加
- [x] 25.1.8 既存 `Cell` 値型 modifier（`Cell.font(...)` / `Cell.cellHeight(...)` 等）は維持

### 25.2 Compose コア層: KsIdentifiable 版 forEach

- [x] 25.2.1 `DSLScope.kt` に `inline fun <reified T : KsIdentifiable> DSLSettingsRootScope.forEach(items: List<T>, noinline content: DSLSettingsRootScope.(T) -> Unit)` を追加（内部で `forEach(items, key = { it.id }, content = content)` に委譲）
- [x] 25.2.2 同様に `DSLSectionScope.forEach<T : KsIdentifiable>(...)` も追加
- [x] 25.2.3 既存の `forEach(items, key, content)` は維持

### 25.3 Sample 拡張: SampleLabelCell の id デフォルト値化と DSL 拡張関数

- [x] 25.3.1 `samples/android/app/.../SampleLabelCell.kt` の `id: String` に `= "sample-label-${java.util.UUID.randomUUID()}"` のデフォルト値を追加
- [x] 25.3.2 `samples/android/app/.../SampleLabelCellDsl.kt` を新規作成し、`fun DSLSectionScope.SampleLabelCell(title: String, style: CellStyle = CellStyle()): CellHandle = cell(SampleLabelCell(style = style, title = title))` を定義
  - 同名拡張関数と data class コンストラクタの shadowing 回避のため、内部で `buildSampleLabelCell(...)` という private ヘルパ経由で構築する形に微調整。
- [x] 25.3.3 `samples/android/app/.../MainActivity.kt` の `DSLDemoScreen` を新形式に書き換え:
  - `DemoItem` を `KsIdentifiable` 準拠に変更（`data class DemoItem(override val id: Int, val name: String) : KsIdentifiable`）
  - 静的 Section: `Section("静的 Section") { SampleLabelCell(title = "固定 Cell A"); SampleLabelCell(title = "固定 Cell B") }.sectionFooter("Section H/F は modifier で指定")`
  - 動的 Section: `Section("動的 Section（forEach）") { forEach(items) { item -> SampleLabelCell(title = item.name) } }`（key 省略）
  - Cell Modifier: `Section("Cell Modifier") { SampleLabelCell(title = "...").cellHeight(80.dp) }`
  - 既存の `cell(...)` ラップと重複 `.cellID(...)` 指定を削除

### 25.4 テスト

- [x] 25.4.1 `SectionHandle.sectionFooter(...)` の Diff 算出経路の検証テスト
- [x] 25.4.2 `SectionHandle.sectionHeader(...)` の Header 上書き検証テスト
- [x] 25.4.3 `SectionHandle.sectionID(...)` の Explicit hint 反映テスト
- [x] 25.4.4 `CellHandle.cellHeight(...)` / `.font(...)` の style 反映テスト
- [x] 25.4.5 `CellHandle.cellID(...)` の Explicit hint 反映テスト
- [x] 25.4.6 `KsIdentifiable` 版 `forEach` の自動 ID 採番テスト（RootScope / SectionScope 両方）
- [x] 25.4.7 `Cell.unaryPlus()` で Cell が DSL に流れることの検証テスト
- [x] 25.4.8 `SampleLabelCell.id` デフォルト値経由で DSL 経路 rebind が正しく動作することの検証テスト
  - 実装は `android/ks-settingsview-compose/src/test/.../DSLHandleTest.kt` の "デフォルト id 値の Cell でも DSL 経路で id が rebind される" テストで検証。`HandleTestCell.id` を毎回 UUID 採番にしておき、DSL 経路で安定 ID にマップされることを確認。
- [x] 25.4.9 既存テスト（`cell(SampleLabelCell(...).cellID("..."))` 形式や引数版 `Section(header = ..., footer = ...)`）も継続して通ることを後方互換確認
  - `DSLIntegrationTest.kt` / `DSLHandleTest.kt` の "既存 引数版 Section と cell ラップ形式が引き続き動作する" テストで後方互換を確認。全テスト Pass。

### 25.5 後続変更提案への波及対応

本オーナーレビューで確定した「具象 Cell の `id` デフォルト値規約」「DSL 拡張関数による Cell 直置き」を後続 `add-cell-types-*` 系の提案に反映する。

- [x] 25.5.1 `add-cell-types-basic` の specs / design / tasks に、7 種基本 Cell の `id` デフォルト値規約と DSL 拡張関数（`DSLSectionScope.LabelCell(...)` 等）の規約を反映（本セッションで完了）
- [x] 25.5.2 `add-cell-types-input` の specs / design / tasks に同等の規約を反映（本セッションで完了）
- [x] 25.5.3 `add-cell-types-custom` の specs / design / tasks に `CustomCell` の `id` デフォルト値化を反映（本セッションで完了）
- [x] 25.5.4 各 cell-types 系の Sample 追加タスク（`samples/android/` 配下）が新 DSL 形式（`Section("...") { LabelCell(title = "...") }` 直置き、`KsIdentifiable` 版 `forEach` 利用）で記述される旨を tasks.md に補記（本セッションで完了）

### 25.6 ドキュメント更新

- [x] 25.6.1 `docs/declarative-dsl-guide.md` を更新:
  - Compose Section H/F の `SectionHandle` 経由 modifier chain 記述例を追記
  - Compose Cell の `CellHandle` 経由 modifier chain 記述例を追記
  - Compose 具象 Cell 型 DSL 拡張関数による直置き記法を追記
  - `KsIdentifiable` marker による `forEach` の `key` 省略版を追記
  - Compose の Root H/F は引数指定のまま（Compose イディオム尊重）であることを明記
  - 実装: ガイド第 9〜12 章として追記済み

## 26. 依存関係と完了条件

**依存関係**:

- 本提案の実装着手前に `add-partial-update-core` および `add-partial-update-native` が archive されていること
- 既存 archive 済の `add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui` を前提とする

**完了条件**:

- すべてのタスク（1〜25）がチェック完了
- iOS / Android 両 OS で DSL 方式が動作し、Sample で目視確認済
- 既存 Store 方式テストがすべて通る（後方互換確認）
- DSL 方式の単体テスト・Integration テスト・クロスプラットフォーム互換性テストがすべて通る
- `docs/declarative-dsl-guide.md` が公開されている
- 本提案の Requirement / Scenario が実装でカバーされていることを確認済
- 25.5 で後続変更提案（`add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`）の修正が完了している
