# Exploration: clarify-host-attach-order-contract

- 起票日: 2026-08-05
- 起票経緯: add-maui-native-bridge 実装中 (グループ5・検証ホスト) に判明した契約の隙間の簡易起票。実装は未着手

## 課題

maui-bridge の spec は Host 生成と `setRoot` の順序について2つの順序 (取り付け後に操作 / 操作後に生成+取り付け) を保証するが、**第3の順序「Host 生成 → 構造操作 → view 階層へ取り付け」では更新が取りこぼされる**:

- iOS の `KsSettingsViewController` は `viewDidLoad` 前に受け取った Diff を `.full` 以外すべて破棄する
- このため Host handle を得てから取り付けるまでの間に `replaceSection` / `updateAccessory` 等を呼ぶと、その変更は表示に反映されない (add-maui-native-bridge の検証ホストで実測。ホスト側は「取り付け → `LoadViewIfNeeded()` → 操作」に修正して回避した)
- phase-2 の MAUI Handler は「platform view の生成 → プロパティ適用 → 取り付け」の順で動く可能性があり、この隙間を踏みやすい

## 検討方向

- 案A: 契約として明示する — 「Host は view load 前の構造 Diff を保証しない。取り付け前の操作は setRoot のみ保証」を spec / concepts に明文化
- 案B: Host 側で view load 前の Diff を保留して load 時に再適用する (または load 時に Store 現在状態から full 再構築する) — 実装で隙間を塞ぐ
- Android 側の同順序の挙動も未確認 — 両 OS で実測してから方針を決める
- phase-2 (MAUI Handler) の設計前に決着させるのが望ましい → maui-support ロードマップの phase-2 agenda に載せる選択肢もある

## 級の推奨

案A なら S (文書のみ)、案B なら M (Host の観察可能挙動の変更 + 両 OS 対称のテスト追加)
