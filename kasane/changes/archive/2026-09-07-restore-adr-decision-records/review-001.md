# レビュー結果: restore-adr-decision-records (001 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

ADR 8 本の本文からの日付マーカー除去と非決定記述の除去は、Context / Decision / Alternatives Considered の意味を一切変えずに達成できている (差分を 1 本ずつ照合。マーカーを溶かした箇所はいずれも前後の文が元のまま残り、決定の射程・断定の強さ・却下理由のどれも動いていない)。新概念 `release-pipeline.md` も `.github/workflows/release.yml` と `scripts/release/` の実物と一致しており、外部 API の不在という再導出コストの高い知識を捕まえている点で価値 lint を通る。追加した相対リンクは 11 ファイル分すべて解決し、構造 lint・local-path lint・identity lint に本変更起因の指摘はない。

一方で、合意済みスコープが「8 本共通」と明記した `現行照合` footer が 2 本にしか入っていない (Major)。また maui/0015 の `整理` footer が「リース共有は未解決」と書いているが、これは maui/ADR-0026 (accepted、2026-08-25) で決着済みの事実誤りである (Major)。ほかに移送先の裏取りが部分的に届いていない箇所と、本変更が潰そうとした二重管理を新概念側で再生産している箇所がある。

## 照合した規約

- `kasane/handbook/index.md` / `cross/index.md` の「適用のきっかけ」で判定 — コード・テスト・`samples/`・`skills/`・ビルドファイルのいずれにも触れないため、`always` を含めて該当する rule は無し (`comment-policy.md` はコメント構文を持つソースコードが対象で、本変更は Markdown のみ)。`cross/release-procedure.md` (guide) はリリース実施時の適用だが、cross/0020 の footer が「再実行の挙動と所要時間は handbook が既に持つ」と主張しているため、裏取りのため本文まで読んだ
- ksn-core `references/decisions.md` (ADR の運用規律・footer 規約・Consequences の導出可能性テスト・改訂の型)
- ksn-core `references/concepts.md` (階層規約・価値 lint・可読性規約・初見可読性レビュー)
- ksn-core `references/paths.md` / `references/doc-structure.md`
- `kasane/concepts/rules.md` (カテゴリ定義と配置判断)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更ゼロの本変更に非該当。「指摘しないこと」は昇格済みルール無し
- `kasane/lessons/process.md` L-009 (合意済みスコープ外のファイルへ手を入れたら deviation.md に 1 行)

## 指摘事項

### [🟠 Major] maui/0015 の `整理` footer が事実と異なる — 「リース共有」は解決済み

**該当箇所**: `kasane/decisions/maui/0015-iconsource-materialized-via-image-source-service.md:31`

**問題点**: footer は「リース共有と未再訪問時の後片付けは未解決」と書くが、除去した残課題 (2) 「複数リースが同一 platform 画像インスタンスを包んだ場合の共有破棄」は `kasane/decisions/maui/0026-ios-icon-cleanup-via-ownership-classification.md` (status: accepted、date: 2026-08-25) が決着させている。同 ADR の Decision は「キャッシュ所有と分類した画像には後片付け口を付けない」と定め、`kasane/concepts/log.md:307` の蒸留記録も「iOS の icon 後片付けは解決時の所有権分類で守る」で閉じている。除去前の本文には「(2) は investigate-maui-icon-lease-sharing で追跡する」というポインタがあり、そちらを辿れば 2026-08-25 に archive 済み (`kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/`) であることが分かる。本変更は変更ログ化した記述を整理する作業なので、その過程で新しく誤った現況を footer に固定してしまうのは元の問題より悪い。なお (3) 「再訪問しない場合にファイナライザが無い」が未解決である点は正しい。

**推奨修正**: footer から「リース共有」を未解決の側から外し、決着先を指す。ksn-core `references/decisions.md` の footer 規約は、この形 (他の ADR がこの決定の適用範囲や実態を決めた) にちょうど `関連:` 行を用意しているので、`関連: maui/ADR-0026 (共有 UIImage は破棄せず所有権分類で守る — 本 ADR の残課題「リース共有」の決着)` を 1 行足し、`整理` 側は「未再訪問時の後片付けは未解決」だけに絞る。

### [🟠 Major] `現行照合` footer が 8 本中 2 本にしか入っていない

**該当箇所**: `kasane/decisions/cross/0020-...md:48` / `cross/0021-...md:43` / `cross/0023-...md:52` / `cross/0028-...md:59` / `android/0019-...md:38` / `maui/0015-...md:31` (いずれも `整理` 行のみで `現行照合` 行が無い)

**問題点**: 合意済みスコープは 2 箇所で 8 本全部を対象と書いている — 決定事項の「**8 本共通の是正の型**: 本文から現況・実測値・消化状況を落とし、**在り処を footer の `現行照合` 1 行で示す**」と、実行節の「全 8 本に共通して footer へ `現行照合: <日付> 確認。<在り処と要点>。判定: 維持` を 1 行足す」。実際に入っているのは cross/0018 と cross/0019 の 2 本だけで、残り 6 本は `整理` 行が移送先を述べるにとどまる。この 2 つの footer は役割が違う: `整理` は今回の作業の記録、`現行照合` は「この ADR の主張が現行の実装で今も成り立つか」の判定であり、ksn-core `references/decisions.md` では ADR 版の timestamp に相当し ksn-drift のディープ検証が更新していく接地点と定義されている。本文を触った 8 本のうち 6 本にそれが付かないと、次の棚卸しは「決定を触った直後なのに照合日が無い」状態から始まることになる。とくに cross/0020 は、直近の `fix-release-central-validation-wait` で publish 段に検証待ちが 1 ステップ挟まったばかりで、Decision の並び (upload → NuGet push → release) が今も成立するかを明示する価値が高い。

**推奨修正**: 6 本それぞれに `現行照合: 2026-09-07 確認。<照合したファイルと要点>。判定: 維持 | 乖離あり` を足す。照合の材料は本変更の作業中に既に見ているはずのもので足りる (cross/0020 → `.github/workflows/release.yml` の publish job / cross/0021 → `scripts/local-path-lint.py`・`scripts/identity-lint.py`・`.githooks/` / cross/0023 → ルート README 2 枚と `skills/README*.md` / cross/0028 → `.github/workflows/ci.yml` / android/0019 → `kasane/concepts/android/architecture/build-toolchain.md` / maui/0015 → `maui/KsSettingsView.Maui.Tests/IconSourceTests.cs`)。8 本すべてに入れないという判断を採るなら、それは合意済みスコープの変更なので exploration.md の決定事項側を直したうえで理由を残す。

### [🟡 Minor] maui/0015 は合意範囲を超えて本文を削っている (残課題 bullet ごと除去)

**該当箇所**: `kasane/decisions/maui/0015-iconsource-materialized-via-image-source-service.md` の Consequences (旧 29 行目「負: 既知の残課題 — (1)(2)(3)」)

**問題点**: exploration.md が maui/0015 について挙げているのは「『上記 (1) は解消済み』(積み残しの消化状況)」の 1 件、実行節でも「maui/0015 の『解消済み』— 落とす」と書かれており、対象は 2026-08-22 の追記行である。実装は追記行に加えて、元から accepted 本文にあった `- 負: 既知の残課題 — …` の bullet も削除した。この bullet は「実装時点の観測」でもあるので削る判断自体は理解できるが、合意済みスコープの列挙 (対象 8 本 / 計 22 行) に無い削除であり、結果として残課題 (3) の内容 (リースにファイナライザが無く、ページを恒久的に離れると後片付けが走らない) が本文からも footer からも具体形では読めなくなった。footer は「未再訪問時の後片付けは未解決」とだけ書くが、何が起きるかは残っていない。

**推奨修正**: 削除の是非をスコープ側で明示したうえで、(3) の内容は落とさずに残す (Consequences の `負:` として最小形で書き直すか、`現行照合` footer に要点を含める)。あわせて deviation.md に「決定事項と違う形に落ち着いた実装」として 1 行残す (下記の指摘参照)。

### [🟡 Minor] android/0019 の footer の移送先の主張が実際より広い

**該当箇所**: `kasane/decisions/android/0019-datepickercell-calendar-compose-datepicker.md:38`

**問題点**: footer は「experimental API の適用範囲と Compose 版整合の方向は、**いずれも** build-toolchain.md の『Compose 版の整合』節が現況として持つ」と書く。版整合の方向は確かに持っている (`kasane/concepts/android/architecture/build-toolchain.md` の同節に「整合の方向は『Gradle の BOM を NuGet 実行時版へ上げる』」がある)。しかし「適用範囲」については、同節にあるのは「Compose Material3 `DatePicker` は experimental API のため、BOM 更新時はカレンダーダイアログのシグネチャ・描画の追随確認を行う」の 1 行だけで、除去した記述の要点である「**experimental への依存はこの 1 箇所に限定される** — Compose 本体 (runtime / ui / foundation) は stable、TimePickerCell は自作ホイールのため非依存」は concepts のどこにも無い (`kasane/concepts/core/cells/date-picker-selection-surface.md:102` も DatePicker が experimental であることを繰り返すだけ)。範囲が 1 箇所に閉じているという事実は不安定 API のリスク評価に効く情報で、再導出には Android の Compose 依存を全部たどる必要がある。

**推奨修正**: 「適用範囲」を build-toolchain.md の当該節へ 1 文追記して footer の主張を成立させるか、footer の主張を実際に持っている範囲 (版整合の方向のみ) に狭め、適用範囲は意図的に落としたと書き分ける。

### [🟡 Minor] release-pipeline.md の「保証すること」が handbook と同内容 — 潰したはずの二重管理を作り直している

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md` の「## 保証すること」1〜2 項目

**問題点**: 冪等性の列挙 (「配信リポジトリの commit は差分ゼロなら skip、Maven upload は前回 deployment の状態で分岐、NuGet push は `--skip-duplicate`、tag は同じ内容なら skip・別内容なら失敗、Release は既存なら触らない」) と deployment ID の引き継ぎ・drop 可能状態は、`kasane/handbook/cross/release-procedure.md` の「失敗したとき」の表および直後の段落が既に同じ粒度で持っている (表の 7 行が上の 5 項目と 1 対 1 に対応する)。exploration.md 論点 2 は、ADR-0020 の実装結果 12 要素のうち「**8 件は既に concepts / handbook にあり削除で足りる**」「移送先が要るのは 3 件」と判定しており、その 3 件は (a) Portal の 2 段階の機構と制約、(b) 公開確認の `repo1.maven.org` HEAD 代替、(c) publish job の鍵つき再ビルドである。冪等条件と ID 引き継ぎはその 3 件に入っていない。本変更の動機そのものが「二重管理の解消」なので、handbook にある記述を新概念へ書き写すのは目的に反する。

**推奨修正**: 「保証すること」からは handbook と重複する列挙を外し、handbook が持たない性質 (「検証が通らない bundle は不可逆な NuGet push より前に止まる」) と、再実行の詳細は `[リリース手順](../../../handbook/cross/release-procedure.md)` が持つ、というポインタに置き換える。残す判断を採るなら、どちらが正かを両文書に明記して片方をポインタ化する。

### [🟡 Minor] deviation.md が無い (lessons L-009)

**該当箇所**: `kasane/changes/restore-adr-decision-records/` (exploration.md のみ)

**問題点**: 昇格済みルール L-009 (`kasane/lessons/process.md:16`、昇格 2026-09-07) は「合意済みスコープが名指ししていないファイルへ手を入れた瞬間に deviation.md へ 1 行書く。tasks.md を持たない S 級では exploration.md の決定事項が挙げた範囲が名指しの基準」「書く対象は付随修正と、決定事項と違う形に落ち着いた実装の両方」と定める。本変更には少なくとも次が該当するのに deviation.md が存在しない: (1) android/0019 の出典パスを archive 後の実配置へ直した付随修正、(2) maui/0015 で追記行以外の本文 bullet も削除したこと、(3) `現行照合` を 8 本ではなく 2 本にしたこと。事後判定の基準 (「`git show --stat` に現れるファイルのうち合意済みスコープが名指ししていないものが、すべて deviation.md の行に現れている」) も満たしていない。

**推奨修正**: `kasane/changes/restore-adr-decision-records/deviation.md` を作り、上記 3 件 (と、指摘を受けて形が変わった箇所) を 1 行ずつ記録する。

### [🔵 Suggestion] cross/0018 は `関連:` 行を併用したほうが機械的に辿れる

**該当箇所**: `kasane/decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md:78`

**問題点**: 論点 3 で採った (ii) `現行照合` は妥当だが、ksn-core `references/decisions.md` は「他の ADR がこの決定の適用範囲や実態を決めたとき」に `関連: <domain>/ADR-NNNN (<何を決めたかの一言>)` を残す型を別に定義しており、Consequences の導出可能性テストの表でも「『Native Android の座標は単一 artifact になった』→ concepts (状態) と footer の `関連:` 行」と、本件そのものを例示している。現状は android/ADR-0016 と maui/ADR-0025 が `現行照合` の散文の中に埋まっていて、ADR 間の関係として拾える形になっていない。

**推奨修正**: `現行照合` はそのままに、`関連: android/ADR-0016 (Android を単一 artifact `jp.kamusoft:kssettingsview` へ統合)` / `関連: maui/ADR-0025 (MAUI を NuGet 3 パッケージへ分割)` の 2 行を足す。

### [🔵 Suggestion] `整理:` 行の位置が ADR ごとにばらつく

**該当箇所**: `出典` の後に置く (cross/0018・0019・0020、android/0019、maui/0015) と、前に置く (cross/0021:43、cross/0023:52、cross/0028:59) が混在

**問題点**: `整理:` は ksn-core の footer 規約 (出典 / 現行照合 / 関連) に無いプロジェクト独自のキーで、その導入自体は exploration.md の決定事項で合意済み。ただし置き場所が揃っていないと、後から機械で拾うときも人が読むときも footer の並びが安定しない。

**推奨修正**: `出典` → `整理` → `現行照合` → `関連` のように順序を決めて 8 本で揃える。

### [🔵 Suggestion] Portal の 2 段階の説明が、NuGet push との前後関係を本文で示していない

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md` の「### Maven Central (Portal の 2 段階)」の番号付きリスト 3

**問題点**: 「3. Portal Publisher API で検証の決着を待ち、`VALIDATED` を確認してから release する」と 1 項目にまとまっているため、待ちと release が連続する操作に読める。実際の `.github/workflows/release.yml` は `Wait for Central Portal validation` (NuGet login/push の前) と `Release Maven Central deployment` (push の後) の別ステップで、待ちを不可逆操作の前に置くことがこの構成の要点である (直近の `fix-release-central-validation-wait` が動かした点でもある)。「保証すること」の「検証が通らない bundle は、不可逆な NuGet push より前に止まる」で辛うじて読み取れるが、機構を説明する節の側が薄い。

**推奨修正**: 3 を「検証の決着を待つ (ここが不可逆な NuGet push の前)」と「NuGet push の後に release する」の 2 項目へ割る。

### [🔵 Suggestion] cross/0021 の Consequences に lint スクリプトのファイル名が残る

**該当箇所**: `kasane/decisions/cross/0021-public-repository-fresh-start.md:40`

**問題点**: 「置いた」→「要する」への書き換えで観測が要求の形になったのは正しい是正だが、`scripts/local-path-lint.py` / `scripts/identity-lint.py` という具体のファイル名は Decision と Context からは導けず、実装後にしか分からない値のまま Consequences に残っている (ksn-core の導出可能性テスト)。実装への接地アンカーとしての価値はあるので即座の違反とまでは言わないが、スクリプトの改名・統合で腐る側の記述である。

**推奨修正**: 本文は「担保に lint と書き込み hook を要する」まででとどめ、具体のスクリプト名は `現行照合` footer 側 (上の Major 2 で足す行) に置く。

### [🔵 Suggestion] 初見可読性レビューの実施証跡が change に残っていない

**該当箇所**: `kasane/changes/restore-adr-decision-records/`

**問題点**: ksn-core `references/concepts.md` は新規概念ファイルの作成を確定する前の初見可読性レビュー (ksn-reviewer に概念本文だけを渡す) を経路共通の品質ゲートと定めており、exploration.md の変更級判定も「品質ゲートは新規概念の初見可読性レビュー + 構造 lint + S 級必須の独立レビューで揃う」と、これを S 級で通す根拠のひとつに数えている。change ディレクトリには exploration.md しかなく、実施されたかどうかを成果物からは確認できない。読んだ限り `release-pipeline.md` の可読性自体は良好 (h1 + 冒頭の「読むと何が分かるか」宣言、保証すること / してはいけないこと、宙に浮いた参照や新造語なし) なので、実質の欠落というより記録の欠落として挙げる。

**推奨修正**: 実施済みなら結果を change 配下に 1 ファイルとして残すか、蒸留時の log エントリに 1 句で触れる。未実施なら確定前に通す。

### [🔵 Suggestion] `concepts/rules.md` の cross/architecture の対象説明が新概念を覆っていない

**該当箇所**: `kasane/concepts/rules.md` の「### cross/」の表 (architecture/ = 「リポジトリ・ビルド構成の責務境界」)

**問題点**: `release-pipeline.md` は責務境界ではなくリリース機構の構成を説明する文書で、カテゴリ定義の文面には収まらない。配置自体は exploration.md 論点 2 で合意済み (層の判定として concepts が正しいことも同意する) だが、rules.md が配置ルールの正である以上、実態と説明がずれた状態が残る。

**推奨修正**: cross/architecture/ の対象を「リポジトリ・ビルド構成の責務境界とリリース機構」等へ広げる (rules.md の改訂はユーザー合意が要るので、本変更の外で扱ってもよい)。

## 確認した観点 (指摘なし)

- **決定内容の不変性**: 8 本すべてで Context / Decision / Alternatives Considered の意味が保たれている。cross/0020 は Decision 2 箇所・Alternatives 5 箇所からマーカーを外しただけで前後の文はバイト単位で同一、cross/0018 は配信リポジトリ名の根拠から `(2026-08-29 追記)` を外しただけ、cross/0023 は `(2026-08-29 追記)` と `**(2026-08-30 決着)**` を「— 」と「そのうえで」で繋いで 1 文として読める形にしており、08-29 の決定 (README は対象外) と 08-30 の決定 (ディレクトリごと非公開) が両立する関係も保たれている
- **cross/0019 の「判定: 乖離あり」**: 妥当。Consequences の「正: 配信 CI は tag 1 本をトリガーにした 1 ワークフローで済む」は現在の本文 35 行目に実在し、`.github/workflows/release.yml:20-21` は `workflow_dispatch` のみ (tag トリガー無し) なので成立していない。決定 (lockstep 単一 version) 自体は生きているので amends を使わない判断も正しい
- **cross/0018 の「判定: 維持」**: 妥当。ADR-0018 は Decision 末尾で artifact 粒度とパッケージ分割を対象外と宣言しており、削除した 2 件はその対象外領域の結果。現況の在り処として footer が指す `repository-boundaries.md` の build root の表は、Android の単一 artifact `jp.kamusoft:kssettingsview` と MAUI の NuGet 3 パッケージを実際に持つ
- **cross/0020 の移送先の裏取り**: 「再実行の挙動と所要時間は handbook が既に持つ」は正しい (`release-procedure.md` の「失敗したとき」の表 7 行と、「起動」の「全体で 40 分前後 (初回リリースの実測は 39 分)」)。`--skip-duplicate` の冪等性も同表の「公開済みのパッケージは skip される」で足りている
- **cross/0028 の footer**: 「起動条件そのものは同節の『開発ハーネスの記録だけの push では CI が起動しない』が述べており」は正しく、除外の規則そのものは Decision 末尾の bullet がより詳しく持っている
- **release-pipeline.md の正確さ**: 段の構成は `.github/workflows/release.yml` の job 依存 (validate → ios/android/maui ∥ package-* → consumer-* → publish → wait-for-registries → smoke-*) と一致。version 注入表は `android/build.gradle.kts:13` の `providers.gradleProperty("version").orNull ?: libs.versions.kssettingsview.get()`・`android/gradle/libs.versions.toml:32` の `0.1.0-SNAPSHOT`・`maui/Directory.Build.props:27` の `0.0.0-dev` と一致。deployment ID の抽出と抽出失敗時の即時失敗は publish job の該当ステップと一致、`.asc` 検査は `scripts/release/check-signatures.sh` の説明と一致、公開確認の `repo1.maven.org` HEAD は `scripts/release/central-portal.sh:40,57` と一致、Trusted Publishing は `NuGet/login` action と `id-token: write` の使用と一致
- **価値 lint**: `release-pipeline.md` の中核 (plugin に release タスクが無く Portal に一覧 API も公開確認 API も無いという**不在**の知識、それが ID のログ抽出と HEAD 代替の理由であること) は再導出コストが高く参照頻度も高い高価値側。腐り度も高いので timestamp (2026-09-07) を打って drift の重点対象にする扱いで妥当
- **リンク**: 変更・追加した 11 ファイルの相対リンクをすべて解決確認、切れ無し。`cross/index.md` への追加行も実在パスを指す
- **lint**: `doc-structure-lint.py` は `release-pipeline.md` / `repository-boundaries.md` / `cross/index.md` で違反なし。ADR 側の箇条書き長超過と `log.md` の項目数超過はいずれも本変更以前からのもの (accepted ADR は ksn-core 上そもそも構造 lint の対象外)。`local-path-lint.py` / `identity-lint.py` は全変更ファイルで指摘なし
- **`repository-boundaries.md` の timestamp 据え置き**: 全体を再検証していない以上、最終検証日を動かさない判断は ksn-core の timestamp 定義に沿う

## アクションプラン

1. maui/0015 の footer から「リース共有は未解決」を撤回し、maui/ADR-0026 への `関連:` 行を足す (Major・事実誤り)
2. `現行照合` footer を残り 6 本へ足す。8 本共通にしない判断を採るなら exploration.md の決定事項を先に直す (Major・合意スコープ)
3. maui/0015 の残課題 (3) を本文か footer に具体形で残し、削除範囲の拡大を deviation.md へ記録する (Minor)
4. android/0019 の footer の主張を、build-toolchain.md へ 1 文追記して成立させるか、実際に持っている範囲へ狭める (Minor)
5. `release-pipeline.md` の「保証すること」から handbook と重複する冪等条件の列挙を外し、ポインタへ置き換える (Minor)
6. deviation.md を作成し、付随修正と決定事項からのずれを 1 行ずつ記録する (Minor・L-009)
7. Suggestion 群 (cross/0018 の `関連:` 行、`整理:` 行の位置統一、Portal 手順の分割、cross/0021 のスクリプト名の移動、初見可読性レビューの証跡、rules.md のカテゴリ説明) は任意
