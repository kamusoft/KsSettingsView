# release-workflow デルタスペック

## MODIFIED Requirements

### Requirement: GitHub Release の発行

release は、publish が全て成功した後に GitHub Release を作る SHALL。

- semver の prerelease 表記を持つ version であっても、GitHub の prerelease としては印を付けない SHALL。
- 作成した Release を、最新のリリースとして**明示的に指定する** SHALL。印を外すだけでは最新の選別が自動判定に委ねられ、案内先が意図した版に着地する保証にならない。
- 0.x の beta を配信している間は、入力された version が alpha / beta / rc のいずれであっても同じ扱いとする。

この印は GitHub 上の表示と最新リリースの選別にのみ関わり、配布経路の解決 (SwiftPM の tag 解決、NuGet と Maven の版選択) には影響しない。版が beta であることは version 文字列の semver prerelease 表記が示す。

正式版を配信した後に、正式版を最新のリリースに保ったまま候補版を出す必要が生じた場合は、この扱いを決め直す。

#### Scenario: beta の版でも最新リリースとして参照できる
- **GIVEN** semver の prerelease 表記を持つ version のリリース
- **WHEN** GitHub Release が作られる
- **THEN** その Release は prerelease の印を持たず、最新のリリースを指す参照から到達できる

#### Scenario: 既に発行済みの Release も最新の選別に乗る
- **GIVEN** prerelease として記録された既発行の Release
- **WHEN** その印を解除する
- **THEN** 最新のリリースを指す参照が、そのうち最も新しいものへ解決される

### Requirement: Release ノートの内容

Release ノートは、前回のリリース以降に取り込まれた変更のうち**利用者向けのもの**だけを種別ごとに並べた SHALL。載せる内容は、変更を取り込んだ pull request の本文に人が明示的に書いたものとする。GitHub がラベル等から推測した分類は使わない。

**対象の決め方**

- 起点は、次の条件をすべて満たす Release のうち、対象 commit から履歴上もっとも近いものの tag とする SHALL。
  - 今回の version ではない
  - draft ではなく公開済みである
  - その tag が `main` の first-parent 上で対象 commit の祖先である
- 条件を満たすもののうち、first-parent 上で対象 commit からもっとも近いものを選ぶ SHALL。
- 条件を満たす Release が 1 つも無い場合 (初回のリリース)、履歴の最初から今回の commit までを範囲とする SHALL。
- 対象は、起点と今回のリリース対象 commit の間に入った commit に紐づく pull request のうち、**`main` を base とするもの**に限る SHALL。
- 公開を伴わないリハーサルは、`main` から起動した場合に限り収集・検査・整形・受け渡しまでを本番と同じ経路で行う SHALL (Release は作らない)。`main` 以外から起動した場合は収集と検査を行わない SHALL。

**記載の規則**

- 各 pull request は、利用者向けの変更を記した所定のセクションをちょうど 1 つ持つ SHALL。
- セクション内の空行でない行は、すべて種別と説明を持つ項目、または「利用者向けの変更なし」を示す単独の記載でなければならない SHALL。当てはまらない行があれば失敗する。
- 種別は破壊的変更・機能追加・不具合修正・ドキュメントの 4 つ。いずれでもない種別があれば失敗する SHALL。
- 説明は前後の空白を除いて非空である SHALL。
- 「変更なし」の記載は単独でのみ許す SHALL。項目が 1 つも無い空のセクションは失敗とする。

**確定と公開**

- 対象の収集・検査・整形は publish より前の段で一度だけ行い、確定した結果を publish へ渡す SHALL。publish は pull request 本文を読み直さない。
- 検査に失敗した場合、release は publish に入る前に止まる SHALL。原因となった pull request と項目が分かる。
- 起点となる Release がある場合、ノートには前回のリリースとの比較を参照できる位置を含める SHALL。起点が無い初回のリリースでは、比較の位置を含めず、最初のリリースであることが分かる記述にする SHALL。
- 対象の pull request が 1 件も無い場合、または全ての pull request が「変更なし」を記している場合、ノートは利用者向けの変更が無いことを示す SHALL。項目の無い種別の見出しは現れない。
- 項目が 1 つも無い種別の見出しは出さない SHALL。
- 重複の除去は pull request 単位で行う SHALL。同じ pull request が複数の commit に紐づいても 1 度だけ数える。異なる pull request に同じ文面の項目があった場合は、出所が異なるため両方を残す。

#### Scenario: 利用者向けの変更が種別ごとに並ぶ
- **GIVEN** 破壊的変更・機能追加・不具合修正の項目を持つ pull request が対象に含まれるリリース
- **WHEN** Release ノートが作られる
- **THEN** 各項目が種別ごとにまとまり、破壊的変更・機能追加・不具合修正・ドキュメントの順に並び、項目の無い種別の見出しは現れない

#### Scenario: 利用者向けでない変更は載らない
- **GIVEN** 開発ハーネスの作業だけを含み、利用者向けの変更が無いことを明示した pull request
- **WHEN** Release ノートが作られる
- **THEN** その pull request からは項目が 1 つも載らない

#### Scenario: 複数の pull request 分が連結される
- **GIVEN** 前回のリリース以降に複数の pull request が取り込まれたリリース
- **WHEN** Release ノートが作られる
- **THEN** すべての pull request の項目が種別ごとにまとめられ、同じ pull request が 2 度数えられることなく並び、各項目の出所が分かる

#### Scenario: Release を持たない tag は起点にならない
- **GIVEN** リリースに対応しない tag が、直前の Release の tag より後に存在する状態
- **WHEN** 対象を決める
- **THEN** 起点は直前の Release に対応する tag になり、対象から変更が漏れない

#### Scenario: 今回の tag がある再実行でも対象が変わらない
- **GIVEN** 今回の version の tag が既に作られた後の再実行
- **WHEN** 対象を決める
- **THEN** 起点は今回の version ではなく直前の Release の tag になり、対象は初回の実行と同じになる

#### Scenario: draft の Release は起点にならない
- **GIVEN** draft の Release が、公開済みの直前の Release より後に存在する状態
- **WHEN** 対象を決める
- **THEN** 起点は公開済みの Release の tag になる

#### Scenario: 対象 commit の祖先でない Release は起点にならない
- **GIVEN** 対象 commit の祖先でない tag を持つ Release が存在する状態
- **WHEN** 対象を決める
- **THEN** その Release は起点にならず、祖先である Release のうち最も近いものが選ばれる

#### Scenario: 初回のリリースでは履歴の最初から集める
- **GIVEN** 公開済みの Release が 1 つも無い状態
- **WHEN** 対象を決める
- **THEN** 履歴の最初から今回の commit までが範囲になり、記載の無い pull request があれば止まる

#### Scenario: 開発ブランチ宛ての pull request は対象にならない
- **GIVEN** 対象範囲の commit に、開発ブランチを base とする pull request が紐づいている状態
- **WHEN** 対象を決める
- **THEN** その pull request は対象から除かれ、集約した pull request の記載と二重に載らない

#### Scenario: 記載の無い pull request があれば publish の前に止まる
- **GIVEN** 所定のセクションを持たない pull request が対象に含まれる状態
- **WHEN** release を起動する
- **THEN** publish に入る前に失敗し、どの pull request が原因かが分かる

#### Scenario: 認識できない記載があれば止まる
- **GIVEN** 種別を持たない行、別の記法で書かれた行、または空の説明を含む pull request
- **WHEN** release を起動する
- **THEN** publish に入る前に失敗し、どの行が原因かが分かる。その行が黙って無視されることはない

#### Scenario: 検査の後に本文が編集されても公開内容が変わらない
- **GIVEN** 検査を通った後、publish に入る前に pull request 本文が編集された状態
- **WHEN** release が Release を作る
- **THEN** 検査を通した時点の内容がノートになり、publish の後に Release の作成だけが失敗することもない

#### Scenario: 開発ブランチからのリハーサルでは対象を集めない
- **GIVEN** `main` の pull request に紐づかない commit からのリハーサル実行
- **WHEN** release を起動する
- **THEN** 対象の収集と検査は行われず、記載が無いことを理由に失敗しない

#### Scenario: main からのリハーサルでは実際の経路を通る
- **GIVEN** `main` から起動したリハーサル実行
- **WHEN** release を起動する
- **THEN** 対象の収集・検査・整形と成果物への受け渡しが本番と同じ経路で行われ、Release だけが作られない

#### Scenario: 初回のリリースでは比較の位置を含めない
- **GIVEN** 起点となる Release が存在しないリリース
- **WHEN** Release ノートが作られる
- **THEN** 比較の位置は含まれず、最初のリリースであることが分かる

#### Scenario: 対象が無いときも本文が決まる
- **GIVEN** 対象の pull request が 1 件も無い、または全てが「変更なし」を記している状態
- **WHEN** Release ノートが作られる
- **THEN** 利用者向けの変更が無いことが示され、空の見出しは現れない

#### Scenario: ノートの組み立てが単独で検査できる
- **GIVEN** pull request 本文の集合を模した入力
- **WHEN** ノートの組み立てを単独で実行する
- **THEN** 期待するノート本文が得られ、リリースを実行せずに検査できる

## ADDED Requirements

### Requirement: リリース手順の定型実行

リリース手順は、道具を通して定型的に実行できる SHALL。

- 手順の記述の正は handbook に置く SHALL。handbook は、上から順に読めば 1 回のリリースが完了する単一の入口を持つ。道具は実行時にその入口を読み、書かれた順に従う。**段の順序・節の並び・コマンドはいずれも handbook が所有し、道具の側に持たない。**
- 道具が自分で持つ内容は、記載の下書きを作る手順と、判断を人へ返す境界に限る SHALL。どの節をどの順に読むかは持たない (それ自体が順序の複製になるため)。
- 道具は、これから作る pull request が `main` へ新たに持ち込む差分から、その pull request の本文へ書く記載の下書きを作れる SHALL。前回のリリース以降の全変更を範囲にしない。既に `main` へ入った変更は、その変更を持ち込んだ pull request の記載が既に持っているため、二重に載ってはならない。
- 道具は、リリースの各段を実行できる SHALL。段は、事前確認 (開発ブランチの検証 CI の状態と、利用者向けドキュメントの追従依頼の要否)・リリース pull request の作成・release の起動・実行の見守り・失敗したときの handbook の該当箇所の案内・公開後の確認項目の実行。
- 道具は判断を担わない SHALL。version 番号の決定、記載の最終的な文面、失敗したときに再実行するかどうかは人が決める。

#### Scenario: 記載の下書きが得られる
- **GIVEN** 前回のリリース以降に複数の変更が入った状態
- **WHEN** 道具に下書きの生成を依頼する
- **THEN** 種別つきの項目の下書きが得られ、人がそれを直して確定できる

#### Scenario: 先行する pull request の変更が下書きに混ざらない
- **GIVEN** 前回のリリース以降に既に `main` へ入った pull request があり、その後に新しい pull request を作ろうとしている状態
- **WHEN** 道具に下書きの生成を依頼する
- **THEN** 下書きに含まれるのは今回持ち込む差分だけで、既に `main` へ入った変更は現れない

#### Scenario: 事前確認が実行される
- **GIVEN** 開発ブランチの検証 CI が失敗している状態
- **WHEN** 道具にリリースの開始を依頼する
- **THEN** その状態が示され、先へ進む前に人の判断を求める

#### Scenario: 各段が順に実行できる
- **GIVEN** 事前確認を通ったリリース
- **WHEN** 道具を通して進める
- **THEN** リリース pull request の作成・release の起動・実行の見守り・公開後の確認が順に行え、各段の到達状態が分かる

#### Scenario: 失敗したときに手順書の該当箇所が示される
- **GIVEN** release の実行が途中で失敗した状態
- **WHEN** 道具が失敗を検出する
- **THEN** handbook の失敗時の記述のうち、その位置に対応する箇所が示され、再実行するかどうかは人が決める

#### Scenario: 手順を変えたときに道具を直さずに済む
- **GIVEN** handbook の手順でコマンドが 1 つ変わった、または節が 1 つ増えた状態
- **WHEN** 道具を通してリリースを実行する
- **THEN** 変更後のコマンドが使われ、道具の側に修正は要らない
