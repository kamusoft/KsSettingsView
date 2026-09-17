# 証跡の一覧

実機 / Simulator で確認した内容と、確認手順の記録。

## Android: 設定リストのスクロールバー

| ファイル | 内容 |
|---|---|
| `android-scrollbar-visible.png` | 設定リスト（基本 Cell 7 種デモ）を低速ドラッグ中に撮影。右端に縦スクロールバーが描かれている |

修正前の症状（スクロールバーが描かれない）は exploration に実機で確認した記録があり、本 change では
解消側のみを撮影している。

## Android: 選択面にスクロールバーが波及しないこと

| ファイル | 内容 |
|---|---|
| `android-wheel-scrollbar-before.png` | 同梱テーマ全体へ `recyclerViewStyle` を差していた版。NumberPickerCell のホイールをドラッグ中、右端にスクロールバーの thumb が描かれている |
| `android-wheel-scrollbar-after.png` | 適用点を設定リストに限定した版。同じ画面・同じドラッグ操作で thumb が描かれない |
| `android-picker-sheet.png` | PickerCell の候補リスト（候補 3 件でスクロールしない状態）。バーなし |

before / after は同じ端末・同じ画面・同じ操作（ホイール領域を 350px / 1200ms でドラッグし、開始から
0.9 秒後に撮影）で撮り、右端 100px を切り出して比較した。

## Header / Footer の背景色（iOS と Android の一致）

| ファイル | 内容 |
|---|---|
| `ios-header-footer-background.png` | iOS / Classic。基本 Cell 7 種デモ |
| `android-header-footer-background.png` | Android / Classic。同じ画面 |
| `ios-header-footer-background-modern.png` | iOS / Modern。Section 装飾デモ |
| `android-header-footer-background-modern.png` | Android / Modern。同じ画面 |

Modern では Header / Footer が箱の外側に置かれ、その領域は canvas と同じ色で塗られる。全幅の背景と
inset された箱が並んでも境目は生じず、Classic / Modern とも両 OS で同じ見え方になっている。

## 既定は透明 (core/ADR-0032) の見え方

| ファイル | 内容 |
|---|---|
| `android-default-transparent-classic.png` | Android / Classic。Sample の Theme から Header / Footer 背景の明示を一時的に外した状態。領域は list 下地と同じ色に見え、右端にはスクロールバーが出ている |
| `ios-default-transparent-classic.png` | iOS / Classic。同じ一時変更での同じ画面 |
| `ios-default-transparent-modern.png` | iOS / Modern。同じ一時変更での Section 装飾デモ |
| `android-default-transparent-modern.png` | Android / Modern。同じ一時変更での Section 装飾デモ |

いずれも、Header / Footer 背景を明示している通常の Sample (`*-header-footer-background*.png`) と
同じ見え方になる。Sample はもともと背景色に list 下地と同じ色を明示しているため、既定が透明に
なっても画面は変わらない — これが「何も指定しない領域に list 下地が見える」の確認になる。
撮影のための一時変更 (背景色の明示を外す / `scrollIndicatorVisible = false` / 候補を 40 件に増やす)
は撮影後に元へ戻し、作業ツリーに残していない。

## スクロールバーの表示切替

| ファイル | 内容 |
|---|---|
| `android-picker-list-scrollbar.png` | Android。PickerCell の候補リスト (一時的に 40 件へ増やしたもの) をドラッグ中。右端にスクロールバーが出る |
| `android-scrollindicator-false-list.png` | Android。`scrollIndicatorVisible = false` で設定リストをドラッグ中。バーが出ない |
| `android-scrollindicator-false-picker.png` | Android。同じ設定で候補リストをドラッグ中。バーが出ない |

iOS のスクロールインジケータは、ドラッグ中しか描かれないうえ `simctl io screenshot` の出力にも
写らなかったため、静止画の証跡を取れていない。iOS 側は実 window に載せたテスト
(`showsVerticalScrollIndicator` の観測) で担保している。

## MAUI サンプルの実行面

| ファイル | 内容 |
|---|---|
| `maui-android-default-transparent-and-scrollbar.png` | MAUI / Android。Header / Footer の背景色の明示を一時的に外した状態で設定リストをドラッグ中。領域は list 下地と同じ色に見え、右端にスクロールバーが出ている |
| `maui-ios-default-transparent.png` | MAUI / iOS。同じ一時変更での同じ画面。Header / Footer の領域に list 下地が見える |

どちらも facade / bridge は無変更のまま、native の契約に追随していることの確認。撮影のための
一時変更 (XAML の背景色 Setter を外す) は撮影後に元へ戻し、作業ツリーに残していない。

## Android: Activity を再生成しない夜間モード変更でのつまみの追従

| ファイル | 内容 |
|---|---|
| `android-night-mode-thumb-before.png` | 夜間モードでない状態で設定リストをドラッグ中。右端のつまみは白地に対する濃いグレー |
| `android-night-mode-thumb-after.png` | 同じ Activity・同じ画面のまま夜間モードへ切り替えた後のドラッグ中。つまみは暗い地に対する明るいグレーへ変わっている |

検証ホストには MAUI の Android サンプルを使った。その `MainActivity` は `ConfigChanges.UiMode` を
宣言しており、夜間モードの変更で Activity を再生成せず受け取る (native の Android サンプルは
uiMode を自前処理しないため、この経路を再現できない)。

同一 Activity のままであることは、切替の前後で `dumpsys activity activities` の
`topResumedActivity` が同じ `ActivityRecord` と同じ task を指していることで確かめた
(切替前・切替後とも `ActivityRecord{9648967 ... /crc[...].MainActivity t535}`)。再生成されていれば
別の `ActivityRecord` になる。画面も同じ「基本 Cell 7 種デモ」のまま外観だけが切り替わっている。

切替は `adb shell cmd uimode night yes` で行い、確認後に `night no` へ戻した。

## 取れていない確認

- **iOS のスクロールインジケータの静止画** — ドラッグ中しか描かれず、`simctl io screenshot` の出力にも
  写らなかった。native / MAUI とも同じで、**オーナーの実機目視が残っている** (tasks 5.2b)。iOS 側は
  実 window に載せたテスト (`showsVerticalScrollIndicator` の観測) で担保している

## iOS: Swift 6 言語モードの一時設定ビルド

`kasane/handbook/ios/swift6-language-mode-check.md` の確認。

- 実施日: 2026-09-17
- 手順: `ios/Package.swift` に `swiftLanguageVersions: [.version("6")]` を一時追加し、
  `xcodebuild build -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` を実行
- 結果: **error 0 件**（warning は本確認の対象外）
- 後処理: 一時設定を取り除き、`ios/Package.swift` が元の内容と一致することを shasum で確認（差分 0 件）

M 級として `ios/` 配下に変更を入れたため再実施した。

- 実施日: 2026-09-17 (2 回目)
- 結果: **error 0 件**
- 後処理: 一時設定を取り除き、`ios/Package.swift` が元の内容と一致することを shasum で確認 (差分 0 件)

レビュー指摘の対応でふたたび `ios/Sources/` を触ったため、3 回目を同じ手順で実施した。

- 実施日: 2026-09-17 (3 回目)
- 結果: **error 0 件**
- 後処理: 同上 (shasum 一致)
