# レビュー結果: settingsview-migration-defects (001 回目)

**日付**: 2026-09-15
**判定**: CHANGES_REQUESTED

## サマリー

論点 1 (暗黙 Style) と論点 2 (Android の付け外しでスクロール位置が消える) の修正はどちらも原因に正しく当たっており、実装の形も合意済みスコープどおり (遅延生成 / Android Native の付け外しに閉じた控えと復元)。ミューテーション検証でも新規テストは修正を外すと確実に落ち、検出力は十分だった (下記「検出力の実測」)。

一方で、本 change が直接無効化した Android の concepts 記述 2 箇所が更新されずに残っており (Major 1)、修正後証跡がどの成果物からも参照されていないため第三者が「どの導線で何を確認したか」を復元できない (Major 2)。どちらも数行で閉じるので、このサイクル内で直してほしい。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / C# の新規コメント) |
| `kasane/handbook/cross/test-execution.md` | テストを実行し結果を報告するため |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (window 付け外し・構築順序) の不具合修正の完了判定 |
| ksn-core `references/evidence.md` | `evidence/` に証跡を置く change のため |
| `kasane/decisions/maui/0022-view-placement-guard-pre-commit-validation.md` | validateValue の検査契約に触れるため |
| `kasane/decisions/maui/0023` (見た目スタイルの控えと再配信) | 同型の対処との一貫性確認のため |
| `kasane/lessons/code-review.md` L-001 / `kasane/lessons/process.md` L-003・L-005・L-009 | 昇格済みルール |

ios / maui / android の handbook に本 change の担当範囲へ当たる文書は無かった (maui は検証ホストの実行と描画性能の計測構成のみ)。

## ビルドとテスト

| 対象 | コマンド | 結果 |
|---|---|---|
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/` | 成功 527 / 失敗 0 / スキップ 0 |
| Android | `./gradlew test` | BUILD SUCCESSFUL。`:kssettingsview:test` は実際に実行 (`:kssettingsview-bridge:test` は UP-TO-DATE)。テスト結果 XML 集計で 2858 件 (debug / release 両 variant の合算)・失敗 0・スキップ 0。うち `AdapterReattachTest` は 7 件 |

lint: `comment-policy-lint.py` 禁止 0 件 (要確認 164 件はすべて既存の Sample 側で、本 change の追加分ではない)、`local-path-lint.py` / `identity-lint.py` 検出 0 件。`doc-structure-lint.py` は下記 Minor 2 を参照。

### 検出力の実測 (code-review L-001)

新規テストが本当に修正を捕まえるかを、一時ミューテーションで確認した (いずれも backup との shasum 一致で原状復帰済み)。

| ミューテーション | 結果 |
|---|---|
| `maui/KsSettingsView.Maui/SettingsView.cs` を修正前の内容へ戻す | `ImplicitStyleTests` 7 件すべて失敗 (構築時 NRE / 値が届かない の両方を捕捉) |
| `KsSettingsView.kt` の `restorePendingScrollPosition()` 呼び出しを外す | `AdapterReattachTest` の新規 2 件が失敗、既存 5 件は通過 |
| `savePendingScrollPosition()` を `recyclerView.adapter = null` の**後**へ移す | 新規 2 件が失敗 — 「切る前に控える」という順序の判断まで固定できている |

3 本目まで落ちるので、修正の存在だけでなく実装コメントが主張する順序の根拠までテストが守っている。

## 指摘事項

### [🟠 Major] Android concepts が「スクロール位置は復元対象外」と述べたまま残っている

**該当箇所**: `kasane/concepts/android/api/android-native-host.md:56`、同 `:164`

**問題点**: 本 change は Android Native の付け外しでスクロール位置を保つようにしたが、その挙動を記述している concepts が旧挙動のまま残っている。

- `:56`「…detach 中の Store 更新を含む最新内容で復帰する。…**スクロール位置は復元対象外。**」
- `:164` (「保証すること」)「detach → 再 attach をまたいでも表示は Store の現在値と一致して復帰する。…(**スクロール位置は保証しない**)」

concepts は「今どうなっているか」の記述層であり、この 2 文は本 change の実装によって直接偽になった。MAUI 側 concepts 2 本は更新されているのに Android 側だけ取り残されており、蒸留時に乖離として再発見されることになる。

なお `maui/ADR-0022` と `kasane/concepts/maui/architecture/view-materialization.md` には「構築前は検査しない」に相当する記述が無いため、deviation に記録された検査厳格化による長命層の乖離は無い (確認済み)。

**推奨修正**: `:56` の「スクロール位置は復元対象外。」を、控えと復元を行う現在の挙動 (detach 直前に LayoutManager のアンカーを控え、adapter を戻した直後に復元する。Host そのものを作り直す場合は対象外) へ書き換え、`:164` の「保証すること」も同様に改める。合意済みスコープが名指ししていない文書なので `deviation.md` に 1 行足す (process L-009)。本 change が自分で開けた穴であり数行で閉じるため、別 change へ逃がさない (process L-005)。

### [🟠 Major] 修正後証跡がどの成果物からも参照されておらず、導線が復元できない

**該当箇所**: `evidence/android-maui-sample-normal-nav-01..04`、`evidence/maui-implicit-style-ios-after.png`、`deviation.md`

**問題点**: ksn-core の証跡規約は「どの verify / review / implement の結果かは、その文書の該当箇所から `evidence/<file>` を change 相対で参照する」と定める。修正**前**の証跡は `exploration.md` の実測表から参照されているが、修正**後**の 5 枚はどこからも参照されていない (この change には tasks.md が無く、`deviation.md` は検査厳格化の 1 行のみ)。結果として次が成果物から確認できない。

1. `maui-implicit-style-ios-after.png` が**どの暗黙 Style を当てた状態**の、どのアプリのどの画面か。画面自体は正常描画されているが、暗黙 Style が実際に適用されていたことも、Style で指定した値が表示へ届いたことも、画像からも文章からも判定できない。論点 1 の症状は構築時クラッシュなので「描画された」だけでも解消の証拠にはなるが、探索で明記された「落ちないが Style の色が効かない」側の解消はこの 1 枚では示せていない
2. `android-maui-sample-normal-nav-*` の導線。画面に写っている「TEMP 子ページへ Push」はリポジトリの Sample に存在せず (`samples/maui/` を検索して 0 件)、検証のための一時追加と見られる。作業ツリーに残っていないのは正しいが、導線を記述した文章が無いため後から再現できない
3. exploration.md の決定事項は完了判定を「`evidence/` の修正前証跡と**同じ導線**で修正後を確認する (Android 実機の通常遷移…)」と定めており、修正前の通常遷移証跡は ColorAnalyzer で取られている。実際の修正後確認は MAUI Sample + 一時ボタンへ導線が変わっているが、この差し替えが `deviation.md` に記録されていない

`kasane/config.yaml` の `distill.archive-media: delete` により画像は archive 時に削除されるため、今どこからも参照されていない証跡は蒸留後に「何を確認したかの記録ごと」失われる。process L-003 (4) はレビューの判定条件に証跡と提出コードの対応を置いている。

**推奨修正**: `deviation.md` (または change 配下に検証メモを 1 枚) へ、修正後証跡 5 枚を change 相対で参照しつつ次を書く。(a) Android 通常遷移の導線を MAUI Sample の Header / Footer デモ + 一時 Push ボタンへ差し替えたこと、`-04` が `restorePendingScrollPosition()` を外したビルドの A/B であること。(b) iOS の 1 枚について、どのアプリ・どの画面で、どのプロパティを持つ暗黙 Style を当てた状態かと、Style の指定が表示に出ていることをどこで読み取れるか。

### [🟡 Minor] 検査厳格化 (validateValue が構築前にも走る) に回帰テストが無い

**該当箇所**: `maui/KsSettingsView.Maui/SettingsView.cs:74-80`、同 `:91-97`、`deviation.md`

**問題点**: `_controller?.` を `Controller.` へ変えたことで、`RootHeaderView` / `RootFooterView` の多重配置検査が基底コンストラクタ中の Setter 適用時にも走るようになった。deviation の判断自体は妥当で、副作用も改善方向にある — 旧実装では検査を素通りした後の `propertyChanged` が `_controller` の NRE になっていたので、`maui/ADR-0022` が定める公開契約どおりの `InvalidOperationException` へ変わるのは一貫性が増している。

ただし `ImplicitStyleTests` にこの挙動を固定するテストが無い。意図して変えた公開契約 (暗黙 Style 経由で他所に置かれている View を置くと `InvalidOperationException`) が、将来 `?.` へ戻されても誰も気づけない。

**推奨修正**: `ImplicitStyleTests` へ 1 件足す。既に別の SettingsView / Section の accessory に置いてある View を値に持つ暗黙 Style を登録し、`new SettingsView()` が `InvalidOperationException` を送出することを確認する。

### [🟡 Minor] phase-8 agenda への追記が doc-structure lint に新規違反を足している

**該当箇所**: `kasane/roadmaps/maui-support/phases/phase-8-scroll-control/agenda.md:10`

**問題点**: `python3 scripts/doc-structure-lint.py` が「箇条書き 1 項目が 383 字 — 段落・表の行・小節のいずれかへ移す」(上限 200 字) を報告する。この違反は本 change の追記で新しく生まれたもの。リポジトリ全体では既存違反があるため lint の終了コードは元から 1 だが、増やす側に回っている。

このファイルは今回レビュー対象外と指定された探索フェーズの成果物なので判定の主因にはしないが、事実として報告する。

**推奨修正**: 論点の見出しを小節 (`### Host 世代をまたぐスクロール位置の保持`) に切り出し、本文を段落へ移す。

### [🔵 Suggestion] 復元アンカーの寿命に関する KDoc の断定が、covered な経路より広い

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:365-370`

**問題点**: `restorePendingScrollPosition` の KDoc は「内容の再投入 (Store からの再同期) が後から走っても、アンカーは同じレイアウトで解決される」と断定している。`LinearLayoutManager` は復元直後のレイアウトで item 数が 0 だと保留状態を消費せずに捨てるため、この断定は「復元時点で内容が空になり得ない」という前提の上でだけ成り立つ。本 change が対象とする経路 (同一 `concatAdapter` を状態ごと戻す) では item 数が 0 にならないので実害は無い。

なお付け外しの寿命について、以下は確認して問題なしと判断した。

- 初回 attach: 控えが無いので `restorePendingScrollPosition` は即 return。既存の「初回 attach での再適用は差分通知を出さない」テストも通過
- 複数回の付け外し: 2 本目の新規テストが「古い控えが残らない」ことを固定している
- Store 差分適用中の detach: 控えも Store 適用も UI スレッド上で、`onAttachedToWindow` は adapter 復帰 → アンカー復元 → pending Store の取り込み直しの順。割り込みは起きない
- 控えた位置が新しい内容の範囲外: `LinearLayoutManager` が範囲外の pending 位置を破棄して通常レイアウトへ倒れるため落ちない
- 元の目的 (ViewPager2 / Compose `AndroidView` での空表示回避) は既存 5 件のテストで維持を確認

`onSaveInstanceState` に含めない判断も妥当と考える。`SavedState` 経由の復元は Activity 再生成のケースであり、そこでは MAUI 側で Host 世代が変わって設定ツリー自体を作り直すため、アンカーだけ持ち越しても整合しない。phase-8 の論点へ積んだ切り分けと一致している。

### [🔵 Suggestion] 通常遷移の修正後証跡 2 枚がバイト同一

**該当箇所**: `evidence/android-maui-sample-normal-nav-01-after-scrolled-bottom.png`、同 `-03-after-back-position-kept.png`

**問題点**: 2 枚は shasum が完全に一致する (`787e9d3c…`)。位置が保たれた結果として期待どおりの状態ではあるが、「同じファイルを複製した」場合と成果物からは区別できない。時計表示が分単位なので同一分内の撮影なら自然に一致し得るため、誤りと断定はしない。

**推奨修正**: Major 2 の記述を足すときに「-01 と -03 はバイト同一 — 位置が保たれた結果」と一言添えると、後から読む側が取り違えを疑わずに済む。

## 確認して問題が無かった観点

- **遅延生成の再入・二重生成**: `KsSettingsController` / `KsItemsSourceBinder` / `KsBindingContextBinder` はいずれも primary constructor でフィールド代入しか行わず、コンストラクタから `owner` のプロパティへ戻らない。`??=` が評価中に再入して 2 個作られる経路は無い。全メソッドを UI スレッドから呼ぶ既存契約の下で `??=` の非原子性も問題にならない
- **`_sectionBinder` の生成時期変更**: 旧実装はコンストラクタで作っていたが、`KsItemsSourceBinder` のコンストラクタは購読も登録も行わないため、遅延化しても観測できる差は無い
- **接続時の配信**: 暗黙 Style で先に入った Theme / ListStyle は controller の `_theme` / `_style` に載り、`Connect` の `gateway.SetTheme` / `SetStyle` で初回接続時に配信される。root accessory の text / View は `ApplyHostViews` → `ApplyRootSlot` が取り付け後に配信する。`Connect` の 2 回目以降 early return は `ConnectGateway` が gateway 未接続のときしか呼ばないため無関係で、Host 世代をまたぐ再配信は従来どおり Store と `ApplyHostViews` が担う。`ImplicitStyleTests` の 5 件がこの 4 経路 (Theme / BackgroundColor / ListStyle / RootHeaderText / RootHeaderView) をそれぞれ固定している
- **concepts の記述と実測の整合**: `maui-rendering-lifecycle.md` の「Push で背後に回ったページでは両 OS とも Handler は切られない」は exploration の MAUI 本体ソース読み (`StackNavigationManager` / `Page.SendNavigatedFrom`) と、`evidence/*-sample-reconnect-*` (Pop 経路では両 OS とも先頭へ戻る) の双方と矛盾しない。`maui-facade.md` の `DynamicResource` 記述も「初期解決は届くが追随しない」という実測と一致する。両ファイルとも doc-structure lint に違反しない
- **comment-policy**: 新規コメントに作業文書パス・change 識別子・論点番号・デルタスペック構文キーワードの混入は無い。新設した `Controller` / `SectionBinder` / `SectionContextBinder` はいずれも非公開 (internal / private) なので、実装根拠を doc コメントに置いても公開 doc コメントの規約には触れない
- **スコープの逸脱**: 合意済みスコープが要求しない抽象化・設定項目・拡張ポイントの追加は無い。Android 側の追加は `onDetachedFromWindow` / `onAttachedToWindow` の対に閉じており、公開 API は増えていない
- **足場の書き換え**: `exploration.md` の差分は探索フェーズの追記 (決定事項・実測表・裏取り) のみで、実装中の書き換えに当たるものは無い

## アクションプラン

1. (Major 1) `kasane/concepts/android/api/android-native-host.md:56` と `:164` のスクロール位置に関する記述を現在の挙動へ改め、`deviation.md` に 1 行足す
2. (Major 2) 修正後証跡 5 枚を `deviation.md` (または検証メモ) から change 相対で参照し、Android の導線差し替えと `-04` の A/B、iOS の 1 枚が示す状態を明記する
3. (Minor 1) `ImplicitStyleTests` へ、暗黙 Style 経由の多重配置が `InvalidOperationException` になることを固定するテストを 1 件足す
4. (Minor 2) phase-8 agenda の追記を小節へ切り出し、doc-structure lint の新規違反を解消する
5. (Suggestion) `restorePendingScrollPosition` の KDoc の断定に前提を添える。証跡 2 枚のバイト同一について一言添える
