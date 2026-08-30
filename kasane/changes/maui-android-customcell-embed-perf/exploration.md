# Exploration: maui-android-customcell-embed-perf

## 課題 / 動機

customcell-android-maui-perf の調査 (2026-08-28、MAUI Android CustomCell のスクロールカクつき) で見つかった構造課題 2 件。当該 change では「Release ビルドでは支配項でない (native 同等以上の実測)」ため見送りとなったが、課題自体は実在する:

1. **MAUI 埋め込みが Compose の再利用最適化から外れている** — native CustomCell は android/ADR-0015 で `ReusableContent` によるノード再利用が効くが、MAUI 埋め込み (`AndroidView`、android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeCellContentView.kt) は `onReset` を渡していないため非 reusable 判定になり、行がリサイクルされるたびにノード破棄→再生成→factory 再実行 (wrapper view の detach→attach) が走る
2. **wrapper に measure キャッシュが無い** — `KsAccessoryHostView.OnMeasure` (maui/KsSettingsView.Maui/Platforms/Android/KsAccessoryHostView.cs) は制約の同一判定も結果キャッシュもなく、毎回 MAUI 側の全ツリー計測 (`IView.Measure`、Label のテキスト計測含む) を呼ぶ。1 と組み合わさり、スクロール中の毎バインドでフル再計測になる

## 検討した選択肢 (却下案と理由を含む)

(未探索。当該 change での判断のみ: Pixel 6a 実機 Release で Janky 4.6% / p90 12ms と native 基準 6.1% / 28ms を上回っており、対処のリスクに見合う効果が見込めず見送り)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)。元 change: customcell-android-maui-perf (レビュー指摘により起票)
- 着手時の前提: `kasane/concepts/maui/architecture/view-materialization.md` が **`AndroidView` へ `onReset` を与える変更は「非 reusable = 強制置換」前提を壊す**と明示警告している (`AndroidViewHolder.onReuse` の子側 `addView` が他行に奪われた実体へ実行されると例外になり得る)。reusable 化はこの節の確かめ直しが前提
- ローエンド端末・大行数・複雑 content での閾値: Pixel 6a / 40 行 / View 7 個の構成では Release で問題なし。どの条件で支配項に転じるかは未計測
- measure キャッシュは制約不変チェックだけでも効く可能性があるが、MAUI 側 (`VisualElement.Measure`) の内部キャッシュ有無は未確認

## UI 素材 (ui/references/ の一覧と注釈)

- なし

## 変更級の推奨: 未判定

(暫定: 触るのが bridge の Kotlin と MAUI platform 実装の 2 箇所で、view-materialization の前提検証を伴うため S では収まらない可能性が高い)
