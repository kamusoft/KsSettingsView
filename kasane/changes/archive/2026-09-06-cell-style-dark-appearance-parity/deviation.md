# Deviation: cell-style-dark-appearance-parity

## 仕様との乖離 (オーナー指示)

- Requirement「Cell 単位の色プロパティの変更は表示中の Cell に届く」(maui-bridge): 当初の spec では手段を「Cell プロパティの `AppThemeBinding`」としていた → tasks 0.1 の spike で届かないと判明し、オーナー裁定により手段を「`RequestedThemeChanged` を購読して再代入」に改めた。spec / proposal / tasks 3.3・8.4 を ksn-propose で修正済み (足場の書き換えではなく承認済み提案の修正)。`AppThemeBinding` を Cell で効かせる改修は `maui-appthemebinding-coverage` へ合流。理由: facade の Section / Cell が MAUI の element ツリーに属さず binding が再評価されない (2026-09-06)

## tasks の手段からの差 (実装者判断、spec との乖離ではない)

- 4.1: テスト置き場を `BasicCellsTest` / `InputCellsTest` ではなく新規 `CellSpecificColorTest` 1 本にまとめた (12 フィールドの表と共通アサーションの重複回避)。DSL 側は指定どおり `BasicCellDslTest` / `InputCellDslTest` (2026-09-06)
- 5.2: `EffectiveStyle` が本体 internal で bridge テストから参照できないため、bridge 側で Host を実描画し EntryCell の入力欄の hint 色が Cell 固有値になることを観測する形で固定した (2026-09-06)
- 6.1: テスト置き場を `EffectiveStyleResolutionTests` / `ThemeDefaultColorAppearanceTests` ではなく新規 `CellStyleAppearanceTests` にした。Scenario「CellStyle の固定色は外観で変わらない」が「表示中に外観を切り替え、同じ行の未指定の背景が dark 既定で描き直される」ことまで求めており、色の解決結果だけを見る既存 2 本には window へ載せて実描画する harness が無いため。観測点は表示中の行の `titleLabel.textColor` と `backgroundConfiguration` (`ThemeDarkAppearanceRenderingTests` の流儀) (2026-09-06)
- 3.3: MauiHost の既定シナリオに ButtonCell が無かったため、行そのもの (`ThemeFollowingButton`) を XAML へ足したうえで再代入の購読を書いた。購読は `SampleThemeFollower` と同じ流儀 (ページの Loaded / Unloaded で持ち外す) だが、MauiHost には追随役のクラスが無いので `SettingsPage` の code-behind に直接置いた (2026-09-06)
- 7.1 / 7.2: テスト置き場は `ThemeAndCellStyleTests` にした (Cell の色プロパティの配信を見る既存テスト群と同じ場所)。7.2 の Cell 固有色 3 種は同じ flush へ入るため、配信は `ReplaceCell` 3 件ではなく `ReplaceCells` 1 バッチ (更新 3 件) として観測している (2026-09-06)
- 4.1 (レビュー修正): `CheckboxCell` の accent (buttonTint) と `EntryCell` の accent (入力欄のハイライト / caret) の消費点を実描画で観測する回帰テストは、`InputCellsTest` ではなく `CellSpecificColorTest` に置いた (Cell 固有色の段解決を実描画で観測する既存テストと同じ場所)。caret tint は `Drawable.setTintList` の設定値を読み戻せないため、同じ実効 accent から作られる `highlightColor` を観測点にした (2026-09-06)
- 3.1 / 3.2 (レビュー修正): 「行が作り直されていないこと」の観測点を、更新前後の `ViewHolder` のインスタンス同一性 (`assertSame`) から Adapter が発行した変更通知の列へ移した。`ViewHolder` は remove + insert でも `RecycledViewPool` から同じインスタンスが引き当てられ、`supportsChangeAnimations = false` により payload の有無にも依らず再利用されるため、同一性では退行を検出できない (`CellListItemDiffCallback.areItemsTheSame` を全等価比較へ退行させた probe で両テストが緑になることを実測)。通知列に移した後の同じ probe では両テストが `[removed, inserted]` を検出して落ちる (2026-09-06)
- 1.4: 消費者 8 箇所へ `isSpecified` 判定を書く代わりに `EffectiveStyle.kt` の internal 拡張 `Color.toArgbOrElse(fallbackArgb)` へ集約。回帰検出力は `SwitchCellViewHolder` を素の `toArgb()` へ戻すとテスト 2 本が落ちることで確認済み (2026-09-06)

## 付随修正

- [付随修正] 1.5 の範囲外の ADR 参照除去: `SwitchCell.equals` の doc (`core/ADR-0010`)、`TimePickerCell` (`core/ADR-0028` 2 箇所)、`PickerCell` (`android/ADR-0005`)、`EntryCell` (`core/ADR-0009`)。いずれも 1.1 で触った Cell ファイル内の公開 doc コメント (2026-09-06)
- [付随修正] 2.1: `KsBridgeTheme` / `KsBridgeCellStyle` の private `color()` ラッパを削除し 22 箇所を `KsBridgeColor.color(...)` 直呼びに統一、不要 import 削除 (2026-09-06)
- [付随修正] `AttachOrderRestoreTest.kt` の `cellTitleColor?.toArgb()` を非 null 化 (警告解消)、4 ViewHolder の未使用 `toArgb` import 削除 (2026-09-06)
- [付随修正] `PickerCellItemProjection.kt` 冒頭の浮遊 KDoc から `core/ADR-0029` 参照を除去 (1.2 で触ったファイル。指揮側で直接修正) (2026-09-06)
- [付随修正] `KsCheckBoxView.swift` の `isEnabled` の doc から作業識別子の裸参照 (`refine-basic-cells-style Suggestion-1 対応`) を除去 (6.2 の対象実装。comment-policy 違反、指揮側で直接修正) (2026-09-06)
