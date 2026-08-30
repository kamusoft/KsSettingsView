---
id: 0020
title: updateAccessory の未知 sectionID は Store で no-op とし、state 更新が成立しない構造 Diff は発行しない
status: accepted
date: 2026-08-09
---

## Context

「canonical UUID だが未知の sectionID」で `updateAccessory` を呼ぶと、Store の内部 state 更新 (`updateSectionAccessory`) は両 OS とも既に黙って no-op になる一方、`updateAccessory` は state 更新の成否に関わらず無条件に `UpdateAccessory` Diff を発行していた。`moveCell` や Cell / Section 操作は「state 更新が成立しなければ Diff を発行しない」ガードを持っており、`updateAccessory` だけがこのパターンから漏れていた。

その結果「state に存在しない section の Diff」が Host に流れ、Host の missing ID 処理に到達する:

- iOS: `reportMissingID` → DEBUG ビルドでは `assertionFailure` で即クラッシュ
- Android: `strictMode = true` (既定) では Diff 購読コルーチン内で `IllegalStateException`。例外ハンドラ次第で `storeCollectJob` だけが死に、以後どの操作も表示へ届かない「Host の沈黙」になる経路がある (Robolectric で実測)

MAUI Bridge (C# 呼び出し側) は Diff を素通しするためこのケースに到達可能で、公開 interop 表面として鋭利だった。また「state に無い変更が Host にだけ流れる」ため、ADR-0019 (attach 時に Store 現在状態から復元) の下では再 attach で表示が巻き戻る非一貫も潜在していた。

## Decision

`updateAccessory` の section 系 target (`sectionHeader` / `sectionFooter`) で sectionID が未知の場合、Store は state 更新も Diff 発行も行わない no-op とする。`moveCell` / Cell / Section 操作と同じ「state 更新が成立しなかった構造 Diff は発行しない」契約に統一する (Store state と発行 Diff の一致保証)。

Root 系 target (`rootHeader` / `rootFooter`) は SettingsRoot 値型に state を持たないため、従来どおり無条件に Diff を発行する。

Host 側の missing ID 検出 (iOS の `assertionFailure` / Android の strictMode 例外) は変更せず、「Store が契約を守る限り到達しない内部整合性チェック」として温存する。

## Alternatives Considered

- **案B: Host 側の missing ID 処理を「購読を殺さない安全な警告」に変える** — 却下。根っこである「state と Diff の不一致」を温存したまま症状だけ抑える形になる。Host の異常検出器を warn 化で弱め、Android の沈黙経路も緩和止まり。修正規模も両 OS の Host 例外経路の再設計となり案A より大きい

## Consequences

- 正: Store state と発行 Diff が常に一致し、ADR-0019 の attach 復元と表示が矛盾しない
- 正: 未知 sectionID で iOS DEBUG クラッシュ / Android strictMode 例外・沈黙経路に到達しなくなり、MAUI Bridge からの呼び出しが安全になる
- 正: Host の missing ID 検出器を温存でき、Host 改修が不要
- 負: 呼び出し側の sectionID 間違いが黙って握り潰され、開発時に気づきにくくなる (従来は DEBUG クラッシュ / 例外で顕在化していた)
- 負: 公開 API の観察可能挙動が変わるため、契約 (concepts) 更新と両 OS 対称のテスト追加が必要

出典: kasane/changes/harden-update-accessory-unknown-id/exploration.md / 2026-08-09 の探索会話 (案A/案B 比較と Store 実装の裏取り)
