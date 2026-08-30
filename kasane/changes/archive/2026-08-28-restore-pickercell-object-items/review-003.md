# レビュー結果: restore-pickercell-object-items (003 回目)

**日付**: 2026-08-28
**判定**: APPROVED

## サマリー

review-002 で保留になっていた 2 件のオーナー裁定 (裁定1 = 候補到着までの選択保留、裁定2 = 正規化は Cell の公開値まで) は、いずれも裁定どおりに実装・テストへ落ちている。裁定1 の 5 件の追加テストと裁定2 の契約固定テストはミューテーション実測で回帰検出力を確認した。裁定1 が壊しかねなかった「候補が既にある状態からの差し替えは位置が正」も、`hadNoItems` の一手を外すミューテーションで既存 2 件が落ちることを確認しており、退行していない。3 プラットフォームの全件テストも green (iOS 973 / Android 2680 / MAUI 501)。

残るのは、deviation 3 件目が明示的に宣言した副次挙動 (`ItemsSource` を null / 空へ差し替えても選択が保たれる) にテストもコード doc も無い点 1 件と、Suggestion 2 件。いずれも実装の欠陥ではなく、アーカイブ前に 1 件テストを足せば閉じるため APPROVED とする。

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド / テスト (iOS 全バンドル) | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → Bridge 165 + Core 88 + SwiftUI 91 + UI 629 = **973 tests / 0 failures**、`** TEST SUCCEEDED **` |
| ビルド / テスト (Android 全件) | `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL / **2680 tests / 0 failures / 0 errors** (test-results XML の合計。前回と同数 — 本周回の Android 変更は doc コメント 1 行のみ) |
| ビルド / テスト (MAUI) | `dotnet test KsSettingsView.Maui.Tests` → **501 / 0 failures** (496 → +5) |
| 裁定1 の実装 (候補が無い間は相互導出をスキップ) | `HasItems` を 4 つの同期経路の入口と `OnItemsSourceChanged` の分岐に一元化する形。候補到着時は「選択項目が保留されていればそれを起点、無ければ位置を起点」で、deviation 3 件目の記述と実装が一致 |
| 裁定1 のテスト検出力 | `HasItems` を `=> true` へ戻す (= 修正前の挙動) ミューテーションで、新規 5 件 (`SettingSelectedItemWithoutItemsSourceIsHeld` / `SelectedItemSetBeforeItemsSourceResolvesOnArrival` / `SelectedItemsSetBeforeItemsSourceResolveOnArrival` / `PendingSelectedItemsDropUnknownElementsOnArrival` / `ViewModelSelectionSurvivesReversedBindingOrder`) が**すべて落ち、他は 496 件全緑**。過不足なく対応している |
| 「候補が既にある状態からの差し替えは位置が正」の非退行 | `if (hadNoItems && HasPendingItemSelection())` から `hadNoItems &&` を外すミューテーションで `ReplacingItemsSourceRederivesSelectedItem` / `ReplacingItemsSourceRederivesSelectedItems` の 2 件が落ちる。値等価な別実体列への差し替えで新 snapshot の実体へ揃う 4 件も引き続き green |
| 裁定2 の契約固定テスト | `NormalizationStopsAtCellAndDoesNotReachViewModel` は実 binding (`SetBinding` + `INotifyPropertyChanged`) 越しに「cell は新実体 / VM は旧実体」を両方向で固定している。review-002 が案 A として挙げた「値等価時に null を挟んで書き戻す」を実装するミューテーションで**このテストだけが落ちる**ことを確認。契約が逆へ動いたら検出される |
| 逆順バインドの回帰テスト | `ViewModelSelectionSurvivesReversedBindingOrder` は `SelectedItem` → `SelectedItems` → `ItemsSource` の順で `SetBinding` し、VM 側の実体が保たれること (`Is.SameAs`) まで見ている。review-001 Major-1 の実測手順と同じ経路 |
| テスト支援の新規追加 | `InlineDispatcher` は binding 書き戻しに伴う変更通知でホスト不在の dispatcher 取得が要るため。`PickerFixtures.SelectionViewModel` の `Set` が参照等価でのみ省略するのは「値等価な別実体が届いたか」を観測するためで、裁定2 の検証に必須。いずれも意図が doc に書かれている |
| Minor (`KsSimpleCheckView` のクラス doc) の修正 | `KsSimpleCheckView.kt:19-20` が「View 自身の寸法比率で算出する (オリジナルの canvas 比率と同義。ソフトウェア Canvas への一括描画時は…)」へ書き換わり、`onDraw` のコメントと整合 |
| Suggestion (iOS のトートロジー注記) | `PickerCellItemsTests.swift:103-106` に「値型セマンティクスが保証しており実装を変えても落ちない / 実際に確かめているのは確定 index の要素が渡ること / Android と対称に置くため名前を揃えた」の 3 点が入り、誤読の余地が消えている |
| Suggestion (MAUI sample の実行証跡) | `ui/verification/maui-sample-picker-object-selection.png` を追加し、`ui/brief.md` に照合行を追記。画像を実見し、主表示 + 副表示の 2 行構成 / 副表示なし行の 1 行構成 / 長い副表示の 1 行末尾省略 / accent 色の選択印が approved.png と一致することを確認。架空のデモデータのみで個人情報の写り込みなし |
| 本周回で触られたファイルの範囲 | review-002 の時刻以降に変更されたのは `PickerCell.cs` / `PickerSelectedItemTests.cs` / `Support/InlineDispatcher.cs` / `Support/PickerFixtures.cs` / `KsSimpleCheckView.kt` / `PickerCellItemsTests.swift` / `deviation.md` / `ui/brief.md` / `ui/verification/maui-*.png` のみ。宣言された修正範囲どおりで便乗の改変は無い |
| 足場アーティファクトの書き換え | spec / proposal / design に変更なし (`git diff` は `tasks.md` と `ui/brief.md` のみ)。裁定 2 件は deviation.md に合意済み差分として記録され、spec の書き換えでは処理していない |
| tasks.md の虚偽チェック | 該当なし (8.1 の視覚照合を含め対応する実装・テスト・証跡を確認) |
| lint | comment-policy 禁止 0 件 (本周回で触れた 6 ファイル)、local-path-lint / identity-lint いずれも exit 0 |

## 指摘事項

### [🟡 Minor] 「`ItemsSource` を null / 空へ差し替えても選択は保たれる」に回帰テストもコード doc も無い

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs:320-337` (`OnItemsSourceChanged` の `if (!HasItems) { return; }`)

**問題点**:

deviation.md の 3 件目は、裁定1 の実装範囲として「`ItemsSource` を null / 空へ差し替えた場合も選択は消えずに保たれる (次に候補が届いた時点で引き直す)」を明示的に宣言している。これは本 change 以前の挙動 (候補が消えれば `SelectedItem` は null、`SelectedItems` は空へ揃い、TwoWay で ViewModel まで届いた) からの**利用者可視な変更**だが、次のいずれにも現れていない:

- テスト: `OnItemsSourceChanged` の早期 return を `SyncSelectionFromIndices()` を呼ぶ形へ差し替えるミューテーション (= 候補が空になった時点で選択を消す実装へ戻す) を入れても、**MAUI 501 件が全緑のまま**通る。現行の保留テストはいずれも「`ItemsSource` を一度も設定していない」経路しか通らず、「一度設定した候補を消す」経路を踏まない
- コード doc: `SelectedItem` / `SelectedItems` / `SyncIndexFromSelectedItem` の doc はいずれも「<see cref="ItemsSource"/> がまだ無いときの設定値」と書いており、*一度あった候補を消したとき*には触れていない。`OnItemsSourceChanged` の doc も「候補が無かった間に設定された…」の側だけを説明している

結果として、この挙動を守っているものが deviation.md の 1 行だけになっている。deviation は変更のアーカイブと共に読まれなくなる層なので、後続の周回でここを「候補が消えたら選択も消すべき」と判断して早期 return を外しても、テストは緑のままになる。

なお挙動そのものは裁定どおりで、実装の欠陥ではない (native へ渡る snapshot は候補 0 件 + 既存の範囲外 index という現行契約の範囲に収まり、表示上の破綻も無いことを確認した)。

**推奨修正**: `PickerSelectedItemTests` に 1 件足す — 候補と選択を持つ Cell の `ItemsSource` を null (または空リスト) へ差し替え、`SelectedItem` / `SelectedItems` が保たれること、続けて値等価な候補列を設定すると位置が引き直されることを固定する。あわせて `OnItemsSourceChanged` の doc に「候補が空になっても選択は捨てず、次に候補が届いた時点で引き直す」旨を 1 行足すと、ファイル単体で意図が読める。

### [🔵 Suggestion] `RestoreSelectionFromItems` だけ番人の入口チェックが無い

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs:406-429`

**問題点**: 同期経路 5 つのうち、`SyncSelectionFromIndices` / `SyncSelectedItemFromIndex` / `SyncIndexFromSelectedItem` / `SyncSelectedItemsFromIndices` / `SyncIndicesFromSelectedItems` は入口で `_syncingSelection` を見て再入を弾くが、`RestoreSelectionFromItems` だけは無条件に `true` を立て、`finally` で `false` に落とす。外側の同期が番人を握っている最中にこの経路へ入ると、戻った時点で外側の番人が解除された状態になる。

ただし到達には「選択の書き戻し → TwoWay 先の VM が同じ Cell の `ItemsSource` を空へ、続けて非空へ差し替える」という二段の同期的な差し替えが要り、実用的な再現手順は構成できなかった (単に空へ差し替えるだけなら `!HasItems` の早期 return に落ちて番人には触れない)。実害の証拠は無いので、防御の非対称としての指摘にとどめる。

**推奨修正**: 他の 4 経路と同じく `if (_syncingSelection) { return; }` を入口に置くか、番人の設定・解除を `OnItemsSourceChanged` 側の 1 箇所へ寄せる。

### [🔵 Suggestion] MAUI sample の証跡が選択面 1 枚で、samples-maui の Scenario そのものは写っていない

**該当箇所**: `ui/verification/maui-sample-picker-object-selection.png` / `ui/brief.md`

**問題点**: 撮られた 1 枚は選択面 (候補行の見た目) で、これは mock 照合の対象としては iOS / Android で既に足りている面である。一方 samples-maui の Scenario は「確定すると行の値表示と ViewModel 側の選択項目が更新される」であり、その絵 (確定後の Cell 行、または `LastEvent` に選択要素の主表示が出た状態) は残っていない。brief.md には目視確認した旨が文章で書かれているが、証跡としては裏が取れない。

review-002 の推奨は「1 枚残す」だったので要求は満たしており、facade 層の書き戻しは `PickerSelectedItemTests` / `PickerItemProjectionTests` が Fakes gateway 経由で押さえているため、判定には影響しない。

**推奨修正**: 必須ではない。次に MAUI sample を起動する機会があれば、確定直後の一覧画面 (行の値表示 + 「最後のイベント」) を 1 枚追加すると、iOS / Android の `*-sample-picker-object-rows.png` と対称になる。

## アクションプラン

1. **Minor** — `ItemsSource` を空へ差し替えたときの保持と、その後の引き直しを固定するテストを 1 件追加する (+ `OnItemsSourceChanged` の doc 1 行)。アーカイブ前に片付けたい
2. **Suggestion 2 件** — 任意
3. 蒸留時: deviation の 2 件 (候補到着までの保留と復元 / 正規化は Cell の公開値まで) はいずれも `kasane/concepts/maui/api/maui-facade.md` の PickerCell 節へ落とす必要がある。同節は現状 `SelectedItem (string?)` と `DisplayFormatter` を記述しており、本 change の内容全体で更新対象になる

## 使用した一時ミューテーションと原状復帰

lessons L-001 に従い、実施後に backup との `shasum` 一致で原状復帰を確認済み。レビュー終了時点で working tree は 39 files changed / 未追跡 23 エントリのまま変化なし。

| 対象 | ミューテーション | 結果 |
|---|---|---|
| `maui/KsSettingsView.Maui/PickerCell.cs` | `HasItems` を `=> true` へ (裁定1 の全面撤回相当) | 新規 5 件がすべて落ちる |
| `maui/KsSettingsView.Maui/PickerCell.cs` | `OnItemsSourceChanged` の早期 return を `SyncSelectionFromIndices()` へ (候補が空になったら選択を消す実装へ戻す) | **501 件全緑** (Minor の根拠) |
| `maui/KsSettingsView.Maui/PickerCell.cs` | `ApplySelectedItem` に review-002 案 A (値等価時に null を挟む) を実装 | `NormalizationStopsAtCellAndDoesNotReachViewModel` のみ落ちる |
| `maui/KsSettingsView.Maui/PickerCell.cs` | `hadNoItems &&` を削除 (保留復元を常時優先) | `ReplacingItemsSourceRederivesSelectedItem` / `...SelectedItems` の 2 件が落ちる |

復帰後 `shasum`: `3a89ca57ac37a6b3ddad5bfc797758f4dc4af74e` (`maui/KsSettingsView.Maui/PickerCell.cs`)

## Minor-1 追補の確認 (2026-08-28)

**判定**: Minor-1 は**解消**。追補後も APPROVED を維持する (残るのは Suggestion 2 件のみ)。

### 追補の範囲

review-003 出力後に変更されたのは次の 2 ファイルのみで、便乗の改変は無い (`find -newermt` と `git status` の件数一致で確認)。

| ファイル | 変更 |
|---|---|
| `maui/KsSettingsView.Maui.Tests/PickerSelectedItemTests.cs` | `ClearingItemsSourceHoldsSelectionUntilCandidatesReturn` を 1 件追加 (27 → 28 件)。他 27 件は名前・順序とも不変 |
| `maui/KsSettingsView.Maui/PickerCell.cs` | `OnItemsSourceChanged` の `<remarks>` へ 2 行 (1 文) 追記のみ。**実行コードの差分はゼロ** (追補前との `diff` で確認) |

### 確認結果

| 観点 | 結果 |
|---|---|
| MAUI 全件 | `dotnet test KsSettingsView.Maui.Tests` → **502 / 0 failures** (501 → +1) |
| review-003 で使ったミューテーションでの検出 | `OnItemsSourceChanged` の早期 return を `SyncSelectionFromIndices()` へ差し替えると **`ClearingItemsSourceHoldsSelectionUntilCandidatesReturn` だけが落ちる** (501 合格 / 1 失敗)。review-003 時点で「501 件全緑」だった穴がふさがり、かつ他テストを巻き込まない狙い撃ちになっている |
| 追補テストの後半 (再到着時の復元) の検出力 | `RestoreSelectionFromItems()` の呼び出しを `SyncSelectionFromIndices()` へ差し替える (= 位置起点へ戻す) ミューテーションで、新テストが既存 4 件と共に落ちる。候補列を**並べ替えて**再設定する設計のため、項目起点と位置起点が結果で区別できている (位置起点なら `SelectedItem` は "プッシュ" になる) |
| テスト内容と宣言済み挙動の対応 | 複数選択 Cell で `SelectedIndex` / `SelectedIndices` / `SelectedItem` / `SelectedItems` の 4 プロパティすべてについて、null 差し替え後の保持と再到着後の再導出を固定しており、deviation 3 件目の宣言 (「選択は消えずに保たれる」「次に候補が届いた時点で引き直す」) を過不足なく写している |
| doc 記述と実装の一致 | 追記文「候補を null / 空へ差し替えたときは選択を消さずそのまま保ち、次に候補が届いた時点で逆引きし直す」は `if (!HasItems) { return; }` と再到着時の分岐に一致。「逆引き」は保留項目がある場合の起点を指し、保留項目が無いとき位置を起点にする経路は同 `<remarks>` の直前 2 文が既に述べているため、ブロック単体で読んで誤解は生じない |
| lint | comment-policy 禁止 0 件 (2 ファイル) |

### 追補確認で使った一時ミューテーションと原状復帰

lessons L-001 に従い、実施後に backup との `shasum` 一致で原状復帰を確認済み。復帰後の `maui/KsSettingsView.Maui/PickerCell.cs` の `shasum` は `30bf7c1aafa56e3582a74931538c664572fe8932`、復帰後の全件再実行も 502 / 0 failures。working tree は 39 files changed / 未追跡 24 エントリ (review-003.md を含む) のまま変化なし。
