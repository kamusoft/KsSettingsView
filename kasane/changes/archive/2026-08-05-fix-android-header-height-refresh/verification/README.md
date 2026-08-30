# 実行時挙動の検証証跡: fix-android-header-height-refresh

規約: [concepts/cross/conventions/runtime-behavior-verification.md](../../../concepts/cross/conventions/runtime-behavior-verification.md)

- 実施日: 2026-08-05
- 実機: Pixel 6a / Android 16 (`<android-device-serial>`)
- アプリ: `samples/android` (composite build で本体ソースを直接参照するため、本体の修正有無がそのままビルドへ反映される)

## 再現手順

`StoreDemoScreen` に検証用の一時ボタン「H高さ」を追加し、`store.replaceSection` で対象 Section の `headerHeight` を `-1.0` (自動) ↔ `160.0` (固定) にトグルさせた。header text は据え置き。

1. アプリ起動 → 「Store 方式デモ」を開く
2. 「H高さ」をタップ (headerHeight: -1.0 → 160.0)
3. 「PoC Section」ヘッダー領域の高さを観察する

検証用ボタンは証跡取得後に削除した (Sample は全 platform で同一画面構成を保つ規約のため)。

## A/B の結果

| 証跡 | ビルド | 操作 | 結果 |
|---|---|---|---|
| `01-before-fix-initial.png` | 修正なし | 画面表示直後 | ヘッダーは自動高さ |
| `02-before-fix-after-tap-unchanged.png` | 修正なし | 「H高さ」タップ後 | **高さが変わらない (症状の再現)** |
| `03-after-fix-after-tap-expanded.png` | 修正あり | 「H高さ」タップ後 | **160dp へ拡大 (解消)** |
| `04-after-fix-toggle-back.png` | 修正あり | もう一度タップ | 自動高さへ復帰 (往復も成立) |

「修正なし」ビルドは `CellListItemDiffCallback.isSameHeaderHeight` の高さ比較を `return true` に置き換えて作成した (`areContentsTheSame` から固定高さの差が見えない、修正前と同じ状態)。

Store の model は修正の有無に関わらず `headerHeight = 160.0` へ更新されている。差が出るのは表示への反映だけであり、症状が DiffCallback の内容比較に起因することを示している。

## 補足: この経路以外の到達性

本検証は Store / View API 経由 (`store.replaceSection` → `SettingsRootDiff.ReplaceSection`) で行った。

**Compose DSL 経由では本修正に到達しない。** [DSLDiffCalculator](../../../../android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt) の `sameStructure` は Section の id / header / footer と cells の size・id だけを比較し、`headerHeight` を含まない。そのため headerHeight のみを変えた更新は `compute` の早期 return で `emptyList()` となり、diff が1つも生成されない。DSL で `headerHeight` を動的に変えても更新自体が発生しないため、利用者から見た「高さが変わらない」症状は DSL 経路では解消していない。

同じ欠落は iOS の SwiftUI DSL にもある (早期 return は抜けるが、headerHeight に対応する diff をどの段でも生成しないため結果的に空になる)。本 change のスコープ外の別レイヤーの問題として、両 platform をまとめて別 change [fix-dsl-header-height-diff](../../fix-dsl-header-height-diff/exploration.md) に起票した。
