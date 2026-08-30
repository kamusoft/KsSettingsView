# Tasks: fix-adapter-not-restored-on-reattach

## 1. 再現の確立 (実装の前提。ここが取れなければ 3 以降には進まない)

- [x] 1.1 detach → reattach でリストが空になることを Robolectric で実測する。`KsSettingsView` に root を反映 → detach → reattach の順で、RecyclerView の `adapter` と表示アイテム数を観測する (→ proposal「未確認事項」)
- [ ] 1.2 再現しなかった場合は、なぜ問題にならないのかを調べて記録し、**実装へ進まずユーザーへ報告する** (RecyclerView 側が adapter を保持し直す、そもそも到達しない等)
- [ ] 1.3 Robolectric で再現しない一方で実環境では起きる疑いが残る場合のみ、`concepts/cross/conventions/runtime-behavior-verification.md` に従い実環境で再現を取る (サンプルアプリの ViewPager2 相当の構成が必要なら、その構成の追加もこのタスクに含む)

## 2. 設計判断

- [x] 2.1 `adapter = null` の元の意図 (リーク防止) を壊さずに再 attach で復帰させる方式を決める。`onAttachedToWindow()` で adapter を戻す案が素直だが、リークを再導入しないことを説明できる形にする (→ proposal「Impact」)
- [x] 2.2 既存 `MemoryLeakTest` の 2 件 (`onDetachedFromWindow で RecyclerView adapter が null になる` / `Store 経由で setRootDirect しても detach 後 adapter が null になる`) と両立することを確認する。両立できない設計を採らざるを得ない場合は、**独断で既存テストを書き換えず、理由を添えてユーザーに判断を仰ぐ**

## 3. 実装

- [x] 3.1 再 attach 時に adapter が復帰するようにする。公開 API は変更しない
- [x] 3.2 復帰後にリストの内容 (Section / Cell の並び・可視状態) が detach 前と一致することを確認する。スクロール位置の保持は本変更のスコープ外 (別途必要なら切り出す)

## 4. テスト

- [x] 4.1 退行テスト: detach → reattach でリスト内容が保たれること (→ 1.1 で確立した再現がそのままテストになる)
- [x] 4.2 `MemoryLeakTest` の既存 2 件が引き続き green であること
- [x] 4.3 追加テストが対象経路を実際に踏んでいることを変異注入で確認する (修正を外すとテストが落ちること。確認後は原状復帰)
- [x] 4.4 全体の回帰確認 (`./gradlew test --rerun-tasks`。実行件数まで確認する)

---

## 補足

- テスト実行時は `ANDROID_HOME=$HOME/Library/Developer/Xamarin/android-sdk-macosx` を環境変数で渡す (`local.properties` は作らない)。`android/` ディレクトリの `gradlew` を使う
- S 級のためデルタスペックはなく、verify は非適用。**独立文脈でのレビューは必須**
