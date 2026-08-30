# セカンドオピニオン: implement-modern-style (spec-001)
**相方**: codex / **日付**: 2026-08-20 / **対象**: 提案一式 (proposal / design / specs 4本 / tasks / ui/brief)
---
# レビュー結果: implement-modern-style

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 8 / Minor 2 / Suggestion 1

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。実装前に解消しないと、承認モックと異なる表示やプラットフォーム間の挙動差が生じる仕様上の未決事項があります。

## 指摘事項

### [🟠 Major] 既定 Theme では Modern の箱と下地が同色になり、承認モックを再現できない

**該当箇所**: [ui/brief.md:19](kasane/changes/implement-modern-style/ui/brief.md:19)、[Theme.swift:131](ios/Sources/KsSettingsViewUI/Theme.swift:131)、[Theme.kt:73](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:73)

**問題点**: 箱は `cellBackgroundColor`、下地は `backgroundColor` とされていますが、現行の `Theme()` は両 OS とも両者が白です。ボーダーの既定も透明・幅0なので、余白や角丸があっても箱の輪郭を視認できません。一方、承認モックは灰色の下地と白い箱を前提にしています。今回の公開 API は4属性だけなので、既定 Modern が目標外観へ到達する方法が定義されていません。

**推奨修正**: Classic を視覚変更せず Modern の既定下地を成立させる方法を決定してください。たとえば style 別 canvas 既定、背景色の未指定状態を表せる API、または別の既定装飾を検討し、`Theme()` + Modern の色関係を Scenario と移行影響に明記してください。

---

### [🟠 Major] Section の縦 margin を置く位置がモック・spec・Android 設計で一致していない

**該当箇所**: [variant-a-ios26.html:24](kasane/changes/implement-modern-style/ui/mock/variant-a-ios26.html:24)、[settings-view-android-ui/spec.md:26](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:26)、[design.md:57](kasane/changes/implement-modern-style/design.md:57)

**問題点**: 承認モックの margin は Header・Cell 箱・Footer 全体を包む Section の外側です。Android spec は Header/Footer を「水平方向の inset 対象にしない」と限定していますが、design は Header/Footer 行を `getItemOffsets` の inset 対象全体から除外しています。そのまま実装すると、上 margin が Header と箱の間、下 margin が箱と Footer の間に入る可能性があります。また隣接 Section の bottom と次 Section の topを加算するかも未確定です。

**推奨修正**: `sectionMargin` と「Cell 箱の inset」を別概念として定義し、top/bottom が Header/Footer の外側・内側のどこに入るか、隣接時に加算するか、先頭・末尾 Section の画面端にも適用するかを両 OS 共通の Scenario で固定してください。

---

### [🟠 Major] Cell 背景・選択背景・ボーダーの合成順が未決のまま実装判断へ先送りされている

**該当箇所**: [design.md:38](kasane/changes/implement-modern-style/design.md:38)、[design.md:57](kasane/changes/implement-modern-style/design.md:57)、[design.md:83](kasane/changes/implement-modern-style/design.md:83)

**問題点**: iOS は背景 decoration、Android は `onDraw` で背景とボーダーを Cell の背後に描く設計ですが、現行 Cell は自身を不透明背景で塗ります。[CellBaseLayout.swift:109](ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:109)、[CellBaseLayout.kt:476](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:476)  
そのためボーダーが Cell 背景に被覆されたり、先頭・末尾 Cell の `CellStyle.backgroundColor` や押下背景が角丸からはみ出したりします。design は対処を実装時に決めるとしており、既存 CellStyle 契約との優先関係もありません。

**推奨修正**: 次を実装前に契約化してください。

- ボーダーを Cell の前面・背面のどちらへ描くか
- stroke を箱の内側へ収めるか
- 先頭・末尾 Cell の通常背景と選択背景を箱形状で clip するか
- `CellStyle.backgroundColor` が箱背景を上書きする範囲

非ゼロ border、異なる Cell 背景、押下状態を含む Scenario と視覚テストを追加してください。

---

### [🟠 Major] Cell が0件の Section／全 Cell が非表示の Section の挙動が未定義

**該当箇所**: [settings-view-ios-ui/spec.md:24](kasane/changes/implement-modern-style/specs/settings-view-ios-ui/spec.md:24)、[settings-view-android-ui/spec.md:24](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:24)、[KsSettingsView.kt:993](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:993)

**問題点**: 現行モデルは Cell が0件でも Header/Footer を表示でき、可視 Cell がすべて除外される場合もあります。spec の「Cell 行の範囲のみ」では箱を描かないことは推測できますが、Header/Footer、Section margin、次 Section との間隔がどうなるか決まりません。Android では margin を載せる Cell 行自体が存在しないため、実装差が出やすい状態です。

**推奨修正**: 「可視 Cell が0件なら箱と separator は生成しない」を明記したうえで、Header/Footer と margin を残すか、Section 全体を空扱いにするかを決定し、空 Sectionと全 Cell 非表示の両 Scenarioを追加してください。

---

### [🟠 Major] direction-aware な型と「左／右」契約の関係が未定義

**該当箇所**: [settings-view-ios-ui/spec.md:7](kasane/changes/implement-modern-style/specs/settings-view-ios-ui/spec.md:7)、[settings-view-android-ui/spec.md:7](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:7)、[settings-view-android-ui/spec.md:40](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:40)

**問題点**: iOS は `NSDirectionalEdgeInsets`、Android は `PaddingValues` を採用しており、本来 leading/trailing と LayoutDirection を扱う型です。一方、separator と margin の契約は物理的な「左端／右端」で書かれています。RTL 時に start側を insetするのか、常に物理左を insetするのか判定できません。

**推奨修正**: 公開契約を leading/trailing 基準または物理 left/right 基準のどちらかに統一し、RTL の非対称 marginと中間 separator を確認する Scenario を両 OS に追加してください。

---

### [🟠 Major] 公開寸法に対する負値・非有限値・過大値の扱いが決まっていない

**該当箇所**: [settings-view-ios-ui/spec.md:5](kasane/changes/implement-modern-style/specs/settings-view-ios-ui/spec.md:5)、[settings-view-android-ui/spec.md:5](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:5)

**問題点**: `CGFloat`、`Dp`、`PaddingValues` には負値、NaN・Infinity相当、`Dp.Unspecified`、箱サイズを超える radius/border widthを渡せます。受理、拒否、0への正規化、幾何上限への clamp のどれを行うか未指定です。このままではプラットフォームごとに不正 geometry、描画例外、無表示など異なる結果になり得ます。

**推奨修正**: 各属性の有効範囲と正規化規則を定義してください。最低限、負値・非有限値・過大 radius・箱幅以上の border width の Scenario を追加し、Theme 構築時に拒否するか描画時に正規化するかも統一してください。

---

### [🟠 Major] 既定値と視覚受け入れ基準が自己完結しておらず、テストの期待値を決められない

**該当箇所**: [design.md:74](kasane/changes/implement-modern-style/design.md:74)、[design.md:94](kasane/changes/implement-modern-style/design.md:94)、[ui/brief.md:22](kasane/changes/implement-modern-style/ui/brief.md:22)、[tasks.md:32](kasane/changes/implement-modern-style/tasks.md:32)

**問題点**: design の Open Questions は iOS 既定値を未確定のまま残していますが、brief では承認済みです。iOS の具体値は HTML 内にしかなく、CSS px を UIKit ptへそのまま写すのかも明記されていません。Android も「現行実装値」という移動する参照です。さらにスクリーンショット取得手順は [config.yaml:57](kasane/config.yaml:57) で空であり、端末、OS、light/dark、viewport、許容差がありません。

**推奨修正**: design/brief に名前付きの既定トークンとして確定値と単位を記録し、Open Questionを解消してください。スクリーンショット検証についても端末・OS・表示 mode・デモデータ・取得手順・比較許容差を固定してください。

---

### [🟠 Major] Android の長い Section をスクロールした場合の箱端処理が仕様・設計にない

**該当箇所**: [design.md:57](kasane/changes/implement-modern-style/design.md:57)、[ModernSectionDecoration.kt:104](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ModernSectionDecoration.kt:104)、[settings-view-android-ui/spec.md:33](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:33)

**問題点**: 現行 decoration は画面内の childだけから Section の top/bottom を集計します。長い Section の先頭または末尾が画面外にあると、最初／最後の可視 Cell を箱端と誤認し、スクロール途中に角丸やボーダー端が現れる可能性があります。今回も同じ `onDraw` 方式を継続しますが、オフスクリーン境界の扱いと Scenario がありません。

**推奨修正**: Section 本来の端が画面外なら箱を viewport 外まで延長し、角丸・上下 borderを描かない等の規則を決めてください。1 Section がviewportより長いケースで、先頭・中間・末尾までスクロールする Scenario と描画テストを追加してください。

---

### [🟡 Minor] Android `PaddingValues` の「値等価性」が定義不足

**該当箇所**: [settings-view-android-ui/spec.md:7](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:7)、[Theme.kt:73](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:73)、[KsSettingsView.kt:201](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:201)

**問題点**: `PaddingValues` はinterfaceなので、実効 padding が同じでも実装クラスの `equals` 次第で Theme の等価性が変わります。逆に可変な独自実装を同一参照のまま変更すると、Theme setter の同値スキップで再描画されない可能性があります。「4属性は値等価性に参加する」だけでは保証範囲が曖昧です。

**推奨修正**: `Theme.equals` は `PaddingValues.equals` に委譲するだけなのか、LayoutDirectionごとの解決値を比較するのかを明記してください。独自・可変 `PaddingValues` を非保証にする場合も公開契約へ明記してください。

---

### [🟡 Minor] デルタスペックが Kasane の UI lint に反している

**該当箇所**: [settings-view-ios-ui/spec.md:24](kasane/changes/implement-modern-style/specs/settings-view-ios-ui/spec.md:24)、[settings-view-android-ui/spec.md:38](kasane/changes/implement-modern-style/specs/settings-view-android-ui/spec.md:38)、[delta-spec.md:29](~/.agents/skills/ksn-core/references/delta-spec.md:29)

**問題点**: デルタスペックに16pt/16dp、1物理 pixel、箱の配置、Header/Footer の内外などの視覚パラメータ・レイアウト記述があります。Kasane規約ではこれらを `ui/brief.md` または mockへ置き、specには観察可能な状態遷移だけを書くことになっています。

**推奨修正**: 公開 API、style切替、Theme更新による状態遷移をデルタスペックへ残し、具体的な配置・寸法・描画文法はbrief/mockへ移してください。テストから参照する値はbrief/design側で名前付きトークンとして固定すると、lintと検証可能性を両立できます。

---

### [🔵 Suggestion] Sample のプラットフォーム間パリティをタスクの明示的な完了条件にする

**該当箇所**: [samples-ios/spec.md:5](kasane/changes/implement-modern-style/specs/samples-ios/spec.md:5)、[samples-android/spec.md:5](kasane/changes/implement-modern-style/specs/samples-android/spec.md:5)、[sample-parity.md:15](kasane/concepts/cross/conventions/sample-parity.md:15)

**問題点**: 両サンプルspecは似た内容ですが、画面タイトル、文言、Section/Cell数、プリセット内容の完全一致を受け入れ条件にしていません。既存規約が要求する「一字一句・構成一致」を実装者が個別タスクから見落とす余地があります。

**推奨修正**: `tasks.md` にiOS/Android対応画面の文言・Section/Cell構成・プリセット値を突き合わせるパリティ確認を追加してください。

## アクションプラン

1. 既定 Modern の canvas／box 色関係と縦 margin の所有範囲を決定する。
2. Cell背景・選択背景・borderの合成順、空 Section、長い Section の描画契約を確定する。
3. directionality、無効寸法、`PaddingValues` 等価性を公開 API 契約へ追加する。
4. 既定トークンとスクリーンショット検証環境を固定する。
5. UI lintに従って視覚仕様をbrief/mockへ整理し、追加 Scenarioとテストタスクを対応させる。

## 突き合わせ結果 (ホスト側判定、2026-08-20)

ホスト側自己レビュー (指摘ゼロ) との突き合わせ。全指摘が「相方のみ」のため根拠で採否判定した。

| # | 指摘 | 採否 | 判定根拠・対応 |
|---|---|---|---|
| M1 | 既定 Theme で箱と下地が同色 | **採用** | コードで検証済み — 両 OS とも backgroundColor / cellBackgroundColor の既定が白 (Theme.swift:245 / Theme.kt:135)。決着 (ユーザー提案): 色既定は導入せず、モック下地をサンプル SampleTheme の PaleBackColorPrimary に差し替え「モック=デモ Theme の見た目」と定義。spec に「Modern は新たな色既定を導入しない」を明記 |
| M2 | 縦 margin の所有範囲がモックと design で不一致 | **採用** | 承認モックは Header+箱+Footer を包む Section 単位の外側余白。spec / design をモックに合わせて再定義 |
| M3 | Cell 背景・選択背景・border の合成順が未決 | **採用** | 観察可能な合成契約 (border 最前面・箱形状 clip・CellStyle との関係) を spec へ昇格 |
| M4 | 可視 Cell 0件の Section の挙動未定義 | **採用** | 現行モデルで到達可能な状態 (lessons L-001 の検算漏れ)。Scenario 追加 |
| M5 | direction-aware 型と「左右」契約の不整合 | **採用 (軽)** | 契約文言を leading/trailing 基準へ統一 |
| M6 | 負値・非有限値・過大値の扱い | **採用 (縮小)** | 描画時正規化 (負値→0、radius clamp) を1行契約化。NaN 等の列挙は既存 API 群も行っておらず過剰と判断 |
| M7 | 既定値の自己完結性・screenshot 環境 | **採用 (部分)** | design の Open Question と brief の矛盾を解消し、確定既定値を design に記録。screenshot 環境の数値固定 (許容差等) は降格 — 視覚照合は approved.png 照合 (ksn-ui 規律) で足りる |
| M8 | Android 長い Section のオフスクリーン箱端 | **採用** | 現行 onDraw の可視 child 集計に実在する欠陥の継承リスク。契約 + Scenario + テストタスク追加。iOS は layout 座標系のため非該当 |
| Min1 | PaddingValues の等価性定義 | **採用 (軽)** | equals 委譲と可変実装非保証を契約注記 |
| Min2 | デルタスペックの UI lint 違反 | **降格** | 指摘引用が不正確 — specs に 16pt/16dp の生値は書いていない (「Classic と同じ規則」参照形式)。「箱の配置・Header/Footer 内外」は観察可能な構造契約で spec の対象。「1物理 pixel」は既存 concepts の挙動契約 (1dp 換算禁止) の再掲で意図的に維持 |
| Sug1 | サンプルのパリティ確認タスク | **採用** | conventions/sample-parity.md (一字一句・構成一致) が実在。ホスト側の conventions 参照漏れ。tasks に照合タスク追加 |

採用 10 (うち縮小・部分 4) / 降格 1 / 未解決 0。全採用指摘は 2026-08-20 に proposal / design / specs / tasks / ui へ反映済み (M1 はユーザー提案の「モック下地をサンプル Theme 色へ」で決着)。
