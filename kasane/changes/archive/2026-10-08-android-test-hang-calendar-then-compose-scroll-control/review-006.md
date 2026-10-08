# レビュー結果: android-test-hang-calendar-then-compose-scroll-control (006 回目)

**日付**: 2026-10-08
**判定**: APPROVED

## サマリー

review-005.md の指摘 2 件 (Minor 1・Suggestion 1) への修正だけを見た。`MainLooperDrainingApplication.kt` の変更は KDoc の 2 か所だけで、文面は review-005.md の実測と合っている。`deviation.md` の (b) も KDoc と同じ内容になっている。指摘は無い。

## 照合した規約

コメント規約 (`scripts/comment-policy-lint.py`) のみ。範囲がコメントと記録の文面に限られるため、ほかの handbook 文書は読み直していない。
ロードしたスキル: ksn-review

## 確認した観点

### 1. 変更がコメントだけであること

- 現行の shasum は `c1805a78e57f26060f68119e9527bd971acbfdad` (パッケージの記載と一致)
- review-005 の写し (`scratchpad/review005/android/.../MainLooperDrainingApplication.kt`) には、レビュー用の探針の行が 3 行足されている (`probeClock`・`probeDialogs`・`ReviewProbe.log`)。その 3 行を除いた内容の shasum は `e4289e7acaff277d84760c3ec71011da31962613` で、パッケージに書かれた変更前の値と一致した
- 写しと現行の差分は、探針の 3 行を除くと `:43-44` と `:77-79` の 2 か所だけで、どちらも KDoc の中。コードの行に差は無い

### 2. 文面が review-005.md の実測と合うこと

- 指摘 1 (`MainLooperDrainingApplication.kt:77-79`): 「同じプロセスの後続のテストで届かなくなる。先に Compose を使ったテストがあるときは、そのテストの中でも届かない。配信を再開して終えても同じ」。review-005.md の表の 4 行 (最初に Compose を使うテストは中では届き後続で届かない / 先に Compose を使ったテストがあると中でも届かない / 再開しても届かない) と合う
- 指摘 2 (`:42-44`): 「終えるテストがあり」→「終えるテストがあると、先に View 階層を外した場合」。実在の言い切りではなく、起こりうる場面の書き方になっている。仕組みの説明 (後から実行された処理が外す相手を見つけられずに例外になる) は変わっていない

### 3. `deviation.md` の (b) が KDoc と同じ内容であること

- `deviation.md:8` の (b): 「同じ JVM の後続のテストで再合成が届かない。先に Compose を使ったテストがあるときは、そのテストの中でも届かない」。KDoc と同じ条件の付け方。用語の違い (JVM / プロセス、再合成 / 再 composition) と、「再開して終えても同じ」を省いている点は、内容の食い違いではない
- `deviation.md:16` は (a)〜(d) を参照で引き継ぐ形なので、(b) の修正がそのまま届く
- 追加された `deviation.md:17` (蒸留送り) は、review-005.md の「範囲外の所見」の 3 点目・4 点目と合う

### 4. コメント規約とコンパイル

- `python3 scripts/comment-policy-lint.py`: 禁止 0 件 (検査対象 860 ファイル)
- `./gradlew :kssettingsview:compileDebugUnitTestKotlin`: BUILD SUCCESSFUL

テストの全件実行と新しい実験は、パッケージの指定により行っていない。

## 指摘事項

なし。

## アクションプラン

なし。
