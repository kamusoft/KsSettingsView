---
kind: rule
applies-when:
  always: false
  tasks: [実行時挙動の不具合調査, 不具合修正の完了判定, スクロール制御の変更の完了判定]
title: 実行時挙動の検証規約
description: 実行時挙動 (IME・フォーカス・アニメーション・タイミング) が絡む不具合修正を「完了」と判定する条件 — 実環境での再現確立と修正後の同一手順による解消確認、iOS 基本 Cell Sample の目視確認項目、スクロール制御の観測点と実機観測の注意
timestamp: 2026-09-30
---

# 実行時挙動の検証規約

この文書は、実行時挙動が絡む不具合修正の**完了判定**を定める。読むと、どの種類の不具合でユニットテストだけの検証が「完了」にならないか、代わりに何を確認するかが分かる。あわせて、スクロール制御を変更したときに実機で確かめる観測点と、実機観測で結果を読み違えないための注意を定める。

## 規約

実行時挙動 — IME・フォーカス・アニメーション・スレッドやフレームのタイミング・OS サービスとの連携 — が絡む不具合の修正は、次の 3 点を満たすまで完了と報告しない:

1. **修正前に実環境で症状を再現する。** 実環境とは実機・エミュレータ/Simulator と実物の依存先 (実 IME・実キーボード操作等) の組み合わせを指す。再現できた操作手順が、そのまま解消確認の手順になる
2. **修正後に同一手順で解消を確認する。** 可能なら修正前後の A/B (修正を外したビルドで症状が出る / 入れたビルドで出ない) まで取ると、修正が原因に効いた証明になる
3. **証跡 (スクリーンショット・ログ等) を change 配下に残す。** レビューと蒸留が「解消した」の主張を検証できる形にする

原因分析の段階でも同じ規律を適用する: **コード読解だけで原因仮説を「真因」と断定しない**。仮説は実環境の観測 (再現・計測・A/B) で裏取りしてから修正に進む。

## なぜ

ユニットテストは「実装した通りに動く」ことしか守れない。原因仮説そのものが誤っていれば、仮説に沿ったテストが全て green でも不具合は 1 ミリも直らない。実行時挙動は Robolectric や macOS 上のテスト環境では再現しない要素 (実 IME の composing、ItemAnimator の実挙動、フレーム間タイミング等) を含むため、テスト環境の green は実機の動作を保証しない。

## 適用範囲

- **対象**: 症状が実行時にしか現れない不具合。目安は「ユニットテストでその症状自体を再現できるか」— できないなら本規約の対象
- **対象外**: 純ロジックの不具合 (ユニットテストで症状を再現できるもの)。この場合は失敗するテストの作成が再現手順を兼ね、テストの green が解消確認になる

ユニットテストが不要になる規約ではない。実環境の再現確認に**加えて**、修正の意図を固定する回帰テストは通常どおり書く。

## iOS Basic Cell Sample の目視確認

`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` を Simulator または実機のライト外観で起動し、Sample Theme と基本 Cell の統合状態を次の観測点で確認する (この画面は list style を指定しないため既定の Classic で描かれる)。これは色値一覧を正典化するものではなく、Theme の値は `samples/ios/KsSettingsViewSample/SampleTheme.swift`、画面の文言と構成は `SampleScreen.swift` および画面実装が正である。

| 観測点 | 確認する結果 |
|---|---|
| Sticky Footer | RadioCell Section の footer `You can select either TypeA or TypeB.` が画面下端へ固定されず、content とともにスクロールアウトする |
| canvas と Cell 背景 | Section 間と Header / Footer 領域にベージュ系の canvas 背景が描かれ、白い Cell 背景とは別の二層として見える |
| Header / Footer の空領域 | Header text や Footer text がない Section（CommandCell / LabelCell / SwitchCell / CheckboxCell / SimpleCheckCell / ButtonCell の Footer など）に不要な余白が生じない |
| separator inset | Section 先頭・末尾の境界線は全幅、Section 内の線は icon の有無にかかわらず 16pt の leading inset を持つ ([設定 list の外観](../../concepts/core/styling/list-appearance.md) の Classic の separator) |
| icon | CommandCell の `Tanaka Taro` に `person.crop.circle`、LabelCell の `Storage` に `externaldrive` の SF Symbols が描画される |
| 順序と文言 | CommandCell → LabelCell → SwitchCell → CheckboxCell → RadioCell → SimpleCheckCell → ButtonCell の順で、title / description / valueText と RadioCell footer が Android Sample と一致する |

この確認を不具合修正の完了証跡に使う場合は、上の一般規約どおり修正前後で同じ操作と観測点を使う。

## スクロール制御の観測点

発火条件: スクロール制御の経路 — 命令ハンドル・Host の命令の待ち行列・位置の計算とアニメーション・位置を控える / 戻す窓口・Bridge のスクロール命令と位置の控え — のいずれかを変更した。このとき、変更した platform (Bridge / facade を変更したら MAUI も) の「スクロール制御デモ」画面を Simulator / Emulator で動かし、次の観測点を確かめて証跡を change 配下に残すまで完了と報告しない。デモ画面は `samples/ios/KsSettingsViewSample/ScrollControlDemoView.swift`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/ScrollControlDemoScreen.kt`、`samples/maui/KsSettingsView.Sample.Maui/Pages/ScrollControlDemoPage.xaml` で、3 platform とも同じ文言と構成である。命令の位置と実行順の定義は [スクロール制御](../../concepts/core/architecture/scroll-control.md) が持つ。

| 観測点 | 確認する結果 | 見方 |
|---|---|---|
| 各命令の到達 | 「Section へ」で Section 4 の見出しが上端に、「Cell を中央へ」で項目 3-3 の行の中央が表示範囲の中央に来る。「末尾へ」で Root Footer まで、「先頭へ」(アニメーションなし) で内容の最上端まで見える | 到着後の静止画 |
| 追加直後の末尾命令 | 「追加して末尾へ」で、足した項目を含む新しい末尾 (Root Footer) まで届く。続けて 2 回押したときも 2 件目を含む末尾に届く | 到着後の静止画 |
| 一方向の着地 | 「末尾へ」と「Cell を中央へ」のアニメーションが、行き先を越えて戻る動きを含まずに止まる。先頭側から下向きに送る場合と、末尾側から上向きに送る場合の両方で見る | 連続フレームか録画。フレームごとの移動の向きが途中で反転しないことで判定する (到着後の静止画では判定しない) |
| Host の作り直し後の位置 | 作り直す前に上端にかかっていた要素が、作り直した後も同じずれで上端にかかる。iOS の MAUI では加えて、SettingsView の領域が画面の下端まで届いている (白い帯が残らない) | 作り直す前と後の静止画 (A/B) |

Host の作り直しは次の操作で起こす。作り直す前に、先頭から離れた位置まで送っておく — 先頭のままでは位置が保たれたかを区別できず、iOS の MAUI の白い帯 (位置の戻しに連動して大きいタイトルが縮むときに出る) も現れない。

| 対象 | 作り直しの起こし方 |
|---|---|
| MAUI (両 OS) | 戻る操作で Pop したページと同じインスタンスをもう一度 Push する (Sample の「離れて戻る（Handler 切断 → 再接続）」、MauiHost のページ再訪問)。上に別のページを Push して戻るだけでは Host は作り直されず、観測にならない。ページが重なる構成そのものを確かめるときは、Sample のナビゲーションバーの「次のデモへ進む」で SettingsView のページを積む (Android では、上に積むと下のページの View は階層から外れる) |
| Android の Activity | 夜間モードの切り替え (`adb shell cmd uimode night yes` / `no`) か画面の回転。Navigation Compose の `NavHost` 配下の画面 (Android Sample のデモ画面はこれに当たる) で行う — `NavHost` は Activity の保存の後に画面の状態を保存し直すため、`NavHost` の外で確かめても観測にならない |
| Android の Navigation Compose | `NavHost` 配下の画面から別の画面へ進んで戻る (Sample のデモ画面の上部バーの「次のデモへ進む」で KsSettingsView の画面へ、「メニューを開く」で KsSettingsView を使わない画面へ進む。行き先が KsSettingsView の場合と、その後に使わない画面へ進んで戻る場合の両方を見る) |

一方向の着地は、アプリを起動し直した直後の 1 回目の命令で見る。行の高さが推定のまま実測で確定していない間のほうが、スクロール中に行き先が手前へ移って行き過ぎが出やすい。2 回目以降は高さが確定していて、行き過ぎがあっても現れにくい。

## 実機観測の注意

- Mac の画面をロックしている間は、Android Emulator の描画が間引かれて連続フレームが取れない。連続フレームと録画は、画面のロックを解除した状態で撮る
- Android の要素一覧の取得 (uiautomator の dump、端末操作ツールの要素一覧) と TalkBack は、アプリのアクセシビリティの経路を実行する。取得の途端にアプリが落ちたら、ツールの不調として読み飛ばさず、logcat の例外をアプリの不具合として証跡に残す

## 出典

fix-entrycell-ime-composition (2026-08-01): Android EntryCell の日本語 IME 即時確定不具合で、コード読解のみから原因を断定して修正し、ユニットテスト全 green・独立レビュー APPROVED まで通したが、オーナー実機確認で症状が全く変わっていないことが判明した (真因は別箇所)。再修正では修正前ビルドで症状を再現 → 修正後ビルドで解消をスクリーンショット証跡付きで確認してから完了報告し、この規約の原型となった。

add-scroll-control (2026-09-30): スクロール制御の実装で、ユニットテストが通った後の実機検証から 3 件の不具合が見つかった — iOS で末尾合わせのアニメーションが行き先 (推定より低い行が実測で確定して手前へ移った) を越えて止まる、Android で `NavHost` 配下の画面が Activity の作り直しで先頭に戻る、MAUI の Handler の切断が window から外れた後に届き位置を控えられない。いずれも静止画の到着位置やテスト環境では見えず、連続フレームと作り直しの A/B で見つかったため、観測点として残した。

## 関連

- [test-execution.md](test-execution.md) — 「実行件数の確認までが検証」— テスト実行そのものに潜む同系の落とし穴
- [local-development-setup.md](local-development-setup.md) — Sample を開いて実行するまでの環境設定と手順
- [sample-parity.md](sample-parity.md) — 3 platform の Sample の文言と構成をそろえる規約 (スクロール制御デモ画面もこれに従う)
