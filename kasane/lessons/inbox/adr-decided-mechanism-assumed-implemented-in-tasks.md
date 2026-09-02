---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-consumer-verification (tasks 3.1 が Android のフィード準備を `publishToMavenLocal -Pversion=` と書き、Scenario「version を与えると全 platform に同じ文字列が流れる」も `-Pversion=` の注入が効く前提だったが、`android/build.gradle.kts` は version をカタログ値で無条件に上書きしており注入の受け口が未実装だった。cross/ADR-0020 (status: proposed) が「CI が `-Pversion=` を注入する」と決めていることを、実装済みの機構として扱っていた。実装フェーズの実測で露呈し、別能力のファイル修正としてオーナー裁定を仰ぐ往復が生じた)
---

## ルール文 (候補)

デルタスペックや tasks が、本体側の機構 (ビルドプロパティの受け口・CLI 引数・環境変数・設定キー) を「使う」前提で書かれるときは、提案の段階でその機構が**コードに実装済みか**を確認する。ADR が決めているだけの機構は、決定と実装の間に空白があり得る (特に `status: proposed` の ADR)。

確認手順: 使う機構の名前 (`-Pversion` 等) で本体のビルド定義・ソースを検索し、受け口が存在するか、存在しないならどの change / フェーズが実装するかを proposal または tasks に明記する。未実装なら本 change の tasks に受け口の実装を含めるか、前提条件として phase の順序に反映する。

事後判定: tasks が本体の機構を引数付きで呼ぶ記述を含む場合、その機構の実装箇所 (パス) が proposal / design / tasks のいずれかで参照されているか、実測タスクが機構の存在自体を検証対象に含めている。

## 関連

[[proposed-decision-treated-as-settled-in-spec]] (proposed の ADR を確定済み扱いにする型) の兄弟。あちらは「値の一致」を契約にしてしまう問題、こちらは「決定された機構が実装済みである」と仮定してしまう問題。[[spec-requirement-targets-nonexistent-external-resource]] (外部状態の実在を確認しない) とも同族で、対象がリポジトリ内のコードである点が異なる。

## 経緯

- 2026-09-02 add-consumer-verification: tasks 1.x のスパイクは参照先の排他性・identity・cache 迂回を対象にしており、version 注入の受け口は対象に含まれていなかった。3.1 の実装で露呈。修正自体は 2 行 (`providers.gradleProperty("version").orNull ?: catalog 値`) で閉じ、オーナー判断「本 change で直す」で付随修正として同梱した。
