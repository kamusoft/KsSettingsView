---
type: concept
title: 設定 list の外観と補助領域
description: Classic・Modern style と Section 装飾4属性・Section・Root Header / Footer の配置原則
tags: [styling, list, section, accessory, modern]
timestamp: 2026-08-25
---

# 設定 list の外観と補助領域

この文書は、iOS / Android の Classic・Modern style と Header / Footer の共通契約を説明する。読むと、style 切替が変更する範囲、Modern の Section Container と Theme の Section 装飾4属性、Root と Section の補助領域、list 下地と Cell 背景の違い、Classic / Modern それぞれの separator 規則が分かる。Theme の一般的な解決順は [スタイルの所有と実効値解決](style-resolution.md) を先に読むと分かりやすい。

## style (Classic / Modern)

| style | 共通の意図 | iOS | Android |
|---|---|---|---|
| Classic | flat な罫線で Cell と Section を区切る | `.plain` list appearance | Cell の1物理 pixel の細線 |
| Modern | 余白と角丸の Container で Section のまとまりを示す | compositional layout 上の自前 Section 装飾 (ios/ADR-0003) | RecyclerView の ItemDecoration による Section 単位の角丸背景 |

style 切替は Section の装飾と区切り方だけを変更し、`SettingsRoot`、Section / Cell の identity (ID による同一性)、[Cell renderer registry](../architecture/cell-renderer-registry.md)、Cell 内容を変えない。

iOS の Modern は `.insetGrouped` list appearance を使用しない — OS 外観への自動追従を捨てる代わりに、下記4属性の制御可能性をライブラリが所有する (ios/ADR-0003)。

## Modern の Section Container

Container (角丸背景と Border) は各 Section の **Cell の範囲のみ**を覆う。Section Header / Footer は Container の上外側 / 下外側に置き、Root Header / Footer は装飾対象に含めない (両 OS 共通)。Container の背景色は `Theme.cellBackgroundColor`、その外側の下地は `Theme.backgroundColor` から解決し、Modern は新たな色既定を導入しない — Container の視認性は両者の対比に依存する。実装の識別子およびプラットフォーム文書ではこの Container を Box と呼ぶ (`SectionBoxMetrics` 等。en の利用者向け文書では section box)。

Container と Cell 背景の合成契約:

- Border は Cell 背景・押下 / 選択背景より前面に描画され、隠されない
- Section 先頭 / 末尾 Cell の背景 (`CellStyle.backgroundColor` を含む) と押下 / 選択背景は Container の角丸形状で clip され、角の外へはみ出さない
- `CellStyle.backgroundColor` は当該 Cell の領域を Container 背景より前面で塗る

Container は構造変更 (`SettingsRootDiff` による Cell の挿入・削除) 後も Cell 範囲に追従する。viewport より長い Section では、角丸と Border の上端 / 下端は実際の Section 端にのみ現れ、viewport 内の最初 / 最後の Cell を Section 端と誤認しない。可視な Cell が0件 (すべて非表示) の Section は Container と separator を生成しないが、後述の `sectionMargin` はその Section にも適用され、余白は残る。

## Header と Footer

Section Header / Footer は `Section` の `SectionAccessory`、Root Header / Footer は Host / Bridge (Native Host と宣言 UI Bridge) の `RootAccessory` から描画する。Root 補助領域を `SettingsRoot` に含めない。

Header / Footer は list 内容と共に scroll し、画面端へ固定しない。text と platform の任意 View を利用できる。

Section Header / Footer の表示は「可視トグル (`Section.isHeaderVisible` / `isFooterVisible`、既定 `true`) && 内容あり」の AND で判定する (core/ADR-0023)。内容の不在は「未設定または空 text」で header / footer 共通・両 OS 共通とし、view accessory は空でも常に「内容あり」として扱う。トグルは内容を保持したまま隠す専用で、内容が無いものを表示させる手段ではない。

text の Section Header は領域の下側、Section Footer は上側へ寄せ、Cell 群とのまとまりを示す。Cell 群に面する側 (Header は下・Footer は上) には 4pt / 4dp の text 間隔を置く。Root Header / Footer の text にはこの間隔を入れない — Root は利用者がカスタム View を設定する想定で、ライブラリ側の余白を所有しない (core/ADR-0027)。

Header 高さは accessory 種別 (text / view) に依らず、正の `Section.headerHeight`、正の `Theme.headerHeight`、内容に応じた自動高さの順で解決する (core/ADR-0021)。公開契約で意味が定まる値はどちらも `-1` (自動) と正値だけで、0 やその他の負値の挙動は契約しない。高さ解決は存在判定 (上記 AND) の後に適用し、高さ指定は Header の存在を作らない (core/ADR-0023)。固定高さのとき内容がはみ出す分は clip し、view accessory の hosted view は Header 領域いっぱいに配置する (両 platform 対称)。`headerHeight` は Header 専用で Footer には適用しない。

Header / Footer の色は Android では Theme の `headerTextColor` / `headerBackgroundColor` と `footerTextColor` / `footerBackgroundColor` から解決する。iOS は text 色を Theme から解決する一方、Header / Footer 領域の背景に `headerBackgroundColor` / `footerBackgroundColor` を適用しない — 既知の platform 非対称として、背景色の共通反映は保証しない。

iOS Footer の既定文字色は移植元ライブラリ (AiForms.SettingsView) 互換の固定 gray を維持し、system appearance (light / dark) に追従する Cell の description text 色とは別に扱う。これは `Theme.footerTextColor` の既定値であり、利用者が明示した値では上書きできる。

## Theme の Section 装飾4属性

`Theme` は次の4属性を公開する。nil / null は未指定を表し、**style 別の platform 既定**へ解決する:

| 属性 | iOS 型 | Android 型 | 未指定時の解決 |
|---|---|---|---|
| `sectionMargin` | `NSDirectionalEdgeInsets?` | `PaddingValues?` | Classic / Modern 同値・両 platform 同値の既定寸法 (Classic は水平を無視) |
| `sectionCornerRadius` | `CGFloat?` | `Dp?` | Modern: platform 既定値 (Classic に角丸はない) |
| `sectionBorderWidth` | `CGFloat?` | `Dp?` | 実効 0 — 既定の Modern に Border は描かれない |
| `sectionBorderColor` | `UIColor?` | `Color?` | 透明 |

Android の型は Compose の型 (`PaddingValues` / `Dp` / `Color`) だが、描画自体は RecyclerView の ItemDecoration が行う — Theme が Compose の型で受け、decoration が描画値へ変換する。

既定 **margin** は style 間・platform 間とも同じ生値 top 22 / 左右 16 / bottom 0 (iOS pt / Android dp) に統一する (core/ADR-0027。Classic の既定値定数は Modern への別名で、実効値は全幅契約により上下のみ)。既定 **角丸** は両 platform で同じ生値 26 (iOS 26pt / Android 26dp) に統一する — いずれも未指定 Theme のクロスプラットフォーム表示を揃えるため (core/ADR-0024)。既定値定数は両 OS とも `SectionBoxMetrics` が持つ (iOS: `SectionBoxMetrics.modernDefaultMargin` 等 / Android: `SectionBoxMetrics.kt`)。負および非有限 (NaN・±∞) の寸法成分は 0 として扱い、`sectionCornerRadius` は Container の寸法から幾何的に許される値へ描画時に clamp する (Theme 構築時には拒否しない — 正規化は描画時のみ)。

`sectionMargin` は **Section 単位 (Header・Cell Container・Footer を一体とした表示単位) の外側余白**であり、Header と Container の間・Container と Footer の間には入らない。水平成分は leading / trailing 基準で、Section Header / Footer 行にも適用する (Container が Header / Footer を覆わない点は変わらない)。隣接 Section 間の間隔は前 Section の bottom と次 Section の top の加算になる。

list 端の上下余白は Root Header / Footer の**内側**に入る: Root Header → 余白 → 先頭 Section、末尾 Section → 余白 → Root Footer の順で、Root Header / Footer が無い場合は list 端に対して適用する (両 OS 共通)。

Classic では `sectionMargin` の**上下成分のみ**を適用し、leading / trailing 成分は無視する — 「Classic の Section 境界は全幅」契約を維持するための意図的な非対称。余白領域には `Theme.backgroundColor` が見える。

## 背景と separator

`Theme.backgroundColor` は list 全体の下地 (canvas)、`Theme.cellBackgroundColor` は Cell または Modern Section Container の背景であり、同じ領域ではない。押下・選択背景は `selectedColor` から解決する。Theme 変更時も style と identity を維持するが、再評価される領域は platform 実装依存であり、本契約では規定しない。

Classic の separator: Section 最初の Cell 上端と最後の Cell 下端は全幅、Section 内の中間 separator は左から16pt / 16dp inset する。icon の有無で inset を変えない。Android は `Theme.separatorColor` で1物理 pixel の細線を描き、Root Header / Footer と Section Accessory 行を対象に含めない。iOS も main list (設定 list 本体) の separator 色を `Theme.separatorColor` から解決する (モーダルのピッカー選択画面も同色)。両 platform とも separator は Theme の固定色で描き、system appearance (light / dark) に追従しない。

Modern の separator: Section 先頭 Cell の上端と末尾 Cell の下端には描かない (Container の縁が区切りを兼ねる)。中間 separator は leading 側を Classic と同じ inset 規則 (Container の内側 leading 端基準)、trailing 側にも同量の inset を Container の内側 trailing 端から取る**左右対称** — Container の両端まで引くと Container が分断されて見えるためで、Classic の「trailing は端まで」とは意図的に異なる。色は `Theme.separatorColor`、icon の有無で inset を変えない。Cell が自身の背景を塗っても separator は視認できる (描画順で保証)。

## 保証すること

- Classic / Modern の切替で設定内容と identity を変更しない。切替後は新しい style の装飾・separator 規則で全 Section を再描画する。
- Modern の Container は Section の Cell のみを覆い、Section Header / Footer は Container の外・Root Header / Footer は装飾対象外とする (両 OS 共通)。これが崩れると platform 間で Section のまとまりの見え方が食い違う。
- 4属性の未指定値は style 別の platform 既定へ解決し、描画時に未解決値を残さない。既定の Modern に Border を描かない。
- `sectionMargin` は Section 単位の外側余白として扱い、Header と Container の間・Container と Footer の間に入れない。
- Header の固定高さ解決を accessory 種別 (text / view) で分岐させない (core/ADR-0021)。
- 空の Header / Footer に表示領域を割り当てず、高さ指定で存在を作らない。表示トグル `false` の Header / Footer には内容があっても領域を割り当てない (core/ADR-0023)。
- Root と Section の Accessory 所有境界を維持する。
- list 下地、Cell 背景、separator / Section Container 背景をそれぞれの表示領域へ適用する。
- `Theme.separatorColor` を main list の separator 色として両 platform で適用する。初期 Theme・実行時の Theme 変更のどちらでも追従する。4属性も実行時の Theme 変更へ追従する。
- Classic の Section 境界は全幅、中間 separator は16pt / 16dp inset とし、icon の有無で変えない。

## してはいけないこと

- style 切替を `SettingsRootDiff` として扱わない。
- iOS の Modern に `.insetGrouped` list appearance を使わない (ios/ADR-0003)。
- Root Header / Footer を Section の Container・角丸装飾の対象に含めない。
- Root Accessory を `SettingsRoot` へ追加しない。
- Modern の separator を Container の上下端に描かない。中間 separator を Container の端まで引かない。
- Classic で `sectionMargin` の水平成分を適用しない (全幅契約が壊れる)。
- Android の1物理 pixel の separator を1dpへ換算しない。

## 関連

- [スタイルの所有と実効値解決](style-resolution.md)
- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [iOS Native Host](../../ios/api/ios-native-host.md)
- [Android Native Host](../../android/api/android-native-host.md)
- [ios/ADR-0003 — Modern は insetGrouped を廃し自前の Section 装飾で実現する](../../../decisions/ios/0003-modern-self-drawn-section-decoration.md)
- [core/ADR-0021 — Header の固定高さは accessory 種別に依らず適用する](../../../decisions/core/0021-header-height-applies-regardless-of-accessory-kind.md)
- [core/ADR-0023 — Header / Footer の表示は可視トグルと内容有無の AND で判定する](../../../decisions/core/0023-accessory-visibility-and-composition.md)
