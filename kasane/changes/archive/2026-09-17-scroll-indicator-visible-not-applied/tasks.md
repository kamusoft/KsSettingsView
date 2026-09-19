# Tasks: scroll-indicator-visible-not-applied

S 級として着手済みのため、1〜3 は作業ツリーに実装とテストがある (未 commit)。実装フェーズではデルタスペックの Scenario と照合し、不足を埋める。4 が本提案で新たに加わる実装。

## 1. スクロールバー表示の反映 (実装済み・照合)
- [x] 1.1 iOS: 初期表示と Theme 差し替えの両方で縦スクロールバーの表示へ反映する (→ Requirement: スクロールバー表示の Theme 反映 (iOS))
- [x] 1.2 iOS: Requirement の全 Scenario をテストで担保する。Store 経由と DSL 経由の対称テスト (core/ADR-0018 の対称テスト義務) と、スクロールバーの表示だけを変えたときの行の View インスタンス・ID・content offset の維持を含む (→ 同上)
- [x] 1.3 Android: 設定リストでスクロールバーが実際に描画される状態にし、初期表示・`theme` 代入・Store 経由・DSL 経由・構造更新と同時の Theme 適用で反映する (→ Requirement: スクロールバー表示の Theme 反映 (Android))
- [x] 1.4 Android: スクロールバーの有効化を設定リスト自身に限定し、選択面の候補リストと回転ホイールへ波及させない (review-001 Major)。Requirement の全 Scenario をテストで担保する。反映処理を消すと落ちるテスト、波及しないことのテスト、構造更新と同時の Theme 適用、行の View インスタンス・ID・スクロール位置の維持を含む (→ 同上)
- [x] 1.6 iOS / Android: PickerCell の選択面の候補リストの縦スクロールバーを、選択面を開いた時点の `scrollIndicatorVisible` に従わせる。Android は候補リストの生成箇所にだけスクロールバーを有効化し、回転ホイールには出さないことのテストを分けて維持する (オーナー指示 2026-09-17) (→ 両 OS の Requirement: スクロールバー表示の Theme 反映)
- [x] 1.5 Android: Activity を再生成しない夜間モード変更に、スクロールバーのつまみの外観を追従させ、Scenario「夜間モードの変更にスクロールバーのつまみが追従する」をテストで担保する。overscroll 効果の色は追従を保証しない旨 (Requirement 本文) をコードコメントにも残す (review-001 Minor) (→ 同上)

## 2. 利用者所有コンテンツの Context (Android、実装済み・照合)
- [x] 2.1 CustomCell の内容と View 形式の Header / Footer に、ホストが渡した Context そのもの (同じインスタンス) が届くことと、ホストが被せたテーマの属性が解決できることを回帰テストで固定する (→ Requirement: 利用者所有コンテンツの Context は設定リストの生成方法に依らない)

## 3. Header / Footer 背景色の反映 (iOS、実装済み・照合)
- [x] 3.1 text 形式の Section / Root Header・Footer に背景色を反映し、Theme 差し替えと外観切替に追従させる。View 形式には適用せず、再利用で色を残さない (→ Requirement: Header / Footer 背景色の Theme 反映 (iOS))
- [x] 3.2 Requirement の全 Scenario をテストで担保する (利用者の dynamic な色の外観追従、text 形式から View 形式へ差し替えた領域に背景色を残さないことを含む) (→ 同上)

## 4. Header / Footer 背景色の既定を透明に (新規)
- [x] 4.1 iOS: 公開定数 `defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor` を、両外観の値を持つ `UIColor` の形のまま両外観とも透明にする (core/ADR-0032 Decision 2。固定色へ変えない)。固定の透明色を明示した Theme が既定と等価でないことをテストで担保する。既定色の生値・等価性を固定している既存テスト (ThemeDefaultColorAppearanceTests ほか) を新しい既定へ追随させる (→ Requirement: Header / Footer 背景色の既定は透明 (iOS))
- [x] 4.2 Android: 既定パレット (light / dark) の Header / Footer 背景色を透明にし、既定色を固定している既存テスト (KsSettingsViewDefaultsTest / ThemeAppearanceResolutionTest ほか) を追随させる (→ Requirement: Header / Footer 背景色の既定は透明 (Android))
- [x] 4.3 iOS / Android の既定値が同じであることの検証 (定数比較テスト) を新しい値へ追随させる (→ 両 Requirement の「相手 platform の既定と同じ」)
- [x] 4.5 MAUI: `ScrollIndicatorVisible` / `HeaderBackgroundColor` / `FooterBackgroundColor` の未指定・指定・表示中の変更が native へ渡ることを、既存の facade / bridge のテストで担保する (不足があれば足す。facade・bridge の実装は変えない) (→ Requirement: スクロールバー表示と Header / Footer 背景色は native の契約に追随する)
- [x] 4.4 samples (iOS / Android / MAUI) で Header / Footer の既定の帯に依存した見た目が無いか確認し、あれば sample 側で色を明示する (→ 両 Requirement)

## 5. 検証
- [x] 5.1 iOS / Android / MAUI の全テストを実行し件数を報告する (kasane/handbook/cross/test-execution.md)。iOS は Swift 6 言語モードの確認結果を evidence/ に記録する (kasane/handbook/ios/swift6-language-mode-check.md)
- [x] 5.2 実機・エミュレータで確認し evidence/ に証跡を残す (kasane/handbook/cross/runtime-behavior-verification.md)。確認項目ごとの状態は下記
  - [x] 5.2a Android で設定リストと PickerCell の候補リストにスクロールバーが出る / 回転ホイールには出ない
  - [x] 5.2b `false` で両 OS とも消える — Android は確認済み。iOS は実機目視を省略しユニットテストでの担保とする (オーナー確定 2026-09-17、deviation.md)。iOS でスクロールバーが出ることはオーナーが実機 (iPhone 11) で確認済み
  - [x] 5.2c 既定で両 OS とも Header・Footer の帯が出ず list 下地が見える (Classic と Modern)
  - [x] 5.2d 背景色を明示すると両 OS とも同じ範囲が塗られる
  - [x] 5.2e Activity を再生成しない夜間モード変更でスクロールバーのつまみが追従する (Android)
  - [x] 5.2f MAUI サンプルの iOS・Android の実行面でも、既定でスクロールバーが出て Header・Footer に list 下地が見える
