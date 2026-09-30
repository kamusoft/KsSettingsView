# Proposal: add-scroll-control

## Why

設定画面では「開いたら通知の Section へ案内する」「先頭へ戻す」「項目を足して末尾を見せる」のように、プログラムからスクロールさせたい場面がある。現状は iOS / Android / MAUI のどれにもスクロールを動かす公開 API が無い。移植元 AiForms には `ScrollToTop` / `ScrollToBottom` の bool プロパティ (true で発火しライブラリが false に戻す) だけがあり、状態で命令を表す形は KsCollectionView が却下した形である。

あわせて、MAUI でページの同じインスタンスを再 Push したときや Android の Activity が作り直されたときに、Host が作り直されてスクロール位置が先頭に戻る (settingsview-migration-defects の探索で両 OS 実測)。

設計判断はフェーズ議論でオーナー承認済み — kasane/roadmaps/maui-support/phases/phase-8-scroll-control/agenda.md の決定事項と、[core/ADR-0037](../../decisions/core/0037-scroll-control-handle-attached-to-host.md)・[maui/ADR-0029](../../decisions/maui/0029-scroll-handle-interface-created-by-settingsview.md)・[maui/ADR-0030](../../decisions/maui/0030-scroll-position-kept-across-host-by-bridge.md) (いずれも proposed、accepted への昇格は蒸留時)。スクロール制御の形は同系統の KsCollectionView (`../KsCollectionView/kasane/decisions/core/0007-scroll-controller.md`) を踏襲する。

## What Changes

- **settings-view-ios-ui**: 命令ハンドル `KsScrollController` (plain な `@MainActor` class) と、それが準拠する protocol、位置 `KsScrollPosition` (start / center / end) を追加。命令は Cell への `scrollTo`、Section への `scrollToSection` (見出しごと見せる)、`scrollToStart` / `scrollToEnd`。UIKit Host (`KsSettingsViewController`) に接続口を足し、命令を保留中のデータ反映 (diffable の apply) の完了後に実行する待ち行列を持たせる。SwiftUI は修飾子で受け、DSL 方式では明示 ID / `ForEach` の key を最終 ID へ引き直す。Host の作り直しをまたぐための「位置を控える・戻す」窓口を Host に足す
- **settings-view-android-ui**: 同名の class / interface / enum と Compose 用の `rememberScrollController()` (KsSettingsView で最初の公開 remember 関数。KsCollectionView の `rememberKsScrollController()` からはオーナー判断で名前を変える)。View Host (`KsSettingsView`) に接続口と、Store の通知の受け取りと一覧の差分反映 (submitList の commit) の完了後に実行する待ち行列。Compose は引数で受け、DSL 方式の引き直しは iOS と同じ。「位置を控える・戻す」窓口に加え、Activity の作り直しでは View の保存状態 (SavedState) から位置を戻す
- **maui-bridge**: iOS / Android の `KsSettingsBridge` が Native のハンドルを 1 つ持ち、Host を作るたびにつなぎ直す。スクロール命令 4 種の Bridge API を追加 (Store 操作 1:1 の枠外。setStyle に次ぐ 2 本目)。Host を手放すときに位置を控え、次の Host で戻す。iOS binding の `ApiDefinition.cs` を追随 (Android は自動束縛)
- **maui-core**: 公開インターフェース (命令 4 種) と、それを SettingsView がつなぐ BindableProperty (既定 OneWayToSource、実体は SettingsView が作る)、準備完了を知らせるコマンドの BindableProperty を追加。手で並べた Section / Cell 用の明示 ID プロパティを追加し、命令の対象を明示 ID と ItemsSource の項目で解決して Bridge の ID へ引き直す。Host が無い間の命令は no-op
- **samples-ios / samples-android / samples-maui**: 3 platform に「スクロール制御」デモ画面を追加 (sample-parity 準拠・同一文言・同一構成)。MAUI 版は ViewModel がハンドルを受け取り、準備完了のコマンドで開いた直後の命令を出す
- 蒸留時に反映: decisions — core/ADR-0037・maui/ADR-0029・maui/ADR-0030 を accepted へ昇格
- 蒸留時に反映: concepts — スクロール制御の共通契約 (命令・位置・順序保証・no-op 条件) を core に新設し、ios-native-host / android-native-host / ios-swiftui / android-compose に接続口と Host の作り直しをまたぐ位置の扱いを追記
- 蒸留時に反映: concepts — maui/api/native-bridge.md の「setStyle は Store 操作 1:1 の枠外にある唯一の更新 API」をスクロール命令を含む形へ改め、maui/api/maui-rendering-lifecycle.md の復元の表にスクロール位置の行を、maui-facade.md にハンドル・準備完了・明示 ID を追記
- 蒸留時に反映: handbook — cross/runtime-behavior-verification.md にスクロール制御の観測点 (到達・データ追加直後の末尾命令・一方向の着地・Host 作り直し後の位置) を追記

## Non-Goals

- 現在のスクロール位置を読む API — 利用者に控えて戻させる形は maui/ADR-0030 で却下した (Host の「位置を控える・戻す」窓口は Bridge と Host 再生成のための口で、ハンドルの語彙には入れない)
- MAUI で Section / Cell の部品そのもの (インスタンス) で対象を指すこと — ViewModel が使うインターフェースに UI の型を入れない決定 (agenda 論点3-2)
- 状態で命令する形 (AiForms の `ScrollToTop` / `ScrollToBottom` 互換) — core/ADR-0037 の却下案
- KsCollectionView 側へのインターフェース追加 — 別リポジトリの判断 (agenda の TODO に申し送りメモ)
- iOS の UIKit Host を利用者が作り直す場合の自動の位置保持 (UIKit の state restoration 対応) — iOS には Android の保存状態に当たる標準作法が無く、利用者は Host の「位置を控える・戻す」窓口で対処できる

## Impact

- 公開 API は追加のみで、既存の挙動は変えない。Store と Diff の契約は変えない (core/ADR-0037)
- 既存の反映経路に手が入る: iOS は全 apply 経路で完了を数え、Android は全 submitList 経路で commit を待てるようにする。既存の表示タイミングを変えないことを既存テストで担保する
- Android の保存状態 (SavedState) に位置を追加する。既定 id の View が同じ階層に複数あるときは保存しない既存の規則 (android/ADR-0021) に従う
- Bridge の公開面が増え、iOS binding は手書きの追随が要る
- リスク: RecyclerView での中央・末尾合わせの一方向の着地、宣言 UI で状態変更と同じ処理から出した命令の順序、iOS で画面外の Section 見出しの位置を取るタイミング

## 級: L

core の契約・iOS・Android の Native Host と宣言 UI・MAUI の facade と Bridge をまたぎ、系統間で揃える設計判断 (命令の形・指し方・位置の保持) があり、ADR 3 本を伴うため。ui/ は作成しない — 見た目の変更が無く、Sample の追加は既存 Cell でデモを組むだけのため (先行の MAUI 系 change と同じ扱い)。

domain: cross
roadmap: maui-support/phase-8-scroll-control
