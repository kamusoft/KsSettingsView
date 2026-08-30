# Exploration: customcell-android-maui-perf

## 課題 / 動機

MAUI Android の CustomCell デモ画面 (ダミー行 40 行) だけスクロールがカクつき、高速フリックで描画が追いつかない。iOS (MAUI) と Native Android サンプルは問題なし。実機 Pixel 6a で顕著。

## 検討した選択肢 (却下案と理由を含む)

調査で挙がった容疑者と切り分け結果 (2026-08-28、Pixel 6a 実機、同一フリング操作 16 回の `dumpsys gfxinfo` 比較):

| ビルド | Janky frames | p50 | p90 | p95 | p99 |
|---|---|---|---|---|---|
| Native Android サンプル (基準) | 6.1% | 6ms | 28ms | 44ms | 81ms |
| MAUI Debug (報告された状態) | 31.7% | 15ms | 121ms | 150ms | 150ms |
| MAUI Release | 4.6% | 5ms | 12ms | 34ms | 85ms |

- **支配項は Debug ビルドのオーバーヘッド (JIT + debuggable + 最適化なし)**。Release では native と同等以上 (p90 で Debug 121ms → Release 12ms)。iOS で問題が出ないのは Debug でも AOT 混在でペナルティが小さいためと推定
- 構造的な容疑者 (実在するが Release では支配項でないため対処見送り):
  - MAUI 埋め込み (`AndroidView`) が `onReset` を渡さず非 reusable → 行リサイクルのたびにノード強制置換 + view 再親付け (ks-settingsview-bridge/KsBridgeCellContentView.kt)。reusable 化は concepts/maui/architecture/view-materialization.md が明示警告するリスク (AndroidViewHolder.onReuse の子側 addView) の割に Release で効果が見込めず見送り
  - `KsAccessoryHostView.OnMeasure` に measure キャッシュ無し (毎回 MAUI 全ツリー計測)。同上の理由で見送り
- サンプル手順の構造問題: samples/maui/README.md の実機手順がすべて `-c Debug`、csproj に Release 最適化 (AOT 等) の設定無し → 手順どおりだと必ず Debug の性能を見てしまう

## 決定事項

- カクつきの原因は Debug ビルド起因と特定 (実測)。構造最適化は行わない
- 対処は「検証手順の落とし穴」への手当てとして進める (README への Release 性能検証手順の追記、性能判断は Release で行う旨の明記、必要なら Release 最適化プロパティの検討)

## ADR 候補 (作成済み: なし / 未起票)

- 現時点でなし (覆すコスト高・境界越え・将来制約のいずれにも該当せず。構造最適化を将来やる場合は view-materialization.md の警告節の確認が前提になる、が既に concepts に記載済み)

## 未決の論点

- ユーザーの体感確認: Pixel 6a に Release ビルドをインストール済み。体感でもカクつきが解消しているかの確認待ち
- Release 最適化 (`AndroidEnableProfiledAot` 等) をサンプル csproj に明示するかどうか
- README 追記の粒度 (性能検証の規約をどこまで書くか)

## UI 素材 (ui/references/ の一覧と注釈)

- なし

## 変更級の推奨: S (理由)

コード本体に触らず、サンプルの README / csproj への手当てが中心。公開 API 変更なし・可逆・UI なし。正解の値 (体感) を実機で確認しながら詰める調整型のため ksn-live で進める。
