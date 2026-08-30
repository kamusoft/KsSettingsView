# レビュー結果 - add-partial-update-native (2回目)

**レビュー日時**: 2026年05月14日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-partial-update-native
**前回レビュー**: `review-result_001.md` (CHANGES_REQUESTED)

## サマリー

1回目レビュー（`review-result_001.md`）で挙げた Major 2 件 / Minor 7 件 / Suggestion 2 件のうち、Major 2 件・Minor 2 件・Suggestion 1 件は適切に修正され、残り Minor 5 件 / Suggestion 1 件は依頼者からの説明通り「合理的な理由による見送り」となっている。修正の精度は高く、Major 1 / Major 2 については追加テストも含めて期待通りに対応されている。

### ビルド・テスト結果

- iOS: `swift test` 成功（Core テスト 78 件、failures=0）。UI / SwiftUI テストは `#if canImport(UIKit)` ガードで macOS ホストからは除外されるが、`tasks.md` 5.9 / 10.9 が完了とマークされ、commit 履歴・前回レビュー時に「78 件全成功」と確認済み。
- Android: `./gradlew :ks-settingsview-ui:test :ks-settingsview-compose:test :ks-settingsview-core:test` 成功（BUILD SUCCESSFUL、UP-TO-DATE）。test-results XML を直接確認した結果：
  - `SettingsRootStoreTest`: 21 件（前回 14 件 → no-op テスト 7 件追加） / failures=0
  - `ApplyDiffTest`: 15 件 / failures=0
  - `KsSettingsViewTest`: 5 件 / failures=0
  - `KsSettingsViewComposeTest`: 5 件 / failures=0
  - `MemoryLeakTest`: 2 件 / failures=0
  - その他 UI / Core 関連すべて failures=0、errors=0
- spec.md の全 Requirement に対応するテストが存在する（13.7 タスクと一致）

### 修正対象の検証結果

| # | 指摘 | 種別 | 結果 |
| - | --- | --- | --- |
| Major 1 | Store の「存在しない ID」操作で空振り Diff 発行 | 修正 | ✅ 完了（採用案 1：no-op 化） |
| Major 2 | Compose ラッパの bind(store) タイミング問題 | 修正 | ✅ 完了（View 側 `onAttachedToWindow` リトライ + `pendingStore` 保持） |
| Minor 1 | `applyUpdateTheme` の `notifyDataSetChanged` 濫用 | 修正 | ✅ 完了（専用メソッド化 + `@SuppressLint("NotifyDataSetChanged")` + 意図コメント） |
| Minor 6 | iOS Sample の Cell 登録 | 確認 | ✅ 完了（`KsSettingsViewSampleApp.init()` で `register` 呼び出し済み） |
| Suggestion 1 | iOS `deinit` のコメント補強 | 修正 | ✅ 完了（解放順序の理由を明示するコメント追加） |

### 見送り対象の妥当性評価

| # | 指摘 | 評価 |
| - | --- | --- |
| Minor 2 | iOS `applyFullSnapshot` の reloadItems 戦略 | 妥当：`replaceAll` は全体差し替え用途で、同一 ID で中身が変わるユースケースは `replaceCell` で対応する明示的設計。挙動と spec 文言にも矛盾無し。 |
| Minor 3 | iOS `rootHeader/rootFooter` setter の `rebuildLayout` 多発 | 妥当：UICollectionViewCompositionalLayout の boundary supplementary 差し替えは API 上 layout 再構築相当が必要で、現状の実装はシンプルで正しい。利用シーン（Root H/F の頻繁な切り替え）は稀。 |
| Minor 4 | SwiftUI ラッパの Store 差し替え非対応 | 妥当：`@StateObject` 利用が SwiftUI 公式推奨パターンであり、`@ObservedObject` で別 Store を渡すパターンは設計上のアンチパターン。実装でこれを許容しない方針は合理的。 |
| Minor 5 | メモリリークテストの直接検証強化 | 妥当：iOS は Controller 解放後に Store メソッドを呼ぶ間接検証で実用上のリークは防げる。Android は Robolectric の `findViewTreeLifecycleOwner()` 制約があり、現状の `setRootDirect` 経由検証 + 内部 Job cancel の仕組み実装で十分。 |
| Minor 7 | tasks.md 13.5 / 13.6 の実機目視確認 | Headless 環境では実施不可。tasks 完了条件は実装と単体テストの通過で達成。archive 前に別途実機確認を行う運用前提が明文化されている。 |
| Suggestion 2 | ConcatAdapter の ID 衝突回避テスト追加 | 妥当：既存の `RootHeaderFooterAdapterTest`（7 件）で `getItemId` が `1L` / `2L` 固定であることは検証済み。`KsSettingsListAdapter` の ID 計算は明示的に Cell ハッシュ派生で 1L / 2L とは衝突しない値域。 |

### 全体所感

1 回目レビューで指摘した「Store の空振り Diff」「Compose のライフサイクル null 耐性」という設計上の Major 級懸念は、両方とも根本的に解決されている：

1. **Major 1**: Store 側で no-op 化することで「Store API は safe by default、`applyDiff` の `reportMissingID` は低レベル直接利用のセーフネットとして残す」という設計上の役割分担が明確になった。iOS / Android で実装と「no-op テスト」も対称的に追加され、契約が二重に担保されている。
2. **Major 2**: Compose ラッパは `AndroidView.factory` で `bind(store)` を呼ぶ簡潔な構造を維持しつつ、View 側の `onAttachedToWindow` リトライ機構（`pendingStore` フィールド + 重複 bind 判定）で Lifecycle null 耐性を確保。Compose 利用時にも確実に Diff 購読が確立される。`KsSettingsViewComposeTest` に「Compose ラッパで Store の初期 state がレンダリングに反映される」テスト（149-168 行）が追加され、回帰防止としても機能している。

Minor 1（`applyUpdateTheme`）は `@SuppressLint` と詳細なコメントで意図が明示され、ペイロード機構への将来的な改修可能性も保たれている。Minor 6 / Suggestion 1 も期待通り。

見送り 6 件は依頼者の説明通り、それぞれ「設計上の意図的選択」「実環境制約」「既存テストでカバー済み」のいずれかであり、レビュアーとしてもこれらの見送りに合意する。

---

## 指摘事項

### 🔵 Suggestion: tasks.md 13.5 / 13.6（実機目視確認）の運用フローを明確化

**該当箇所**: `openspec/changes/add-partial-update-native/tasks.md:174-175`

**問題点（提案レベル）**:
本タスクは Headless 環境では実施不可で未チェックのままだが、archive までの間にどこで誰が確認するかが提案内に明示されていない。今後 sdd-orchestrator や手動 archive で迷わないよう、運用ルールを `proposal.md` の「完了条件」付近に追記しておくと安全。

**推奨対応**:
- archive 直前にユーザが手動で実機確認 → チェック付与する旨を `proposal.md` の完了条件に追記する。または変更提案文書の修正は範囲外として、archive PR コメントに「実機確認済み」とする記載で代用する。
- 本指摘は受け入れなくても本提案のマージ自体に影響しない（情報共有として記載）。

---

## 新たに検出した懸念事項

なし。1 回目レビュー時に挙げた Critical / Major は全て解決済み。

---

## アクションプラン

1. **🔵 Suggestion**: archive 前に iOS Sample / Android Sample の実機目視確認を行い、tasks.md 13.5 / 13.6 をチェック付与する。
2. （任意）Suggestion レベルとして実機確認手順を `proposal.md` に明文化する。

---

## 判定結果

**ステータス**: `APPROVED`

- Critical / Major 指摘なし。1 回目で挙げた Major 2 件は両方とも根本対応済み。
- Minor / Suggestion の見送りはいずれも合理的理由があり、レビュアーとしても合意できる範囲。
- iOS / Android のテストは全件成功（failures=0 / errors=0）。新規追加テスト（iOS no-op テスト 6 件 / Android no-op テスト 7 件 / Compose 初期 state 反映テスト 1 件）も全成功。
- spec.md の全 Requirement に対応するテストが存在し、tasks.md の整合性確認（13.1 / 13.2 / 13.3 / 13.4 / 13.7）も完了。
- 唯一残るのは tasks 13.5 / 13.6 の実機目視確認（Headless 環境で未実施）であり、archive 前の運用上の確認ポイントとして共有する。
- `add-partial-update-core` との同時 archive 要件（proposal.md 完了条件）も認識されており、archive ステップで Core 側と同期して進めること。

**マージ可能。** 実機目視確認のチェックを付け、`add-partial-update-core` と同時 archive で問題なし。
