---
type: concept
title: 表示への反映と Host の寿命 (KsSettingsView.Maui)
description: facade への変更がいつどう表示へ届き (構造は即時・内容はバッチ・IconSource は非同期・View は参照が正で内容は live)、Host の解放と再生成をまたいで何が保たれるか、Android の measure 契約による配置の制約
tags: [maui, facade, lifecycle, handler]
timestamp: 2026-09-04
---

# 表示への反映と Host の寿命 (KsSettingsView.Maui)

この文書を読むと、`KsSettingsView.Maui` のプロパティやコレクションへの変更がどの単位・どのタイミングで画面へ反映されるか、ページ離脱と再訪問 (Handler の切断と再接続) をまたいで何が保たれるか、そして Android で避けるべき配置が分かる。公開面の骨格は [MAUI facade の公開契約](maui-facade.md)、前提となる Store の一般契約は [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) を先に読むと分かりやすい。決定の経緯は maui/ADR-0007 (releaseHost)・maui/ADR-0014 (Android measure 契約)・maui/ADR-0015 (IconSource 実体化)・maui/ADR-0016〜0018・0020 (accessory View と CustomCell.Content の更新セマンティクス)・maui/ADR-0022 (View 配置の検査)・maui/ADR-0026 (iOS icon の所有権分類)。

## 更新の意味論

### 構造の更新と内容の更新

`Root` / `Cells` の実体が `INotifyCollectionChanged` なら購読され、構造イベント (Add / Remove / Move / Replace) は即時に反映、`Reset` は全体再構築になる。observable でない実体 (素の `List<T>`) は接続時点の内容の静的描画で、以後の操作は反映されない。

Cell の内容更新は同一 UI サイクル内の変更がまとめて 1 回で画面に反映される (Store のバッチ契約に乗るため)。可視性 (`IsVisible`) と Section 単位の変更 (`Section.IsVisible` / `HeaderHeight` 等) はこのバッチに乗らず個別に反映されるが、いずれも同一サイクル内に描画されるため利用者から見た差はない。

**全操作は UI スレッドから行う** (呼び出し側契約。facade は marshal しない)。

### IconSource の解決

`IconSource` は MAUI 標準の image source service で**非同期に** platform 画像へ解決されてから表示される (maui/ADR-0015)。Handler 未接続の間は解決が保留され、接続時にまとめて解決される。連続変更は最後の値が勝つ (latest-wins)。解決失敗は icon なしとして確定し、次の変更で再試行される。

iOS で UIKit の名前付き画像キャッシュが所有する画像 (asset catalog・拡張子なしファイル名) に解決された場合、facade はその画像を破棄しない — 同一 UIImage が複数 Cell / SettingsView に共有され得るため、後片付けは解決時の所有権分類で facade 所有の画像だけに行う (maui/ADR-0026)。

### accessory View と CustomCell.Content の更新

**accessory View** = Root / Section の header / footer に置く View (`RootHeaderView` / `RootFooterView` / `Section.HeaderView` / `FooterView`)。accessory View の更新は「参照が正、内容は live」(maui/ADR-0018): View プロパティへ別インスタンスを設定すると表示が差し替わる。同一インスタンスの内部変化 (バインド値の更新等) はプロパティ再設定なしに表示へ反映され、サイズが変わる場合は自動高さの領域が追従する (`HeaderHeight` 指定時は固定高さで切り詰め)。`CustomCell.Content` も同じ規律に従う (maui/ADR-0020)。

accessory View と `CustomCell.Content` は (Section / Cell と異なり) **logical tree に接続され**、所有者の `BindingContext` を継承する。View 自身に明示的な BindingContext があれば上書きしない。継承と変更伝播は Handler 接続の有無に依らない。

| View 配置プロパティ | 所有者 (BindingContext の継承元) |
|---|---|
| `SettingsView.RootHeaderView` / `RootFooterView` | SettingsView |
| `Section.HeaderView` / `FooterView` | 所有 Section |
| `CustomCell.Content` | 所有 CustomCell (ItemsSource / ItemTemplate 生成では item が BindingContext) |

### 同一 View インスタンスの多重配置

Section / CellBase そのものを複数箇所へ置くこと、および同一の View インスタンスを複数の accessory View / `CustomCell.Content` へ置くことは、いずれも `InvalidOperationException` (ItemsSource のテンプレートが既配置インスタンスを返す場合も同様)。View 配置プロパティ (accessory View / `Content`) の検査は値が確定する**前**に行われ、失敗しても公開値・論理所有・表示はいずれも動かない。構造変更バッチ (Section / Cell の追加・差し替え・Root 再構築) 内の重複は native へ触れる前に全件検査され、どの位置の要素が衝突しても部分更新を残さない。

失敗後も公開コレクション (`Root` / `Cells`) はロールバックされず、回復は呼び出し元による Root の全体再構築 (再代入 / Reset) で行う (maui/ADR-0022)。設定ツリーに未参加の Section (XAML 構築中等) へ既配置の View を設定した場合は既存配置を奪わず、その Section が SettingsView の変換経路 (Section / Cell ツリーを Bridge の写しへ変換して native へ配信する経路) に加わった時点で例外になる — null に戻した View の別 slot への再利用はいつでも可。

## lifecycle の保証

ページ表示 (Handler 接続) で Native Host が生成され、その時点の状態が表示される。ページ離脱 (Handler 切断) で Host は解放されるが、**facade・Bridge・Store は生き続け、切断中の変更も Store へ流れ続ける** — 再訪問時は Store 現在状態から表示が復元される (maui/ADR-0007)。解放 → 再生成のたびに Host は新しい**世代**になる。復元の正はそれぞれ次が所有し、利用者から見ればいずれも再訪問後も保持されている:

| 対象 | 復元の正 | 切断中の変更 | 再接続時 |
|---|---|---|---|
| 設定ツリーと Theme | Bridge の内部所有 Store | Store へ流れ続ける | Store 現在状態から表示を復元 |
| `RootHeaderText` / `RootFooterText` | facade (Store の復元対象外 — core/ADR-0019) | facade が値を保持 | Host の attach 後に再適用 |
| accessory View と `CustomCell.Content` | facade が所有する VisualElement (platform 実体 (wrapper) は Host 世代ごとに作り直される — maui/ADR-0016・0020) | View 差し替え・内容変化とも保持 | 再接続後の表示に反映 |
| ユーザー操作通知の購読 | — | Host が無いため操作は発生し得ない | Handler の接続で開始・切断で解除 (取りこぼしはない) |

iOS の Host は ViewController であり、facade が親 Page への子 VC embed (containment) を管理する。利用者側の作業はない。

コレクション・Cell への購読に限らず、model (Section / Cell) から SettingsView 側へ向かう内部参照 (配置検査の問い合わせ先 — 多重配置の可否を SettingsView 側へ尋ねる口 — を含む。maui/ADR-0022) はすべて weak であり、外部 (ViewModel 等) がコレクションや Cell — `Content` 設定済みの CustomCell を含む — を保持し続けても SettingsView の回収は妨げられない。

## 配置の制約 (Android の measure 契約)

Android では、SettingsView が「大きさの確定しない制約」で measure されると、measure の途中で内部の一覧の配置まで走り、編集中の入力欄が一時的に幅ゼロになってフォーカスを失うことがある (Android は幅ゼロになったフォーカス中 View のフォーカスを外すため)。これを避けるため、SettingsView は割当領域を fill するコントロールとして Android の `SettingsViewHandler` が measure 契約を閉じる (maui/ADR-0014):

| 配置 | 例 | 制約 | Android の measure | 結果 |
|---|---|---|---|---|
| **大きさが決まる配置** (通常はこれを使う) | Grid の `*` 行 / ページ直下 / 固定サイズ指定 | 幅・高さとも有限 | handler が制約から desired size を即答 | フォーカス喪失経路が消える |
| **内容サイズを問われる配置** (推奨しない) | `VerticalStackLayout` 直下 / 縦 `ScrollView` の content / Grid の `Auto` 行 | 一方が無限 | 内容ぶんの大きさに答えられるのは Native Host だけなので既定の measure へフォールバック | 表示は保たれるが、Android ではフォーカス喪失経路が残る |

iOS の handler は measure を override しない。大きさが決まる配置では Android の即答値と measure 結果が一致するため差は出ず、内容サイズを問われる配置では両OSとも既定の measure に揃う — いずれも同一 XAML の配置結果が OS 間で割れない (割当領域を fill する前提が成り立たない配置 (非 Fill 配置) の例外は maui/ADR-0014 の Consequences を参照)。

## 関連

- [MAUI facade の公開契約](maui-facade.md) — 公開面の骨格と禁止事項
- [Cell の MAUI 表現](maui-cells.md) — CustomCell の更新規律の利用者向け要約
- [Store の状態と更新通知](../../core/architecture/store-and-update-streams.md) — 内容更新のバッチ契約と Host 接続時の復元
- [MAUI Native Bridge の interop 境界](native-bridge.md) — `releaseHost()` / `makeHost*` と root accessory の再適用順序
- [MauiView の native 実体化機構](../architecture/view-materialization.md) — accessory View と `CustomCell.Content` の platform 実体の寿命と退役順序

決定の経緯: maui/ADR-0007 (releaseHost)、core/ADR-0019 (attach 時復元)、maui/ADR-0014 (Android measure 契約)、maui/ADR-0015 (IconSource 実体化)、maui/ADR-0016〜0018 (accessory View)、maui/ADR-0020 (content の live view)、maui/ADR-0022 (View 配置の検査)、maui/ADR-0026 (iOS icon 後片付けの所有権分類)
