# レビュー結果: ios-effectivestyle-visibility (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

前回 (001) の Major — 公開型 `Theme` の doc コメント 3 箇所と `// MARK:` 行に internal 化後の `EffectiveStyle` が残っていた件 — は解消済み。書き換え後の 4 箇所はいずれも公開識別子だけで構成され、参照している解決順序も実装と一致していることを本レビューで実測確認した。降格そのものの網羅性・テスト・Swift 6 適合も自前で再検証し、いずれも問題なし。Critical / Major はなく、残るのは優先度の低い Suggestion 2 件のみ。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (always)。本 diff は全編がコメントの書き換えのため、「公開メンバーの doc コメント」節の事後判定・「禁止する参照」節・「書き換え時の判断基準」節を逐節で照合 |
| `kasane/handbook/cross/test-execution.md` | テストを実行し結果を報告するため (iOS 節: Simulator 実行・バンドル集計行での件数確認) |
| `kasane/handbook/cross/user-skill-api-listing.md` | 本 diff が同文書を編集するため |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定のため |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (ミューテーション実測) は本 diff が挙動を持たないコメント変更のため適用対象外。「指摘しないこと」は昇格済みルールなし |

参照した決定・概念: `kasane/decisions/core/0025-cell-icon-radius-applies-to-square-frame.md`、`kasane/decisions/android/0013-resource-reference-via-declaring-library-r-class.md`、`kasane/concepts/core/styling/style-resolution.md`。3 者とも `EffectiveStyle` に言及するが、いずれも「実効値を解決する内部機構」としての記述で、可視性や公開 API としての位置づけを述べていない。本 diff と矛盾せず、concepts 側に修正を要する記述も無い。

## 検証した内容 (実行結果)

- **テスト**: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<iPhone 17 Pro>'` を `ios/` で実行 → `** TEST SUCCEEDED **`。バンドル集計行のみを合算して **1000 tests / 0 failures** (`KsSettingsViewBridgeTests` 166 / `KsSettingsViewCoreTests` 88 / `KsSettingsViewSwiftUITests` 94 / `KsSettingsViewTestSupportTests` 7 / `KsSettingsViewUITests` 645)
- **Swift 6 言語モード適合**: `ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時追加してパッケージ全体を build → `** BUILD SUCCEEDED **`、`error:` 0 件。一時設定を戻し、`shasum` が元の値に一致すること・`git diff -- ios/Package.swift` が 0 行であることを確認済み (前周の結果に依存せず本レビューで再実測)
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py` → 761 ファイル検査 / 禁止 0 件。ただし同規約が明記するとおり検出範囲は規約本文より狭いため、以下の事後判定は本文から手で行った
- **001 Major の解消 (主軸)**: `Theme.swift` の 4 箇所を書き換え後の本文で確認した。いずれも `EffectiveStyle` の名指しが消え、公開 doc の事後判定 (公開 doc だけで意味が通る) を満たす
  - 型 doc — 「`CellStyle.X` が `nil` のときの **フォールバック値** として参照される（解決順序: `CellStyle.X` → `Theme.cellX` → プラットフォーム既定）」。参照句だけを落として括弧内の解決順序が説明として自立しており、書き換え類型 1 (定型句型) の処理として妥当
  - `defaultButtonTitleColor` — 「`ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → 本値 の順で解決される」。**実装と一致することを実測確認**した (`EffectiveStyle.effectiveButtonTitleColor` の 4 段の分岐順と同一)
  - `defaultHeaderFooterFont` — 「`Theme.headerFont` / `Theme.footerFont` が `nil` のときのフォールバック先。`headerFontSize` / `footerFontSize` が `> 0` のときは pointSize が上書きされる」。`effectiveHeaderFont` / `effectiveFooterFont` の実装 (`?? defaultHeaderFooterFont` → `> 0` なら `withSize`) と一致
  - `// MARK: - Cell 全体既定 / フォールバック先既定値（内部の実効値解決と共有）` — 区切りコメントからも内部型名が消えた
- **書き換え後の doc が参照する識別子の公開性**: `ButtonCell.titleColor` (`ButtonCell.swift`)、`CellStyle.titleColor` (`CellStyle.swift`)、`Theme.cellTitleColor` / `headerFont` / `footerFont` / `headerFontSize` / `footerFontSize` (`Theme.swift`) はすべて `public`。利用者が名前で辿れる識別子だけで doc が閉じている (これが 001 Major の是正条件そのもの)
- **`KsCellRenderer` の doc (前周の付随修正)**: 書き換え後も内部用語ゼロで、公開型 `Theme` だけを案内している。利用者定義 Renderer の解決手順の説明として自己完結する
- **公開面に残る `EffectiveStyle` 参照の全数確認**: `ios/Sources/` 全体で `EffectiveStyle` を含むコメントは `ButtonCellView.swift:32` (内部クラスの実装内行コメント) と `KsSettingsViewController.swift:1215` / `:1216` / `:1223` (`internal static func resolveHeaderFont` / `resolveFooterFont` の doc) の 4 行のみ。いずれも非公開メンバーで、公開 doc コメントの事後判定の対象外
- **降格の網羅性**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift` に `public` の出現 0 件。型は `internal struct`、メンバーは修飾子省略 (Swift 既定の internal) で、UI ターゲット内の既存スタイルと整合する
- **利用者向け資産への波及**: `skills/` 配下に `EffectiveStyle` の言及ゼロを再確認 (利用者ドキュメントの書き換えは不要)。`samples/`・`maui/`・`verification/`・`ios/Sources/KsSettingsViewBridge/` にも参照なし
- **足場の凍結**: `exploration.md` の差分は探索フェーズでの本文充填 (現状の把握・選択肢表・決定事項・変更級の確定) であり、実装中の書き換えではない。`deviation.md` は付随修正 2 件 (`KsCellRenderer.swift` / `Theme.swift` の doc) を記録済み。いずれも本 change が直接生んだ不整合の是正で、2 ファイル・数行に閉じ、公開 API の**シグネチャや挙動**には触れないため ksn-core の付随修正の同梱条件に収まる。`review-001.md` に改変なし
- **handbook の編集**: `user-skill-api-listing.md` の iOS 除外リストから `EffectiveStyle` 行を削除し `timestamp` を更新。除外の**基準行**を残す扱いを含め、`adopt-android-explicit-api-mode` の先例と同形

## 指摘事項

### [🔵 Suggestion] `defaultButtonTitleColor` の解決順序に「平常時」の限定が無い

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:300`

**問題点**:
新しい文は「ButtonCell の title 色は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → 本値 の順で解決される」と無条件に読める。実際には `isEnabled = false` の ButtonCell では `Theme.disabledTextColor` が優先され、この 4 段は `isEnabled = true` の平常時の解決順序である (`ButtonCellView.render` は 4 段の結果を `titleColorOverride` として渡し、無効時の色は `applyCellBaseLayout` 側で `isEnabled` を見て決まる)。

**本 diff による劣化ではない** — 直前の行が「4 段解決で『いずれも未指定』のときに使う既定色」と述べており、書き換え前の doc も無効時に言及していなかった。精度を上げるなら任意で扱う類の指摘であり、判定には算入していない。

**推奨修正**:
必要と判断する場合のみ、「ButtonCell の title 色は平常時 (`isEnabled = true`) に〜の順で解決される」と 1 語補う。無効時の色は隣接する `defaultDisabledTextColor` の doc が既に説明している。

### [🔵 Suggestion] `KsSettingsViewController` の内部 doc に履歴記述が残る (001 からの持ち越し・本 diff 由来ではない)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1216`

**問題点**:
`resolveHeaderFont` の doc に「**旧コードからの呼び出し名互換のため**ここに残し、責務は `EffectiveStyle` 側で一本化する」とあり、`comment-policy.md` の「禁止する記述類型」(時間軸を含む記述) に当たる。本 diff はこのファイルを触っておらず、`EffectiveStyle` の名指し自体は内部メンバーの doc なので公開面の問題でもない。**本 change では扱わない判断が示されており、それはスコープ判断として妥当**である (comment-policy lint は増分ラチェット方式のため、触らない限り検出は増えない)。

判定には算入していない。オーナーへの申し送りが行われる前提で、レビュー側の記録としてここに残す。

**推奨修正**:
扱う場合は「`EffectiveStyle.effectiveHeaderFont(theme:)` への薄いラッパ。Header フォント解決の責務は `EffectiveStyle` 側に一本化する」のように現在形へ寄せる。扱わない場合は既存債務として据え置きでよい。

## アクションプラン

1. **[なし・必須の修正は無い]** 001 の Major は解消済み。APPROVED として次工程 (オーナーレビュー → 蒸留) へ進んでよい
2. **[任意]** `Theme.swift:300` の解決順序に「平常時」の限定を補う (Suggestion 1)。公開 doc の精度改善であり、行う場合は既存テストの再実行だけで足りる
3. **[任意・本 change 外]** `KsSettingsViewController.swift:1216` の履歴記述の扱いをオーナーに申し送る (Suggestion 2)
4. 蒸留時の所見: `EffectiveStyle` は `kasane/concepts/core/styling/style-resolution.md` で「実効値を解決する内部機構」として記述されており、可視性の記述を持たないため concepts の追随は不要。ADR も exploration の判断どおり不要 (公開面の縮小のみで platform 境界を越えず、将来の決定を制約しない)
