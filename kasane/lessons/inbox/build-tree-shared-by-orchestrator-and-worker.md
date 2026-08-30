---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-11
last-seen: 2026-08-11
evidence:
  - colorPrimary 宣言元参照への是正 (S 級・2026-08-11。レビューワーカーにテスト実行を依頼した状態でオーケストレーターも同じ Gradle ビルドツリーへ `clean` と `--stop` を実行し、双方の実行が BUILD FAILED。release variant の ClassNotFoundException 8 件と依存モジュール型の Unresolved reference が出て、変更起因の失敗との切り分けに 2 往復を要した)
---

## ルール文

ワーカーエージェントにテスト・ビルドを含むタスクを委譲している間は、オーケストレーター自身が同じビルドツリーへビルド・テスト・`clean`・デーモン停止を実行しない。実行は一方に寄せ、他方の完了通知を待ってから行う。委譲時にどちらがビルドを回すかを明示し、回さない側はその旨をコンテキストパッケージに書く。

## 経緯

- 2026-08-11 colorPrimary 宣言元参照への是正 (S 級): レビューワーカーへ「テスト名変更を含むのでビルド・テストへの影響を確認してほしい」と依頼した直後、オーケストレーター側も同じ `android/` ビルドルートで `./gradlew clean test` と `./gradlew --stop` を実行した。Gradle デーモンが同時刻帯に 2 つ (14:46:08 / 14:47:35) 起動し、中間出力を奪い合った結果、両者が BUILD FAILED になった。症状は「`testReleaseUnitTest` だけで `ClassNotFoundException` / `NoClassDefFoundError` が 8 件」「Kotlin コンパイルデーモンが ready 直後に terminated し、依存モジュールの型が軒並み Unresolved reference」というもので、いずれもコードの欠陥と区別がつかない形で現れた。切り分けのためデーモンとプロセスの起動時刻を調べ、干渉を特定したうえで単独実行し直して 2164 tests / 0 failures を確定させた。ワーカー側も自分の操作が原因と誤って自己帰責しており、双方が相手の実行を認識できていなかった。
