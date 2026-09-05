# レビュー結果: ios-effectivestyle-visibility (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

`EffectiveStyle` の internal 降格そのものは完全かつ正確で、参照経路・テスト・Swift 6 適合・handbook 更新のいずれにも欠落は見つからなかった (降格後 `public` 0 件、iOS 全テスト 1000 tests / 0 failures を再実行で確認)。一方で、本 change が「利用者から参照できない型を公開 doc コメントで案内しない」という理由で `KsCellRenderer.swift` を付随修正したにもかかわらず、**同型の記述が公開型 `Theme` の doc コメントに 3 箇所残っている**。同じ判断を同じ change 内で適用しきれていない不整合であり、これを Major として修正を求める。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 (always)。特に「公開メンバーの doc コメント」節と「禁止する記述類型」節 |
| `kasane/handbook/cross/test-execution.md` | テスト実行・テスト結果の報告 |
| `kasane/handbook/cross/user-skill-api-listing.md` | 本 diff が同文書を編集するため |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触る変更の完了判定 |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (今回は静的読解で争点にならず未適用)、「指摘しないこと」は昇格済みルールなし |

参照した決定・概念: `kasane/decisions/core/0025-cell-icon-radius-applies-to-square-frame.md`、`kasane/decisions/android/0013-resource-reference-via-declaring-library-r-class.md`、`kasane/concepts/core/styling/style-resolution.md`。いずれも `EffectiveStyle` に言及するが、内部の解決機構としての記述であり公開 API としての案内ではない。本 diff と矛盾しない。

## 検証した内容 (実行結果)

- **テスト**: `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → `** TEST SUCCEEDED **`。xcresult の集計で **1000 passed / 0 failed / 0 skipped** (iPhone 17 Pro, iOS 26.5)。`@testable import KsSettingsViewUI` のためテスト側の変更は不要で、`EffectiveStyleTests` / `EffectiveStyleResolutionTests` はそのまま通っている
- **Swift 6 言語モード適合** (`handbook/ios/swift6-language-mode-check.md`): `ios/Package.swift` へ `swiftLanguageVersions: [.version("6")]` を一時追加してパッケージ全体を build → `** BUILD SUCCEEDED **`、error 0 件。一時設定を戻し、`shasum` 一致 (`73ef8ac0…`) と `git diff -- ios/Package.swift` 0 行を確認済み
- **降格の網羅性**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift` に `public` の残存 0 件。`isValidIconSize` / `isValidIconRadius` は元から `private` で、降格し過ぎ (必要な内部参照の遮断) も無し。Swift は internal 型を public シグネチャに露出できないため、build 成功自体が「公開 API に internal 型が残っていない」ことの証明になる
- **利用者経路の非破壊**: `EffectiveStyle` の参照は `ios/Sources/KsSettingsViewUI/` と `ios/Tests/KsSettingsViewUITests/` に閉じる。`samples/`・`skills/`・`maui/`・`verification/`・`ios/Sources/KsSettingsViewBridge/` に参照ゼロを再確認
- **comment-policy lint**: `python3 scripts/comment-policy-lint.py` → 761 ファイル検査 / 禁止 0 件
- **足場の凍結**: `deviation.md` は付随修正 1 件 (`KsCellRenderer.swift` の doc コメント) を記録済み。本 change 起因かつ数行で閉じるため、ksn-core の付随修正の同梱条件に収まる。スコープ外の無断変更は見当たらない
- **先例との整合**: `kasane/handbook/cross/user-skill-api-listing.md` の除外リストからの行削除 + timestamp 更新は、`adopt-android-explicit-api-mode` の蒸留コミットが Android 3 API に対して行った編集と同形。「可視性引き下げ候補」の**基準行**を残す扱いも先例と一致しており、これは指摘に当たらない

## 指摘事項

### [🟠 Major] 公開型 `Theme` の doc コメントが internal 化後の `EffectiveStyle` を案内したまま残っている

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:28`、`ios/Sources/KsSettingsViewUI/Theme.swift:300`、`ios/Sources/KsSettingsViewUI/Theme.swift:304` (付随して `Theme.swift:280` の `// MARK:` 行)

**問題点**:
本 change は `KsCellRenderer.swift` の公開 protocol の doc コメントについて「internal 化により利用者から参照できない型を案内する記述になった」ことを理由に書き換え、その判断を deviation.md に付随修正として記録している。しかし**同じ判断が適用されるべき記述が `Theme` に 3 箇所残っている**。`Theme` は利用者が直接構築・設定する最も表側の公開型であり、`KsCellRenderer` より露出が大きい。

- `Theme.swift:28` — `public struct Theme` 自身の型 doc: 「個別 Cell の `CellStyle.X` が `nil` のときの **フォールバック値** として `EffectiveStyle` 経由で参照される」
- `Theme.swift:300` — `public static let defaultButtonTitleColor` の doc: 「`EffectiveStyle.effectiveButtonTitleColor` の 4 段目フォールバック値として参照される」
- `Theme.swift:304` — `public static let defaultHeaderFooterFont` の doc: 「`EffectiveStyle.effectiveHeaderFont` / `effectiveFooterFont` から参照される」

`handbook/cross/comment-policy.md` の「公開メンバーの doc コメント」節が置く事後判定は、**公開 doc コメントだけを抜き出して読んだとき、ライブラリの利用方法以外の予備知識なしで意味が通ること**。降格後の `EffectiveStyle` は利用者から見えず、名前で辿ることも生成ドキュメントで引くこともできないため、この 3 箇所は判定を満たさなくなった。降格前は「公開型どうしの相互参照」として成立していた記述が、本 diff によって成立しなくなったという意味で、**本 change が直接生んだ不整合**であり、`KsCellRenderer.swift` と同じ扱いにすべき対象。

この状態を放置すると、本 change の目的 (`EffectiveStyle` を利用者向けの公開面から外す) が半分しか達成されない — 型宣言は隠れたが、利用者が最初に読む `Theme` の doc からはその名前が案内され続ける。

**推奨修正**:
`KsCellRenderer.swift` と同じ方針で、`EffectiveStyle` の名指しを「解決の意味」の記述へ置き換える。いずれも参照が説明と一体化しているため単純削除ではなく書き直しになる (comment-policy の書き換え類型 2「理由一体型」)。

- `Theme.swift:28` — 例: 「個別 Cell の `CellStyle.X` が `nil` のときの **フォールバック値** として参照される (解決順序: `CellStyle.X` → `Theme.cellX` → プラットフォーム既定)」。`EffectiveStyle 経由で` の 5 文字を落とすだけで、括弧内の解決順序の説明が残るため文意は保たれる
- `Theme.swift:300` — 例: 「ButtonCell の title 色は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → 本値 の順で解決される」と、解決順序そのものを書いて主語を消す
- `Theme.swift:304` — 例: 「`Theme.headerFont` / `Theme.footerFont` が `nil` のときのフォールバック先。`headerFontSize` / `footerFontSize` が `> 0` のときは pointSize が上書きされる」
- `Theme.swift:280` の `// MARK: - Cell 全体既定 / フォールバック先既定値（EffectiveStyle と共有）` は doc コメントではなく区切りコメントのため上記の事後判定の対象外だが、公開型の本文中に internal 型名が残る点は上と同源。あわせて `（内部の実効値解決と共有）` 等へ言い換えると全体が揃う (必須ではない)

修正後は `KsCellRenderer.swift` と同様に deviation.md の付随修正へ追記し、`xcodebuild test` を再実行して 1000 tests / 0 failures を再確認すること (コメントのみの変更でも、公開 doc の書き換えは comment-policy の適用対象)。

### [🔵 Suggestion] `KsSettingsViewController` の内部 doc に履歴記述が残る (本 diff 由来ではない)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1216`

**問題点**:
`resolveHeaderFont` の doc コメントに「**旧コードからの呼び出し名互換のため**ここに残し、責務は `EffectiveStyle` 側で一本化する」とある。`handbook/cross/comment-policy.md` の「禁止する記述類型」は、時間軸を含む記述 (「〜から移植」「旧実装を〜」の類) をコメントの仕事ではないとして禁じており、これに当たる。`comment-policy-lint.py` は履歴記述を機械判定できず (同文書が「検出範囲は本規約より狭い」「検出 0 件は適合の証明にならない」と明記)、規約本文からの判定はコードレビューの責務とされている。

ただし**本 diff はこのファイルを触っておらず、この記述は本 change 以前からの既存債務**である。内部メンバーの doc であるため上記 Major の公開面の問題とも別件。判定には算入していない。

**推奨修正**:
本 change で扱うかは orchestrator / オーナーの判断に委ねる。扱う場合は「`EffectiveStyle.effectiveHeaderFont(theme:)` への薄いラッパ。Header フォント解決の責務は `EffectiveStyle` 側に一本化する」のように現在形へ寄せる。扱わない場合は既存債務として据え置きでよい (comment-policy lint は増分ラチェット方式のため、触らない限り止まらない)。

## アクションプラン

1. **[必須]** `Theme.swift:28` / `:300` / `:304` の公開 doc コメントから `EffectiveStyle` の名指しを外し、解決順序の説明として書き直す (Major)。`:280` の MARK 行も揃えるのが望ましい
2. **[必須]** 上記を deviation.md の付随修正へ追記する (`KsCellRenderer.swift` と同一の理由・同一の閉じ方)
3. **[必須]** `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` を再実行し、バンドル集計で 1000 tests / 0 failures を再確認する。あわせて `python3 scripts/comment-policy-lint.py` の禁止 0 件を再確認する
4. **[任意]** `KsSettingsViewController.swift:1216` の履歴記述を扱うか判断する (Suggestion)
5. Swift 6 一時設定ビルドは本レビューで error 0 件を実測済み。コメントのみの追加修正であれば再実行は不要
