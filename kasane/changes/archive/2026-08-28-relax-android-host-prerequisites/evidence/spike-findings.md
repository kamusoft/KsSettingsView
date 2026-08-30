# スパイク確定事項 (tasks 1.1 / 1.2、2026-08-27)

検証環境: Pixel 6a 実機 / Android 16 (API 36)。samples/android を一時改変した ComponentActivity ホスト (改変は巻き戻し済み)。静止画・logcat 抜粋は本ディレクトリの `spike-*` を参照。

## 1.1 ComposeView-in-ComponentDialog — 成立

- `ComponentDialog` + `ComposeView` + Compose M3 `DatePicker` は ComponentActivity ホストで表示・操作・確定まで動作
- ViewTree owner (`lifecycleOwner` / `savedStateRegistryOwner`) は `ComponentDialog` 自身が `setContentView()` 時に供給。AppCompatDialog への切替 (design Decision 4 代替案B) は不要
- **`viewModelStoreOwner` は null** — M3 DatePicker は要求しないため成立。実装ではダイアログ内 Compose で `viewModel()` 系を使わないこと
- DisplayMode 切替・日付タップ・確定・UTC 日単位往復の正しさを確認

## 1.2 View インスタンス状態の成立条件 — 全形態成立 (縮退契約が必要な形態なし)

分かれ目は「安定した View id の有無」のみ (経路には依存しない):

| ホスト形態 | View id | 結果 |
|---|---|---|
| View 直置き (setContentView) | あり | 成立 (recreate / 実回転とも復元) |
| View 直置き | なし | 不成立 (onSaveInstanceState が呼ばれない) |
| Compose AndroidView | あり | 成立。**中継実装は不要** (AndroidView が saveHierarchyState を Compose 側 SaveableStateRegistry に登録する) |
| Compose AndroidView | なし | 不成立 (登録枠は空) |
| Compose AndroidView + rememberSaveable 自前中継 | あり | 成立するが標準経路と二重保存になる — **採らない** |

## 実装方針の確定 (design Decision 5 の「スパイクで確定する」範囲)

1. **`KsSettingsView` は id 未設定のときだけライブラリ既定 id を自前付与する** (ホストの明示 id は上書きしない)。これで View 直置き / Compose AndroidView の両形態で復元が成立し、design の「ホストが id を与えること」という前提より緩い条件で足りる
2. **同一 Activity に明示 id なしの複数インスタンスが attach された場合は復元対象外** (既定 id の衝突で状態が混ざるのを防ぐ。ADR-0011 の「単独 attach でなければ復元しない」原則の踏襲。多重配置ケースは未検証のため防御側に倒す)
3. 状態中継 (`rememberSaveable` 等) は実装しない (二重保存)
4. **回転時はダイアログを明示 dismiss してから状態保存する** (dismiss しないと WindowLeaked が発生する実測あり)。`onSaveInstanceState` / `onDetachedFromWindow` での dismiss 順序を実装に含める
5. Compose ラッパ利用時の保存キーは composition キーに従属する — ラッパ内の `AndroidView` 呼び出し位置を安易に動かさない
