---
kind: rule
applies-when:
  always: false
  tasks: [描画性能の評価・計測, カクつき報告の裏取り]
title: MAUI の描画性能を測るビルド構成
description: MAUI Android の Debug は Mono インタープリタ実行のため描画性能が実力より大幅に低く見える。性能の評価・調査・カクつき報告の裏取りは必ず Release ビルドで行う (iOS 側は本件では未計測)
timestamp: 2026-09-05
---

# MAUI の描画性能を測るビルド構成

この文書を読むと、MAUI アプリでスクロールの滑らかさなど描画性能を評価するときに、どのビルド構成で測らなければ意味がないかが分かる。計測値の出典は Pixel 6a 実機の `dumpsys gfxinfo` 実測 (2026-08-28、change: customcell-android-maui-perf)。

## 規約

**性能の評価・調査・「カクつく」という報告の裏取りは、必ず Release ビルドで行う。**

MAUI Android の Debug ビルドは既定で Mono インタープリタ実行 (C# Hot Reload を成立させるため) であり、描画性能が実装の実力より大幅に低く出る。**Debug の遅さは、それ単独では実装欠陥の証拠にならない。**実装を疑うかどうかは Release の計測で判断し、**Release でもフレーム予算を超えるなら実装側を疑う**。

## 実測値 (Pixel 6a 実機、2026-08-28)

同一の CustomCell デモ画面を同一操作でスクロールさせた計測:

| ビルド構成 | Janky frames | p90 |
|---|---|---|
| Debug (既定・インタープリタ有効) | 31.7% | 121ms |
| Debug + `UseInterpreter=false` | 8.8〜19.4% | 53〜65ms |
| Release | 4.6% | 12ms |
| (参考) Android native サンプル | 6.1% | 28ms |

計測回数は Debug / Release / native が各 1 回、`UseInterpreter=false` のみ連続 2 回で、表の幅はその 2 回の値 (2 回目には連続計測による発熱の影響が混ざり得る)。

Release は native サンプルと同等以上であり、**Release で測ったときに性能問題は観測されない**。Debug 構成に `UseInterpreter=false` を入れると中間の性能になるが、**最良値 (p90 53ms) でも Release (12ms) と大差がある**ため **Release の代替にはならない** (計測用途には使わない)。2 回の値に幅があることは補足材料にとどまる (上記のとおり発熱が交絡し得る)。

## CustomCell content の重さと端末クラスの目安 (Pixel 4a / 6a 実機、2026-09-05)

MAUI の CustomCell は行のリサイクルごとに content の platform view を付け直すため、content が重いほど・端末が遅いほど bind のあるフレームが伸びる (構造は [MauiView の native 実体化機構](../../concepts/maui/architecture/view-materialization.md) の「Android — 行リサイクルのコスト」)。同一操作の Release 計測:

| content (行あたり View 数) | Pixel 6a (2022 年ミドル) | Pixel 4a (2020 年ミドル) |
|---|---|---|
| デモの標準行 (7 個) | Janky 4.2% / p90 12ms | Janky 4.4% / p90 25ms |
| 重い行 (15 個) | Janky 1.0% / p90 19ms | Janky 14.5% / p90 73ms |

**CustomCell のカクつき報告を裏取りするときは、Release であることに加えて、content の View 数と端末クラスを添える。** Pixel 4a 級 × 行あたり View 15 個は既知の限界で、wrapper 側の最適化 (計測キャッシュ) では改善しない (measure は 1 フレームの 2% 未満)。この組で予算内に収める手当ては content を軽くすること (View 数を減らす) であり、ライブラリ側の対処は行のリサイクル停止という設計変更になる (未着手。探索メモ: `kasane/changes/archive/2026-09-05-maui-android-customcell-embed-perf/exploration.md`)。計測手順とスクリプトは同 archive の `evidence/` にある。

## 計測手順

[ローカル開発環境と Sample の実行](../cross/local-development-setup.md) の MAUI 実行手順は機能確認用の `-c Debug` であり、**そのままでは性能評価に使えない** (性能を測るときは `-c Release` に置き換える)。この落とし穴が本件の発端である。

実際に使った手順:

```
dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj -f net10.0-android -c Release
adb install -r samples/maui/KsSettingsView.Sample.Maui/bin/Release/net10.0-android/jp.kamusoft.kssettingsview.samples.maui-Signed.apk
adb shell dumpsys gfxinfo <pkg> reset
# 同一画面で高速フリング往復 (上下各 8 回)
adb shell dumpsys gfxinfo <pkg>
```

最後の `dumpsys gfxinfo` の出力から Janky frames と p90 を採取する。生ログと詳細手順の証跡は `kasane/changes/archive/2026-08-28-customcell-android-maui-perf/evidence/gfxinfo-pixel6a.md` にある。

## iOS との非対称

iOS 側は本件では**未計測**である。「iOS では問題が出ない」という観察は [ローカル開発環境と Sample の実行](../cross/local-development-setup.md) にある Simulator 経路 (`iossimulator`、JIT 実行かつ Mac の描画性能) のもので、Android 実機の観察と同じ土俵にない。実機の iOS Debug は既定 (`MtouchInterpreter` 未指定 = インタープリタ無効) で AOT 主体のため乖離は小さいと**推定**されるが、裏取りはしていない。

いずれにせよ **「Android だけ遅い」という報告が来たときの第一容疑者はビルド構成**であり、platform 実装の差を疑う前にどちらの構成で観察したかを確認する。

## 関連

- [Android ビルドツールチェーンの契約](../../concepts/android/architecture/build-toolchain.md) — Android 側のビルド構成の前提
- [テスト実行規約](../cross/test-execution.md) — 機能検証側の「黙って検証にならない範囲」
- [実行時挙動の検証規約](../cross/runtime-behavior-verification.md) — 実行時挙動の裏取りと証跡の規約。本文書はそれに「どのビルド構成で測るか」を足す関係
