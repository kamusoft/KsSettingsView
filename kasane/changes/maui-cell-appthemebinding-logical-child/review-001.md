# レビュー結果: maui-cell-appthemebinding-logical-child (001 回目)

**日付**: 2026-09-16
**判定**: APPROVED

## サマリー

デルタスペックの 5 Requirement (ADDED 4 + MODIFIED 2) の全 Scenario に対応する実装とテストが揃っており、facade テストは 552 件 / 失敗 0 (ベースライン 528 + 新規 24) で緑、Sample と MauiHost の Android ビルドも警告 0 で通った。所有の器 (`KsLogicalChildOwnership`) は付け外しを対にし、解除条件を「`Parent` が自分 かつ コレクションにもう含まれない」に絞っていて、二重追加・Reset・コレクション差し替え・ItemsSource 再生成の 4 経路がすべて 1 つの規律で覆えている。多重配置の判定と文言を `KsPlacementDiagnostics` へ 1 箇所に集めた点、`LeakTests` に「論理子として繋がったまま回収される」前提アサーションを足して弱参照依存を明示した点 (2.4) も、後から読む人が意図を復元できる形になっている。

指摘は Critical / Major なし。Minor 3・Suggestion 3 で、いずれも本 change の契約そのものではなく周辺 (例外後の中間状態・記録と成果物の記述精度・テストの検出力) に関するもの。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| handbook cross/comment-policy.md | 常時 (コメント構文を持つ全ソース) |
| handbook cross/test-execution.md | テスト実行とテスト結果の報告 |
| handbook cross/diagnostic-message-language.md | 本体コードの例外 message の追加・変更 |
| handbook cross/sample-parity.md | `samples/` のデモ画面の変更 |
| handbook cross/runtime-behavior-verification.md | 外観変更という実行時挙動の完了判定 (証跡の確認) |
| handbook maui/integration-host-verification.md | `maui/` facade 層に触れる end-to-end 疎通確認 |
| lessons process.md L-002 / L-003 / L-005 / L-006 / L-009、lessons code-review.md | 昇格済みルール |

機械検査の実行結果: `comment-policy-lint.py` 禁止 0 件 (796 ファイル)、`local-path-lint.py` 検出なし、`doc-structure-lint.py` は本 change が触った 6 文書に違反なし (残る違反は roadmaps 配下の既存分)。`samples/maui` の `RequestedThemeChanged` / `SampleThemeFollower` / `SampleTheme.Apply` / `MauiTitleText` / `IsDark` の参照が 0 件であることも確認した (samples-maui spec の「購読コードが無い」Scenario)。

証跡 (L-003) は `evidence/` に 20 枚の静止画と 2 本の記録があり、提出コードと対応している — MauiHost のダーク後画像で「外観追随ボタン」が `SettingsPage.xaml` の `Dark=#FFFF00FF` どおりマゼンタで描かれていること、Sample のダーク live 画像で「ログアウト」が `BasicCellsDemoPage.xaml` の `Dark={x:Static local:SampleTheme.MauiDarkHeaderText}` (#E0B040) で描かれていることを目視で確認した。画素比較 32 組は差分 0 で、sample-parity の「文言・構成を変えない」とも整合する。

## 指摘事項

### [🟡 Minor] コレクション差し替え時、新コレクションの検査より先に旧要素の所有を解いている

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsLogicalChildOwnership.cs:51`-`66` (`OnTargetChanged`)、`maui/KsSettingsView.Maui/SettingsView.cs:39`

**問題点**: `OnTargetChanged` は「購読解除 → `ReleaseAll()` → 新コレクションを購読 → `Apply()`」の順で進む。新コレクションに他所所有の要素が混ざっていると、`Apply()` の `Adopt` で例外になる時点で旧要素はすでに全部解放され (`Parent` が null)、購読も新コレクションへ移り終えている。さらに `Root` の場合は器が先に走るため `Controller.SetRootCollection` と `SectionBinder.OnTargetChanged` が実行されない。結果として、**表示され続けている旧 Section / Cell が論理親を失い、`AppThemeBinding` / `DynamicResource` が黙って再評価されなくなる** — 本 change が足した能力そのものが、表示は生きたまま失われる状態になる。あわせて controller は旧コレクションを見張ったままで、公開 `Root` は新コレクションを返す。

要素の `Add` 経路 (`OnObservedCollectionChanged`) は例外時に旧要素へ触れないため、この中間状態はコレクション再代入のときだけ生じる。spec の「失敗後も公開コレクションはロールバックされず、回復は Root の全体再構築で行う」の範囲内ではあるが、`concepts/maui/api/maui-rendering-lifecycle.md` の「追加の時点で弾かれた場合、既存の配置 (`Parent` / 継承 `BindingContext` / 表示) は動かない」という記述とは、再代入経路に限って食い違う。テストも無い。

**推奨修正**: `Apply()` の前 — できれば `ReleaseAll()` と購読差し替えの前 — に新コレクション全件を `KsPlacementDiagnostics.IsOwnedElsewhere` で検査し、1 件でも該当すればその場で例外にして旧状態へ触れないようにする (器の中で数行)。これで再代入も「既存の配置は動かない」に揃う。採らない場合は、再代入経路だけ中間状態が残ることを concepts か spec のどちらかに 1 文で書き、テストで固定する。

### [🟡 Minor] deviation.md の購読順に関する記述が実装と食い違う

**該当箇所**: `deviation.md:5`、`maui/KsSettingsView.Maui/Section.cs:138`-`139`

**問題点**: deviation は `Root` の購読順の入れ替えを記録したうえで「(`Section.Cells` は元から器が先)」と書いているが、`Cells` の `propertyChanged` も実際には順序が変わっている — 変更前は `_cellBinder.OnTargetChanged()` → `_cellContextBinder.OnTargetChanged()`、変更後は `_cellOwnership.OnTargetChanged()` → `_cellBinder.OnTargetChanged()` で、器は元は 2 番目だった。L-009 の事後判定 (合意済みスコープが名指ししていない変更が deviation の行として現れている) は満たしているが、記録の内容が diff と一致していないため、後から「なぜこの順序なのか」を読み直す人が誤った前提を受け取る。

**推奨修正**: 括弧内を削るか、「`Section.Cells` も器を先へ移した (ItemsSource 生成 Cell が生成と同時に論理子になるようにするため)」へ書き直す。

### [🟡 Minor] `maui-facade.md` の frontmatter description だけが本 change の主題を含んでいない

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md:4`

**問題点**: 本 change で書き換えた 4 文書のうち `maui-styling.md` / `maui-rendering-lifecycle.md` / `style-resolution.md` は description を追随させているが、`maui-facade.md` だけ旧文面 (「経路・導入と前提・型名衝突・…」) のままで、今回この文書の中心になった「論理子と binding の解決」に触れていない。同時に `concepts/maui/index.md` の 1 行説明は「論理子と binding の解決」を含む形へ更新されており、index と文書自身の自己申告が食い違う。次の drift 棚卸しで同じ箇所を再検出する種になる。

**推奨修正**: `maui-facade.md` の `description` に論理子と binding の解決を加え、index.md の 1 行説明と揃える。

### [🔵 Suggestion] 再評価の配信テストが「届いた値」まで見ていない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/AppearanceBindingTests.cs:139`-`141`

**問題点**: `ReevaluatedCellColorIsDeliveredAsAContentUpdate` は `ReplaceCell` と `ReplaceCells` の合計が 0 より大きいことしか見ていない。行が作り直されないこと (`RemoveCell` / `InsertCell` が空) は固定できているが、「再評価後の色が載った内容更新」であることは検出できず、古い値を載せたまま `ReplaceCell` を投げる回帰は素通りする。同じ Requirement の Section 側 (`ReevaluatedSectionHeaderTextIsDeliveredAsAnAccessoryUpdate`) は `update.Text` まで見ているので、片側だけ検出力が落ちている。

**推奨修正**: 配信された snapshot の `TitleColor` が dark 値 (`Colors.White`) であることを追加でアサートする。

### [🔵 Suggestion] `EnsureCellContentsAreFree` だけ検査と数えあげの順序が逆

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2252`-`2253`

**問題点**: `EnsureTreeHasNoDuplicates` では `EnsureNotOwnedElsewhere` → `AddSeenView` の順に書いているのに、ここだけ `AddSeenView` → `EnsureNotOwnedElsewhere` になっている。同じ例外型なので外から観測できる違いは出ないが、同じ契約を表す 2 箇所が別の順序で書かれていると、後から「順序に意味があるのか」を読む人が判断できない。

**推奨修正**: 他の検査点と同じ「検査 → 数えあげ」の順へ揃える。

### [🔵 Suggestion] release-note の accessory View に関する 1 文が例外の時点を誤読させ得る

**該当箇所**: `release-note.md:11`、`release-note.md:21`

**問題点**: 直前の文が「**追加したその時点で**」/「**at the moment of the add**」と時点を強調したうえで、続けて accessory View と `CustomCell.Content` に「同じ判定が加わりました」/「The same check now also applies」と書いている。実際には View 側は時点が異なり、所有者が既に変換経路に加わっていれば設定の時点、未参加なら所有者が変換経路に加わった時点 (spec の「同一 View インスタンスの多重配置は例外になる」および `LogicalChildTests.PlacingAccessoryViewOwnedByAnotherSettingsViewThrowsOnConversion` の挙動)。利用者が移行時に「設定した瞬間に落ちるはず」と期待して原因調査を誤る余地がある。

**推奨修正**: 「判定は同じだが例外になる時点は配置経路により異なる (設定時または表示へ変換する時点)」を 1 節足す。

## 所見 (指摘ではないもの)

- **例外 message が新しい失敗理由を区別しない**: 他所所有の要素を追加したときも、同じコレクションへ二重に入れたときも `The same {kind} instance cannot be placed more than once.` が出る。handbook cross/diagnostic-message-language.md の「原因の特定に要る情報を補間で載せる」から見ると情報が薄いが、tasks 1.1 が「`DuplicatePlacement` と同じ文言」を明示しているため、変えるなら spec 側の判断になる。実装の逸脱ではない。
- **`cross/ADR-0032` は proposed のまま**: `CellBase.cs` / `Section.cs` の公開 doc コメントと concepts 4 文書が ADR-0032 を参照しているが、ADR は proposed (昇格は蒸留時と proposal に明記)。proposed を根拠に判定は変えていない。蒸留で accepted へ昇格する前提が崩れた場合はコメント側も追随が要る。
- **`_owned` と非 observable コレクション**: `IList<T>` が `INotifyCollectionChanged` でない場合、コレクションから外れた要素は `_owned` に残り論理子のままになる。「observable でない実体は以後の構造変更を追跡しない」という現行契約の範囲内で、spec も触れていないため指摘にしていない。
- **`ReleaseAbsent()` (Reset) の一般性**: 既定の `SettingsRoot` / `ObservableCollection` は `Clear()` でしか Reset を出さないため、spec の Scenario では `ReleaseAll()` と区別が付かない。差が出るのは非空のまま Reset を出す実体だけで、そこまで見た実装になっている点は design Decision 1 のとおり。

## アクションプラン

1. Minor 1 (コレクション差し替え時の解放順) — 器の中で新コレクションを先に全件検査するか、中間状態を concepts / spec に明記してテストで固定する
2. Minor 2 (deviation の購読順の記述) / Minor 3 (`maui-facade.md` の description) — どちらも 1 行の修正
3. Suggestion 3 件 — 余力があれば同じサイクル内で
