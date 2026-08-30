# セカンドオピニオン: fix-cell-icon-size-parity (code-001)
**相方**: codex / **日付**: 2026-08-23 / **対象**: Android + iOS の共通行 icon 枠・角丸・主行の幅配分の実装 diff (新規テスト 3 ファイル・視覚証跡を含む)
---
# レビュー結果: fix-cell-icon-size-parity

**日付**: 2026-08-23  
**判定**: **APPROVED**  
**件数**: Critical 0 / Major 0 / Minor 2 / Suggestion 1

## サマリー

Android 18 Scenario、iOS 15 Scenario のすべてに対応する実装・テストを確認しました。テストは実 View の寸法、outline、Auto Layout 制約、実 ellipsis、再 bind、Theme 更新を観測しており、主要 assertion に回帰検出力があります。PNG 証跡も実在し、表示内容は提出実装の契約と整合しています。

## 指摘事項

### [🟡 Minor] deviation の ButtonCell 対象範囲が実装より広い

**該当箇所**: [deviation.md:3](kasane/changes/fix-cell-icon-size-parity/deviation.md:3)、[CellBaseLayout.kt:466](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:466)

**問題点**: deviation は `valueText`・`icon`・`hintText` のいずれかがある ButtonCell 全般で title がコンテンツ幅になり、CENTER / END が視覚に出ないと記録しています。しかし実装は `valueText == null` なら title を主行全幅にするため、icon のみ／hintText のみでは alignment が引き続き視覚に反映されます。合意済み記録と実装が一致していません。

**推奨修正**: ADR-0026 と実装に合わせ、deviation の対象を「行内 trailing（valueText）がある場合」に限定してください。全 aux で alignment を失わせるのが本来の合意なら、ADRとの整合を再確認した上で実装・テストを変更する必要があります。

### [🟡 Minor] 変更した KDoc にアーカイブ作業資料への参照が残っている

**該当箇所**: [CellRowWidthAllocationTest.kt:686](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:686)

**問題点**: `ui/verification/after-buttoncell-center-alignment-pixel6a.png` は実際には過去 change の archive 配下にある作業証跡です。`comment-policy.md` は、ソースコメントからアーカイブ文書・作業資料を参照することを禁止しています。

**推奨修正**: この一文を削除してください。テストの意図は前後の自己完結した説明と assertion だけで理解できます。

### [🔵 Suggestion] 制約ログの原出力との対応を強化する

**該当箇所**: [ios-test-constraints.log:9](kasane/changes/fix-cell-icon-size-parity/ui/verification/ios-test-constraints.log:9)

**問題点**: ファイルは実行結果の要約であり、`xcodebuild` の原出力や提出ツリーの識別子を含みません。このレビューでは依頼文のホスト実行結果を受理できますが、archive 後に第三者が「0件」の検索結果と提出コードの対応を再検証することは困難です。

**推奨修正**: 今後は原出力を保存するか、少なくとも tree/commit hash、検索コマンド、原ログの SHA-256 を併記してください。

## 客観条件の確認

- デルタスペック: 全 Requirement / Scenario に実装と対応テストあり。
- テスト検出力: Android は Native graphics の実 ellipsis、iOS は実寸・制約状態を観測しており、主要テストに明白な空振りなし。
- 視覚証跡: 指定 PNG とログはすべて実在。Android の icon 枠・角丸・幅配分、iOS の列揃え・狭幅表示を視認確認済み。
- ホスト実行結果: Android 2582件、iOS 581件、失敗および制約衝突0件として受理。
- `tasks.md`: 完了済みの虚偽チェックはありません（現状は全項目未チェック）。


---

## 突き合わせ結果 (2026-08-23)

ホスト側レビュー `review-001.md` (APPROVED / Minor 2 / Suggestion 2) と突き合わせた。両者とも Critical 0 / Major 0 で判定は一致 (APPROVED)。指摘の重複はなく、双方が相手の見落としを 2 件ずつ拾う結果になった。

| 指摘 | 出典 | 重要度 | 採否 | 根拠 |
|---|---|---|---|---|
| deviation の ButtonCell 対象範囲が実装より広い | 相方のみ | Minor | **採用** | 実装の分岐が `fillsRow = valueText == null && !views.hasFillingInlineTrailing` であることを確認。icon のみ / hintText のみでは title が全幅で alignment は効く。deviation の記述をその範囲へ狭めた |
| KDoc にアーカイブ配下の PNG 参照が残る | 相方のみ | Minor | **採用** | `comment-policy.md:29` の「アーカイブ文書のパス」禁止に該当。参照先は `kasane/changes/archive/2026-08-01-fix-android-cell-width-allocation/ui/verification/` に実在。lint が PNG パスをカバーせず、ホスト側も見逃していた (ホスト側の見逃しとして扱う)。該当の一文を削除。あわせて同ファイルの `（タスク 2.4 / 3.3）`・`（ADR-0002 検証事項 / タスク 1.3）` の変更提案内通番と、履歴記述型 KDoc 1 件も同じ規約違反として掃き出し・是正した |
| 制約ログに原出力・提出ツリーの識別子がない | 相方のみ | Suggestion | **採用 (軽微対応)** | 修正サイクル反映後に iOS テストを再実行してログを作り直し、検証対象ソース 6 件と原出力の SHA-256・検索コマンド・検索が空振りでないことの確認を併記した |
| `tasks.md` のチェックボックス全未消化 | ホストのみ | Minor | **採用** | 全 23 項目を `[x]` へ更新 |
| `ui/brief.md:39` の証跡ファイル名が実体と不一致 | ホストのみ | Minor | **採用** | `android-overflow-long-value-after.png` → `android-overflow-long-value.png` (実体は tasks 3.3 の指定どおり) |
| `invalidateOutline()` の除去をテストが検出できない | ホストのみ | Suggestion | **採用** | ミューテーション実測で確認済みの回帰検出力の穴。`IconFrameOutlineProvider.radiusPx` を `val` 化してインスタンス差し替え方式へ変更し、穴が閉じたことを再度ミューテーションで実測 |
| `setIconVisible` の `size` 既定値 0 で枠を無言で潰せる | ホストのみ | Suggestion | **採用** | `showIcon(size:)` / `hideIcon()` の 2 入口へ分割し、誤用をコンパイル段階で不可能にした |

**未解決・降格**: なし (全 7 件を採用)。両者の指摘が矛盾した論点もなかった。

**相方レビューの寄与**: `comment-policy` 違反の検出はホスト側 lint が PNG パスをカバーしておらず、ホスト側レビューも「禁止参照 0 件」と判定していた。相方が規約本文から判定したことで、同ファイル内の別種の違反 (変更提案内通番 2 件・履歴記述 1 件) の掃き出しにもつながった。
