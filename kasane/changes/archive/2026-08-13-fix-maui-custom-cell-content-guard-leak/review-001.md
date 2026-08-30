# レビュー結果: fix-maui-custom-cell-content-guard-leak (001 回目)

**日付**: 2026-08-13
**判定**: APPROVED

## サマリー

`CustomCell.ContentGuard` の弱参照化は `Section.AccessoryGuard` と逐語的に同一の形で実装されており、合意済みスコープ 3 項目をすべて満たす。追加された回帰テストはミューテーション実測で回帰検出力を確認済み (強参照へ戻すと当該テストだけが落ちる)。ビルド・全テスト (417 件) 成功。Critical / Major / Minor いずれも無し。

## 確認した観点

**合意済みスコープの充足**

1. **backing store の弱参照化**: `maui/KsSettingsView.Maui/CustomCell.cs:94,189-196` は `maui/KsSettingsView.Maui/Section.cs:154,265-272` と型引数以外まったく同一の形 (フィールド宣言位置・getter の `is not null && TryGetTarget` パターン・setter の null 分岐・`<remarks>` の文面)。「形は Section.AccessoryGuard と同一にする」の要求どおり。
2. **回帰テスト**: `maui/KsSettingsView.Maui.Tests/LeakTests.cs:65-80` を追加。既存の `FacadeAndGatewayAreCollectedWhileExternalHoldsModel` と同じ `BuildConnectedViewThenDrop` 経由の流儀で、末尾の `Assert.That(cell.Content, Is.SameAs(content))` が GC 検査時点までの Cell 生存も担保している (既存テストの `Assert.That(cells, Does.Contain(cell))` と対称)。
3. **既存 guard 動作テストの退行なし**: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → 失敗 0 / 合格 417。`CustomCellContentTests` の多重配置検査テスト群は例外送出を期待する型であり、弱参照化で guard が失われれば落ちる性質を持つ。通過していることが guard の到達可能性が保たれている証拠になっている。

**回帰検出力の実測 (lessons code-review L-001)**

「新テストが強参照実装でも通ってしまわないか (トートロジーでないか)」を静的読解で終わらせず、実装を一時的に `internal IKsCellContentGuard? ContentGuard { get; set; }` (元の auto-property) へ戻して `LeakTests` を実行した。

```
失敗 FacadeAndGatewayAreCollectedWhileExternalHoldsCustomCell [29 ms]
  SettingsView が回収されず、強参照が残っている。
失敗! -失敗: 1、合格: 9、合計: 10
```

争点のテストだけが落ち、他 9 件は通過 — 検出力の証明として決定的。一時変更は backup からの復元で shasum 一致 (`8c9f0d883d2dbc281fe567a261ca52d80bb3a337`) を確認し、原状復帰済み。

**Non-Goals の遵守**

- guard 配線の形 (`?.EnsureContentCanBePlaced` / `= this` / `= null`) は不変。`KsSettingsController.cs:1030,1991,2004` と `CustomCell.cs:57` に差分なし。
- 変更ファイルは `CustomCell.cs` と `LeakTests.cs` の 2 件のみで、`Section.cs`・他プラットフォームへの波及なし。未追跡ファイルは `kasane/changes/fix-maui-custom-cell-content-guard-leak/exploration.md` のみ (ソースの追加なし)。

**堅牢性・設計品質**

- **弱参照化による機能退行なし**: `KsSettingsController` は `SettingsView.cs:426` の `private readonly KsSettingsController _controller` で強参照保持される。Cell が変換経路に載っている間は SettingsView が生きており、guard は必ず到達可能。SettingsView が回収済みの状態では guard が null に落ちるが、これは「設定ツリーに載っていない Cell には尋ねる相手がいない」(`IKsCellContentGuard` の契約・maui/ADR-0022) と一致する挙動で、Section.AccessoryGuard で確立済みの前例と同一。
- **同型リークの残存なし**: model 側が controller を保持する経路は `KsSettingsController.cs` の `section.AccessoryGuard = this` (1953) と `custom.ContentGuard = this` (1991) の 2 箇所のみで、両方とも弱参照化された。`CellBase` / `SettingsView` に他の強参照経路はない。
- `WeakReference<T>.TryGetTarget` はスレッドセーフであり、facade の UI スレッド契約と併せて競合の余地なし。
- コーディングスタイル: `_camelCase` の private フィールド、visibility 明示、型を明示した `new WeakReference<IKsCellContentGuard>(value)` — いずれも規約およびファイル既存スタイルに合致。ビルド警告なし。
- コメント規約 (`concepts/cross/conventions/comment-policy.md`): 追加された `<remarks>` は change-id / Phase / アーカイブ文書パスの参照を含まず、ファイル単独で意味が通る。

## 指摘事項

### [🔵 Suggestion] concepts の弱参照記述が「購読」に限定されている (蒸留フェーズ向け申し送り)

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md` の「lifecycle の保証」節

**問題点**: 該当節は「コレクション・Cell への購読は weak であり、外部 (ViewModel 等) がコレクションや Cell を保持し続けても SettingsView の回収は妨げられない」と、参照強度の保証を**購読**に限って記述している。本変更と align-maui-accessory-placement-guard により、model → controller の参照は購読以外 (配置検査の尋ね先) も含めてすべて弱参照になり、保証の根拠が広がった。現在の文言のままだと、guard 参照が保証の対象外に読める。

**推奨修正**: 実装は現状で正しいためコード変更は不要。蒸留 (ksn-distill) で concepts を追随させる際に、この一文を「facade 内部から model 経由で controller へ戻る参照 (購読・配置検査の尋ね先) はすべて weak」の趣旨へ広げることを検討する。

### [🔵 Suggestion] リーク経路は `Content` の有無に依存しない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/LeakTests.cs:65-71`

**問題点**: `ContentGuard` は `KsSettingsController.RegisterCell` (`KsSettingsController.cs:1989-1993`) で CustomCell の登録時に無条件で差し込まれるため、リーク経路の成立に `Content` の設定は不要。テスト名とコメント (「内容を置いた CustomCell」) だけを読むと、内容を置いた場合限定の不具合と受け取られる余地がある。

**推奨修正**: 修正不要。`Content` を置く形は実体化経路 (`PlaceCellContent`) も通す分だけ実利用に近く、合意済みスコープの文面 (「Content 設定済み」) どおりであり、このままでよい。将来この経路に手を入れる際、検出条件が Content の有無に依らないことを前提にしてよい、という申し送りとして記録する。

## アクションプラン

実装側の対応は不要。マージ可。

1. (蒸留時) `concepts/maui/api/maui-facade.md` の weak 保証の文言を guard 参照まで含む表現へ広げるか検討する。
