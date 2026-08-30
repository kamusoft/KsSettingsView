# レビュー結果: add-cell-types-custom (004 回目)

**日付**: 2026-08-04
**判定**: APPROVED

レビュー対象: 未コミット working tree diff (`ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift` /
`CustomCellHostedContent.swift` / `CustomCellView.swift`、`ios/Tests/KsSettingsViewUITests/CustomCellTests.swift`、
`kasane/changes/add-cell-types-custom/ui/verification/index.md` と新規フレーム 6 枚)。
S 級フォローアップのため、基準は合意済みスコープ (CustomCell 折りたたみアニメーション修正) と
`concepts/` の規約 (`cross/conventions/comment-policy.md` / `test-execution.md` /
`runtime-behavior-verification.md`、`core/styling/cell-row-layout.md`)、および `deviation.md`。

## サマリー

配置規則の変更 (`restingHeight = min(bounds.height, max(natural.height, effectiveCellHeight))`) は
合意済みスコープどおりで、content の縦位置を行の高さの遷移から切り離すという意図が最小の差分で
表現されている。実効 cellHeight は `EffectiveStyle` から `CustomCellView.render` → `CustomCellHostedContent`
→ `CustomCellRowPlacement` へ素直に注入されており、レイアウト値を Layout の stored property に
持たせたことで値が変われば SwiftUI 側の再レイアウトも自然に走る。他の呼び出し箇所は無く
(`CustomCellHostedContent` の生成は `CustomCellView` とテストの 2 箇所のみ)、足場アーティファクト
(proposal / design / specs / tasks) は書き換えられていない。

レビュアー側で以下を実測した。

- 全件テスト (iPhone 17 Simulator / `xcodebuild test -scheme KsSettingsView-Package`):
  Core 83 件 + SwiftUI 68 件 + UI 397 件 = **548 件 / 0 failures**、`** TEST SUCCEEDED **`。
  `CustomCellTests` は 38 件 / 0 failures。
- **回帰テストの識別力を A/B で確認**: `restingHeight` を修正前の `bounds.height` に戻すと
  `test_行が実効行高さより高い間は定常高さ基準の縦位置を維持する` **だけ**が失敗する
  (38 件中 1 失敗)。新規テストは今回の修正を正しく差別化しており、ファイルは検証後に
  元の内容へ復元済み (sha 一致を確認)。
- コメント規約 lint: 触れた 4 ファイルで禁止 0 件。
- 静止状態の退行: 固定高行の縦中央 / 収まらないときの上端揃えの既存 2 件が引き続き成功し、
  self-sizing 行の定常縦中央が新規 1 件で追加されている。数式上も定常状態では
  `bounds.height == max(natural, effective)` となり従来と同値。

指摘は Minor 3 件と Suggestion 1 件で、いずれも挙動の誤りではなく記述精度・テスト網羅・
証跡の厳密さに関するもの。実装をブロックしない。

## 指摘事項

### [🟡 Minor] 型 doc の要約行が新しい配置規則を正しく言い表していない

**該当箇所**: `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:11-12`

**問題点**: 要約行は「実効行高さを基準に縦中央へ置き、行に収まらないとき・**行が実効行高さより
高い間は上端揃えにする**」と書いているが、実装は「行が実効行高さより高い間」に必ず上端揃えに
なるわけではない。`natural < effectiveCellHeight` の行 (例: `cellHeight = 120` に対し content 40pt)
では、bounds が 200pt に膨らんでいる遷移中も `restingHeight = 120` となり offset は 40pt、すなわち
**定常高さの中での縦中央**が維持される。上端揃えになるのは `natural >= effectiveCellHeight` の
ときだけである。同ファイル 27-38 行の「# 配置規則」節は「定常高さ基準の位置を維持する」と
正確に書けているので、要約行だけが本文と食い違っている。要約だけ読んだ次の実装者が
「遷移中は常に上端」と誤解しうる。

**推奨修正**: 要約行を本文と揃える。例: 「content を行の中に配置するレイアウト。縦位置は
**定常状態の行の高さ**(`max(content の自然高, 実効行高さ)`) を基準に決め、行の高さの変化には
追従させない。定常高さに収まるときは縦中央、収まらないときは上端揃え」。

### [🟡 Minor] `restingHeight` の実効行高さ側の枝が遷移中のケースで未検証

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:796-854` /
`ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:77`

**問題点**: 新規テスト 2 件のうち、遷移中 (bounds > 定常高さ) を扱う
`test_行が実効行高さより高い間は定常高さ基準の縦位置を維持する` は `natural(60) > effective(44)` の
配置しか取っていない。この配置では `max(natural, effective) == natural` となり、期待値は
「offset 0 = 上端」に縮退する。したがって **`max()` の実効行高さ側の枝が遷移中に効くこと**
(bounds 200 / effective 120 / natural 40 → offset 40 を維持) はどのテストでも固定されていない。
定常状態のテストは `bounds == effective` の状況しか作れないため、この枝を代替できない。
実装を `min(bounds.height, natural.height)` に退化させても遷移中テストは通ってしまう
(定常テストが落ちるので全体としては検出できるが、遷移中の契約としては無防備)。

**推奨修正**: `test_行が実効行高さより高い間は定常高さ基準の縦位置を維持する` と同じ
`CustomCellHostedContent` 直接ホストの手法で、`effectiveCellHeight: 120` / content 40pt /
host frame 高さ 200pt のケースを 1 件足し、probe の midY が「行上端 + 60pt」(= 定常高さ 120 の
中央) に留まることを検証する。

### [🟡 Minor] 4 巡目の証跡に修正前の再現フレームが無く、サンプリング間隔の限界も明示されていない

**該当箇所**: `kasane/changes/add-cell-types-custom/ui/verification/index.md:241-262`
(「iOS — 4 巡目」節) と `ios-sim-iphone17-ios265-13-*` / `14-*`

**問題点**: `concepts/cross/conventions/runtime-behavior-verification.md` は実行時挙動の修正について
「修正前に実環境で症状を再現 → 同一手順で解消を確認 → 証跡を残す」を完了条件としている。
3 巡目は `08-expand-before-f1-overshoot` (修正前) と `09-expand-after-*` (修正後) の A/B を
残しているのに対し、4 巡目は **postfix フレームのみ**で修正前の折りたたみ症状の再現フレームが無い。
加えて `simctl io screenshot` の連写は約 3.5 fps であり、60fps のアニメーション上で 1〜2 フレームだけ
現れる中間状態は原理的に取りこぼしうる。この 2 点が重なると、証跡だけからは「症状が消えた」と
「症状のフレームを撮り逃した」を区別できない。

なお実害は小さいと判断している。レビュアー側の A/B (上記サマリー) で、修正を外すと新規テストが
落ちることを確認しており、修正後の配置は `bounds.height` を参照しないため縮小遷移中のどの中間
フレームでも縦位置が動かないことが演繹的に保証される。

**推奨修正**: 次のいずれか。(a) `restingHeight` を一時的に修正前へ戻したビルドで同一手順の
連写を撮り、`13-collapse-prefix-*` として A/B を揃える。(b) 撮り直さない場合は index.md の
4 巡目節に「連写は約 3.5 fps でありフレームの取りこぼしがありうること」「修正前との識別は
`CustomCellTests` の該当テストが修正前実装で失敗することで担保していること」を明記し、
証跡の射程を読み手に伝える。

### [🔵 Suggestion] `effectiveCellHeight` の property doc が下限ガードに触れていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:41-42`

**問題点**: 「`CellStyle.cellHeight ?? Theme.rowHeight` の解決値」と書かれているが、実際の解決は
`CellStyle.cellHeight` → `Theme.rowHeight` (正値のときのみ) → 最低行高、さらに最終値を最低行高 (48pt) で
下限ガードする (`EffectiveStyle.effectiveCellHeight`)。`cellHeight = 20` を指定した場合の実引数は
20 ではなく 48 になるため、この doc だけを読むと配置の基準値を読み違える可能性がある。

**推奨修正**: 「最低行高で下限ガードされた実効行高さ」と一言添えるか、解決規則の詳細は
`EffectiveStyle.effectiveCellHeight` にあると (コード識別子参照は規約上許容) 示す。

## アクションプラン

1. (任意・低コスト) 型 doc の要約行を本文に合わせて修正する。
2. (任意) 遷移中に `effectiveCellHeight` 側が基準になるケースのテストを 1 件追加する。
3. (任意) index.md の 4 巡目節に証跡の射程 (サンプリング間隔・修正前との識別根拠) を追記する。
   撮り直す場合は prefix フレームを揃える。
4. (任意) property doc に下限ガードの一言を足す。

いずれも挙動を変えない記述・テストの追補であり、本判定の APPROVED を保留する条件ではない。

## 確認した観点 (指摘に至らなかったもの)

- **合意済みスコープとの一致**: 配置規則の改良・実効 cellHeight の受け渡し経路・静止状態の
  非退行のいずれもスコープどおり。スコープ外の変更 (公開 API・他 Cell 種別・Android 側) は無い。
- **足場凍結**: proposal / design / specs / tasks に変更なし (`git status` で確認)。
  更新は `ui/verification/` の証跡のみで、これは足場の書き換えに当たらない。
- **deviation.md**: 記録済みの乖離 (content が行に収まらないときの縦位置は spec 未規定の実装判断)
  の延長線上にあり、新たな無断逸脱は無い。
- **エッジケース**: `effectiveCellHeight` が 0 や負でも `max`/`min` により上端揃えへ縮退し
  クラッシュしない。`bounds < 定常高さ` の遷移 (行が伸びる途中) では `min(bounds, ...)` の
  クランプにより content が bounds 外へ押し出されない。
- **性能・リソース**: `sizeThatFits` の呼び出し回数は従来と同じで、追加の測定・確保は無い。
- **命名・スタイル**: 既存の `EffectiveStyle.effectiveCellHeight` と同一語彙で、
  `KsCellViewSupport.applyEffectiveHeight` が使う値と同じ解決結果を渡している (二重解決なし)。
- **コメント規約**: 触れた 4 ファイルの禁止参照 0 件。新規コメントに変更提案 ID・通番・
  デルタスペック構文キーワード・履歴記述は無い。
- **concepts との整合**: `core/styling/cell-row-layout.md` は共通行レイアウトの契約であり、
  CustomCell は ADR-0015 で適用除外。今回の変更が抵触する規約は見当たらない。

---

## 修正確認 (2026-08-04)

指摘 4 件に対する修正を適用した working tree diff を再レビューした。**最終判定: APPROVED (確定)**。
Minor 3 件・Suggestion 1 件はすべて解消、新規の指摘なし。

### 再実測

- 全件テスト (iPhone 17 Simulator / `xcodebuild test -scheme KsSettingsView-Package`):
  Core 83 件 + SwiftUI 68 件 + UI **398 件** = **549 件 / 0 failures**、`** TEST SUCCEEDED **`。
  `CustomCellTests` は **39 件 / 0 failures** (前回 38 件 + 追加 1 件)。
- コメント規約 lint: 触れた 4 ファイルで禁止 0 件。
- 足場アーティファクト (proposal / design / specs / tasks) は引き続き未変更。更新は
  `ios/` の 4 ファイルと `ui/verification/index.md` のみ。

### 指摘ごとの解消状況

| 指摘 | 状態 | 確認内容 |
|---|---|---|
| 🟡 Minor-1 型 doc の要約行 | **解消** | `CustomCellRowPlacement.swift:11-13`。「定常状態の行の高さ (`max(content の自然高, 実効行高さ)`) を基準に縦位置を決め…content が行に収まらないときは上端揃え」となり、誤りだった「行が実効行高さより高い間は上端揃え」は消えた。要約行と「# 配置規則」節の食い違いは無くなっている |
| 🟡 Minor-2 実効行高さ側の枝の未検証 | **解消** | `CustomCellTests.swift` に `test_行が実効行高さより高い間も実効行高さの中での縦中央は維持される` を追加 (effective 120 / natural 40 / host 高さ 200 → minY 40)。**A/B で識別力を実測**: `restingHeight` から `max(…, effectiveCellHeight)` を外して `min(bounds.height, natural.height)` に退化させると、39 件中 3 件 (本テスト + 定常状態の 2 件) が失敗する。遷移中に実効行高さ側が基準になる契約がテストで固定された。検証後にファイルは復元済み (sha `1540517e…` 一致) |
| 🟡 Minor-3 証跡の射程 | **解消** | `ui/verification/index.md` 4 巡目節に「修正前フレームは撮っていない」「連写は約 3.5 fps でフレーム単体では撮り逃しと区別できない」「修正前後の差別化はユニットテストの A/B 実測が担保する」「修正後の配置は行の高さを参照しないため演繹的に決まる」が明記された。読み手が証跡の限界を誤読しない記述になっている。末尾のテスト件数も 2 件へ更新済み |
| 🔵 Suggestion-1 property doc の下限ガード | **解消** | `CustomCellRowPlacement.swift:42-43`。「`CellStyle.cellHeight ?? Theme.rowHeight` を最低行高さで下限ガードした解決値」となり、`cellHeight` に最低行高未満を指定した場合の実引数を読み違えない |

### 補足 (指摘に至らない観察)

- 要約行の「行の高さがそれと異なる間 (高さ遷移中) も縦位置を動かさない」は、行が定常高さより
  **低い**方向の遷移では厳密には成り立たない (`min(bounds.height, …)` のクランプが働き、
  offset は bounds に応じて縮む)。ただしこのクランプは「content を bounds の外へ押し出さない」
  ための意図的な設計で、`placeSubviews` のインラインコメントに理由が書かれており、
  該当する遷移も実運用ではほぼ起きない (行が伸びる方向の遷移では content の自然高が bounds を
  上回るため上端揃えに縮退する)。指摘として修正を求める性質のものではない。
