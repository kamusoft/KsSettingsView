# phase-3-samples-foundation 議論履歴

## 2026-08-09: ページ構成 (単一ページ vs 複数ページ骨格)

- 選択肢: 案A (デモ一覧ページ + NavigationPage) / 案B (AppShell) / 案C (単一 MainPage = 原案 Decision 3)
- 採用: **案A**。roadmap 再編で phase-4/6/5 がサンプルページ追加を自フェーズで持つと確定済みのため、単一ページのままだと phase-4 が「ナビゲーション導入の構造改変 + ページ追加」の二重責務を負う手戻りが確定する。土台フェーズの責務は「後続がページ追加 + 一覧1行で拡張できる構造」の確立。原典 AiForms Sample (MainPage + NavigationPage + 多数デモページ) および maui/tests/KsSettingsView.MauiHost と同型
- AppShell 不採用理由: Shell の作法 (ルーティング等) がライブラリデモの本質に混ざり、利用者が読むコードの素直さで劣る
- 補足決定 (オーナー指示): サンプルの ViewModel 層に OSS の **ReactiveProperty** を採用し、バインディング記述量を減らす。パッケージ選定の詳細 (ReactiveProperty / ReactiveProperty.Core) は propose の設計で確定
- ADR: 起こさない (サンプル構成の可逆な判断。roadmap / agenda への記録で十分)

## 2026-08-09: <ProjectReference> 構成

- 選択肢: 案A (KsSettingsView.Maui 1本参照) / 案B (Binding 2 プロジェクトも直接参照 = 原案)
- 採用: **案A**。原案の Binding 直接参照は MAUI 本体 csproj 構成が未確定だった時期の想定。phase-2 実装の KsSettingsView.Maui.csproj が TFM 条件付き ProjectReference で Binding を内包し、maui/tests/KsSettingsView.MauiHost が同構成で両 OS ビルド・E2E PASS 済み (実証済み)
- 付随論点の切り出し: MauiHost にある AndroidX Lifecycle 競合 (NU1608/NU1107) のアプリ側 PackageReference ピンは、踏襲の前にライブラリ側で吸収する解決策を模索する (オーナー方針)。綺麗な解決が無ければピン踏襲で確定 — 新論点として agenda に追加

## 2026-08-09: AndroidX Lifecycle 競合 (NU1608/NU1107) の解決方式

- 発端: MauiHost の AndroidX ピンをサンプルに踏襲する前に、ライブラリ側で吸収する綺麗な解決策を模索したい (オーナー方針)
- ksn-scout 調査結果 (要点):
  - 真因は「LiveData 本体だけが古い」版ねじれ — Microsoft.Maui.Core 10.0.1 が Xamarin.AndroidX.Lifecycle.LiveData 2.9.2.1 (Core を [2.9.2.1, 2.9.3) に縛る) を要求する一方、Binding.Android の Fragment.Ktx 1.8.9.4 連鎖が LiveData.Core(.Ktx) 2.11.0.1 を要求 → NU1608
  - MauiHost 既存のアプリ側ピン (LiveData.Core/.Ktx 2.11.0.1) は NU1608 に対し no-op (ピンなしでも同版に解決済み、警告は残存)。csproj コメントは実態と不一致。NU1107 への対処だった可能性はあるが記録なし・未確認
  - MAUI 最新 patch (10.0.90) でも LiveData 要求版は 2.9.2.1 のまま → patch 更新での自然消滅なし
  - gradle 側 androidx.lifecycle (2.8.6) は .NET 側 PackageReference 連鎖と無関係 → gradle 側調整は効かない
- 採用: **Binding.Android.csproj に Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1 を明示宣言** (family 整合を binding 層で取る)。NuGet 化時も nuspec 依存に反映され利用者ピン不要になる副次効果。restore 実証は実装フェーズの検証タスク
- fallback: NoWarn NU1608 + README 既知の制約記録。アプリ側ピンは fallback から除外 (no-op と判明)。NU1107 実発生時のみアプリ側直接参照
- スコープ追加: MauiHost の効いていないピン + 誤コメントの整理

## 2026-08-09: net10.0 workload・ビルド確認

- 原案 (net9.0 前提) 由来の「net10 で workload・ビルドが通るか」という不確実性を引き継いだ論点だったが、phase-2 で同一 TFM 構成 (net10.0-ios;net10.0-android) の MauiHost が両 OS ビルド・E2E 全項目 PASS 済み (2026-08-08)。環境の .NET SDK 10.0.300 も確認済み
- 採用: フェーズ固有の論点としては**解消済みクローズ**。サンプル自体の両 OS ビルド確認 + AndroidX 解決 (ADR-0010) の restore 実証は propose の tasks の検証項目とする
- ADR: 起こさない (新規判断なし、既存実績の確認のみ)

## 2026-08-09: sample-parity 規約との突き合わせ (ksn-propose 時の上位層チェック)

- 発見: concepts の cross/conventions/sample-parity.md (cross/ADR-0016) は「MAUI Sample は着手時点で既存 platform の構成へ一致させる」を要求。既存 iOS/Android サンプルはデモ7画面 + メニュー文言一元定義 (SampleScreen) を持ち、LabelCell だけの暫定ページはデモ画面集合と不一致
- 選択肢: 案1 (検証枠に置く — 規約の「platform 固有の技術検証画面」例外に乗せる) / 案2 (デモ枠 + 一時的片側先行の追跡) / 案3 (基本 Cell 7種デモの器を先行)
- 採用: **案1**。「LabelCell 検証」等の表記でデモと別区分に置き、デモ画面集合のパリティ判定を汚さない。phase-4 の基本 Cell 7種デモページ追加時に削除。iOS の MinimalDiffable 検証が前例
- 共通で入れる対応: メニュー文言一元定義 (SampleScreen 相当) を土台に含める / Store・DSL 方式デモの MAUI 対応要否は phase-4 agenda へ申し送り

## 2026-08-09: 提案化 (ksn-propose) と相方 spec-review

- add-maui-samples-foundation (M 級) として proposal / specs (samples-maui, maui-bridge) / tasks を作成。ui/ はなし (前例 add-maui-core 踏襲、見た目の正は native ライブラリ側 — オーナー確認済み)
- 相方 (codex) spec-review: Major 7 / Minor 3 → 採用4 (fallback Scenario 強化・ReactiveProperty 確定・phase-4 申し送りの実記録・README 実証タスク等) / 降格3 / 却下1 (詳細: second-opinion-001.md)
- オーナー裁定: 級は M 維持 / ReactiveProperty は **Core** 版に確定 (Slim 系で足り System.Reactive 非依存。full への切替は非破壊で可逆) / サンプル付属 README は docs-refresh 規約対象外で実装タスク
