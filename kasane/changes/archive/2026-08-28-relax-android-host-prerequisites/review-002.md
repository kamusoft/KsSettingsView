# レビュー結果: relax-android-host-prerequisites (002 回目)

**日付**: 2026-08-27
**判定**: APPROVED

## サマリー

review-001 / second-opinion-code-001 で確定した 6 件はいずれも正面から修正されている。焦点だった「バックグラウンド遷移でカレンダー選択面が失われる」は、`onSaveInstanceState` から dismiss を落として破棄経路 (ViewTree lifecycle の `ON_DESTROY` + `onDetachedFromWindow`) へ一本化する形で解消し、時刻シートのホスト破棄追随は共有ヘルパ `showAnchoredTo` として 4 シート + カレンダーに統一された。追加・書き換えされたテストは**ミューテーション実測で検出力を確認済み**で、指摘の再発を実際に落とせる。

修正が新たな問題を持ち込んでいないかも確認した。挙動・契約に関する新規の Critical / Major / Minor は無く、残るのは文書参照の腐りが中心の Suggestion 4 件。

### 実行して確認した客観的事実 (本レビューで自ら実行)

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL (230 tasks executed)。`build/test-results/**/TEST-*.xml` 集計で **2560 tests / 0 failures / 0 errors / 0 skipped** (debug + release の延べ)
- `cd maui && dotnet test` → exit 0
- `cd samples/android && ./gradlew assembleDebug` → BUILD SUCCESSFUL
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも exit 0 (禁止 0 件 / 検査対象 683 ファイル)
- **ミューテーション実測** (lessons code-review L-001。使用後は backup と shasum 一致で原状復帰を確認済み):
  - `HostAnchoredDialog.kt` の lifecycle 購読を無効化 → `DateCalendarDialogTest > ホストが破棄されると選択面を閉じる` / `SheetHostDestructionTest` 3 件 / `TimeSelectionSheetTest > 構成変更で Activity が再生成されると選択面は閉じられ再提示も通知もしない` の **計 5 件が FAILED** (919 tests / 5 failed)。相方が指摘した「回転テストが不備を検出できない」は解消している
  - `KsSettingsView.onSaveInstanceState` へ `dismissActiveCalendarDialog()` を戻す → `DateCalendarRecreationTest > 状態保存だけで破棄が続かなければ選択面は開いたまま選択も保つ` の **1 件だけが FAILED** (919 tests / 1 failed)。前回 Major のピンポイントな回帰検出になっている

## 前回指摘の再確認 (6 件すべて解消)

| # | 前回指摘 | 状況 |
|---|---|---|
| 1 | 🟠 バックグラウンド遷移でカレンダー選択面が失われる | **解消**。`KsSettingsView.kt:866-875` の `onSaveInstanceState` は状態を控えるだけになり、閉じるのは `HostAnchoredDialog.showAnchoredTo` の `ON_DESTROY` 購読と `onDetachedFromWindow` (`KsSettingsView.kt:292-296`) に一本化。`DateCalendarRecreationTest` は「保存だけ」と「破棄をまたぐ」の 2 Scenario に分かれ、実機証跡 `evidence/calendar-background-return-01-opened.png` / `-02-after-home-return.png` もある |
| 2 | 🟡 未使用 id `ks_date_picker_today_button` の残存 | **解消**。`android/ks-settingsview-ui/src/main/res/values/ids.xml` は `ks_settings_view` 1 件のみで、コメントも現行機構 (ID 未設定時の自己付与) の説明に置き換わっている |
| 3 | 🟡 CustomCell テストが本番配線を通っていない | **解消**。`HostThemeIndependenceTest.kt:375-419` は `showRoot` 経由で `registerCustomCell` の factory を通し、`KsAnyView` 側と同じ「ライブラリ所有の行は別の `colorPrimary` を解決する」反証アサーションを備える |
| 4 | 🟡/🟠 TimeSelectionSheet がホスト破棄に追随しない + 回転テストの検出力不足 | **解消**。共有ヘルパ `HostAnchoredDialog.kt` を新設し、4 シート + カレンダーの提示経路を `showAnchoredTo(anchor)` へ統一。`TimeSelectionSheetTest` の回転テストは実 View 階層への取り付け・旧シートの `isShowing == false`・`shownSheet()` の「表示中」判定へ書き換わり、`SheetHostDestructionTest` 3 件が既存 3 シートを担保。実機証跡 `evidence/timesheet-rotation-logcat-excerpt.txt` で `WindowLeaked` 0 件 |
| 5 | 🟡 `ui/verification/` に旧ラウンドの証跡が併存 | **解消**。1.3.1 時点の 9 枚は削除され、`ui/verification/` は m3-140 系 + landscape-fixed 系 + timepicker 系 + portrait-after-height-cap の最終周集合のみ。`ui/brief.md` 末尾に削除と据え置きの理由が明記されている |
| 6 | 🟡 (相方) テストコメントが旧実装の履歴に依存 | **解消**。`DateCalendarDialogTest.kt:46-51` / `TimeSelectionSheetTest.kt:38-43` とも「Fragment に依存せず `ComponentActivity` だけのホストでも提示できることを観測する」と現在保証する条件だけの記述になり、`comment-policy-lint.py` も exit 0 |

## 指摘事項

### [🔵 Suggestion] KDoc の参照先が移動したヘルパを指したままになっている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:861`

**問題点**: `閉じるのは、選択面自身がホストの破棄を購読して行う（[DateCalendarDialog.showAnchoredTo]）。` とあるが、今回の修正で `showAnchoredTo` は `DateCalendarDialog` のメンバから `HostAnchoredDialog.kt` の `Dialog` 拡張関数へ移っている。`DateCalendarDialog.showAnchoredTo` という KDoc リンクは解決せず、読み手が実体を辿れない。説明そのものは現行実装と一致しているので、影響は参照だけ。

**推奨修正**: リンクを `[showAnchoredTo]` (同一パッケージの拡張関数として解決する) へ変更する。

### [🔵 Suggestion] 共有ヘルパが呼び出し側の `OnDismissListener` を無条件に上書きする

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/HostAnchoredDialog.kt:32-35`

**問題点**: `showAnchoredTo` は `setOnDismissListener { ... }` で自分のリスナを張る。`Dialog` の dismiss リスナは 1 本しか持てないため、呼び出し側が事前に `setOnDismissListener` を設定していると黙って捨てられ、しかも捨てられた側ではなくヘルパ側 (observer の解除) が生き残る。逆に呼び出し後に設定されると **observer が解除されず lifecycle に残り続ける**。

現行の 5 つの呼び出し箇所はいずれも自前の dismiss リスナを持たないため実害は無い。ただし前回まで各シートに閉じ方が散っていた構造を 1 箇所へ寄せた直後であり、今後 6 つ目の呼び出し箇所が増えたときに踏みやすい。KDoc にもこの占有について記述がない。

**推奨修正**: KDoc に「このダイアログの `OnDismissListener` はヘルパが占有する。追加処理は `onDismissed` へ渡すこと」と明記する (実装を変えるなら、既存リスナを退避してから連鎖させる)。

### [🔵 Suggestion] `evidence/spike-findings.md` の実装方針が現行実装と食い違ったまま残っている

**該当箇所**: `kasane/changes/relax-android-host-prerequisites/evidence/spike-findings.md:29`

**問題点**: 「回転時はダイアログを明示 dismiss してから状態保存する ... `onSaveInstanceState` / `onDetachedFromWindow` での dismiss 順序を実装に含める」とあるが、今回の修正で `onSaveInstanceState` からの dismiss は意図的に落とした。この行はスパイク時点の実測に基づく方針で、当時としては正しい。ただし蒸留時にこの evidence を読む側からは、現行実装が方針に反しているように見える。

**推奨修正**: deviation.md に「スパイク時の実装方針 4 のうち `onSaveInstanceState` での dismiss は採らない (状態保存は破棄と同義ではないため。破棄追随は `showAnchoredTo` の `ON_DESTROY` 購読と `onDetachedFromWindow` が担う)」を 1 行足す。evidence 本体 (実測記録) は書き換えない。

### [🔵 Suggestion] `ui/brief.md` の照合記録が削除済みファイル名を証跡として挙げている

**該当箇所**: `kasane/changes/relax-android-host-prerequisites/ui/brief.md` (「照合結果 — カレンダー選択ダイアログ」節の証跡一覧、およびランドスケープ節の「既存証跡 `datepicker-calendar-light.png` と突き合わせ」)

**問題点**: 節の見出し直下に「証跡 (…`ui/verification/`): カレンダー表示: `datepicker-calendar-light.png` …」と並ぶが、これらは最終周整理で削除済み。末尾の整理メモが「上の比較記述が名前で挙げているものを含む」と包括的に断ってはいるものの、上から読むと存在しないファイルを指す索引に見える。指摘 5 の推奨 (注記で補う) 自体は満たしている。

**推奨修正**: 該当節の証跡一覧に「(1.3.1 時点。最終周整理で削除済み — 後述)」のような一言を添えて、その場で解決できるようにする。

## 確認したが問題なしと判断した観点

- **修正が新たな挙動問題を生んでいないか**: `onSaveInstanceState` から dismiss を落としたことによる window leak の穴は無い。カレンダー選択面は (a) `showAnchoredTo` の `ON_DESTROY` 購読と (b) `KsSettingsView.onDetachedFromWindow` の二重で塞がれており、ミューテーション実測でも (a) を殺した状態で `DateCalendarDialogTest > ホストが破棄されると選択面を閉じる` が落ちることを確認した。ホーム復帰時に View が detach されないことが (a)(b) いずれも発火しない条件で、これは意図した挙動そのもの
- **`onSaveInstanceState` と `onDetachedFromWindow` の順序依存**: 構成変更では保存 → 破棄の順であり、保存済みの `SavedState` は dismiss の影響を受けない。逆順 (ホストが先に View を外す) では View 階層の状態保存自体が起きないため、状態を持ち越さないのが正しい
- **共有ヘルパ化の副作用**: `showAnchoredTo` を適用した 5 箇所のうち、自前で `OnDismissListener` / `OnCancelListener` を張っているものは無く (main 配下の grep で確認)、確定・取消・外側タップ・下スワイプの各経路の無発火契約は既存テストが全経路で担保している
- **付随修正のスコープ**: 既存 3 シートへの破棄追随の横展開はファイル数・新規抽象の観点では ksn-core 同梱条件 ③ の目安を超えるが、review-001 の推奨修正が第一案として「シート系にも共有ヘルパとして適用する」を挙げており、レビュー起因の同梱として妥当。`SheetHostDestructionTest` 3 件で担保され、deviation.md:25-26 に記録済み。公開 API への影響も無い (internal の `DateCalendarDialog.onDismissed` 廃止と `trackCalendarDialog` の戻り値化のみ)
- **`minDate > maxDate` の非提示 (前回 Suggestion)**: deviation.md:28 に記録済み。ホイール型の既存防御 (`DatePickerCellViewHolder.kt:120` の `DateCandidates.of(...) ?: return`) と同型で、記録の「ホイール型と揃えた」は実装と一致する。`DateCalendarDialogTest > minDate が maxDate より後なら選択面を提示しない` が担保
- **足場アーティファクトの書き換え**: `specs/` 3 本・`proposal.md` / `design.md` に差分なし。`tasks.md` は 1.3 (エスカレーション条件・不発動) と 8.4 (蒸留へ持ち越しの明示) の 2 件が未チェックで、虚偽チェックは無い
- **ホストテーマ隔離の一貫性**: `parent.context` の直接使用は利用者所有コンテンツ側 (`SectionAccessoryViewHolders.kt:199,274` / `KsCellRegistryCustomCell.kt:29` — いずれも `ksHostContext()` 経由) だけに残り、ライブラリ所有 UI は `buildCellBaseViews` / `ksThemedContext()` に寄っている。アクセサリ生成の `val ctx = views.root.context` 化も同じ境界に沿う
- **復元予約の再入**: `scheduleRestoreScanIfReady` の呼び出し口は attach (`:290`)・root 反映 (`:546`)・`onRestoreInstanceState` (`:887`) の 3 つで、`runRestoreScan` が detach 中に空振りしたときは `pendingCalendarRestore` を残したままラッチを下ろすため、次の attach で再予約される
- **前回 Suggestion の未対応 2 件**: `SavedState` の書き出し/読み込みの判定フィールド不一致 (`KsSettingsView.kt` の `writeToParcel` は `calendarDisplayState`、読み込みは `calendarCellId`) と `TimeWheelSeries` + `@Suppress("UNUSED_PARAMETER")` の残置 (`TimeSelectionSheet.kt:25,484-485`) は、今回の修正でいずれも状況が変わっておらず、格も上がっていない。再指摘はしない

## アクションプラン

Critical / Major / Minor なし。以下はいずれも任意で、蒸留前のついで作業として実施できる。

1. **Suggestion**: `KsSettingsView.kt:861` の KDoc リンクを `[showAnchoredTo]` へ直す
2. **Suggestion**: `HostAnchoredDialog.kt` の KDoc に `OnDismissListener` を占有する旨を明記する
3. **Suggestion**: `onSaveInstanceState` で dismiss しない判断を deviation.md へ 1 行足し、spike-findings.md との食い違いを解消する
4. **Suggestion**: `ui/brief.md` の証跡一覧に削除済みである旨を添える
