# バッチC 統合結果 — Styling と Theme Bridge

統合日: 2026-07-18
レビュー結果: 2026-07-18 承認

対象:

- `settings-view-ios-style`
- `settings-view-ios-theme-bridge`
- `settings-view-android-style`
- `settings-view-android-theme-bridge`
- 補助資料: `docs/styling-and-theming.md`、`docs/platform-guide-ios.md`、`docs/platform-guide-android.md`、`docs/architecture.md`

抽出結果:

- 概念候補: 14件
- ADR候補: 5件
- drift所見: 21件

## 統合後の concepts 案

### `styling/style-resolution.md`

- Theme、CellStyle、Native型のスタイル値はUI層が所有し、Coreの構造モデルへ混在させない。
- 実効値は「Cell固有の意味値 → CellStyle → Theme → platform default」の段階で解決する。
- CellStyleの未指定値は、空値ではなく上位値を継承する意思を表す。
- 画面背景とCell背景は別の表示領域として独立させる。
- iOS / AndroidでNative型は異なるが、同じ解決階層を共有する。

統合元: 両Styleの所有境界・実効スタイル候補、両Theme BridgeのNativeスタイル階層。

### `styling/cell-visual-states.md`

- 通常、押下または選択、無効の視覚状態を実効スタイルの上に重ねる。
- 操作可能なCellだけが選択フィードバックを示し、解除後は実効背景へ戻る。
- 無効状態は選択状態より優先し、Cellと内包controlの操作を抑止する。
- 無効表現は行全体のopacityを下げず、意味色の置換とNative controlのdisabled表現を使う。
- Cell固有の意味色がある場合も、無効状態を最終優先とする。

統合元: iOS / Android Cellの視覚状態優先順位。

### `styling/cell-row-layout.md` (`type: design-tokens`)

- すべてのCellは、任意icon、titleと任意description、valueまたはtrailing control、任意hintという共通の視覚文法を使う。
- 任意要素がない場合は、そのための空領域を残さない。
- 可変高さを既定とし、platform別の最低行高を守りながら内容に応じて伸びる。固定高さは利用者が明示した場合だけ使う。
- 最低行高はiOS 48pt、Android 60dpとし、機械的に共通化しない。
- iconの既定一辺は両platformとも24pt / 24dp。Androidの個別icon寸法・角丸反映はdrift C-3が解消するまで長命契約へ含めない。
- trailing controlの内容と操作はCell種別側、共通配置とStyle反映は共通行側が担う。

統合元: iOS共通視覚契約、Android行寸法契約・共通行候補、ADR-0011。

### `styling/list-appearance.md`

- 同じ設定ツリーを保ったまま、フラットなClassicと角丸グルーピングのModernを切り替える。
- 視覚モードはSection装飾と区切り方だけを変え、モデル、安定ID、Renderer登録を変えない。
- Section / Root Header・Footerは内容と共にスクロールし、画面端へ固定しない。
- 空のHeader・Footerは領域を生成しない。
- Headerは後続Cell側、Footerは先行Cell側へ近付け、補助領域とCellの意味的なまとまりを示す。
- iOS Footerの既定色はAiForms互換の固定グレーを維持し、system appearanceへ自動追従しない互換規則として明記する。

統合元: iOSリスト外観・補助領域、Android視覚モード・Section装飾。

## 既存 concepts への合流

- Themeを構造変更から分離する原則は `architecture/display-state-synchronization.md` に確定済み。
- Themeの永続状態、同値更新抑制、更新通知は `architecture/store-and-update-streams.md` に確定済み。
- 宣言方式とStore方式から同じTheme更新経路へ収束する責務は `architecture/declarative-ui-bridge.md` に含まれる。
- 共通行をCell種別固有controlとのコンポジションで構成する判断はADR-0011に記録済み。

## 見送る独立 concepts

- iOS / Android別のStyle階層とTheme Bridgeは共通原則の重複になるため作らない。
- 個別Themeフィールド一覧、旧プロパティ名、具象Cell列挙、具体的なlayout APIはコードから再導出できるため移さない。
- 画像派生ごとのNative解決は独立概念にせず、Cell icon契約へ合流する。

## ADR 統合判断

スタイルのUI層所有、Native型の直接利用、実効値の段階解決、Themeの独立更新はADR-0009で充足する。共通行のコンポジションはADR-0011、RootとSectionの装飾境界はADR-0005で充足する。

iOS Styleから抽出された5候補のうち、上記既存ADRと重複しないFooter固定色は局所的なdesign defaultであり、変更コストも限定的なためADRへ昇格させない。`list-appearance.md`の互換規則として残す。

したがって新規ADRは起票しない。

## drift 所見の統合

| ID | 所見 | 主な根拠 | 推奨する扱い |
|---|---|---|---|
| C-1 | Style specとTheme Bridge specは変換責務を互いに分離するとしながら、同じ型と解決規則を重複して所有する。PurposeはCore論理型の変換を前提にするが現行Coreにスタイル型はない | spec内部 / spec ↔ code/ADR | ADR-0009と現行コードを正とし、単一のStyle解決概念へ統合する |
| C-2 | 両Theme Bridge specに削除済みのThemeプロパティ名が残る | spec ↔ code/test/docs | 現行名を正とする。具体名はconceptsへ移さず、docsは移行完了後に更新する |
| C-3 | Androidはicon寸法・角丸を論理解決するが共通行の描画へ反映しない | code/test ↔ spec | 実装不具合候補。未反映値はdesign tokensへ確定せず、後続Kasane changeで扱う |
| C-4 | AndroidのfontFamilyがNative View変換で無視される | code ↔ spec | 実装不具合候補。後続changeで反映またはAPI縮小を判断する |
| C-5 | scroll indicatorとHeader / Footer背景の公開値が描画で消費されない。iOS / Androidとも一部フィールドは保持だけされる | code/test ↔ spec | 予約値として正当化せず、後続探索で実装かAPI削除かを決める。conceptsへ含めない |
| C-6 | iOSのseparator色はTheme値を描画へ反映せず、Theme再適用時に表示中のSection / Root Header・Footerが再構成されない可能性がある | code ↔ spec/docs | 実装不具合候補。Theme全領域再適用の統合テストとともに後続changeで扱う |
| C-7 | Androidの論理fallbackは固定黒、Context付き描画経路はplatform theme色を使い、同じ「既定」の意味が経路で異なる | code/test ↔ spec | platform defaultを長命原則とし、論理アクセサ単体の固定黒は後続探索で統一方針を決める |
| C-8 | spec/docsは旧描画経路、旧minimum row height、旧対象Cell数、Compose contentの再生成を記述する | code/test ↔ spec/docs | 現行コードとテストを正とし、共通行・行高・Composition再利用の低腐食原則だけを移す |
| C-9 | iOS descriptionの未指定色はspecの固定RGBではなくdynamicなsecondary色を使う | code/test ↔ spec | system appearanceへ追従する現行コードを正とする。Footerの固定互換色とは別契約として扱う |

## 推奨レビュー結果

1. 新規4 conceptsを承認する。
2. 新規ADRは起票せず、ADR-0005 / 0009 / 0011を維持する。
3. C-1、C-2、C-8、C-9は推奨どおり現行コード / accepted ADRを正とする。
4. C-3、C-4、C-6は実装不具合候補として移行範囲外に残し、後続Kasane changeへ送る。
5. C-5、C-7は未確定のままconceptsへ含めず、後続探索へ送る。
