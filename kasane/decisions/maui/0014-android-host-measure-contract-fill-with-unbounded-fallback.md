---
id: 0014
title: MAUI Android Host の measure 契約 — 大きさが決まる配置では handler が fill で即答し、内容サイズを問われる配置のみ platform measure へ降ろす
status: accepted
date: 2026-08-11
---

## Context

MAUI Android の EntryCell で、キー入力のたびに入力欄がフォーカスを失い連続入力できない不具合が実機 (Pixel 6a) で確認された。jdb によるスタックトレース採取で原因を確定した:

- MAUI のクロスプラットフォーム measure (`LayoutViewGroup` → `PlatformInterop.measureAndGetWidthAndHeight`) は、明示サイズ未指定の場合 Native Host を **非 EXACT の measure spec** (有限制約 = AT_MOST / 無限制約 = UNSPECIFIED) で measure する (`ContextExtensions.CreateMeasureSpec` の規則)。
- RecyclerView の auto-measure は非 EXACT spec を受けると **measure 中に layout (`dispatchLayoutStep2`) を実行**する。この measure 時 layout で Cell 行の weight 幅配分 ([android/ADR-0002](../android/0002-cell-row-width-allocation-linearlayout-weight.md)) により、フォーカス中の EditText が**一時的に幅ゼロ**へ `setFrame` される (`this.mRight - this.mLeft = 0` を jdb で実測)。
- Android フレームワークの `View.sizeChange` はフォーカス中 View のゼロサイズ化を検出すると `clearFocus` する。ここでフォーカスが失われる。

native (Compose `AndroidView`) は Host を常に EXACT spec で measure するため measure 中 layout が走らず、この経路が存在しない。[android/ADR-0001](../android/0001-content-update-preserves-viewholder.md) の ViewHolder 維持保護は正しく機能しており (EditText インスタンスは同一のまま)、本件は別経路の問題である。

## 用語の整理 (この決定が扱う境界)

この決定が扱うのは「**MAUI のレイアウトシステムが SettingsView コントロール自体をどう measure するか**」という境界だけである。SettingsView を利用者がページ上のどこに置くか (親レイアウトが何か) で挙動が分かれる:

| 配置 (親レイアウト) | SettingsView に渡る制約 | 呼び方 |
|---|---|---|
| Grid の `*` 行 / ページ直下 / 固定サイズ指定 | 幅・高さとも有限 (「この領域に収まって」) | **大きさが決まる配置** |
| `VerticalStackLayout` 直下 / 縦 `ScrollView` の content / Grid の `Auto` 行 | 高さが無限 (「内容ぶんの高さを教えて」) | **内容サイズを問われる配置** |

**SettingsView の内側 — Cell (CustomCell 含む) や Section Header/Footer の内容 — はこの契約と無関係**である。内側は Native Host (RecyclerView) が自分で measure/layout する領域であり、この決定の分岐はどの Cell 種にも影響しない。

## Decision

Android の `SettingsViewHandler` が `GetDesiredSize` を override し、measure 契約を handler 層で閉じる:

- **大きさが決まる配置 (幅・高さの両制約が有限) のとき**: platform measure へ降りず、MAUI 本体 `CreateMeasureSpec` と同じ規則 (明示指定 → 最小クランプ → 最大クランプ) で制約から desired size を即答する。SettingsView は割当領域を fill するコントロールであり、大きさが決まっているなら内容を問い合わせる必要がない。Native Host の measure は `PlatformArrange` の EXACT 1 か所のみになり、非 EXACT spec 由来の measure 中 layout (= フォーカス喪失経路) が**消える**。
- **内容サイズを問われる配置 (制約の少なくとも一方が無限) のとき**: override せず MAUI 既定の platform measure へフォールバックする。内容ぶんの大きさに答えられるのは中身を知っている Native Host だけだからである。表示は従来どおり保たれるが、measure 中 layout も従来どおり残るため、**この配置に限りフォーカス喪失は解消されない** (本決定以前から存在する既知動作の維持であり、悪化ではない)。

iOS の handler は `GetDesiredSize` を override しない (この問題は Android の RecyclerView auto-measure 固有であり、iOS には対応する不具合経路がない)。内容サイズを問われる配置では両 OS とも platform measure に揃うため、同一 XAML の配置結果が OS 間で割れない。

## Alternatives Considered

- **内容サイズを問われる配置でも 0 を返す (全面 fill)**: 却下。`VerticalStackLayout` 直下などで SettingsView が無警告で高さ 0 になり画面から消える。iOS (platform measure のまま) と挙動が割れ、同一 XAML の互換性を壊す。独立レビューで差し戻された。
- **Native Host (KsSettingsViewLayout) 側で非 EXACT spec を EXACT に正規化する**: 不採用。android/ ビルドルートは native アプリ (Compose ラッパ) からも使われており、MAUI 固有の事情を native 層の measure 契約に持ち込むことになる。MAUI handler 層で完結する手段が存在するため、そちらを採る。
- **clearFocus 後に requestFocus で復元する対症療法**: 却下。フォーカス喪失そのものは防げず、IME の composing 状態やカーソル位置の破壊が残る可能性が高い。原因 (ゼロサイズ化) を放置して症状だけ隠す形になる。
- **書き戻し (replaceCell) の抑止・間引き**: 却下。書き戻しは Store への必須コミット ([ADR-0012](0012-interaction-value-transport-contract.md)) であり、抑止はコミット欠落バグの温床になる。実測でも書き戻し自体は無実で、measure 経路が原因だった。

## Consequences

- 正: 大きさが決まる配置 (Grid `*` 行・ページ直下など、SettingsView の通常の使い方) では、EntryCell の連続入力・連続削除・日本語 IME の確定がフォーカス維持のまま行える (Pixel 6a 実機 A/B で確認)。中に置く Cell の種類は問わない。
- 正: 大きさが決まる配置では MAUI 側 measure が platform へ降りなくなり、打鍵ごとの余分な RecyclerView measure/layout が消える。
- 負: 内容サイズを問われる配置 (`VerticalStackLayout` 直下・縦 `ScrollView` content・Grid `Auto` 行) ではフォーカス喪失経路が従来どおり残る。SettingsView をこれらの配置に置く構成は推奨しない。
- 負: `CreateMeasureSpec` の制約解決規則の写しを handler 側に持つため、MAUI 本体の規則が変わった場合に追随が必要。
- 負: OS 間の挙動一致が保たれるのは内容サイズを問われる配置に限る。大きさが決まる配置かつ非 Fill 配置 (`VerticalOptions="Start"` 等) では、Android は制約 fill の desired size、iOS は `SizeThatFits` 由来の desired size を返すため一致しない (SettingsView は Fill 配置を前提とするコントロールであり、この差は許容する)。
- 補足: 将来 MAUI 版 CustomCell (MAUI View を Cell へ埋め込む形) を導入する場合、「RecyclerView の内側に MAUI の measure が入り込む」という本決定と逆向きの統合面が生まれる。それは本決定の範囲外であり、導入フェーズで別途扱う。

出典: fix-maui-entrycell-focus-loss (exploration.md の実測記録 / review-001.md Major-1)
