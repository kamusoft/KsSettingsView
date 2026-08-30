# レビュー結果 - add-samples-ios (再レビュー)

**レビュー日時**: 2026年05月10日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-samples-ios
**前回レビュー**: `review-result_001.md` (APPROVED, Minor 2 / Suggestion 2)

## サマリー

`review-result_001.md` で挙げた Minor 2 件・Suggestion 2 件の追加対応に対する再レビュー。
implementer 報告のとおり、修正 2 / 修正 3 / 修正 4 はファイルに反映されており、修正 1 は実環境（Swift 6 言語モード × `UICollectionViewListCell` 継承 × `nonisolated` プロトコル）でビルド失敗を避けるための必要記述と判断され「維持（撤回）」されている。

検証結果（本レビューで再実行）:

- **xcodebuild (generic/platform=iOS Simulator)**: `xcodebuild -project samples/ios/KsSettingsViewSample.xcodeproj -scheme KsSettingsViewSample -destination 'generic/platform=iOS Simulator' build` → **BUILD SUCCEEDED**
- **swift test (本体)**: `cd ios && swift test` → **50 tests, 0 failures**
- **openspec validate add-samples-ios --strict**: **valid**
- **タスク完了率**: 34 / 34 (100%) — 変更なし

修正によって新たな問題は発生しておらず、初回レビューで指摘した 4 件のうち 3 件が反映、1 件は妥当な根拠付きで撤回されている。

**判定**: `APPROVED`

---

## 修正対応の検証

### 修正1（Minor）— `SampleLabelCellView` の重複 `@MainActor` 適合修飾の削除

**状況**: 維持（撤回）

**検証**:

- `samples/ios/KsSettingsViewSample/SampleLabelCellView.swift:24-25` は引き続き
  ```swift
  @MainActor
  final class SampleLabelCellView: UICollectionViewListCell, @MainActor KsCellRenderer {
  ```
  となっている。
- 同ファイル `:20-23` のコメントで「Swift 6 strict concurrency 下では `UICollectionViewListCell` が `@MainActor` 隔離されているため、本クラスも `@MainActor` で隔離し、`KsCellRenderer` 適合を main actor 上に閉じ込める」旨が明記されている。
- Sample プロジェクトの `SWIFT_VERSION = 6.0`（Swift 6 言語モード）であり、本体側 `KsCellRenderer` は `nonisolated` の通常プロトコルとして定義されている。Swift 6 言語モードでは `UICollectionViewListCell`（main actor isolated）の継承クラスから `nonisolated` プロトコルへの適合は cross-actor conformance 警告／エラーの対象となる。
- implementer の報告どおり「適合宣言側にも `@MainActor` を付ける」のは Swift 6 で要求される正規の書き方であり、初回レビューで「冗長・任意」と評したのは Swift 5 言語モード基準の判断だった。Sample が Swift 6 を採用している以上、現状コードのまま維持するのが正しい。

**評価**: 撤回判断は妥当。前回レビューの記載「（任意・コンパイル可否に影響なし）」は Sample プロジェクトの Swift 6 設定下では誤りで、撤回された結果のコードが Swift 6 整合の正解。コードコメントによる正当化注釈も十分。**指摘解消（撤回扱い）**。

### 修正2（Minor）— Preview 用 Cell 登録ヘルパの別ファイル分離

**状況**: 完了

**検証**:

- 新規ファイル `samples/ios/KsSettingsViewSample/SampleLabelCellPreview.swift` が存在する。
  - `#if DEBUG` で囲まれ、`enum SampleLabelCellPreviewRegistration` の `@MainActor static let registerOnce` で 1 回限りの登録を実装。
  - ファイル冒頭コメントで「ContentView は『画面構成』に専念」「Preview 専用初期化ヘルパを ContentView.swift から本ファイルへ切り出している」旨を明記し、責務分離意図が読み取れる。
  - 仕様 spec.md への参照コメントも保持。
- `samples/ios/KsSettingsViewSample/ContentView.swift:46-53` の `#Preview` ブロックでは `SampleLabelCellPreviewRegistration.registerOnce` を呼び、コメントで「登録ヘルパ本体は `SampleLabelCellPreview.swift` に切り出している」と所在を明示。前回レビューで懸念した「ContentView の責務肥大」は解消。
- `project.pbxproj` を確認し、4 箇所すべてに新ファイルが追加されていることを確認:
  - `PBXBuildFile`（行 14）: `A1000001000000000000A007 /* SampleLabelCellPreview.swift in Sources */`
  - `PBXFileReference`（行 27）: `A1000002000000000000A007 /* SampleLabelCellPreview.swift */`
  - `PBXGroup`（行 65）: 既存ソースグループに参照追加
  - `Sources Build Phase`（行 171）: ビルドソースに参加
- `xcodebuild -destination 'generic/platform=iOS Simulator' build` → **BUILD SUCCEEDED** で、ターゲット組み込み・グループ参照に破綻なし。

**評価**: 責務分離・Xcode プロジェクト変更とも適切。**指摘解消**。

### 修正3（Suggestion）— README 関連リンクの直リンク化

**状況**: 完了

**検証** (`samples/ios/README.md:96-100`):

- `add-settings-view-ios-ui` のリンク先が `../../openspec/changes/archive/2026-05-09-add-settings-view-ios-ui/` への直リンクに修正されている。実ディレクトリと一致しており、リンク先存在確認済み（`openspec/changes/archive/` 配下にある）。
- `docs/ios-ui.md` のリンクから「（存在する場合）」表記が削除されている。
- 加えて、ディレクトリ構成（`samples/ios/README.md:77-92`）に `SampleLabelCellPreview.swift` のエントリが追加されており、修正2 の追加ファイルと整合する。

**評価**: 推奨修正どおり。波及修正（ディレクトリ構成への追記）も適切。**指摘解消**。

### 修正4（Suggestion）— `xcodebuild` 汎用 destination の併記

**状況**: 完了

**検証** (`samples/ios/README.md:65-75`):

- `-destination 'generic/platform=iOS Simulator'` のビルド検証用例が「特定機種に依存しないためビルド成否のみを確認したい CI 用途などに有用」という用途説明とともに併記されている。
- 既存の `name=iPhone 17` 例も保持されており、ローカル動作確認 / CI 検証の両ユースケースに対応。
- 本レビューで `generic/platform=iOS Simulator` を使った `xcodebuild` を実行し **BUILD SUCCEEDED** を確認。

**評価**: 推奨修正どおり。**指摘解消**。

---

## 指摘事項

新規 Critical / Major / Minor 指摘なし。

#### 🔵 Suggestion: `SampleLabelCellView.swift` のクラスヘッダコメントへ「Swift 6 言語モードでは `@MainActor KsCellRenderer` 適合修飾が必要」である旨をより明示的に追記すると、後続の実装者が「冗長に見えるので削除」する誤修正を予防できる

**該当箇所**: `samples/ios/KsSettingsViewSample/SampleLabelCellView.swift:20-25`

**問題点**:

現在のコメントは「Swift 6 strict concurrency 下では `UICollectionViewListCell` が `@MainActor` 隔離されているため、本クラスも `@MainActor` で隔離し、`KsCellRenderer` 適合を main actor 上に閉じ込める」という記述で、クラス側 `@MainActor` の必要性は説明されている。一方、適合宣言側 `, @MainActor KsCellRenderer` の `@MainActor` についてのみ明示されておらず、Swift 6 言語モードでなければ冗長に見える書き方であるため、本レビューの初回判定（撤回前）と同じ誤修正が将来再発するリスクがある。

**推奨修正**（任意・スコープ外、後続提案でも可）:

```swift
/// - Note: Swift 6 strict concurrency 下では `UICollectionViewListCell` が
///   `@MainActor` 隔離されているため、本クラスも `@MainActor` で隔離する。
///   さらに、`KsCellRenderer` は本体側で `nonisolated` の通常プロトコルとして
///   定義されているため、Swift 6 言語モード（`SWIFT_VERSION = 6.0`）では
///   適合宣言側にも `@MainActor` を付与しないと cross-actor conformance
///   エラーとなる。冗長に見えるが両方とも必要。
@MainActor
final class SampleLabelCellView: UICollectionViewListCell, @MainActor KsCellRenderer {
```

本提案のスコープ外であり、archive を妨げるものではない。気になる場合は別 follow-up でカバーできる。

---

## アクションプラン

優先度順:

1. （任意・スコープ外）`SampleLabelCellView` のクラスコメントに「適合宣言側 `@MainActor` も Swift 6 言語モードでは必須」と明示する一文を追加 — **Suggestion**

修正1〜4 のレビュー指摘はすべて解消済みのため、本提案を archive する上での追加の必須対応は無し。

---

## 判定結果

**ステータス**: `APPROVED`

理由:

- 初回レビューの Minor 2 / Suggestion 2 のうち、修正2 / 修正3 / 修正4 は提示どおりに反映、修正1 は Swift 6 言語モード下での必要記述として撤回判断が妥当。
- 修正による新たな問題（ビルド失敗、テスト失敗、責務逆転、リンク切れ、proposal/design/spec/tasks との乖離）は発生していない。
- proposal.md / design.md / spec.md / tasks.md と実装の整合性は前回レビューと同等以上に維持されている（タスクは 34/34 のまま改ざんなし）。
- xcodebuild（generic/platform=iOS Simulator）BUILD SUCCEEDED、swift test 50/50 成功、openspec validate strict valid。
- 残った Suggestion 1 件はクラスコメントの拡充に関するもので、archive 阻害要因にはならない。

このまま archive 可能。
