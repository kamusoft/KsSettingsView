# phase-3-samples-foundation

`samples/maui/` に LabelCell のみの最小デモアプリ (土台) を構築する。各 Cell のページ追加は後続の Cell フェーズが担当する。

原案: `openspec/changes/add-samples-maui` (凍結・参照のみ)

## 論点

(なし — 全論点解消済み)

## 決定事項

- **sample-parity 上の位置づけ (2026-08-09、propose 時の上位層チェックで追加)**: LabelCell 暫定ページは**検証枠** (「LabelCell 検証」等、デモと別区分) に置き、sample-parity (cross/ADR-0016) の「platform 固有の技術検証画面」例外に乗せる — デモ画面集合のパリティ判定を汚さない。phase-4 の基本 Cell 7種デモページ追加時に削除。メニュー文言一元定義 (SampleScreen 相当) を土台に含める。Store/DSL 方式デモの MAUI 対応要否は phase-4 agenda へ申し送り

- **net10.0 workload・ビルド確認 (2026-08-09)**: フェーズ固有の論点としては解消済みでクローズ — 原案 (net9.0 前提) 由来の不確実性は、サンプルと同一 TFM 構成 (`net10.0-ios;net10.0-android`) の MauiHost が phase-2 で両 OS ビルド・E2E 全項目 PASS したことで退役済み (環境の .NET SDK 10.0.300 も確認済み)。サンプル自体の両 OS ビルド確認と AndroidX 解決の restore 実証は propose の tasks の検証項目に落とす

- **AndroidX Lifecycle 競合 (NU1608/NU1107) の解決方式 (2026-08-09)**: ライブラリ側解決を第一候補として採用 — `KsSettingsView.Binding.Android.csproj` に `Xamarin.AndroidX.Lifecycle.LiveData 2.11.0.1` を明示宣言し、LiveData family の版ねじれ (本体 2.9.2.1 vs Core 2.11.0.1) を binding 層で吸収する。これにより サンプル・将来の NuGet 利用者ともアプリ側ピン不要になる見込み。restore での実証は実装フェーズの検証タスク (nuspec 整合性からの推論で未検証のため)。**fallback**: 検証で NU1608 が消えなければ `NoWarn` NU1608 + README への既知の制約記録 (調査の結果、MauiHost 既存のアプリ側 `LiveData.Core(.Ktx)` ピンは NU1608 に対して no-op と判明したため fallback からも除外。NU1107 が実際に出た場合のみアプリ側直接参照で対処)。併せて MauiHost の効いていない既存ピン + 誤コメントの整理も本変更のスコープに含める。調査詳細: history 参照。ADR: maui/ADR-0010 (proposed)

- **`<ProjectReference>` 構成 (2026-08-09)**: 案A 採用 — サンプルは `KsSettingsView.Maui.csproj` 1本だけを参照する。Binding 2 プロジェクトの直接参照 (原案) は不採用 — phase-2 実装済みの `KsSettingsView.Maui.csproj` が TFM 条件付き `ProjectReference` で Binding を内包しており、`maui/tests/KsSettingsView.MauiHost` が同構成で両 OS PASS 済み。NuGet 化後の「1パッケージ参照」の形にも近い。AndroidX Lifecycle ピンの扱いは別論点として切り出し (上記)

- **ページ構成 (2026-08-09)**: 案A 採用 — デモ一覧ページ (MainPage) + NavigationPage の複数ページ骨格を土台フェーズで建てる。初期ページは MainPage (デモ一覧) + LabelCellPage (原案 Decision 5 の内容: 1 Section・LabelCell 3行 + Section Header/Footer)。後続フェーズ (4・6・5) は「XxxCellPage 追加 + 一覧に1行」だけで拡張する。原案 Decision 3 (単一 MainPage・AppShell 不使用) は「ページ増は後続判断」という前提が roadmap 再編 (各フェーズがサンプルページ追加を自フェーズで持つ) で解消されたため単一ページ部分を不採用。AppShell も不採用 (Shell の作法がデモの本質に混ざる)。原典 AiForms Sample・`maui/tests/KsSettingsView.MauiHost` と同型の構成。**補足**: ViewModel 層に OSS の ReactiveProperty を採用しバインディング記述量を減らす (オーナー指示)。パッケージ選定の詳細 (ReactiveProperty / ReactiveProperty.Core) は propose の設計で確定する

## TODO

- [x] 論点の解消 (2026-08-09 全3論点 + 追加のAndroidX論点を決定事項へ昇格)
- [x] ksn-propose で変更提案を起こす (2026-08-09 [add-maui-samples-foundation](../../../../changes/archive/2026-08-09-add-maui-samples-foundation/proposal.md) として M 級の提案一式を作成。相方 spec-review 採用4件を反映済み — second-opinion-001.md 参照)

## 実装結果 (2026-08-09 反映)

- [add-maui-samples-foundation](../../../../changes/archive/2026-08-09-add-maui-samples-foundation/proposal.md) として実装完了 (review-001 APPROVED / verify-001 VALID / second-opinion-002 APPROVED)。サンプル土台 (デモ一覧 + `SampleScreen` 一元定義 + LabelCell 検証ページ、ReactiveProperty.Core) と AndroidX Lifecycle の binding 層吸収 (maui/ADR-0010 — 蒸留で accepted) の両方が成立。restore 実証 + mutation probe で primary 案が確認でき、**fallback (`NoWarn` + README 記録) は発動しなかった**
- スペック沈黙領域の乖離 3件 ([deviation.md](../../../../changes/archive/2026-08-09-add-maui-samples-foundation/deviation.md)): MAUI パッケージの明示宣言 (`<MauiVersion>` + Controls / Compatibility)、`Microsoft.Maui.Controls` の 10.0.70 全プロジェクト統一、MauiHost への随伴宣言 (NU1605 回避)。いずれもオーナー指示の合意済み差分
- 保守メモ: MAUI 版更新時は `MauiVersion` を持つ 4 csproj (Maui / Maui.Tests / MauiHost / Sample) を同時に動かす (整合維持点 — review-001 記録)
- 申し送り (受け皿確定済み):
  - LabelCell 検証ページの削除・置換 / Store・DSL 方式デモの MAUI 対応要否 → [phase-4 agenda](../phase-4-basic-input-cells/agenda.md) 論点 (2026-08-09 登録済み)
  - review Suggestion 2・3 (選択解除の位置 / ReactiveProperty 破棄作法) と `Microsoft.Maui.Controls.Compatibility` 参照の要否 → [phase-4 agenda](../phase-4-basic-input-cells/agenda.md) TODO (2026-08-09 追記)
  - ルート README の `samples/maui`「未着手」表記の更新 → docs-refresh 待ち (自動発動禁止のため、オーナーの明示依頼で実行。concepts 側の追随は蒸留で完了済み)
  - NuGet パッケージ参照経由の AndroidX 競合解消効果は未検証 → パッケージング着手時の課題として見送り (roadmap 非ゴール — add-maui-core 蒸留時の合意と同じ扱い。maui/ADR-0010 の Consequences に記録)
