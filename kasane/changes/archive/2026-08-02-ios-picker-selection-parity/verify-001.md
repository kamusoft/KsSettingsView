# Verify 001: ios-picker-selection-parity

- 検証日: 2026-08-02
- 対象: 未コミット working tree 差分 (ios ドメイン)
- デルタスペック: `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md` (5 Requirement / 15 Scenario)
- deviation.md: なし
- 判定: **VALID**

---

## 対応表

### Requirement: PickerCell 選択面のスタイル継承 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 候補行のタイトルが実効値で描画される | `PickerListViewController.swift:224-225` (`effective.titleColor` / `effective.titleFont`)。`cellTitleFontSize` 上書き規則は `EffectiveStyle.swift:119-135` の既存 resolver を経由 | `PickerSelectionScreenTests.swift:81` | ✅ 一致 |
| CellStyle は Theme より優先される | `PickerListViewController.swift:75` で `EffectiveStyle(theme:cellStyle:)` を合成 (解決順序は resolver 側)。適用は `:224`, `:226`, `:95` | `PickerSelectionScreenTests.swift:91` | ✅ 一致 |
| 背景・区切り線・ハイライトが Theme から解決される | 面背景 `:95` / 区切り線 `:96` / 行背景 `:226` / ハイライト `:229-231` (`selectedBackgroundView` 差し替え) | `PickerSelectionScreenTests.swift:103` | ✅ 一致 |
| 選択印は Cell 固有値が最優先される | `PickerListViewController.swift:77` (`cellAccentColor ?? effective.accentColor`) + `:93` (`tableView.tintColor`)。素材の受け渡しは `PickerCellView.swift:84-87` | `PickerSelectionScreenTests.swift:120` | ✅ 一致 |
| 選択印は CellStyle へフォールバックする | 同上 (`effective.accentColor` = `EffectiveStyle.swift:212-215` の `cellStyle.accentColor` 段) | `PickerSelectionScreenTests.swift:133` | ✅ 一致 |
| 選択印は Theme の既定色へフォールバックする | 同上 (`theme.cellAccentColor` 段) | `PickerSelectionScreenTests.swift:146` | ✅ 一致 |

補足: 3 段解決が「VC 直接生成」ではなく `render` → `makeListViewController` の**提示経路と同一の配線**で成立していることを、配線検証 seam (`PickerCellView.swift:119-121` / `PickerSelectionScreenTests.swift:157`) が別途担保している。

### Requirement: ナビゲーションバーへのスタイル適用 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 単一選択の Cancel へ解決値が反映される | `PickerListViewController.swift:99-105` (単一は left の `.cancel` のみ、right は未設定) + `:134-141` (`item.tintColor = resolvedAccentColor`) | `PickerSelectionScreenTests.swift:181` (Cancel 色 = `_resolvedAccentColor`、`rightBarButtonItem` が nil) | ✅ 一致 |
| 複数選択の Cancel / 確定とタイトルへ解決値が反映される | `:106-117` (Cancel + `.done`) / `:147-183` (`applyNavigationBarTitleAppearance` — 現行 appearance を複製し `effective.titleColor` のみ差し替え) | `PickerSelectionScreenTests.swift:190` | ✅ 一致 |

補足: 「Theme を別途参照しない」制約は、ナビバーが `resolvedAccentColor` (3 段解決の結果値) を共有する構造 (`:139`) で満たされている。フォントサイズはシステム既定のまま (サイズ指定コードなし) で spec どおり。

### Requirement: 選択面のタイトル解決 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| pageTitle が nil なら title を表示する | `PickerCellView.swift:84` (`picker.pageTitle ?? picker.title`) → `PickerListViewController.swift:81` (`self.title = navigationTitle`) | `PickerSelectionScreenTests.swift:211` (nil フォールバック) / `:206` (pageTitle 指定時) | ✅ 一致 |

### Requirement: 候補行のアクセシビリティ状態 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 選択状態が公開される | `PickerListViewController.swift:234` (`accessibilityLabel = displayText`) + `:248-255` (`applyCheckState` が `.selected` trait を挿入/削除) | `PickerSelectionScreenTests.swift:219` | ✅ 一致 |
| トグル後に公開状態が更新される | `:272-274` / `:284-286` — トグル経路も `applyCheckState` を通す (旧実装の直接 `accessoryType` 代入は削除済み) | `PickerSelectionScreenTests.swift:232` (チェック → 解除の双方向) | ✅ 一致 |

### Requirement: 選択中の項目への初期スクロール (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 単一選択は選択中の項目が中央付近に来た状態で開く | `PickerListViewController.swift:193-201` (`initialScrollTargetRow`) + `:205-208` (`scrollToRow(at: .middle)`) + `:122-128` (レイアウト確定後に 1 度だけ発火) | `PickerSelectionScreenTests.swift:248` (可視領域中央との差を 1 行分の許容誤差で判定) | ✅ 一致 |
| 複数選択は選択中の最小 index が中央付近に来た状態で開く | `:199` (`currentMulti.filter { items.indices.contains($0) }.min()`) | `PickerSelectionScreenTests.swift:256` | ✅ 一致 |
| 範囲外 index はスクロール対象から除外されるが選択集合には残る | `:199` の filter は**スクロール先計算のみ**。`currentMulti` (`:37`, `:72`) は正規化せず `handleDone` (`:298-303`) がそのまま callback へ渡す | `PickerSelectionScreenTests.swift:265` (スクロール先 = 1、確定 callback = `{1, 5}`) | ✅ 一致 |
| 未選択・範囲外のみの場合は先頭から表示する | `:196` (単一の範囲チェック) / `:199` (複数は該当なしで nil) → `:206` で早期 return | `PickerSelectionScreenTests.swift:280` / `:289` (単一・複数の両方) | ✅ 一致 |
| items が空でも選択面は提示される | `:196`/`:199` が空 `items.indices` で nil を返しスクロールなし。行数は `:212-214` で 0 | `PickerSelectionScreenTests.swift:301` | ✅ 一致 (注記あり) |

注記 (欠落ではない): 空 items の Scenario の WHEN 「行をタップする」に対し、テストは `render` → `makeListViewController` (提示経路と同一の組み立て、`PickerCellView.swift:74-95`) までを実行してレイアウトさせている。`presenter.present` の呼び出し自体は key window を要する UIKit 境界のため未実行だが、Scenario の THEN (候補 0 件の面が成立し、クラッシュしない) は構築 + レイアウト + 行数 0 の検証で担保されている。

---

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ✅ 16/16 チェック済み。**虚偽なし** — 1.1〜1.5 / 2.1 は上表の実装列、3.1〜3.7 は新規テスト 21 件、3.8 の退行確認は既存 `InputCellsTests.swift:320` / `:341` / `:364` が新シグネチャへ追随して全件成功、4.1/4.2 は `ui/verification/` に 4 点の実画像が存在し brief.md に照合結果が追記されている |
| 逆流検査 (足場アーティファクトの書き換え) | ✅ なし。`git status` で変更されている kasane 配下は `tasks.md` (チェック更新のみ) と `ui/brief.md` (「照合結果 (実装フェーズで追記)」以降の追記のみ、既存節の書き換えなし) の 2 件。`proposal.md` / `specs/` / `ui/mock/` は無変更 |
| 未記録乖離 | ✅ なし (❌ 0 件のため deviation.md 不在は問題にならない) |
| UI 変更: brief.md の承認モック記録 | ✅ `ui/brief.md:37` に `mock/plan-a.html` 採用・approved.png・2026-08-02 オーナー承認の記録あり。プラットフォーム由来の見え方の差 (ナビバーのボタン形状) は「合意事項ではなく観測結果」として本 change 以前からの描画である旨とともに記録されている |
| テスト全件成功 | ✅ `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5'` → **Executed 359 tests, with 0 failures** (`** TEST SUCCEEDED **`)。うち `PickerSelectionScreenTests` 21 件も個別実行で全件成功 |

---

## 判定

**VALID** — 5 Requirement / 15 Scenario すべてが「✅ 一致」。未記録乖離・虚偽チェック・逆流はなく、テストは全件成功。

参考 (verify の判定対象外): `ui/brief.md:44` に「オーナーの最終承認は未取得 (実装ワーカーからの提示待ち)」との記載がある。これはデルタスペックとの一致の問題ではなく、UI 照合結果に対するオーナー確認という運用ゲートであり、アーカイブ前に呼び出し元で消化されるべき事項。
