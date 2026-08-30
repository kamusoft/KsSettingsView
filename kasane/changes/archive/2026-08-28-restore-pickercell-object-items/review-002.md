# レビュー結果: restore-pickercell-object-items (002 回目)

**日付**: 2026-08-28
**判定**: NEEDS_DISCUSSION

## サマリー

前回サイクルで修正対象とされた 4 件はいずれも正しく修正されており、うち 3 件はミューテーション実測で回帰検出力まで確認した。3 プラットフォームの全件テストも green (iOS 973 / Android 2680 / MAUI 496、いずれも失敗 0)。

一方、採用 Major (MAUI `SelectedItem(s)` の snapshot 実体への再正規化) の修正は **`PickerCell` の公開値の層では成立しているが、TwoWay binding を通して ViewModel が観測する層では成立していない**ことを実測で確認した。相方レビューが修正手順として明記していた「MAUI の `BindableObject` 側でも値等価によって更新が抑止されないことまで確認」の一手が踏まれておらず、確認すると実際に抑止されている。追加された 4 件のテストは `cell.SelectedItem` を直接読むため、この残りを素通りする。実装で解消できるが取りうる手当てに副作用 (VM への一過性の null 通知) があり設計判断を要するため、NEEDS_DISCUSSION とする。

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド / テスト (iOS 全バンドル) | `xcodebuild test -scheme KsSettingsView-Package` → 973 tests / 0 failures (前回と同数。iOS の追加テストは無い) |
| ビルド / テスト (Android 全件) | `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL / 2680 tests / 0 failures / 0 errors (2676 → +4 = 新規 2 件 × debug+release) |
| ビルド / テスト (MAUI) | `dotnet test` → 496 / 0 failures (491 → +5 = SelectedItem 系 4 件 + 射影 1 件) |
| 採用 Major の修正 (`ApplySelectedItem` / `SameItems` の ReferenceEquals 化) | 実装は意図どおり。ミューテーション (`ReferenceEquals` → `Equals` へ戻す) で新規 4 件が**すべて落ちる**ことを実測、検出力あり。原状復帰は `shasum` 一致で確認 (`0a8ccf6afc9676035dca64bdc9b9add4ea00cd72`) |
| 採用 Minor の修正 (`KsMemberProjection.Resolve` の `GetProperties` 絞り込み) | 型階層を `DeclaredOnly` で降りて名前・public instance getter・引数なしで絞る形。派生側の名前隠しにも正しい。ミューテーション (旧 `GetProperty` 実装へ戻す) で `AmbiguousMemberNameFallsBackInsteadOfThrowing` が落ちることを実測、検出力あり。原状復帰は `shasum` 一致で確認 (`c7892c62113db476700e0584c062f05a6d01b524`) |
| Major-2 の修正 (`KsSimpleCheckViewDrawTest.kt`) | ミューテーション (`width/height` → `canvas.width/height`) で `expected:<8.8> but was:<44.0>` として落ちることを実測、検出力あり。原状復帰は `shasum` 一致で確認 (`87edc157cfc6f5ebe7247947a4015268fcda8383`)。未チェック時に線を描かないことも併せて固定されている |
| Minor-1 の修正 (`bindRow(row, position)` の doc) | 本番の bind 実装としての説明へ書き直され、`internal` の理由も添えられている。検証用フック側 (`bindRow(index)`) の doc との役割分担も取れている |
| review-001 Major-1 (binding 適用順) | 未修正。オーナー判断待ちとして正しい状態 (本判定には含めない) |
| 修正周回で触られたファイルの範囲 | 前回レビュー時刻以降に変更されたのは `PickerSelectionSheet.kt` / `PickerSelectedItemTests.cs` / `PickerFixtures.cs` / `PickerItemProjectionTests.cs` / `KsMemberProjection.cs` / `KsSimpleCheckViewDrawTest.kt` / `PickerCell.cs` のみ。宣言された修正範囲どおりで、便乗の改変は無い |
| 足場アーティファクトの書き換え | spec / proposal / design に変更なし (`git diff` は `tasks.md` と `ui/brief.md` のみ) |
| lint | comment-policy 0 件、local-path-lint / identity-lint いずれも exit 0 |
| 新規 fixture (`PickerFixtures.Indexed`) の妥当性 | `int` / `string` の 2 indexer を持ち、C# 既定の indexer 名 `"Item"` で同名衝突を再現できている。主表示・副表示の両方のフォールバックを見ている |

## 指摘事項

### [🟠 Major] 再正規化した `SelectedItem` が TwoWay binding で ViewModel へ届かない

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs:475-483` (`ApplySelectedItem`)

**問題点**:

`ApplySelectedItem` の `ReferenceEquals` 化により `cell.SelectedItem` は候補の写しの実体へ揃うようになった。しかし `BindableObject.SetValue` は**値等価 (`Equals`) が成立するときプロパティ変更の伝播を抑止する**ため、正規化した実体が TwoWay binding 越しに ViewModel へ書き戻されない。値は `PickerCell` の内部には格納されるので `cell.SelectedItem` を直接読むテストは通り、ViewModel だけが古い実体を持ち続ける。

`Microsoft.Maui.Controls` 10.0.70 + 本 change の `KsSettingsView.Maui` を参照する scratch harness で、実 binding (`SetBinding` + `INotifyPropertyChanged` の ViewModel) を通して実測した結果:

```
[1] VM が値等価な別実体を SelectedItem へ設定した場合
  cell.SelectedIndex = 1
  cell.SelectedItem SameAs 候補の実体 = True     ← セル側は正規化されている
  vm.Selected       SameAs 候補の実体 = False    ← VM は自分の別実体のまま

[2] index を保ったまま ItemsSource を値等価な新しい列へ差し替えた場合
  cell.SelectedItem SameAs 新 snapshot の実体 = True
  vm.Selected       SameAs 新 snapshot の実体 = False  ← VM は「古い snapshot の実体」を握り続ける

[3] SelectedItems (複数選択) の場合
  cell.SelectedItems[0] SameAs 候補の実体 = True
  vm.SelectedMany[0]    SameAs 候補の実体 = True       ← こちらは伝播する
```

[3] が通るのは、書き戻す `List<object>` が参照等価でしか等しくならず `SetValue` の抑止に掛からないためで、`SelectedItem` (要素そのものを渡す) だけが取り残されている。つまり相方レビューが挙げた実害シナリオのうち「**選択中の index を維持したまま `ItemsSource` を値等価な新しい object 列へ差し替えると、古い snapshot の object が残る**」は、ViewModel から見る限り**まだ再現する**。相方レビューの推奨修正が「`BindableObject` 側でも値等価によって更新が抑止されないことまで確認し、必要なら正規化専用の書き戻し方法を設けてください」と明記していた一手が抜けている。

追加された 4 件のテストはいずれも `cell.SelectedItem` / `cell.SelectedItems` を直接読むため、この差を検出しない (= 修正の完了証明としては層が足りない)。

なお本件は review-001 Major-1 (binding 適用順) とは独立で、`ItemsSource` を先に宣言していても再現する。

**推奨修正** (実装可能だが副作用の選択が要るため、オーナー/orchestrator の裁定を求める):

- **A: 正規化専用の書き戻し経路を設ける** — 値等価だが参照が違うときだけ、いったん `SetValue(SelectedItemProperty, null)` を挟んでから実体を書く。同 harness で実測したところ [1][2] とも `vm.Selected SameAs 実体 = True` になり解消する。ただし TwoWay の VM には一過性の null が 1 度届く (`null` を「未選択」と解釈して副作用を持つ VM では観測される)
- **B: 現状を受け入れて契約として明記** — 「`SelectedItem` の正規化は `PickerCell` の公開値まで。TwoWay 先の VM が値等価な別実体を持ち続けることがある」を spec / concepts (maui-facade) に書き、Scenario として固定する。実装変更なし
- **C: `SelectedIndex` の書き戻しに寄せる** — `SelectedItem` の TwoWay 利用者には index 経由の同期で十分とみなし、正規化は表示・輸送の内部整合のためだけと位置づける (実質 B の一種)

いずれを採るにせよ、**binding を張った状態での期待値**を固定するテストを 1 件足すこと。現在の直読みテストだけでは同じ指摘が再発しても検出できない。

### [🟡 Minor] `KsSimpleCheckView` のクラス doc がまだ「canvas 比率」と書いている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSimpleCheckView.kt:19`

**問題点**: 付随修正で座標算出は View 自身の寸法基準に変わり、`onDraw` の中には「`canvas.width/height` は…View 自身の寸法を使う」というコメントが入った。一方でクラス doc の箇条書きは「線の座標は canvas 比率で算出する（オリジナル準拠）」のままで、修正で捨てたはずの根拠を残している。今回この付随修正はテストで固定された (`KsSimpleCheckViewDrawTest`) ので、doc だけが逆を指している状態になった。このファイルだけを読む人が `canvas` 基準へ戻す改変を「doc どおり」と判断し得る。

**推奨修正**: 「線の座標は **View 自身の寸法**の比率で算出する (オリジナルは `canvas` 基準だが、View より大きいソフトウェア Canvas への一括描画で破綻するため View 寸法を使う)」の趣旨へ 1 行書き換える。

### [🔵 Suggestion] review-001 の Suggestion 2 件は未対応 (判定には影響しない)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/PickerCellItemsTests.swift:103-119` / `kasane/changes/restore-pickercell-object-items/ui/verification/`

**問題点**: iOS のトートロジーテストへの注記追加、MAUI sample の実行証跡の 2 件はいずれも未対応のままである (現物を確認済み)。**どちらも今回の判定には影響しない** — 前者は必須ではないと明記した Suggestion、後者は mock 照合の対象外である。

ただし後者について 1 点補足する。今回のレビューでは MAUI sample の代わりに実 binding harness で facade の挙動を直接測っており、その過程で上記 Major が出た。実アプリの証跡は依然として未取得なので、Major の手当てを決める周回で MAUI sample の 1 往復を実行し、スクリーンショットを `ui/verification/` (または `evidence/`) へ 1 枚残しておくと、review-001 Major-1 (binding 適用順) の実害度とあわせて片付く。

## アクションプラン

1. **Major (再正規化が VM へ届かない)** — A / B / C のいずれを採るかオーナー判断を仰ぎ、採用案に対応するテスト (binding を張った状態での期待値) を追加する。B / C を採る場合は spec の Requirement「PickerCell の選択項目の相互導出」へ、正規化が及ぶ範囲を追記する足場更新も要る
2. **Minor (`KsSimpleCheckView` のクラス doc)** — 1 行書き換える
3. **review-001 Major-1 (binding 適用順)** — 引き続きオーナー判断待ち。上記 1 と同じ設計領域 (公開 TwoWay API が VM に何を見せるか) なので、まとめて裁定すると齟齬が出にくい
4. Suggestion 2 件は任意。1 の周回で MAUI sample の実行証跡を撮ると Major-1 の判断材料も同時に揃う

## 再レビュー時の確認点

- Major の採用案が実装・テスト・(必要なら) spec に一貫して反映されているか。特に **binding 経由の期待値を固定するテスト**が入っているか
- そのテストが検出力を持つか (採用案の変更を戻したときに落ちるか) をミューテーションで確認する
- 3 プラットフォームの全件テストが引き続き green か (件数併記)

## 使用した一時ミューテーションと原状復帰

いずれも lessons L-001 に従い、実施後に backup との `shasum` 一致で原状復帰を確認済み。レビュー終了時点で working tree は 39 files changed / 未追跡 21 件のまま変化なし。

| 対象 | 目的 | 復帰後 shasum |
|---|---|---|
| `maui/KsSettingsView.Maui/PickerCell.cs` | 新規 4 件の検出力確認 / 推奨修正 A の効果確認 | `0a8ccf6afc9676035dca64bdc9b9add4ea00cd72` |
| `maui/KsSettingsView.Maui/Internals/KsMemberProjection.cs` | 同名 indexer テストの検出力確認 | `c7892c62113db476700e0584c062f05a6d01b524` |
| `android/.../ui/KsSimpleCheckView.kt` | `KsSimpleCheckViewDrawTest` の検出力確認 | `87edc157cfc6f5ebe7247947a4015268fcda8383` |
