---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-18
last-seen: 2026-09-18
evidence:
  - android-accessory-view-late-insert-animation (証跡の撮影後に Pixel 6a へ「修正後ビルドを再配備して元に戻した」端末で、オーナーが Root の header / footer が表示されない状態を見つけた。ソースは撮影時と同一で、`bin` / `obj` を捨てたフルビルドの入れ直しでは 13 条件すべてで再現しなかった。C# 側と apk 内の Kotlin 側の世代がずれた配備と推定)
---

## ルール文

native (Kotlin / Swift) を変更した change で実機・Emulator・Simulator へアプリを入れ直すときは、証跡の撮影用か後片付けの再配備かを問わず、Sample・facade・binding の `bin` / `obj` を捨ててフルビルドしてから配備し、配備後に本 change の対象画面を 1 度開いて期待どおりに表示されることを確かめてから作業を終える。事後判定: 作業報告に、最後に端末へ入れたビルドがフルビルドであることと、配備後に対象画面を開いて確かめた結果が書かれている。

## 経緯

- 2026-09-18 android-accessory-view-late-insert-animation: 撮影ワーカーは修正前ビルドでの撮り直しのあと、端末を「元の状態に戻す」ために修正後ビルドを増分ビルドで再配備し、画面を開いて確かめずに作業を終えた。その端末をオーナーが触って Root の header / footer の欠落を見つけた。症状は本 change が Android Host で塞いだ穴そのもの (取り付け前に渡した Root 対象の更新が失われる) で、C# 側だけが新しい順序 (取り付け前に渡す) で動き、Kotlin 側が受け口を持たない古い世代だったと考えると説明がつく
- 証跡 (23:56 撮影) は正しいビルドで撮られており、レビューの証跡確認も通っていた。壊れていたのは証跡の後に入れ直した成果物で、テスト・レビュー・証跡のどれも検出できない継ぎ目だった
- 切り分けに調査ワーカー 1 回ぶんを使った。当時の apk は上書きされ、世代の混在の直接の証拠は取れていない
- 同時に `kasane/handbook/cross/local-development-setup.md` の「MAUI Android」節へ同趣旨の注意を追記した (iOS 節には既に在った)。このパターンは規約側で受けているため、再発が続くなら昇格ではなく規約の適用のきっかけの見直しを検討する
