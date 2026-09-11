# Design: install-examples-and-release-notes

## Context

利用者から見えるリリース情報を扱う 2 つの機構を同時に組み替える。インストール例の version 同期 (撤去する) と、Release ノートの分類 (ラベル推測から pull request 本文へ移す)。前提と却下案は [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) と [cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md) に記録済みで、ここでは実装の形を決める。

## Goals / Non-Goals

Goals:

- インストール例が陳腐化しない状態にし、陳腐化を検査する対象そのものを無くす
- Release ノートに載る内容を人が明示的に決められるようにする
- リリース手順を定型的に実行できるようにし、本変更で増える手作業を相殺する

Non-Goals:

- publish 内部の挙動 (待機・再実行・引き継ぎ) — 別 change が扱う
- ブランチ運用と branch protection の変更
- 利用者向け Skill の本文の見直し

## Decisions

### Decision 1: インストール例のプレースホルダは `{version}`

**採用案:** 14 行の version を `{version}` に置き換える。

```
.package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "{version}")
implementation("jp.kamusoft:kssettingsview:{version}")
<PackageReference Include="KsSettingsView.Maui" Version="{version}" />
```

**理由:** 3 経路すべてで構文として合法であり、かつどこでも version として解決できない。埋め忘れは依存解決の失敗として必ず露見する。

**代替案:**
- **`<version>`** — 却下。NuGet の例が XML 属性値であり、山括弧が XML を壊す。
- **`X.Y.Z`** — 却下。それ自体が version の形をしているため、埋め忘れが見過ごされる余地と、エージェントが実在の version と誤認する余地が残る。

### Decision 2: 対象は「直前の Release の tag」から今回の commit までの、`main` を base とする pull request

**採用案:** 次の手順で対象を決める。

1. 起点の tag を選ぶ — 次の条件をすべて満たす Release のうち、対象 commit から履歴上もっとも近いものの tag とする。
   - 今回の version ではない
   - draft ではなく、公開済みである
   - その tag が `main` の first-parent 上で対象 commit の祖先である
   条件を満たすもののうち、first-parent 上で対象 commit からもっとも近いものを選ぶ。該当する Release が 1 つも無ければ (初回のリリース)、履歴の最初から今回の commit までを範囲とする。
2. 起点の tag と今回のリリース対象 commit を比較し、その間に入った commit を得る。
3. 各 commit に紐づく pull request を引き、**base が `main` のもの**だけを残して重複を除く。

**理由:** 単純に「直前の tag」とすると、Release を持たない tag を起点に選んでしまう。実際にこのリポジトリには作業中に作られた `9.9.9` というローカル tag があり (2026-09-11 に削除済み)、同じことは再び起こりうる。今回の version を除くことで、Release 作成だけが失敗した再実行でも起点が今回の tag にずれない。base を `main` に限るのは、commit から pull request への紐づけが base を問わないため — 実際に `0.1.0-beta.2` のノートには `develop` 宛ての pull request が載っており、限定しないと集約 pull request と二重に載る。

**代替案:**
- **`main` へマージされた pull request を日時で絞る** — 却下。範囲の境界が commit ではなく時刻になり、再実行や tag の打ち直しで結果が変わりうる。
- **直前の tag を version 順で選ぶ** — 却下。Release を持たない tag を起点にしうる。また version 順は履歴の前後と一致しない。
- **Release の作成日時で「直前」を決める** — 却下。対象 commit の祖先でない Release (別の枝で作られたもの) を選びうる。祖先であることを条件にすれば、履歴上の近さで一意に決まる。
- **release 起動時に人が本文を貼る** — 却下。ADR-0030 で複数 pull request から集めると決めた以上、人が手で集約する形は現実的でない。

### Decision 2b: リハーサル (dry-run) では収集と検査を行わない

**採用案:** `dry-run` を `main` から起動した場合は、収集・検査・整形・成果物への受け渡しまでを本番と同じ経路で行う (Release は作らない)。`main` 以外のブランチから起動した場合は収集と検査を行わず、その旨を出力に残す。

**理由:** `dry-run` は起動ブランチの制限が外れており ([cross/ADR-0020](../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md))、`main` 以外から起動すると pull request に紐づかない commit が範囲に入り、正当なリハーサルが失敗する。一方で収集を一律に省くと、GitHub API の認証・ページ送り・commit と pull request の関連付け・成果物の受け渡しという中心の経路が、本番リリースまで一度も実行されない。取りこぼしは失敗ではなく「項目の足りないノート」として静かに成功しうるため、`main` からのリハーサルでは実経路を通す。

**代替案:**
- **`main` に open な pull request があるときだけ dry-run を許す** — 却下。ADR-0020 が定めた「リハーサルは起動ブランチの制限を免除する」を狭めることになり、ADR の改訂が要る。
- **どのブランチからの dry-run でも一律に収集を省く** — 却下。実経路が本番まで検証されない。

### Decision 3: `## Changes` の入力文法を閉じ、認識できない入力は失敗させる

**採用案:** 入力の規則を次のとおり固定する。

- `## Changes` の見出しはちょうど 1 つ。0 個または 2 個以上は失敗。
- 見出しの直後から、次の見出しまたは本文末までを対象範囲とする。
- 対象範囲の**空行でない行はすべて** `- <種別>: <説明>` または `- none` でなければならない。当てはまらない行があれば失敗する (`* feature:` のような別記法、地の文、コードフェンスの行を含む)。
- 種別は `breaking` / `feature` / `fix` / `docs` のいずれか。他は失敗。
- 説明は前後の空白を除いて非空。空なら失敗。
- `- none` は単独でのみ許す。他の項目と共存したら失敗。
- 項目が 0 個 (空のセクション) は失敗。利用者向けの変更が無いなら `- none` と書く。

出力は次の形にする。種別ごとにまとめ、項目が無い種別の見出しは出さない。同一種別内は pull request のマージ順、同一 pull request 内は記載順。各項目には出所の pull request 番号を添える。

```markdown
## What's Changed

### 破壊的変更
- Cell の色指定が非 null 型になった (#42)

### 不具合修正
- ダーク表示で行の背景が塗り残される問題を修正した (#45)

**Full Changelog**: <前回 Release の tag>...<今回の tag> の比較
```

**理由:** 「`- ` で始まる行だけを読む」とすると、記法違いや地の文が静かに無視され、ノートから項目が消える。ADR-0030 の「認識できない入力は黙って無視せず失敗させる」を満たすには、認識する形を閉じたうえで、それ以外を失敗にする必要がある。

**代替案:**
- **種別を行の接頭辞ではなく小見出しで表す** — 却下。書く側が 4 つの小見出しを毎回用意することになり、負担が増える。
- **認識できない行を読み飛ばす** — 却下。書き間違いが静かに通り、ノートから項目が消える。
- **不正な種別を「その他」に落とす** — 却下。同上。

### Decision 4: 組み立て処理はスクリプトに切り出し、自己テストで検査する

**採用案:** ノートの組み立て (pull request 本文の集合を受け取り、ノート本文を返す部分) を `scripts/release/` のスクリプトとして実装し、自己テストを持たせる。workflow はそれを呼ぶ。

**理由:** `dry-run` では Release を作らないため、workflow に直接書くと次回の本番リリースまで一度も実行されない。解析と整形は入力を与えれば検査できる部分なので、そこを切り出す。

**代替案:**
- **workflow の step に直接書く** — 却下。検証手段が本番リリースしか無くなる。

### Decision 5: インストール例の lint は対象を明示した表で持つ

**採用案:** `scripts/` に lint を 1 本置き、検査対象 (ファイル・その中の宣言の種別・期待する本数) を表として持つ。撤去する `set-readme-version.py` が持っていた対象の知識をここへ引き継ぐ。検査項目は、プレースホルダであること・SwiftPM の宣言が `exact:` であること・Releases への案内があること・en と ja が同一構成であること。

加えて、`skills/en/*/SKILL.md` と `skills/ja/*/SKILL.md` の実際の構成を列挙し、対象表に載っていない Skill・表にあるのに実在しない Skill・英日で数や名前が食い違う状態を失敗させる。

**理由:** 対象を明示しないと検査が緩くなるが、明示表だけでは新しい Skill が表に追加されなかったときにその存在自体を検出できない。実構成との突合を併せて初めて「Skill が増えたときの検査漏れを防ぐ」が成立する。

**代替案:**
- **正規表現でリポジトリ全体を走査する** — 却下。対象外の文書に書かれた例まで拾い、除外規則が増える。

### Decision 6: リリース用スキルは handbook の単一の入口を読み、順序も handbook が所有する

**採用案:** handbook `cross/release-procedure.md` を、上から順に読めば 1 回のリリースが完了する単一の入口として整える。スキルはその入口を読み、書かれた順に従う。スキル自身が持つのは、`## Changes` の下書きを作る手順と、判断を人へ返す境界の 2 つだけ。**どの節をどの順に読むかはスキルが持たない。**

**理由:** 「参照する節の順序」をスキルが持つと、それは順序の複製そのものになる。handbook で節を足したり並べ替えたりするたびにスキルを直すことになり、「手順を変えても道具を直さずに済む」が成立しない。順序を handbook 側に一本化して初めて、スキルは順序に対して透明でいられる。

**代替案:**
- **スキルが参照する節とその順序を持つ** — 却下。上記のとおり順序の複製であり、handbook を正とする決定と両立しない。
- **スキルが順序を所有し、handbook は各段の詳細だけを持つ** — 却下。「手順の正は handbook」という決定 ([cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md)) を覆すことになる。採るなら ADR の改訂と、乖離検査の設計をやり直す必要がある。

### Decision 7: ノートは publish より前に確定させ、成果物として publish へ渡す

**採用案:** validate の段で対象 pull request の収集・検査・整形をすべて行い、確定したノート本文と、対象 pull request の番号・起点 tag・対象 commit を成果物として保存する。publish は pull request 本文を読み直さず、その成果物をそのまま Release 本文に使う。

validate の段の権限は、次の**完全な形**で書く。

```yaml
permissions:
  contents: read
  pull-requests: read
```

job レベルの `permissions` は列挙しなかった権限を `none` にするため、`pull-requests: read` だけを足すと `contents: read` が失われ、checkout と commit / Release の取得が壊れる。API を呼ぶ step には token を明示的に渡す。

**理由:** 検査と組み立てを別の段で行うと、その間に pull request 本文が編集された場合に 2 つの壊れ方をする — 再取得した本文が不正になって**公開の後に Release 作成だけ失敗する**か、検査したものと違う本文で Release が作られるか。不可逆な公開の後に失敗する経路を残さないため、確定を publish より前に寄せる。

**代替案:**
- **publish の段で取得と組み立てを行う** — 却下。上記の失敗経路が残る。
- **両方の段で取得し、一致を検査する** — 却下。一致しなかったときに publish 後で止まるという問題は同じ。

### Decision 8: スキルの下書き範囲は「今回の pull request が `main` へ持ち込む差分」

**採用案:** リリース用スキルが `## Changes` の下書きを作る範囲は、これから作る pull request が `main` へ新たに持ち込む差分 (`origin/main..origin/develop` に相当) とする。前回のリリース以降の全変更ではない。

**理由:** ノートの組み立ては前回 Release 以降の全 `main` pull request を連結する。下書きを「前回リリース以降の全変更」から作ると、先行して `main` へ入った pull request の変更が、その pull request の記載と今回の記載の両方に現れ、ノートに二重に載る。範囲を今回の差分に限れば、各変更はちょうど 1 つの pull request の記載にだけ現れる。

**代替案:**
- **前回リリース以降の全変更から作る** — 却下。上記の二重掲載が起きる。
- **下書きの生成時に既存の `main` pull request の記載を突き合わせて除く** — 却下。差分で範囲を切れば済むものを、突き合わせで解く必要がない。

## Risks / Trade-offs

- pull request の本文は後から編集できる。ノートは release 実行時点の本文を読むため、実行後の編集はノートに反映されない。
- `## Changes` を必須にしたため、書き忘れた pull request が範囲にあると release が止まる。止まる位置は validate であり、publish の前である必要がある。
- リリース用スキルは handbook の更新に追随する必要がある。追随の漏れを検出する仕組みは本変更では持たない。
- インストール例から具体 version が消えることで、利用者は version を 1 箇所差し替える手間を負う。

## Migration Plan

1. `.github/release.yml` の廃止と組み立て処理の導入は同時に行う。分類設定だけ先に消すと、次のリリースでノートが空になる。
2. **遡っての追記は不要。** `0.1.0-beta.2` の tag 以降に `main` へマージされた pull request は 0 件であり、次回リリースの範囲に入るのはこの変更以降に作る pull request だけである。初回に限る例外の経路は設けない。
3. 既発行の Release 2 件の prerelease 印は、`/releases/latest` を案内先にする前に解除する。解除前に README へ案内を書くと、その間リンクが 404 になる。
4. `set-readme-version.py` の削除は、インストール例の置換と lint の導入が済んでから行う。

## Open Questions

なし。

## ADR 候補

- 起票済み: [cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md) (Decision 1 を含む)、[cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md) (Decision 2・3・6 の前提を含む)
- Decision 4・5・7・8 は ADR にしない。実装の形であり、覆すコストが低く境界を越えない。
