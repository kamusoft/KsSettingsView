---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-20
last-seen: 2026-08-20
evidence:
  - implement-modern-style (review-001 Major: Modern の箱 clip [SectionBoxCellClip] を Cell インスタンスの associated object にキャッシュし、解決を dequeue / reconfigure 時のみに置いたため、SettingsRootDiff で Section の先頭 / 末尾が変わっても隣接する既存 Cell の clip が古いまま残り角丸が破綻。修正は willDisplay + 構造更新後の可視走査での再解決と、実 layer.mask を観測する回帰テスト 2 本)
---

## ルール文

Section 内での位置など構造に依存する表示状態を Cell インスタンス (associated object / ViewHolder フィールド) にキャッシュするときは、構造変更 (`SettingsRootDiff`) 後にそれを再解決する経路を同時に設け、挿入・削除後の**隣接 Cell** (reconfigure 対象にならない Cell) の表示を検証するテストを完了条件に含める。構造 Diff は内容が変わった item しか reconfigure しないため、dequeue / reconfigure 時のみの解決は隣接 Cell の失効を検出できない。

## 経緯

- 2026-08-20 implement-modern-style: 箱 clip のキャッシュが Cell 挿入・削除後の隣接 Cell で失効せず角丸破綻 (review-001 Major、一時プローブで実測)。willDisplay と構造更新後再解決の二重化 + 実 mask 観測の回帰テストで解消 (review-002 で確認)。
