# レビュー結果: implement-modern-style (003 回目)

**日付**: 2026-08-20
**判定**: APPROVED

## サマリー

review-002 の Major (Root Header / Footer の accessory が全 Diff で factory から作り直される) は**解消**を確認した。`appliedRootAccessoryMargin` に直近適用値を保持し、`sectionUnitMargin()` の解決値が変わったときだけ Root accessory を作り直す形に絞られている。ミューテーションプローブ (lessons/code-review.md L-001) で双方向の検出力を実測し、追加された回帰テスト 2 本が「作り直さないこと」「作り直すこと」の両方を実際に固定していることを確認した。

追従漏れの懸念 (空⇔非空・Theme 変更・style 切替・Root accessory が画面外) は一時プローブで全経路を実測し、いずれも新しい余白へ正しく追従することを確認した。Major / Minor は無し。carried-over の Suggestion 2 件は未対応のままだが、うち 1 件 (可視 Section 0 件経路) は**挙動そのものは実測で正しい**ことを本レビューで確認済みであり、残るのは回帰テストの網羅性の問題である。

## 確認した観点

- **ビルド / テスト** (concepts/cross/conventions/test-execution.md に従い件数まで確認):
  - iOS: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → Bridge 148 / Core 88 / SwiftUI 91 / UI 558 = **885 tests, 0 failures** (`** TEST SUCCEEDED **`)。review-002 時点の 883 から +2 (今回追加の回帰テスト 2 本)
  - iOS サンプル: `samples/ios` `xcodebuild build` → `** BUILD SUCCEEDED **`
  - Android: 今回の diff は iOS に閉じているため再実行していない。`android/` 配下の全変更ファイルの最終更新は 11:54〜12:30 で review-002 の出力 (14:04) より前であり、この修正サイクルでは触れられていないことを mtime で確認した
- **コメント規約**: `python3 scripts/comment-policy-lint.py` → 検査 639 ファイル / 禁止 0 件。今回追加のコメント (`appliedRootAccessoryMargin` の説明・`refreshRootAccessoriesIfMarginChanged` の doc) は change-id / レビュー通番の裸参照を含まず、外部文書 ID に依存せず単独で理解できる
- **足場**: `specs/` `proposal.md` `design.md` `exploration.md` は未変更 (`git diff HEAD --stat` で確認)。更新は `tasks.md` のチェックと `ui/brief.md` のみ
- **deviation.md**: 記録済み 6 件は合意済み差分として指摘対象から除外した
- **tasks.md**: 未チェック 0 / チェック済み 23。今回の修正で新たに虚偽チェックとなる項目は無い
- **視覚証跡**: 今回の修正は Root accessory の**再適用の頻度**だけを絞るもので、解決される inset の値は変えない (下記プローブで実測: Modern の Root Header 内側余白は既定 22pt、Theme 指定時は指定値と一致)。review-002 の判断どおりスクリーンショットの再撮影は不要と確認した
- **範囲**: review-001 / review-002 で確認済みの Android・サンプル・視覚照合・その他 iOS 実装は再レビューしていない。今回の diff はそれらへ波及していない

## 確認点1: review-002 の Major の解消

**該当実装**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:123` (`appliedRootAccessoryMargin`)、`:762-768` (`takeRootAccessoryContentInsets(isFooter:)`)、`:776-787` (`refreshRootAccessoriesIfMarginChanged()`)、`:1146` (`refreshSectionUnitPresentation()` からの呼び出し)

`refreshSectionUnitPresentation()` は `refreshRootSupplementary` を直接呼ばなくなり、`refreshRootAccessoriesIfMarginChanged()` を経由する。`guard appliedRootAccessoryMargin != margin else { return }` により、Root accessory と無関係な Diff では `applyAccessoryToListCell` が走らない。

concepts/core/architecture/display-state-synchronization.md の判断 (「View accessory で高さ差を内容差とすると `KsAnyView` の View が factory から作り直されて内部状態を失う」ため、その類型を避ける) と整合する。

### ミューテーションプローブによる検出力の実測

テストが実際にこの挙動を固定していることを、実装への一時的なミューテーションで両方向から実測した (実施後、対象ファイルは backup から復元し `shasum` 一致 `075a851c…` を確認済み)。

| ミューテーション | 結果 |
|---|---|
| `guard appliedRootAccessoryMargin != margin else { return }` を削除 (= review-002 時点の無条件再構成に戻す) | `test_余白が変わらないDiffではRootHeaderのfactoryを呼び直さない` が 3 アサーション全てで失敗。factory 呼び出し回数は baseline 1 → 内容 Diff で 2 → 構造 Diff で 3 → 無関係な Theme 変更で 4 と増加し、review-002 が実測した副作用を再現する |
| 再構成の分岐を常に false にする (= 一切作り直さない) | `test_余白が変わる遷移ではRootHeaderを作り直す` (2 アサーション) に加え、既存の geometry テスト `test_可視Sectionが0件ならRootHeader内側の余白も0になる` (期待 24 に対し 2.0) と `test_実行時のTheme変更でRootHeader内側の余白も追従する` (期待 24 に対し 0.0) も失敗 |

後者が示すとおり、追従の正しさは factory 呼び出し回数だけでなく**実寸を見る既存テスト**でも守られている。回数だけを数えるトートロジーになっていない。

## 確認点2: 今回の修正が持ち込む追従漏れの有無

「余白が変わる遷移で Root accessory が更新されないケース」を洗い出すため、まず `sectionUnitMargin()` の入力 (可視 Section の有無 / `currentTheme` / `style`) を変えうる経路を静的に列挙し、そのうち机上で判断しきれないものを一時プローブ (実行後 `trash` で削除済み) で実測した。

`visibleSections` を書き換える箇所は full snapshot と各部分 Diff ハンドラのみで、いずれも `applyDiff` 末尾 (`:1407`) か `applyFullSnapshot` の completion (`:1592`) から `refreshSectionUnitPresentation()` に合流する。`currentTheme` は `applyTheme` (`:413`) から、`style` は `didSet` → `rebuildLayout()` → `applyFullSnapshot` から同じ合流点に入る。

プローブ実測 (いずれも合格):

| 経路 | 実測 |
|---|---|
| style 切替 (Classic → Modern → Classic、Theme 既定) | Root Header 内容と先頭 Cell の間隔が 2.0 → **24.0** (delta 22.0 = Modern 既定の `margin.top`) → 2.0 と往復で追従。`contentInset.top` は Root Header がある側なので常に 0 |
| 全 Section を `isVisible = false` にする `replaceSection` (review-002 Suggestion 2 が未固定と指摘した経路) | `contentInset` が top 24 / bottom 18 → **0 / 0**。`applyFullSnapshot` の completion 経由でも余白は正しく落ちる |
| Root accessory が画面外の間に `sectionMargin` を変える Theme 変更 | スクロールアウト中は factory 呼び出し 1 のまま (`refreshRootSupplementary` は可視 supplementary のみ対象のため何もしない)。スクロールで戻すと provider が再実行され (factory 2)、Root Header の高さは content 40pt + 新しい余白 40pt = **80pt** となり、記録済み値ではなく**その時点の解決値**が適用される |

3 番目が「supplementary provider との記録タイミングの整合」の要点である。`appliedRootAccessoryMargin` は `takeRootAccessoryContentInsets` (provider 経路) と `refreshRootAccessoriesIfMarginChanged` (更新経路) の双方から書かれるため、「記録値 = 実際に表示されている値」が破れる余地を疑ったが、

- 更新経路で可視 view が無く何も適用できなかった場合でも、次に view が作られるときは provider が `sectionUnitMargin()` を読み直して最新値を適用する
- provider が Diff の途中で先に呼ばれて新しい値を記録した場合は、その view 自体が既に新しい inset を持っているため、続く `refreshRootAccessoriesIfMarginChanged` が skip しても表示は正しい

のいずれの向きでも実測・静的確認で破れがなく、`margin` は値型 (`NSDirectionalEdgeInsets`) の等価比較なので Theme インスタンスの再代入だけでは差分と見なされない (`test_余白が変わらないDiffでは…` の Theme 変更ケースが固定)。

また `takeRootAccessoryContentInsets` が使うのは `margin.top` / `margin.bottom` のみで、比較は margin 全成分で行うため、**比較が等しいのに inset が異なる**という取りこぼしの方向は原理的に起きない (逆方向 — 水平成分だけが変わる style 切替で不要な再構成が 1 回走る — は起きるが、style 切替は元より layout ごと作り直す経路なので実害はない)。

## 指摘事項

### [🔵 Suggestion] `SectionBoxMetrics.clampedCornerRadius(for:)` (インスタンス版) の呼び出し元がテストだけのまま

**該当箇所**: `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:82-84`

review-002 で挙げた同内容の Suggestion が未対応 (合意済み)。判定条件にはしない。蒸留前に「削除してテストを static 版へ寄せる」か「残す」かだけ決めておくと、本番未使用 API が長命化しない。

### [🔵 Suggestion] 可視 Section 0 件の回帰テストは依然 2 経路のみ (挙動自体は本レビューで実測確認済み)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:423` / `:440` / `:457`

review-002 で挙げた同内容の Suggestion が未対応 (合意済み)。ただし本レビューのプローブで、`Section.isVisible = false` による `replaceSection` → `applyFullSnapshot` 経路でも `contentInset` が 0 に落ちることを実測した (24/18 → 0/0)。したがって**実装の穴ではなく、テストが覆っていないだけ**であることが確定した。テストを足すなら 1 本で足りる。

### [🔵 Suggestion] Root accessory のテキスト色 / フォントは Theme 変更に追従しない (本変更以前からの挙動)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:405-428` (`applyTheme`)

**問題点**: `applyTheme` は Cell を `reconfigureItems` で作り直すが、Root Header / Footer の supplementary は余白が変わったときしか `refreshRootSupplementary` を通らない。そのため `headerTextColor` / `headerFont` だけを変える Theme 変更では Root accessory の文字色・フォントが古いまま残る。

これは **HEAD (81bf2c4) からの既存挙動**である (HEAD の `applyTheme` は `refreshRootSupplementary` を一切呼んでいない) — 本変更の回帰ではない。review-002 時点のコードでは無条件再構成の副作用として偶然追従していたが、それは Major 指摘の対象そのものであり、今回の絞り込みが正しい。

**推奨修正**: 本 change では対応しない。別変更として簡易起票する候補 (対応するなら「余白が変わったとき」に加えて「Root accessory の描画に使う Theme 属性が変わったとき」も再構成条件に入れる形になる)。現状 Root accessory のテキスト色を Theme 変更で確認する回帰テストも無い。

## アクションプラン

1. 実装側の必須対応は無し。この状態で `ksn-verify` / 蒸留へ進んでよい
2. **[Suggestion]** `clampedCornerRadius(for:)` の去就を決める (削除 or 本番利用) — 蒸留前の任意
3. **[Suggestion]** 全 Section 非表示経路の余白 0 テストを 1 本足す — 任意 (挙動は実測済み)
4. **[Suggestion]** Root accessory の Theme 追従 (テキスト色 / フォント) を別変更として簡易起票するか判断する
