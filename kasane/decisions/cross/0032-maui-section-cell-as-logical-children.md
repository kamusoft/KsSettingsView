---
id: 0032
title: MAUI の Section / Cell は SettingsView の論理子にし、Cell の binding は accessory View と同じ論理所有の型に載せる
status: accepted
date: 2026-09-16
amends: core/0031
---

## Context

MAUI facade の `Section` / `CellBase` は MAUI の element ツリーに繋がっておらず (`Parent` が `null`)、facade が継承 BindingContext だけを手作業で配っていた。一方、accessory View と `CustomCell.Content` は論理子にする (maui/ADR-0016: 論理所有は View プロパティの寿命、Handler の有無に依らず維持)。facade 内で「View は論理子、Section / Cell は非論理子」という非対称があった。

core/ADR-0031 Decision 2 の MAUI 節は、この非対称の帰結として「Cell の色プロパティは `AppThemeBinding` が再評価されないため `Application.RequestedThemeChanged` を購読して再代入する」を利用者の手段と定め、ツリーへ繋ぐ改修は別途探索すると切り離していた。

前提の裏取り (maui-appearance-change-tracking の spike): 継承 BindingContext の配布処理で `AddLogicalChild` を 1 行足しただけで、Cell の `AppThemeBinding` が外観切替で再評価され native の描画まで届いた (iOS / Android)。`AppThemeBinding` / `DynamicResource` は親の Resources 変更通知だけを再評価の契機にしており、`Parent` の無い `Element` には通知が届かないため。

本決定の前提:

- `Section` / `CellBase` は素の `Element` のまま。暗黙 Style は `NavigableElement` 以上にしか当たらず、素の `Element` には及ばない
- MAUI 本体の `Element.Parent` は弱参照で、外部が Section / Cell を保持し続けても SettingsView は回収される (facade の leak テストが論理子化込みで固定)
- facade は増減を通知しないコレクション (素の `List<T>`) も `Root` / `Cells` の入力として正式に扱い、表示へ変換する時点の内容を静的に描画する

## Decision

core/ADR-0031 の決定のうち、Decision 2 の MAUI 節 (Cell の色プロパティは `RequestedThemeChanged` 購読 + 再代入を手段とする範囲) と、Alternatives「MAUI の Cell プロパティに `AppThemeBinding` を書く」の却下を本決定で置き換える。他の決定 (Android の Cell 固有色の 1 流儀・明示色をライブラリが置き換えない契約・iOS / Android の手段・beta 中の互換方針・DTO の輸送項目) は維持する。

### 1. Section / Cell は SettingsView の論理子にする

SettingsView → Section、Section → Cell を `AddLogicalChild` / `RemoveLogicalChild` で繋ぐ。Section / Cell に書いた `AppThemeBinding` / `DynamicResource` はページ配下の Element と同じく初期解決と再評価 (外観変更・Resources 差し替え) を受ける。対象プロパティは限定しない (色に限らず全 BindableProperty)。

### 2. 論理所有の型は accessory View と同じ「所属の寿命」にする

Section の Root への所属、Cell の Section への所属が始まる時点で繋ぎ、終わる時点 (Root からの除去・Section からの除去・ItemsSource 再生成による除去・Root / Cells コレクションの差し替え) で外す。付け外しは Handler (Host) の有無に依らない。

継承 BindingContext の配布と論理子化は同じ器 (`KsLogicalChildOwnership`。継承 BindingContext を配っていた器を所有の器に育てたもの) が 1 手順として行い、controller (Host 依存) には寄せない。器はコレクションの増減通知で付け外しし、増減を通知しないコレクションについては表示へ変換する直前に現在の内容と照合して付け外しを揃える (所属の確定がその時点に寄る)。

### 3. 論理所有の変更口は所有の器 1 つにし、他所に所有された Section / Cell の追加は所属の時点で例外にする

期待する所有者以外の facade 所有者 (別の Section / SettingsView、SettingsView から外れた Section) の論理子である要素を `Root` / `Cells` へ追加すると、Host の有無に依らず追加の時点で `InvalidOperationException` になり、既存の配置には触れない。同じコレクションへの二重追加は現行どおり変換時の数えあげで例外にする。controller は `Parent` を触らない。

### 4. accessory View / `CustomCell.Content` の多重配置検査にも「期待する所有者以外の facade 所有者に所有された View」を加える

例外の型・時点 (maui/ADR-0022 の値確定前 guard) と「構造・snapshot を配信する前に全件検査し部分更新を残さない」契約は現行のまま。

### 5. 利用者向けの手段は `AppThemeBinding` / `DynamicResource` に一本化する

`RequestedThemeChanged` 購読 + 再代入は動作し続けるが、契約の記述 (concepts・Sample・検証ホストの既定シナリオ・Skill) からは外し、併記しない。`SettingsView` の Theme プロパティと Cell のプロパティで手段が割れる非対称を解消する。

## Alternatives Considered

論理所有の範囲:

- **`AppThemeBinding` を効かせるためだけの最小の `Parent` 付け (所有モデルには手を入れない)**: 却下。所有の正が controller の対応表と `Parent` の二系統になり、外す側 4 経路の一本化ができない。maui/ADR-0016 の寿命規律と食い違う型が facade 内に二つ残る

付け外しの置き場:

- **controller に寄せて Host 接続中だけ論理子にする**: 却下。論理所有が Host 世代の寿命になり accessory View と規律が割れる。表示前 (XAML 構築時) の `DynamicResource` 解決と BindingContext 継承が Host 接続まで遅れ、改変が controller の登録 / 解除と Host 接続 / 切断時の付け外しに散る

所有の引き取りと多重配置:

- **所属の経路でも常に引き取る (奪う)**: 却下。正しく置かれている側の論理親と継承 BindingContext を XAML 構築の時点で黙って奪う
- **所属の経路では引き取らず BindingContext だけ配る**: 却下。表示中の要素の BindingContext を変換前に変え、既存配置が無傷でなくなる
- **所属の経路では触れず、変換経路 (検査後) で `Parent` null の要素を引き取る**: 却下。旧所有が解除された後、変換まで `Parent` null の空白が残り、「所属中は Host に依らず論理子」と矛盾する
- **他所所有中は pending として旧 `Parent` の解除を監視し自動的に引き取る / pending 配置を変換まで保証外と明記する**: 却下。誤用のために機構や例外規定を足すことになる
- **SettingsView をまたぐ配置は検出せず「最後に変換した側が勝つ」と契約化する**: 却下。`Parent` は 1 つしか持てず、両方のコレクションに残る要素を両方の論理子にはできない。同じ SettingsView 内と同じく誤用として例外にする (オーナー裁定)

利用者向けの手段:

- **購読 + 再代入を代替手段として併記で残す**: 却下。Theme プロパティと Cell プロパティで手段が割れる非対称 (core/ADR-0031 の負の帰結) を解消するのが本決定の価値で、併記すると「どちらを書けばよいか」に戻る

## Consequences

- 正: Section / Cell の色に限らず全プロパティで `AppThemeBinding` / `DynamicResource` が効き、Theme プロパティと Cell プロパティの手段が揃う。利用者の購読 + 再代入コードが不要になる
- 正: facade 内の論理所有が View と Section / Cell で同じ型 (所属の寿命・Host 非依存) に揃い、継承 BindingContext の手配布は MAUI 本体の `Parent` 経由の配布と同じ規則の 1 手順に畳める
- 正: `Parent` が無いことに依存した実装・テストは facade に無く、暗黙 Style は素の `Element` には当たらないため、公開挙動の非互換は生じない
- 負: 外す側 4 経路すべてで `RemoveLogicalChild` を対にする必要があり、片道で済ませると `Parent` が古い所有者を指し続ける (多重配置の誤判定・古い親の Resources を引く)
- 負: 他所に所有された Section / Cell の追加が追加時に例外になる (現行は同じ SettingsView 内なら変換時、またぐ場合は黙って通る)。誤用であり beta 中の破壊的変更として受容する
- 負: 増減を通知しないコレクションでは、付け外しも他所所有の例外も表示へ変換する時点に揃う。除去した要素を別の場所へ置き直すには、`Root` / `Cells` へ新しいコレクションを代入して所属の解除を確定させてから追加する
- 負: CSS StyleSheet を使うアプリでは `Parent` 設定時に `ApplyStyleSheets()` が走り、Section / Cell がセレクタに掛かり得る (適用先は `Style` 経由なので実害は薄い見立て)
- 負: `AppThemeBinding` は対象が `VisualElement` でないと Window ごとの外観を見ず `Application.Current` に落ちるため、マルチウィンドウで窓ごとに外観が異なる構成では誤った外観を引き得る
- 負: 購読 + 再代入を正としていた利用者向けの記述 (concepts・Sample・検証ホストの既定シナリオ・Skill) の追随が要る

## Revisit When

- `Section` / `CellBase` を `VisualElement` (または `NavigableElement`) に変えるとき — 暗黙 Style が当たるようになり、Window ごとの外観解決にも乗るため、本決定の副作用の評価を見直す
- MAUI 本体が `Element.Parent` の参照の強さ、または `AppThemeBinding` / `DynamicResource` の再評価契機を変えたとき
- マルチウィンドウで窓ごとに外観が異なる構成を公式に支えるとき

---
出典: kasane/changes/archive/2026-09-16-maui-cell-appthemebinding-logical-child/exploration.md (論点 1〜4 の比較表と決定事項) / 同 design.md (Decision 1〜4 の採用案・理由・代替案) / kasane/changes/maui-appearance-change-tracking/exploration.md (spike ②-b の結果、2026-09-15) / 2026-09-15 ksn-explore での議論 (論点 1・2・4 の採用と SettingsView をまたぐ配置の例外化はオーナー判断)
関連: maui/ADR-0016 (論理所有と platform lease の寿命分離 — 本決定が Section / Cell に広げる型) / maui/ADR-0022 (View 配置プロパティの多重配置検査の時点 — 本決定が維持する契約)
