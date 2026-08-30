# レビュー結果 - add-samples-ios

**レビュー日時**: 2026年05月10日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-samples-ios

## サマリー

`add-monorepo-foundation` で配置された `samples/ios/` placeholder を実 Sample アプリ `KsSettingsViewSample` で置き換える、SwiftUI App ライフサイクル + Local Swift Package 参照ベースの最小デモ実装。

検証実施結果:

- **xcodebuild**: `xcodebuild -project samples/ios/KsSettingsViewSample.xcodeproj -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,name=iPhone 17' build` → **BUILD SUCCEEDED**
- **swift test (本体)**: `cd ios && swift test` → **50 tests, 0 failures**
- **openspec validate add-samples-ios --strict**: **valid**
- **タスク完了率**: 34 / 34 (100%)

提案・設計・spec・タスクと実装の整合性は高く、ビルド・テストもグリーン。
完了条件はすべて達成されている。

実装は全体的に design.md / spec.md の意図を忠実に反映しており、Cell 登録・DSL 構築・Local Package 参照・Bundle Identifier・Swift 6 対応など主要 Decision に齟齬はない。

ただし、**Critical/Major 問題は無し**、軽微な改善余地（README の表記揺れ、冗長な actor 修飾、Sample 起動時の暗黙シングルトン状態への副作用注釈）が数点あるのみ。

**判定**: `APPROVED`

## 指摘事項

#### 🟡 Minor: `SampleLabelCellView` の `KsCellRenderer` 適合宣言における冗長な `@MainActor`

**該当箇所**: `samples/ios/KsSettingsViewSample/SampleLabelCellView.swift:25`

**問題点**:

```swift
@MainActor
final class SampleLabelCellView: UICollectionViewListCell, @MainActor KsCellRenderer {
```

クラスが既に `@MainActor` 隔離されているため、適合宣言側の `@MainActor KsCellRenderer` の `@MainActor` は冗長。
Swift 6 ではプロトコル適合時の global actor 修飾は「適合自体を main actor 上に閉じ込める」役割を持つが、本ケースのようにクラス自体が `@MainActor` の場合は unnecessary。`KsCellRenderer` プロトコルは本体側で `nonisolated` の通常プロトコルとして定義（`public protocol KsCellRenderer: AnyObject { ... }`）されており、本体の `PoCLabelCellView` でも `@MainActor` 修飾なしで適合させている（本体は Swift 5 言語モード）。

**推奨修正**（任意・コンパイル可否に影響なし）:

```swift
@MainActor
final class SampleLabelCellView: UICollectionViewListCell, KsCellRenderer {
```

備考: クラス側の `@MainActor` 隔離自体は、Sample プロジェクトが `SWIFT_VERSION = 6.0`（Swift 6 言語モード）であり、`UICollectionViewListCell`（main-actor isolated）の継承クラスから `nonisolated` プロトコルへ適合する際の整合性を取るために必要であり妥当。design.md には明記されていないが、コンパイル要件への合理的な対応であり、最小デモという本提案の目的を破壊しない。

---

#### 🟡 Minor: `ContentView` Preview 用 Cell 登録のシングルトン汚染

**該当箇所**: `samples/ios/KsSettingsViewSample/ContentView.swift:55-63`

**問題点**:

`SampleLabelCellPreviewRegistration.registerOnce` 経由で `KsCellRegistry.shared` に対して `register(...)` を呼んでいる。これは `init()` 経由で App ライフサイクル登録と機能的に重複しており、Preview 専用キャンバスでも問題なく動作する反面、

- `KsCellRegistry.shared` はプロセス共通シングルトンであり、Preview 起動 → App 本体起動の順で同一プロセスを共有した場合、登録が二重に上書きされる（同じ key への代入なので実害は無い）
- `private enum` を 1 ファイル内に追加したことで、ContentView.swift の責務が「画面構成」を超えて「Preview 専用初期化ヘルパ」までに膨らんでいる

実装としては動作上の不具合は無く、**問題は意図通り**だが、design.md / tasks.md には Preview 用登録の言及がないため、レビューとしては設計判断の補足コメントを足すか、Preview ヘルパ自体を別ファイル（例: `SampleLabelCellPreview.swift`）に切り出すことを推奨。

**推奨修正**（任意）:

ContentView.swift の `#if DEBUG` ブロック先頭に「App 本体の `init()` は Preview では呼ばれないため、本ヘルパで明示的に登録する」旨のコメントが既にあり、内容は十分明示されている。さらに 1 ファイル 1 責務を厳格にするなら別ファイル化が望ましいが、必須ではない。Sample 用途を考慮すれば現状で許容できる。

---

#### 🔵 Suggestion: README の関連リンクの整備

**該当箇所**: `samples/ios/README.md:83-87`

**問題点**:

```markdown
- [`add-settings-view-ios-ui` 変更提案](../../openspec/changes/archive/) - Sample 本体（KsSettingsViewUI / KsSettingsViewSwiftUI）の実装
- ...
- [`docs/ios-ui.md`](../../docs/ios-ui.md) - iOS UI 利用ガイド（存在する場合）
```

- `add-settings-view-ios-ui` のリンク先が `archive/` ディレクトリ全体になっており、実際のターゲット（`archive/2026-05-09-add-settings-view-ios-ui/`）に直接たどり着けない
- `docs/ios-ui.md` は実在するため、「（存在する場合）」の表記は不要

**推奨修正**:

```markdown
- [`add-settings-view-ios-ui` 変更提案](../../openspec/changes/archive/2026-05-09-add-settings-view-ios-ui/) - Sample 本体（KsSettingsViewUI / KsSettingsViewSwiftUI）の実装
- ...
- [`docs/ios-ui.md`](../../docs/ios-ui.md) - iOS UI 利用ガイド
```

---

#### 🔵 Suggestion: `xcodebuild` コマンド例で固定機種名を使っている

**該当箇所**: `samples/ios/README.md:55-60`

**問題点**:

`-destination 'platform=iOS Simulator,name=iPhone 17'` という固定機種名を README コマンド例に書いている。`name=` の補足説明（`xcrun simctl list devices available` で利用可能な機種を確認）はあるが、実行例自体が将来 Xcode 同梱シミュレータのデフォルトラインアップが変わった際に陳腐化しやすい。

**推奨修正**（任意）:

`-destination 'generic/platform=iOS Simulator'` 形式（特定機種に依存しないビルド検証用）も併記すると将来耐性が増す。本提案のスコープ外なので必須ではない。

---

## アクションプラン

優先度順:

1. （任意）`SampleLabelCellView` の適合宣言から重複 `@MainActor` を削除 — **Minor**
2. （任意）README の `add-settings-view-ios-ui` archive リンクをサブディレクトリ直リンクに更新 — **Suggestion**
3. （任意）README から `(存在する場合)` 表記を削除 — **Suggestion**
4. （任意）`xcodebuild` コマンド例に generic/platform 形式を併記 — **Suggestion**

いずれも本提案を **archive させない理由にはならない** 軽微な指摘である。

## 判定結果

**ステータス**: `APPROVED`

理由:

- proposal / design / spec / tasks と実装の整合性が確認できた（DSL 形式、Bundle Identifier、Deployment Target、Swift 6、Local Swift Package 参照、Sample 専用 Cell の独自定義と KsCellRegistry 登録、README 構成、テストターゲット非配置など、全 Decision を満たしている）
- 完了条件「`xcodebuild` 成功」「Sample が 1 セクション・3 行のセルを描画」「README 置換」「全タスク完了」をすべて達成
- ビルド成功・本体テスト 50/50 成功・openspec strict validation 成功
- Critical / Major 指摘なし。Minor / Suggestion はいずれも archive を阻害しない軽微な改善余地

このまま archive 可能。Minor / Suggestion を別途 follow-up で対応するかは判断に委ねる。
