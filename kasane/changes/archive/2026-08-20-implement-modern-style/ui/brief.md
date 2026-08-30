# UI Brief: implement-modern-style

## 画面と状態

対象はライブラリの表示 style であり、アプリ画面固有の loading / empty / error 状態は持たない。確認すべき表示状態は:

- Modern (既定 Theme): 箱型 Section、Header / Footer は箱の外、セクション内の中間 separator のみ
- Modern (4属性指定): margin / radius / border の指定値が箱へ反映された表示
- Classic (sectionMargin 上下指定): flat リストのまま Section 間に余白
- 構成バリエーション: Header/Footer 付き Section・icon 付き Cell・単一 Cell の Section

## リファレンス注釈

- `references/ios-settings-top.png` — **採用**: 箱型 Section のまとまり方、箱の左右余白、セル間 separator の見え方。**対象外**: 検索バー、大型プロフィール行、Liquid Glass の質感・ぼかし表現
- `references/ios-settings-display-brightness.png` — **採用**: Section Header (外観モード) が箱の上外側・Footer (説明文) が箱の下外側にある配置。**対象外**: 画面ナビゲーション、外観モード選択 UI などの Cell 内容そのもの

## デザイントークン参照

- 色は Theme の既定値から解決する ([style-resolution](../../../concepts/core/styling/style-resolution.md) / [list-appearance](../../../concepts/core/styling/list-appearance.md)): 箱 = `cellBackgroundColor`、下地 = `backgroundColor`、罫線 = `separatorColor`、Header/Footer 文字 = `headerTextColor` / `footerTextColor`
- Modern の iOS 既定寸法 (論点8) は承認モックの値を正とする。Android の既定寸法は現行実装値を維持 (platform 既定は統一しない)

## 承認モック

- **mock/variant-a-ios26.html を採用 (approved.png、2026-08-20 ユーザー承認)** — iOS 26 風。iOS の Modern 既定値はこのモックの値を正とする (探索の論点8の決着、確定値は design.md Decision 6)
- 不採用: mock/variant-b-inset-grouped.html (従来 insetGrouped 風)
- 改訂 (2026-08-20、ユーザー提案): モックの下地色をサンプル共通 `SampleTheme` の PaleBackColorPrimary に差し替え、approved.png を再撮影。モックの下地は**デモ Theme の `backgroundColor`** であり `Theme()` 既定 (白) の表現ではない — Modern は新たな色既定を導入せず、箱の視認性は `backgroundColor` と `cellBackgroundColor` の対比に依存する (second-opinion-spec-001 M1 の決着)。この色のおかげでサンプル実機スクショと approved.png の視覚照合が下地込みで一致する
- 補足: モックが定めるのは**未指定時の iOS 既定寸法**のみ。利用者は Theme の4属性で自由に上書きできる。Android の既定は現行実装値を維持 (platform 既定は統一しない)

## iOS 実装・視覚照合メモ (2026-08-20)

デモ画面: `samples/ios/KsSettingsViewSample/SectionDecorationDemoView.swift` (ルートメニュー「Section 装飾デモ（style 切替）」)。style を Classic / Modern の segmented で、装飾4属性を3プリセット (既定 / 余白広め・角丸小 / ボーダーあり) で切り替える。Section 構成・文言はモックの4 Section と一致させた。

### 照合結果

`verification/` の実機 (iPhone 17 Simulator, iOS 26) スクリーンショットと approved.png を照合。実測で一致した項目:

- 箱の上余白 22pt・左右余白 16pt (箱幅 = 端末幅 - 32)
- Header は箱の上外側・Footer は箱の下外側
- separator は箱の中間のみ・箱の内側 leading / trailing から 16pt・単一 Cell の Section には無し
- 箱の角丸の見え方、下地 (PaleBackColorPrimary) と箱 (白) の対比
- プリセット切替で margin / radius / border が切り替わる (余白広め・角丸小 = margin 32・radius 8、ボーダーあり = width 2・#C7C7CC)
- Classic では箱を描かず、プリセットの差は Section 間の上下余白にのみ現れる (左右余白は無視)

オーナーの最終承認は未取得 (証跡を提出済み)。

### 合意が必要な差分

- **4番目 Section のボーダー**: モックは既定表示のまま4番目の箱にだけボーダーが描かれているが、装飾4属性は SettingsView 全体の Theme が持つため、実装ではボーダーは全 Section へ一括で効く (Section 単位の上書きは Non-Goal)。照合は「既定」プリセット (箱・余白・separator) と「ボーダーあり」プリセット (枠線) の2枚に分担した
- **アイコンの字形**: モックは絵文字バッジ。実装は SF Symbols を白抜きにして色地の 29pt 角丸バッジ画像へ焼き込む (`Theme.cellIconSize` 29 / `cellIconRadius` 7)。バッジの幾何 (寸法・角丸・地色) は一致、字形のみ異なる
- **端末幅**: モック 393pt に対し照合機は 402pt。箱幅が 9pt 広い (余白値は一致)
- **行高**: 実装の行はモック (min-height 46px) より数 pt 高い (ライブラリの行高既定)
- **画面上部**: style / プリセットの操作 UI と NavigationStack のタイトルはデモ固有。モックの「設定」タイトルと注釈枠は実装対象外

### 色の出どころ

デモ Theme は `SampleTheme.sectionDecorationDemo(...)`。下地と Header / Footer 背景に PaleBackColorPrimary を敷き、箱 (`cellBackgroundColor`)・separator・Header / Footer 文字色は Theme 既定のまま。switch の緑・バッジ地色4色・ボーダー灰はモックの値を `SampleTheme` の定数として定義した (ライブラリのトークンではなく Sample の値)。

### 証跡

- `verification/ios-modern-standard.png` — Modern + 既定
- `verification/ios-modern-bordered.png` — Modern + ボーダーあり
- `verification/ios-modern-wide-margin.png` — Modern + 余白広め・角丸小
- `verification/ios-classic-standard.png` — Classic + 既定 (従来表示のまま)
- `verification/ios-classic-wide-margin.png` — Classic + 余白広め・角丸小 (上下余白のみ反映)
- `verification/compare-mock-vs-ios-modern-standard.png` — approved.png との横並び
- `verification/compare-mock-vs-ios-modern-bordered.png` — 同上 (ボーダーあり)

## Android 実装・視覚照合メモ (2026-08-20)

デモ画面: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationDemoScreen.kt` (ルートメニュー「Section 装飾デモ（style 切替）」)。style を Classic / Modern の SegmentedButton で、装飾4属性を3プリセット (既定 / 余白広め・角丸小 / ボーダーあり) で切り替える。Section 構成・文言は iOS デモと一字一句一致させた。

### 照合結果

`verification/android-*.png` の実機 (Pixel 6a, 411dp 幅) スクリーンショットと approved.png を照合。構造として一致した項目:

- 箱が Section の Cell 行だけを覆い、Header は箱の上外側・Footer は箱の下外側に置かれる
- separator は箱の中間のみ・箱の内側 leading / trailing から 16dp・単一 Cell の Section (True Tone) には無し・icon の有無に依存しない
- 下地 (PaleBackColorPrimary) と箱 (白) の対比、角丸の見え方
- プリセット切替で margin / radius / border が切り替わる (余白広め・角丸小 = margin 32・radius 8、ボーダーあり = width 2・#C7C7CC)
- Classic では箱を描かず、プリセットの差は Section 間の上下余白にのみ現れる (左右余白は無視)
- Header / Footer 行にも sectionMargin の水平成分が効き、箱と左右端が揃う

寸法は Android の既定値 (margin 16/12dp・radius 12dp) を維持しているため、モックの iOS 既定寸法 (上 22・左右 16・radius 26) との差は許容範囲として扱う。

オーナーの最終承認は未取得 (証跡を提出済み)。

### 許容した差

- **既定寸法**: Android の Modern 既定は margin 16/12dp・radius 12dp。platform 既定は統一しない方針のためモックとの寸法差は許容
- **4番目 Section のボーダー**: iOS と同じ扱い。照合を「既定」プリセットと「ボーダーあり」プリセットの2枚に分担した
- **行高・端末幅**: 実機 411dp・Android の行高既定によりモックより行が高く、4 Section 全体が1画面に収まらない (照合用の全景は上下2枚を継ぎ合わせた `compare-mock-vs-android-*.png` を使用)
- **アイコンの字形**: モックは絵文字バッジ、iOS は SF Symbols、Android は Material Symbols を白抜きにして色地の正方形画像へ焼き込む。バッジの地色 (4色) は iOS と同一 RGBA。Bluetooth 行のシンボルは iOS が電波アイコン、Android が bluetooth アイコン
- **画面上部**: style / プリセットの操作 UI と TopAppBar のタイトルはデモ固有。モックの「設定」タイトルと注釈枠は実装対象外

### 本体ライブラリ起因の乖離 (未修正・要判断)

- **`Theme.cellIconSize` / `cellIconRadius` が Android の描画に効かない**: `EffectiveStyle.effectiveIconSize` / `effectiveIconRadius` は解決関数が存在しテストもあるが、`CellBaseLayout` は `iconView` を 24dp 固定で構築し、角丸 clip も行わない (呼び出し箇所が存在しない)。デモ Theme は iOS と同じ 29dp / radius 7dp を渡しているが、実機では 24dp・角丸なしの正方形バッジになる。iOS は同じ値で 29pt・角丸 7pt を反映するため、両 platform で見た目が揃わない。本 change の Modern 実装とは独立した既存の欠落であり、修正は行っていない

### 色の出どころ

デモ Theme は `SampleTheme.sectionDecorationDemo(...)`。下地と Header / Footer 背景に PaleBackColorPrimary を敷き、箱 (`cellBackgroundColor`)・separator・Header / Footer 文字色は Theme 既定のまま。switch の緑・バッジ地色4色・ボーダー灰は iOS と同一 RGBA を `SampleTheme` の定数として定義した。

### 証跡

- `verification/android-modern-standard.png` — Modern + 既定 (画面上部)
- `verification/android-modern-standard-bottom.png` — 同上 (スクロール後、4番目 Section と Footer)
- `verification/android-modern-bordered.png` — Modern + ボーダーあり
- `verification/android-modern-wide-margin.png` — Modern + 余白広め・角丸小
- `verification/android-classic-standard.png` — Classic + 既定 (従来表示のまま)
- `verification/android-classic-wide-margin.png` — Classic + 余白広め・角丸小 (上下余白のみ反映)
- `verification/compare-mock-vs-android-modern-standard.png` — approved.png との横並び
- `verification/compare-mock-vs-android-modern-bordered.png` — 同上 (ボーダーあり)

## 追補: iOS 構造変更 (Cell 挿入 / 削除) の視覚照合 (2026-08-20)

レビュー指摘 (挿入 / 削除後に隣接 Cell の箱 clip が更新されない) の修正に伴う追加証跡。既存の
`ios-modern-*.png` は静止状態のみを写しており、構造変更後の状態を含まない — その範囲を補う。

撮影条件: iPhone 17 Simulator (iOS 26.0)、Modern、ボーダー 3pt (箱の縁を見えるようにするため)、
中央の Cell に `CellStyle.backgroundColor` (淡黄) を指定 (箱の塗りと同色だと clip の破綻が見えないため)。
`KsSettingsViewController` を window に載せ、`applyDiff` で末尾 Cell の削除 → 末尾への挿入を行い、
各時点の実描画を撮影した。

- `verification/ios-modern-insert-delete-1-before.png` — 3 Cell (末尾は白い Cell)
- `verification/ios-modern-insert-delete-2-after-delete.png` — 末尾 Cell を削除 (淡黄の Cell が新しい末尾)。
  背景が箱の角丸に収まり、ボーダーが全周で見える
- `verification/ios-modern-insert-delete-2-after-delete-before-fix.png` — 同じ操作を修正前の実装で撮影した A/B。
  淡黄の背景が箱の角丸と下端ボーダーを塗りつぶしている
- `verification/ios-modern-insert-delete-3-after-insert.png` — 末尾へ挿入 (淡黄の Cell が中間へ戻る)。
  中間 Cell に角丸が残らない
