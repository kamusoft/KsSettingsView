---
id: 0030
title: Theme のダーク対応はライブラリ所有の light / dark 既定値と未指定色の描画時解決で行う
status: accepted
date: 2026-09-05
amended-by: 0031
---

## Context

Theme を渡さずにダーク外観 (iOS のダーク / Android の夜間モード) で使うと、3 platform とも「白地に淡色の文字」になり判読できない (2026-09-05 に 4 実行面で実測。kasane/changes/archive/2026-09-06-fix-default-colors-dark-appearance/exploration.md)。Theme の既定色のうち外観に追随するのは Cell title (iOS `.label` / Android 同梱テーマの `textColorPrimary`) と iOS の description (`.secondaryLabel`) だけで、list 下地・Cell 背景・separator・Header / Footer・disabled・selected はライト前提の固定 RGB である。

根本原因は、Theme の色が (iOS の `UIColor` を除き) 外観を持たない静的な値しか表せないこと。Compose の `Color` は不変の値で、外観に応じて変わる色は存在しない。この制約は既定色だけでなく、利用者が自分の色を渡す場合のダーク対応 (light / dark でどう切り替えるか) にも同じ形で効く。

制約と前提:

- 色は iOS `UIColor`・Android Compose `Color` の一系統で公開し、中間の論理色型を置かない (core/ADR-0009)
- Android のライブラリ UI はホストの XML テーマ・Compose `MaterialTheme` から視覚隔離され、同梱 Material3 DayNight 派生テーマでラップした Context で描かれる (android/ADR-0020)
- MAUI facade は未指定の色を null で native に渡し、native の `Theme()` 既定に任せる。native の既定を直せば MAUI は自動的に追随する
- 既存利用者のライト時の見た目は変えない
- ライブラリは未配信で外部利用者がおらず、公開型の破壊的変更の代償は今が最も小さい
- Compose 開発者の慣習 (並走調査 2026-09-05、相方 codex + ホスト側調査ワーカーで双方一致): 色は原色トークン → 用途名のロール → light / dark ごとの割り当ての 3 層で持つ。ダーク対応は light / dark の 2 セットを定義し composition 時に `isSystemInDarkTheme()` で選ぶ。明示指定の色は外観で変えず、未指定の色だけを描画直前に現在の外観へ解決する。未指定の印は `Color?` ではなく `Color.Unspecified` (`takeOrElse` で解決)。既定値は `XxxDefaults` オブジェクトの factory に集約し、実装本体ではなく既定値式でテーマを読む。ライブラリが独自の light / dark 配色を所有することは Compose 的に不自然ではない (公式の Fully custom design system)

## Decision

Theme のダーク対応は次の契約で行う。

1. **既定色は全部、外観に追随させる。** ライト側の既定値は現行の固定 RGB をそのまま使い、ダーク側の既定値をライブラリが新たに持つ。dark の生値は iOS の dark システムパレットを不透明に近似した値を採り、3 platform で同じ値を写す (iOS の Cell title / description / ButtonCell title はシステム色 `.label` / `.secondaryLabel` / `.systemBlue` のまま残し、Android は同じロールに近似値を置く)
2. **未指定の色だけを描画時に現在の外観の既定へ解決する。明示指定された色は外観で変えない。** 旧既定値との値比較で「未指定だったはず」と推測しない
3. **Theme の型は変えない。light / dark の対を持つ新型や、外観を受け取って Theme を返すコールバックは作らない。** 利用者が両外観の色を自分で決める場合の手段は各 platform の慣用に乗せる: iOS は dynamic な `UIColor` (asset catalog の色や `UIColor(dynamicProvider:)`) を渡す。Android は構築時に `isSystemInDarkTheme()` (Compose) または Configuration の uiMode (View) で light / dark の Theme を選んで渡す (夜間切替は Activity 再生成で再構築される)。MAUI は XAML の `AppThemeBinding` で色プロパティを書く
4. **iOS**: 既定色の `public static let` を `UIColor(dynamicProvider:)` で light = 現行値 / dark = 新規生値の対にする。`UIColor` 自体が両外観を持つため、描画側の変更は CGColor を layer に置く箇所 (Section 装飾の枠線) の trait 変更時の再解決だけになる
5. **Android**: Theme と CellStyle の色フィールドはすべて `Color` 型で、未指定の既定値を `Color.Unspecified` にする (既存の `Color?` の色フィールドも同じ形に揃える。色以外の nullable フィールドは対象外)。ライブラリ所有の light / dark 2 セットを `KsSettingsViewDefaults` の factory (`lightTheme()` / `darkTheme()` / `theme(darkTheme:)` / `@Composable theme()`) として公開し、それを唯一の正とする。`values-night` の色リソースは置かない。Compose 入口の `theme` の既定値式で `isSystemInDarkTheme()` により片方を選び、View は Theme を受け取った時点で Configuration の夜間モードから同じ 2 セットの既定で未指定を埋めた解決済み Theme を 1 箇所で作り、描画に関わる全箇所はそれだけを読む。既定色の public 定数 (`DEFAULT_ACCENT_COLOR` 等) は削除し、生値は internal のパレットに一本化して、公開面は factory と用途名のフィールドだけにする。ライブラリ View は uiMode の Configuration 変更を受けたら (`configChanges` で uiMode を自前処理するホスト。.NET MAUI のテンプレート既定 `MainActivity` がこの形)、未指定の既定を現在の外観で再解決し、既存の Theme 更新経路で再適用する。明示指定された色は再解決しない
6. **Android の Cell title / description の既定は Theme に載せず、実効 style の解決の最終段で外観から選ぶ。** 公開 factory が返す Theme と解決済み Theme のどちらも `cellTitleColor` / `cellDescriptionColor` は `Unspecified` のままにする。Theme の title が常に値を持つと ButtonCell の 4 段解決 (ButtonCell → CellStyle → Theme の title → ButtonCell 既定) が最終段へ到達せず、ライトでも ButtonCell の title が青から黒へ変わる回帰と iOS との不一致が生じるため (実装フェーズのオーナー裁定 2026-09-05)。Android のライト既定 title は light セットの #000000 とし、同梱テーマの `textColorPrimary` (#1D1B20) から動的に解決しない — 承認モックの light 列と iOS `.label` に揃え、platform 差を増やさないため
7. **MAUI**: facade は変えない (未指定を null で native に渡す現行契約のまま native の既定に追随する)

## Alternatives Considered

方向:

- **文字色の追随もやめて全既定色を固定ライトに揃える**: 却下。判読性は確保できるが、iOS で OS 外観を無視する挙動を契約にしてしまう
- **現状維持 + 「ダークで使うなら Theme に dark の色値を渡す」と案内する**: 却下。不具合を仕様に格上げするだけ

iOS の手段:

- **OS のセマンティック色 (`.systemBackground` / `.separator` / `.secondaryLabel` 等) に置き換える**: 却下。ライト時の見た目が変わる (separator が半透明の rgba に、Header 文字が `.secondaryLabel` 相当に)。dark 側の値の参考には使う
- **dark 側をセマンティック色の dark 解決値にする**: dynamicProvider 案に吸収。半透明色は Android の描き方と合わないため生値を明示する
- **iOS にも `Defaults` 相当の factory を新設し static を internal 化する**: 却下。UIKit に無い概念で、中身が `Theme()` と同じになる

dark 値の出所:

- **独自のダーク配色を新規デザイン**: 却下。light は iOS 風のまま dark だけ独自になり、特定の趣味の配色になりやすい
- **Material3 の dark baseline**: 却下。light と dark で文法が変わる

仕組み:

- **light / dark の Theme の対を持つ新型を 3 面に追加する**: 却下。利用者が減らせるのは「どちらを渡すか」の 1 行だけで、両外観分の Theme を定義する手間は変わらない。Store / View / facade の受け口が既存の `Theme` と二重になる。Compose の慣習に無い発想 (並走調査で両調査者とも挙げず)
- **外観を受け取って Theme を返すコールバックを登録する**: 却下。同上に加え XAML と相性が悪い
- **ホストの `MaterialTheme.colorScheme` に自動追随する (自前の既定色を捨てる)**: 却下。Compose 開発者の違和感は最小だが、android/ADR-0020 (視覚隔離) に反し、View / MAUI の入口には composition が無いため適用できず 3 面の見た目の統一が崩れる。将来 opt-in の adapter として足す余地は残す

Android の未指定の印:

- **`Color?` (null = 未指定)**: 却下。`Color` は inline value class で nullable にするとボクシングが起きる。公式の sentinel は `Color.Unspecified` (androidx material3 `Text.kt` の `color: Color = Color.Unspecified` + `takeOrElse` の解決チェーン)
- **今回追随させる色だけ `Unspecified` にし、既存の `Color?` はそのまま**: 却下。1 つの data class に未指定の表現が 2 流儀混在する。未配信の今なら揃える代償が最も小さい
- **`Theme.default(context)` ファクトリを利用者が呼ぶ**: 却下。Theme を渡さない利用者が直らない

Android の解決先:

- **`values-night` の色リソースを正にする**: 却下。公開面が Android リソース依存になり、iOS / MAUI と同じ生値であることの検証にリソース解決 (Robolectric 等) が要る。Kotlin の 2 セットなら定数比較で機械的に確認できる
- **同梱 Material3 テーマの attr (`colorSurface` 等) から解決する**: 却下。ライト時の見た目が変わる (Material の surface は純白ではない)。Material Components 更新時の追従保守が加わる
- **各消費者が描画時に `takeOrElse` で解決する**: 却下。Theme の消費者が 6 箇所に散っており、解決漏れが `Unspecified` の ARGB 変換 (透明な黒) として現れる。解決点を 1 つにする
- **Store 側で解決済み Theme を配信する**: 却下。Store は Context を持たず外観を知らない

Android の外観切替の再適用:

- **Activity の再生成に任せる**: 却下。`configChanges` に uiMode を持つホスト (MAUI 既定テンプレート、一部の Compose アプリ) で表示中に追随しない

## Consequences

- 正: Theme を渡さない利用者は 3 platform とも何も書かずにダーク外観で判読できる。一部だけ上書きした Theme (accent だけ等) の未指定フィールドも外観に追随する
- 正: 既存利用者のライト時の見た目は、Android の Cell title / valueText を除いて変わらない (light 側 = 現行定数)
- 正: Android の公開面が Compose の慣習 (`Defaults` factory・`Color.Unspecified`・light / dark 2 セット) に揃い、Compose 開発者に違和感のない API になる
- 正: Theme の型・Store の `theme` 経路・MAUI facade の契約は変わらず、Theme 更新経路 (display-state-synchronization) にも手を入れない。MAUI の `AppThemeBinding` は facade → snapshot → bridge → native の既存経路で Theme 再適用まで届き、Android で Activity が再生成されないホストでも表示中に切り替わる (出典: 実装結果。spike で同一 Activity / 同一 View の識別子一致を実証)
- 負: Android の色フィールドの型が `Color?` から `Color` (`Unspecified` 既定) へ変わる破壊的変更。`null` を明示的に渡す呼び出しと読み出し側 (EffectiveStyle・bridge の変換・samples・tests) に追随作業が発生する。`Theme.DEFAULT_*` の色定数を参照していたコードは `KsSettingsViewDefaults` の factory 経由に書き換える
- 負: ダークの既定値 (生値) が製品契約に加わり、3 platform で同じ値を保つ検証 (定数比較テスト) を持ち続ける必要がある。iOS の ButtonCell title (`.systemBlue`) と description (`.secondaryLabel`) は色空間差・半透明合成のため Android の生値と厳密一致せず、値一致の検証は Android / MAUI(Android) の範囲になる (出典: 実装結果)
- 負: Android のライト既定 Cell title / valueText が #1D1B20 (同梱 Material3 テーマの `textColorPrimary`) から #000000 へわずかに濃くなる。「ライト時の見た目は変えない」の唯一の例外で、実装前の「両者は実質同じ」という推定が実測で覆ったことによる (出典: 実装結果、オーナー裁定 2026-09-05)
- 負: 既定と同じ生値を固定色で明示した Theme は既定 Theme と等価でなくなり、その色はダークでも変わらない (iOS / Android 共通の等価性の意味変化)
- 負: Android で「Theme を渡す利用者」は構築時の分岐を自分で書く。明示指定した色は表示中の外観変更で変わらない (再生成または再構築で切り替わる)。`KsSettingsViewDefaults.darkTheme()` を端末がライトのまま渡すような食い違った組み合わせでは、Theme に載せない title / description だけが端末側の外観の既定になる (出典: 実装結果、review-001)
- 負: Android の View に uiMode 変更の検出と、解決済み値を持つキャッシュ (EffectiveStyle 等) の無効化が加わる。同梱テーマ付き Context のラッパは生成時のテーマ属性を固定するため、外観が変わったら作り直す必要があり、行の ViewHolder が同梱テーマ属性から引く値 (placeholder の hint 色・Switch のオフ色) も再 bind 時に現在の外観のラッパから引き直す (出典: 実装結果。独立レビューが検出した 2 件の欠陥の修正)
- 負: iOS の CGColor を layer に置く箇所は trait 変更時の再解決が要る (既定の枠線は透明のため既定値の範囲では実害なし)
- 負: concepts (core/styling/style-resolution.md の「既定色と外観の追随」節ほか) の改訂が要る

## Revisit When

- Android で明示指定した色まで表示中の外観変更に追随させる要望が出たとき (対の型・コールバックの再検討)
- Android の Theme に ButtonCell の title 既定用の独立フィールドを設ける要望が出たとき (factory と解決済み Theme が Cell title / description を埋められるようになり、Decision 6 の食い違った組み合わせの帰結が消える)
- ホストの `MaterialTheme` への追随 (Dynamic Color を含む) が利用者から求められたとき。android/ADR-0020 の改訂と合わせて opt-in adapter を検討する
- ライブラリ配信後に色フィールドの型変更が互換問題を起こすと分かったとき (本決定は未配信を前提に破壊的変更を受容している)

出典: kasane/changes/archive/2026-09-06-fix-default-colors-dark-appearance/exploration.md (検討した選択肢・決定事項・調査記録: 並走調査) / 同 design.md (Decision 1〜9) / 同 deviation.md (裁定 B / B'・ライト title の値・定数の削除) / 同 review-001.md (Minor: factory と端末外観の食い違い) / 同 tasks.md (0.1 spike の再実施) / 探索の会話中の議論 (2026-09-05)
