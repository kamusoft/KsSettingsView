---
id: 0010
title: AndroidX 依存の版競合は Binding 層の明示宣言で吸収し利用者アプリにピンを書かせない
status: accepted
date: 2026-08-09
---

## Context

`KsSettingsView.Binding.Android` の AndroidX 依存 (Fragment.Ktx 1.8.9.4 の連鎖) が `Xamarin.AndroidX.Lifecycle.LiveData.Core(.Ktx)` 2.11.0.1 を要求する一方、`Microsoft.Maui.Core` (10.0.1〜10.0.90 で同一) は `Xamarin.AndroidX.Lifecycle.LiveData` 2.9.2.1 を要求し、この版は Core を `[2.9.2.1, 2.9.3)` に縛る。結果、LiveData 本体 (2.9.2.1) と中身 (2.11.0.1) の版がねじれて利用側プロジェクトに NU1608 が出る。phase-2 の検証ホスト (MauiHost) はアプリ側 `PackageReference` ピンで対処したとされていたが、調査の結果このピンは NU1608 に対して no-op だった (ピンなしでも同版に解決され警告は残存)。サンプル・将来の NuGet 利用者すべてに波及する競合であり、どの層で解決するかは利用者の体験と将来のパッケージングを制約する。

## Decision

AndroidX 依存の版競合は **Binding 層 (`KsSettingsView.Binding.Android.csproj`) が上位版を明示 `PackageReference` 宣言して吸収する**。具体には `Xamarin.AndroidX.Lifecycle.LiveData` 2.11.0.1 を宣言し、LiveData family の版整合を binding 層で取る。利用者アプリ (サンプル含む) には競合解消のためのピンを書かせない。NuGet パッケージング時にはこの宣言が nuspec の依存へそのまま反映され、パッケージ利用者にも同じ効果が伝播する。

restore での実証は実装フェーズの検証タスクとする。検証で NU1608 が消えない場合の fallback は `NoWarn` NU1608 + README への既知の制約記録 (アプリ側ピンは no-op と判明したため fallback に含めない。NU1107 が実際に発生した場合のみアプリ側直接参照で対処)。

## Alternatives Considered

- **アプリ側 `PackageReference` ピンの踏襲 (MauiHost 方式)** — NU1608 に対して no-op と判明。効果がない上、利用者全員に儀式を強いる。却下
- **gradle 側の androidx.lifecycle を MAUI 同梱版へ下げる** — NU1608 の原因は .NET 側 PackageReference の連鎖であり、gradle 側 (2.8.6) は無関係で効かない。却下
- **Microsoft.Maui.Controls の patch 更新待ち** — 最新 10.0.90 でも LiveData 要求版は 2.9.2.1 のままで自然消滅しない。却下

## Consequences

- 正: サンプル・将来の NuGet 利用者がピンなしで NU1608 の無い restore を得られる見込み
- 正: 競合解消の知識が binding 層の csproj 1箇所に集約され、versions 更新時の見直し点が明確
- 負: Binding 層が MAUI 本体の AndroidX 要求版と binding 側 AndroidX 連鎖の両方を意識して版選定する責務を持つ (MAUI / AndroidX 更新時に再確認が要る)
- 正 (実装結果 2026-08-09): ProjectReference 経路で実証済み — ピン・`NoWarn` なしの利用側 (`KsSettingsView.Sample.Maui` / `KsSettingsView.MauiHost`) の restore で NU1608 / NU1107 とも 0 件、`project.assets.json` 上の LiveData family が 2.11.0.1 で解決。mutation probe (宣言を一時削除すると NU1608 再出現 + NU1107 + LiveData 2.9.2.1 へ後退) で宣言が load-bearing であることを確認し、fallback は発動しなかった。`Microsoft.Maui.Controls` 10.0.70 でも要求版は 2.9.2.1 のままで競合の前提は不変 (add-maui-samples-foundation の verify-001 / review-001)
- 未検証のまま残るもの: NuGet パッケージ参照経由の利用者への効果 (パッケージング着手時の課題として扱う)
- 正 (実装結果 2026-09-02): 上記の未検証項目を解消 — ローカルフィードの facade パッケージ (`KsSettingsView.Maui`) 1 行を足した素の MAUI アプリ (SDK 10.0.300、`Microsoft.Maui.Controls` 10.0.70) の restore で NU1608 / NU1107 とも 0 件、LiveData family 3 本が 2.11.0.1 に解決し、binding の明示宣言が nuspec の依存として利用者へ届くことを確認した。ピン・`NoWarn` は不要のまま
- 負 (実装結果 2026-09-02): 版の宣言元は binding csproj から `maui/Directory.Packages.props` (CPM) へ移り、版整合の理由もそこに同居する。MAUI / AndroidX 更新時の見直し点は 1 か所だが、CPM のため複数 NuGet ソースを構成した環境では restore に NU1507 が出る (扱いは release workflow の論点)

出典: kasane/roadmaps/maui-support/phases/phase-3-samples-foundation/history.md (2026-08-09: AndroidX Lifecycle 競合の解決方式) / kasane/changes/archive/2026-08-09-add-maui-samples-foundation/verify-001.md (実装結果)
出典 (2026-09-02 NuGet 経路の実証): kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/history.md (2026-09-02: AndroidX Lifecycle 競合の NuGet 経路実証) / kasane/changes/archive/2026-09-02-add-maui-nuget-distribution/evidence/consumer-verification.txt (1 節)
