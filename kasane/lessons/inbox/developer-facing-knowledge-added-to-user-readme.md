---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-28
last-seen: 2026-08-28
evidence:
  - customcell-android-maui-perf (開発者向けの性能検証規約「性能確認は Release ビルドで」を利用者向けの samples/maui/README.md へ追記し、オーナー指摘「README はユーザー向けなので concepts に書くのが良い」で取り消し・concepts へ移動)
---

## ルール文

ドキュメントへ追記する前に読者を確定する。開発ハーネス・検証手順・計測規約など**このリポジトリを開発する人**向けの知識は kasane/concepts/ へ、samples の README や skills/ など**ライブラリ利用者**向けドキュメントには利用者が必要とする内容だけを書く。「手順の近くに書くと親切」は読者違いの追記を正当化しない。

## 経緯

- 2026-08-28 customcell-android-maui-perf: MAUI Android の Debug ビルドが遅い件の手当てとして samples/maui/README.md に Release 検証手順の節を追加したが、オーナーが読者違いを指摘。README への追記を取り消し、kasane/handbook/maui/performance-verification.md として記録し直した。
- 関連 (逆方向の混同): [readme-convention-scope-misapplied-to-sample-artifacts](readme-convention-scope-misapplied-to-sample-artifacts.md) は「README 規約を新設サンプル付属 README に過剰適用」の話で、こちらは「開発者向け知識を利用者向け README に書いた」話。
