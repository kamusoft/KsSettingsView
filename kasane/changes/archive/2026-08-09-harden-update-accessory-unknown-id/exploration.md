# Exploration: harden-update-accessory-unknown-id

- 起票日: 2026-08-04
- 起票経緯: add-maui-native-bridge 実装中 (グループ3/4) に確認された既存 Host 挙動の鋭利なエッジの簡易起票。実装は未着手
- 議論再開: 2026-08-09 (Store 実装の裏取り → 案A 確定)

## 課題

「canonical UUID だが未知の sectionID」で `updateAccessory` を呼ぶと、Store は現行契約どおり Diff を発行する (add-maui-native-bridge の design Decision 2 で Bridge は素通しと確定済み) が、その Diff を適用する Native Host 側の挙動が危険かつ OS 非対称:

- **iOS**: `updateSectionAccessoryAndReload` → `reportMissingID` → DEBUG ビルドでは `assertionFailure` で即クラッシュ
- **Android**: `strictMode = true` (既定) では `IllegalStateException` が Diff 購読コルーチン内で発生。実機ではアプリクラッシュだが、例外ハンドラ次第で **`storeCollectJob` だけが死に、以後どの操作も表示へ届かない「Host の沈黙」**になる経路がある (Robolectric で実測)。`strictMode = false` では `Log.w` のみ
- Bridge (C# 呼び出し側) からこのケースに到達可能であり、phase-1 のデルタスペックは未知 ID の no-op 契約を Cell / Section 操作に限定しているため spec 違反ではないが、公開 interop 表面としては鋭利

### 裏取りで判明した根本原因 (2026-08-09)

両 OS とも Store の内部 state 更新 (`updateSectionAccessory`) は未知 sectionID で既に no-op (iOS `SettingsRootStore.swift:330` / Android `SettingsRootStore.kt:288`)。問題は `updateAccessory` が state 更新の成否に関わらず**無条件に Diff を発行**していること (iOS `:283` / Android `:258`)。`moveCell` は `guard updated else { return }` で発行をガードしており、`updateAccessory` だけがこのパターンから漏れている。

## 検討した選択肢 (却下案と理由を含む)

- **案A (採用)**: Store の `updateAccessory` にも未知 sectionID の no-op 契約を広げる。state 側は既に no-op のため、実体は「section 系 target で state 更新が失敗したら Diff を発行しない」ガードの追加 (`moveCell` と同じ既存パターン)。Root H/F は state を持たないため従来どおり無条件発行
- **案B (却下)**: Host 側の missing ID 処理を「購読を殺さない安全な警告」に変える — 「state と Diff の不一致」という根っこを温存したまま症状だけ抑える形になり、Host の異常検出器を弱め、修正規模も大きい

## 決定事項

- 案A 採用 (2026-08-09 ユーザー確定)。契約は core/ADR-0020 (proposed) として起票
- Host 側の missing ID 検出 (iOS assert / Android strictMode) は「Store が契約を守る限り到達しない内部整合性チェック」として温存する

## ADR 候補

- 作成済み: core/ADR-0020 (proposed) — updateAccessory の未知 sectionID は Store で no-op とし、state 更新が成立しない構造 Diff は発行しない

## 未決の論点

なし (2026-08-09 時点ですべて確定)

- Android の `storeCollectJob` 死亡→沈黙経路への Host 側防御は**本変更のスコープ外**と確定。ADR-0020 で Host 検出器の温存を決定済みのため、将来 Host hardening をやる場合は別途起票

## UI 素材

なし (UI 見た目に触れない変更)

## 変更級: M (2026-08-09 ユーザー確定)

- 現行挙動は phase-1 デルタスペック上 spec 違反ではないため「バグ修正」ではなく挙動契約の変更 → S の枠から外れ M の「公開 API の小変更」に該当 (シグネチャ不変・観察可能挙動の変更)
- デルタスペックが ADR-0018 の両 OS 対称テストの設計図として実益を持つ
- 進め方: ksn-propose で簡略 proposal + デルタスペックを作成
