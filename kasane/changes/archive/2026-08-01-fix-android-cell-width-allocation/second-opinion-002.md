# セカンドオピニオン: fix-android-cell-width-allocation (code-review / review-001 対応)

**相方**: codex / **日付**: 2026-08-01 / **対象**: 未コミットの実装 diff 全体 (CellBaseLayout.kt / EntryCellViewHolder.kt / ButtonCellViewHolder.kt + テスト群)
**モード**: code-review (M 級のため `second-opinion.code-review: [m, l]` により発動)
**注**: 番号は既存の `second-opinion-001.md` (提案フェーズの spec-review 証跡) との衝突を避けて 002 とした。対応するホスト側レビューは `review-001.md`。

---

# レビュー結果: fix-android-cell-width-allocation

**日付**: 2026-08-01  
**判定**: **APPROVED**

**指摘件数**: Critical 0 / Major 0 / Minor 2 / Suggestion 0

## サマリー

幅配分、EntryCell の行内移設、ベースライン、縦チェーン、ButtonCell の ConstraintSet 切替は仕様・ADR と整合しています。公開 API、スタイル解決、IME payload 経路への意図しない変更も認めませんでした。

提示された `./gradlew test` の結果（532件、失敗0）を前提とした静的レビューです。既存の review / verify 結果は判断材料に使用していません。`deviation.md` の2件も合意済みとして違反扱いしていません。

## 指摘事項

### [🟡 Minor] accessory + description の2 Scenarioに直接的な自動回帰テストがない

**該当箇所**:

- [cell-types-basic/spec.md:18](kasane/changes/fix-android-cell-width-allocation/specs/cell-types-basic/spec.md:18)
- [cell-types-basic/spec.md:24](kasane/changes/fix-android-cell-width-allocation/specs/cell-types-basic/spec.md:24)
- [CellRowWidthAllocationTest.kt:351](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:351)

**問題点**: SwitchCell と PickerCell の実装は共通制約により正しく、実機証跡でも非重なりを確認できます。しかしテストでは、長い description の右端と accessory の左端、および Picker の valueText・description・chevron の3系統配置を直接測っていません。将来 `descriptionView` の END 制約などが壊れても、現在の自動テストだけでは検出できません。

**推奨修正**: 固定幅で次を測る Robolectric テストを追加してください。

- SwitchCell: `descriptionView.right <= accessoryHolder.left` と accessory の縦中央。
- PickerCell: valueText が `contentRow` 内、chevron が `accessoryHolder` 内、`descriptionView.right <= accessoryHolder.left`。

現状の実装不良ではなく、将来の回帰検出力に関する非ブロッキング指摘です。

### [🟡 Minor] 末尾省略の実機検証について証跡内の記述が矛盾している

**該当箇所**:

- [CellRowWidthAllocationTest.kt:75](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:75)
- [ui/brief.md:74](kasane/changes/fix-android-cell-width-allocation/ui/brief.md:74)
- [ui/brief.md:106](kasane/changes/fix-android-cell-width-allocation/ui/brief.md:106)

**問題点**: テストコメントと brief 末尾は実際の「…」表示を実機スクリーンショットで確認したと説明していますが、brief の照合表では長い title/valueText の実機確認は未実施と明記されています。Robolectric の制約自体は正しく認識されており、実装も `singleLine + ellipsize END + 幅上限` を設定しているため、コード上の問題ではありません。

**推奨修正**: 長文 fixture の実機画像を追加するか、コメントを「実際のグリフ描画は未確認で、構成と幅制約を検証した」に統一してください。

## Scenario 対応

| Scenario | 実装 | テスト・証跡 | 評価 |
|---|---|---|---|
| EntryCell が残り幅全体を占有 | `title=wrap_content`、EditText=`0dp + weight=1` | `CellRowWidthAllocationTest.kt:105` | 充足 |
| パスワードも同配分 | 同じ layout、password は inputType のみ変更 | `:142` | 充足 |
| 固定最低幅に依存しない | 160dp `minWidth` を撤去 | `:175` で幅0まで検証 | 充足 |
| trailing なしは title 全幅 | title=`0dp + weight=1`、valueText=`GONE` | `:250` | 充足 |
| valueText はコンテンツ幅 | valueText=`wrap_content`、title が weight 1 | `:275` | 充足 |
| 超長文 valueText を末尾省略 | singleLine + ellipsize END、親幅で上限 | `:323`。実グリフは上記指摘あり | 実装充足 |
| Switch description 非重なり | description END と accessory START を制約 | 実機証跡あり、直接自動テストなし | 充足・Minor |
| Picker の2系統配置 | valueText は contentRow、chevron は accessoryHolder | 実機証跡あり、直接自動テストなし | 充足・Minor |
| Android EntryCell 行内配置 | EditText の親を contentRow に変更 | `:223` | 充足 |
| iOS EntryCell 行内維持 | `trailingViews: [fieldWrapper]` | 既存 iOS テスト `InputCellsTests.swift:301` | 充足 |

## その他の重点確認

- valueText と title のベースラインは、`LinearLayout.isBaselineAligned` と座標比較テストで保証されています。
- title + description の packed 縦中央チェーンは `contentRow` を head として再構成され、直接テストされています。
- ButtonCell は aux なしの全幅中央揃えと、ボタンスタイルから通常レイアウトへの復帰を測定しています。
- accessoryHolder の root 右端・縦中央制約と、description の accessory leading 側制約は維持されています。
- EntryCell の `inputType`・hint・filter の差分ガードは維持され、テスト用注入経路も新しい `contentRow` 構造へ追随しています。
- `PAYLOAD_CONTENT`、change animation 無効化、同一 ViewHolder 再 bind のコードには変更がなく、android/ADR-0001 と整合しています。
- production diff は `internal` な View 階層変更のみで、公開 API・依存関係・色・寸法・フォントトークンの変更はありません。


---

## 突き合わせ結果

ホスト側 `review-001.md` (NEEDS_DISCUSSION / Major 1・Minor 1・Suggestion 3) と本相方レビュー (APPROVED / Minor 2) を突き合わせた結果。

| # | 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|---|
| 1 | ButtonCell の `titleAlignment` が初めて実効化する (Major) | ホストのみ | **確定** | 該当箇所が特定され、既定値 `CENTER` により既存利用コードの見た目が左寄せ→中央寄せに変わる実害シナリオを伴う。相方は未検出 (見落とし。矛盾ではない)。オーケストレーター側で原典 `ButtonCellRenderer.cs:97` (`TitleLabel.Gravity = TitleAlignment.ToGravityFlags()`) と iOS `ButtonCellView.swift:61` を照合し、実効化の方向は原典・iOS 同型と裏取り済み → オーナー判断へ |
| 2 | title の 1 行化が未合意のまま「合意済み妥協」節にある (Minor) | ホストのみ | **解決済み** | レビュー起動後にオーナー承認を取得し `deviation.md` へ記録済み。相方には `deviation.md` を合意済みとして渡したため指摘対象外だった。残作業は brief.md の節見出しの訂正のみ |
| 3 | accessory + description の 2 Scenario に直接的な自動回帰テストがない (Minor) | 相方のみ | **採用** | 追加すべき検証 (SwitchCell の `descriptionView.right <= accessoryHolder.left`、Picker の 3 系統配置) が具体的に特定されている。現在の実装不良ではないが回帰検出力の穴は実体がある。非ブロッキング |
| 4 | 末尾省略の実機検証について証跡内の記述が矛盾 (Minor) | 相方のみ | **採用** | テストコメント / brief 末尾は「実機で "…" を確認」と読めるが、brief の照合表は長い title / valueText の実機確認を未実施と明記しており食い違いは事実。証跡の正確性の問題 |
| 5 | `contentRow.visibility = VISIBLE` は到達不能な防御コード (Suggestion) | ホストのみ | **採用 (軽微)** | 事実。削除またはコメントで明示 |
| 6 | title と行内 trailing の間に余白がない (原典は `paddingRight="6dp"`) (Suggestion) | ホストのみ | **オーナー判断へ** | 原典との差分は事実。ただし brief.md は spacing を明示的に非規範としており、本 change のスコープに含めるかは判断が要る |
| 7 | before スクリーンショットの行構成ずれ (Suggestion) | ホストのみ | **降格 (事実誤認の訂正)** | オーケストレーター側で確認した結果、before は現行コードから `:app:installDebug` でビルドした APK であり、写っている行順 (メール→電話→パスワード→ニックネーム→PickerCell…) は現行 `InputCellsDemoScreen.kt` と一致する。「名前」行がスクロールで画面外だっただけで「古い APK」は事実誤認。brief.md の当該記述を訂正する |

**判定の扱い**: ホスト NEEDS_DISCUSSION / 相方 APPROVED と割れたが、これは相方が #1 に到達しなかったことによるもので、両者の主張が矛盾しているわけではない。#1 の根拠が強くオーナー判断を要するため、**NEEDS_DISCUSSION を採用**する。

**件数**: 確定 1 / 採用 3 (うち相方由来 2) / 解決済み 1 / オーナー判断 1 / 降格 1
