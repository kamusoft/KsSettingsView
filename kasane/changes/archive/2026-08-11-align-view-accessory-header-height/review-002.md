# レビュー結果: align-view-accessory-header-height (002 回目)

**日付**: 2026-08-11
**判定**: APPROVED

## サマリー

review-001 の指摘 5 件 (Major 1 / Minor 1 / Suggestion 3) はいずれも解消されている。Major の「固定高さ時に hosted view が領域を埋めない」は `applyHostedViewFill` の追加で iOS の 4 辺 pin と同じ占有へ揃い、追加契約は deviation.md に記録済み。実測でも状態B の Header 領域 96px (= 48dp) が accessory の背景色で全面塗りになっており (画素検査で y 398-493 が一様に `(159,192,238)`)、修正前の下部 32px の未描画帯は消えている。

ビルド・テストは Android (1111 件 × debug/release = 2222 件, 0 failures) / iOS (457 件, 0 failures) ともに green。追加された占有テスト 4 件には回帰検出力があることをミューテーション実測で確認した (下記)。デルタスペック 2 Requirement / 11 Scenario と deviation の追加契約はすべて実装・テストに対応が取れている (詳細は verify-002.md)。

残るのは Suggestion 2 件のみで、いずれも挙動に影響しない記述・防御コードの整合であるため APPROVED とする。

## 前回指摘の解消状況

| review-001 の指摘 | 状態 | 根拠 |
|---|---|---|
| 🟠 Major: 固定高さ時に hosted view が領域を埋めず iOS と非対称 | 解消 | `SectionAccessoryViewHolders.kt:359-373` (`applyHostedViewFill`) / テスト 4 件追加 / `verification/android-header-height-states.png` の画素検査で全面塗りを確認 / deviation.md に追加契約を記録 |
| 🟡 Minor: `PAYLOAD_CONTENT` の KDoc が「3 引数版は未実装」と矛盾 | 解消 | `KsSettingsListAdapter.kt:240-241` を現在形へ書き換え。`KsSettingsView.kt:639` / `:907` の `PAYLOAD_THEME` 側も併せて整合。リポジトリ内に「3 引数版 `onBindViewHolder` は未実装」の記述は残っていない |
| 🔵 Suggestion: `applySectionHeaderHeight` の可視性 | 解消 | `SectionAccessoryViewHolders.kt:322` が `private` へ。同ファイルの他ヘルパと揃った |
| 🔵 Suggestion: `layoutParams == null` 分岐が到達不能 | 解消 | 分岐を削除し、非 null 前提の根拠 (生成元が必ず layoutParams を設定する) を KDoc に自己完結で明記 (`:317-318`) |
| 🔵 Suggestion: rebound スクショが states と byte 同一 | 解消 | 3 枚とも再取得済み (states/rebound の SHA-256 は前回の `e21904cb…` から `5c4c8dda…` へ更新)。`ui/brief.md:37` に「再表示後の画面が states と同一表示になるためバイト一致」と意図を記録 |

## 指摘事項

### [🔵 Suggestion] `SectionAnyViewAccessoryViewHolder` のクラス KDoc「ライフサイクル」が bind 経路だけを述べている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:125-130`

**問題点**:

```
 * # ライフサイクル
 *
 * `bind` のたびに container をクリアして新しい中身を入れ直す。これは `KsAnyView` が
 * 差分検出に参加しないため、bind の度に最新の中身が入ってくる前提によるもの。
```

本変更で、この ViewHolder には `bind` を通さずに状態を更新する 2 つ目の入口 (`applyHeaderHeight`、`:171`) が加わった。しかもその経路は「container をクリアしない」ことそのものが目的で、デルタスペックの Requirement「表示済み Header の headerHeight 変更は hosted view を維持したまま反映される」を成立させている契約上の要である。

クラス KDoc の「ライフサイクル」節は、読み手が「hosted view は更新をまたいで生き残るのか」を最初に確かめに来る場所であり、そこに「bind のたびにクリアされる」だけが書かれていると、中身が保持される経路の存在を見落とす。`applyHeaderHeight` 側の KDoc には書かれているが、そちらへ辿り着く前に結論を出せてしまう。

**推奨修正**: ライフサイクル節に「高さのみの更新 (`applyHeaderHeight`) は container をクリアせず、hosted view とその内部状態を維持する」旨の 1 文を足す。

### [🔵 Suggestion] `applyHostedViewFill` に到達不能な null 分岐がある (前回 Suggestion と同型)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:367`

```kotlin
val lp = child.layoutParams ?: continue
```

**問題点**: container の子は `bindKsAnyView` が必ず `FrameLayout.LayoutParams` を明示して `addView` するため (`:415-421` / `:433-439`)、`layoutParams` が null になる経路はない。review-001 の Suggestion を受けて `applySectionHeaderHeight` からは同型の分岐を削除し、非 null 前提の根拠まで KDoc に書いたところなので、その 20 行下で同じ防御が復活しているのは方針として揃っていない。

**推奨修正**: `?: continue` を外して非 null 前提で扱う。残す場合は、`applySectionHeaderHeight` と同様に「どの経路で null になり得るか」を KDoc に自己完結で書く。

## 確認した観点 (指摘に至らなかったもの)

- **回帰検出力の実測** (lessons/code-review L-001): `applyHostedViewFill` の `if (fill)` を `if (false)` に固定するミューテーションを入れて `:ks-settingsview-ui:testDebugUnitTest` を実行したところ、834 件中 **3 件だけが FAILED**、いずれも新規の占有テスト (`固定高さのとき hosted view は Header 領域いっぱいに広がる` / `hosted view の占有範囲は固定と自動の切り替えに追随する` / `高さのみの変更でも hosted view の占有範囲が追随する`)。自動高さ側のアサーションと既存の高さ解決テストは PASSED のままで、占有テストがトートロジーではなく実際に fill を検出していることを確認。実施後 shasum 一致で原状復帰し、再実行で green を確認済み
- **payload 振り分けの堅さ**: `payloads.all { it == PAYLOAD_HEADER_HEIGHT }` は、`PAYLOAD_THEME` / `PAYLOAD_CONTENT` が混在・蓄積した場合と payload 空の場合 (`isNotEmpty()` ガード) の双方で必ずフル bind へ落ちる。`getChangePayload` の `heightOnly` 判定は `areContentsTheSame` が false のときのみ到達するため、「View 同士 + 中身参照が同一」= 高さだけが違う、が成立する。Text↔View の切替時は `isSameAccessoryContent` が false になり `PAYLOAD_CONTENT` へ倒れる
- **Footer 経路の隔離**: 3 引数版 `onBindViewHolder` の高さ経路は `item is CellListItem.SectionHeader` でガードされ、`SectionFooter` は常に `super` へ。`CellListItem.SectionFooter` に `headerHeight` は存在せず、`bind(accessory, theme, isHeader = false)` 経由で常に `WRAP_CONTENT` + 中身 `WRAP_CONTENT` に戻る
- **hosted view の占有切り替えの往復**: `applyHeaderHeight` は固定/自動どちらでも必ず `applyHostedViewFill` を通るため、ViewHolder 再利用でも動的変更でも `MATCH_PARENT` が残留しない (container 自身が `WRAP_CONTENT` のとき子が `MATCH_PARENT` だと高さが決まらなくなる問題を回避)。テスト `hosted view の占有範囲は固定と自動の切り替えに追随する` が往復を押さえている
- **Root accessory への波及なし**: `RootAnyViewAccessoryViewHolder` は無変更で、Root H/F の hosted view は従来どおり内容なりの高さ (Root に高さ指定は存在しない)
- **Theme.headerHeight 変更時の経路**: `applyThemeInternal` は `PAYLOAD_THEME` でフル bind へ落ちるため hosted view は作り直される。ただしデルタスペック Requirement 2 が保持を求めるのは `Section.headerHeight` の変更経路であり、Theme 更新が全行フル bind になるのは本変更以前からの既定動作。仕様逸脱ではない
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` = 禁止 0 件 (572 ファイル)。新規・改訂コメントに change-id / Phase / レビュー通番 / デルタスペック構文キーワードの混入なし
- **足場の逆流**: `proposal.md` / `specs/` / `exploration.md` は無変更。`tasks.md` はチェックの反転のみ、`ui/brief.md` は「視覚照合結果」節の追記のみで既存記述の書き換えなし
- **iOS プロダクションコード**: 無変更 (差分はテストのみ) で契約どおり
- **本レビューの独立性**: `second-opinion-code-001.md` は独立文脈を保つため読んでいない (review-001 と同じ扱い)

## アクションプラン

1. Suggestion 2 件 (クラス KDoc のライフサイクル節への追記、`applyHostedViewFill` の到達不能分岐) は、蒸留前の仕上げでまとめて処理するか、見送りを明示する。いずれも挙動に影響しないため、本変更のマージ判断を保留する理由にはならない

## 追記: Suggestion 2 件の修正確認 (2026-08-11)

上記 Suggestion 2 件の修正差分を `SectionAccessoryViewHolders.kt` について確認した。判定は **OK** (追加の指摘なし)。

| Suggestion | 修正内容 | 判定 |
|---|---|---|
| クラス KDoc の「ライフサイクル」節が bind 経路だけを述べている | `:130-131` に「ただし [applyHeaderHeight] は container をクリアしないもう 1 つの入口で、高さのみの更新では hosted view が入れ替わらずに生き残る。」を追記 | OK — `applyHeaderHeight` (`:194-202`) は `applySectionHeaderHeight` と `applyHostedViewFill` しか呼ばず container をクリアしないため、記述は実装と一致する。読み手が「ライフサイクル」節だけで hosted view の生存を判断できるようになり、指摘の意図どおり |
| `applyHostedViewFill` の到達不能な null 分岐 | `:390` の `?: continue` を削除し、直前に「子は bindKsAnyView が LayoutParams 付きで addView するため、layoutParams は非 null」のコメントを付与 | OK — `bindKsAnyView` は Compose 枝 (`:438-444`) / AndroidView 枝 (`:456-462`) の双方で `FrameLayout.LayoutParams` を明示して `addView` しており、container の子はこの経路以外から追加されない。コメントは外部文書に依存せず自己完結しており、`applySectionHeaderHeight` の非 null 前提と方針が揃った |

- 差分は上記 2 箇所のみで、挙動を変えるコードの変更はない (`?: continue` の削除は、到達しない null 経路の扱いが skip から NPE へ変わるだけ)
- ビルド・テスト: `:ks-settingsview-ui:testDebugUnitTest` は現在のソース内容で UP-TO-DATE (= この差分を含む実行が成功済み)、結果 XML の集計で **834 件 / failures 0 / errors 0 / skipped 0**
- コメント規約: 追加コメントに change-id / Phase / レビュー通番 / デルタスペック構文キーワードの混入なし

この修正により review-002 の指摘は全件解消。**判定は APPROVED のまま**で、verify-002 の VALID 判定にも影響しない (デルタスペック対応部分のコードは無変更)。
