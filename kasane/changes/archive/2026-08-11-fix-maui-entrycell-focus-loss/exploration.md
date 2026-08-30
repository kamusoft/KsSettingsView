# Exploration: fix-maui-entrycell-focus-loss

## 課題 / 動機

MAUI Android サンプル (Pixel 6a 実機) の EntryCell で、文字確定・BackSpace のたびに入力欄がフォーカスを失い、連続入力・連続削除ができない。日本語 IME に限らず ASCII 入力でも発生する (ユーザー確認済み)。iOS 実機・Android native サンプルでは発生しない。add-maui-basic-input-cells の実機確認 tasks 7.2 で検出され、deviation.md に既知不具合として記録済み (別 change 送り)。

## 実測記録 (2026-08-11, Pixel 6a / bluejay, 実機)

### A/B 比較 (`dumpsys input_method` の mServedView フォーカスフラグ)

| | タップ後 | 1文字入力後 | 2文字目入力後 |
|---|---|---|---|
| native サンプル (EditText `712d10b`) | `.F` あり | `.F` 維持 | `.F` 維持 |
| MAUI サンプル (EditText `a1d040`) | `.F` あり | **`.F` 喪失** | — (フォーカス無しで入力不可) |

- MAUI 側でも **EditText インスタンスは同一のまま** (`a1d040` が維持) → ViewHolder / EditText の再生成は起きていない。android/ADR-0001 の保護 (payload 付き通知 + change アニメーション無効) は正しく機能している。
- 書き戻し自体は正常動作 (サンプルの「最後のイベント」ラベルが更新される)。IME も表示されたまま (`mInputShown=true`)。フォーカスフラグだけが失われる。

### jdb によるスタックトレース採取 (現行犯の特定)

`android.view.View.clearFocus` にブレークポイントを張り、キー入力 1 回で命中。スタック (要約):

```
View.clearFocus
← View.sizeChange (View.java:26128)     … フォーカス中 View のゼロサイズ化検出
← View.setFrame ← TextView.setFrame
← LinearLayout.layoutHorizontal          … Cell 行 (ADR-0002 の weight レイアウト)
← ConstraintLayout.onLayout
← RecyclerView$LayoutManager.layoutDecoratedWithMargins
← LinearLayoutManager.layoutChunk / fill / onLayoutChildren
← RecyclerView.dispatchLayoutStep2
← RecyclerView.onMeasure (RecyclerView.java:4133)   ★ measure 中の layout (auto-measure)
← FrameLayout.onMeasure                  … KsSettingsViewLayout (Host)
← com.microsoft.maui.PlatformInterop.measureAndGetWidthAndHeight (PlatformInterop.java:438)
← crc….LayoutViewGroup.onMeasure / ContentViewGroup.onMeasure   ★ MAUI のクロスプラットフォーム measure
← … ← ViewRootImpl.performTraversals (measure フェーズ)
```

ブレークポイント命中時の `this` (= フォーカス中 EditText) のフレームサイズ: **`this.mRight - this.mLeft = 0` (幅ゼロ)**。

## 原因 (確定)

1. 打鍵 → TextWatcher → C# 書き戻し (maui/ADR-0012 の必須コミット、設計どおり毎打鍵発火) → `replaceCell` → payload 付き `notifyItemChanged` → RecyclerView が requestLayout。同時にサンプルの「最後のイベント」Label 更新で MAUI 側レイアウトも無効化される。
2. 次フレームのトラバーサルで、MAUI の `LayoutViewGroup` / `PlatformInterop.measureAndGetWidthAndHeight` が Host (KsSettingsViewLayout = FrameLayout) を **非 EXACT の measure spec で measure** する。
3. RecyclerView の auto-measure は非 EXACT spec を受けると **measure 中に layout (`dispatchLayoutStep2`) を実行**する。この measure 時 layout で、Cell 行の weight ベース幅配分 (android/ADR-0002) により EditText が**一時的に幅ゼロ**に `setFrame` される。
4. Android フレームワークの `View.sizeChange` は**フォーカス中 View のゼロサイズ化を検出すると `clearFocus` する**。ここでフォーカスが失われる。その後の arrange (SettingsViewHandler.PlatformArrange が EXACT で再 measure) で最終レイアウトは正常サイズに戻るが、フォーカスは戻らない。

native (Compose `AndroidView`) は Host を常に EXACT spec で measure するため、measure 中 layout 自体が走らず、この経路が存在しない。→ MAUI 統合固有である実測と完全に整合。

## 検討した選択肢 (未決 — propose / 実装フェーズで確定)

- **案A: MAUI handler 層で measure 契約を閉じる** — `SettingsViewHandler` (または facade の MeasureOverride / GetDesiredSize) で、MAUI からの非 EXACT measure に対して platform measure へ降りずに fill サイズを返す (SettingsView は常に割当領域 fill であり、内容サイズを問い合わせる必要がない)。native 層に触らず MAUI 層で完結。
- **案B: Host (KsSettingsViewLayout) 側で非 EXACT spec を EXACT に正規化** — 前回確定サイズがあるとき、非 EXACT spec での子 RecyclerView measure を EXACT に置き換える。native アプリにも影響し得るため影響検討が必要。
- **案C: フォーカス復元の対症療法** — clearFocus 後に requestFocus し直す。IME 状態・カーソル位置の破壊が残る可能性が高く筋が悪い (第一候補にしない)。

## 決定事項

- (未決 — 修正方針の確定は次フェーズ)

## ADR 候補

- 未起票。修正方針 (案A/B) が確定した時点で「MAUI Host の measure 契約」に関する ADR を起票する価値が高い (境界を越える契約であり、覆すコストが高い)。

## 未決の論点

- 案A と案B の選択 (MAUI 層で完結できるか、Host 側の一般対策も要るか)
- 非 EXACT spec が飛んでくる正確な条件 (幅/高さどちらが UNSPECIFIED/AT_MOST か) — 修正実装時に確認
- BackSpace 連続削除・日本語 IME 確定でも同経路であることの最終確認 (修正後の実機確認に含める)

## UI 素材

- なし (挙動のみの修正)

## 変更級の推奨: S (条件付き)

- 単一課題のバグ修正、公開 API 変更なし、UI 見た目変更なし。案A (MAUI 層完結) が成立するなら触るのは maui/ の 1〜2 ファイルで S 級。
- ただし案B (android/ Host 側の措置) が必要になる場合は native への影響検討が入るため M 級へ昇格。
- 完了条件: **Pixel 6a 実機での連続入力 (ASCII + 日本語 IME 確定 + BackSpace 連続削除) のフォーカス維持確認**を必須とする (Robolectric 緑は実 IME 挙動を保証しない — kasane/lessons の既知教訓)。

## 実測ログ出典

- 本探索セッション (2026-08-11)。jdb スタックトレース全文はセッション記録参照。
