# 実測メモ (修正前ビルドのみ)

本変更は **実装ゲート不成立** (修正前ビルドで欠陥が再現しなかった) のため、コード修正・修正後 (B) の
測定は行っていない。ここにあるのはすべて **A (修正前) 側の測定**である。

## 結論

iOS ネイティブの 2 経路 (SwiftUI DSL / Store 直接) では、書き戻しレースによる文字の欠落・並び替えは
**1 件も再現しなかった** (計 8 セット・有効 165 試行・FAIL 0)。

原因は配信側の値の保持方式が Android と非対称であること:

- `KsSettingsViewController.cellProvider` は render 時点で `self.cellIndex[itemID]` を**ライブに引く**
  (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1084`)
- `applyReplaceCell` は `dataSource.apply` より**前に同期で** `cellIndex[cellID] = new` を更新する (同 :1935)

よって `apply` が遅延しても「更新時点で握った古い Cell」で render される経路が存在しない。Android の
RecyclerView adapter が `submitList` 時点のリストを保持するのとは逆。実測ログ (WDA freq=3000 で
`editingChanged` 10 連続に対し `render` 2 回、いずれも最新値) とも一致する。

## 測定環境

- Simulator `<ios-simulator-udid>` (iPhone 17 / iOS 26.5)、Xcode 26.5 (17F42)
- 実機 `pixie4` (iOS 16.6.1)。WDA (`com.facebook.WebDriverAgentRunner.xctrunner`) を `ios runwda` +
  `ios forward 8101 8100` で駆動
- 注入: mobilecli `io text` (約 33ms/文字) / WebDriverAgent `/wda/keys` の `frequency` 指定 (1〜3ms/文字)

## 結果一覧

| ログ | 対象経路 | 注入 | 有効試行 | FAIL |
|---|---|---|---|---|
| `before-email-mobilecli-iotext.txt` | メール欄 (SwiftUI DSL) | Sim / mobilecli | 22 | 0 |
| `before-email-wda1000.txt` | メール欄 | Sim / WDA freq=1000 | 22 | 0 |
| `before-email-wda3000-10char.txt` | メール欄 | Sim / WDA freq=3000・10 文字 | 24 | 0 |
| `before-email-pixie4-wda1000.txt` | メール欄 | 実機 / WDA freq=1000 | 17 | 0 |
| `before-email-pixie4-wda3000-10char.txt` | メール欄 | 実機 / WDA freq=3000・10 文字 | 20 | 0 |
| `before-store-mobilecli-iotext.txt` | Store デモ EntryCell | Sim / mobilecli | 20 | 0 |
| `before-store-wda1000.txt` | Store デモ EntryCell | Sim / WDA freq=1000 | 20 | 0 |
| `before-store-pixie4-wda1000.txt` | Store デモ EntryCell | 実機 / WDA freq=1000 | 20 | 0 |

## 再現時の注意 (重要)

- **Store 経路の測定対象セルは現存しない**。`samples/ios/KsSettingsViewSample/StoreDemoView.swift` に
  試験用の EntryCell (固定 id・初期値 `store.entry@example.com`・`onTextChanged` で `store.replaceCell`)
  を一時的に追加して測定し、**測定後に戻した** (オーナー判断、2026-08-22)。`before-store-*.txt` を
  再現するには同等のセルを再度置く必要がある
- `repro-burst-loop-ios.sh` の既定 `INJECT_MODE=mobilecli` は約 33ms/文字で、書き戻しの往復 (実測 1ms)
  より遅くバースト試験にならない。真のバーストには `INJECT_MODE=wda` が必要
- メール欄の SKIP は「最後のイベント」ラベルの行折り返しで入力欄が 16〜18px 下へずれたもの。欠陥では
  なく測定側の前提失敗として除外している
- `repro-burst-device-wda.py` は実機 (simctl が使えない iOS 16 系) 用に書いた同一判定ロジックのドライバ

## 未測定で残るもの

MAUI 経由の iOS (`KsSettingsController.ScheduleFlush` の dispatcher post) は proposal の Non-Goals により
未測定。**送る側が古い値を積んだまま配信し得る**かは確認していない — 本変更の結論「iOS に窓なし」は
ネイティブ 2 経路についてのものであり、MAUI 経路には及ばない。
