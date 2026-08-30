# セカンドオピニオン: restore-pickercell-object-items (code-001)
**相方**: codex / **label**: so-code-restore-pickercell-object-items / **日付**: 2026-08-28 / **対象**: HEAD (205b9eb) に対する未コミット working tree 変更一式
---
# レビュー結果: restore-pickercell-object-items

**日付**: 2026-08-28  
**判定**: **CHANGES_REQUESTED**

## サマリー

全体として仕様との対応、プラットフォーム間のモデル・輸送・UI の整合性は良好です。一方、MAUI の object 選択値が source snapshot の実体へ正規化されない問題があり、変更目的の主要契約に影響するため修正が必要です。

## 指摘事項

### 🟠 Major: 値等価な別インスタンスが source snapshot の要素へ再正規化されない

**該当箇所**: `maui/KsSettingsView.Maui/PickerCell.cs:472`、`maui/KsSettingsView.Maui/PickerCell.cs:484`

**問題点**:  
`ApplySelectedItem` は `Equals(SelectedItem, item)`、`ApplySelectedItems` は要素ごとの値等価が成立すると書き戻しを省略します。

そのため、例えば `ItemsSource` に含まれる `Plan("竹")` とは別インスタンスの `Plan("竹")` を `SelectedItem` に設定すると、index は正しく解決されても、公開される `SelectedItem` は source snapshot の要素ではなく、呼び出し側が渡した別インスタンスのまま残ります。

同様に、選択中の index を維持したまま `ItemsSource` を値等価な新しい object 列へ差し替えた場合も、古い snapshot の object が `SelectedItem(s)` に残ります。これは次の契約に反します。

- 選択の正は index であり、公開値はそこから再導出される
- `ItemsSource` 差し替え後は新しい snapshot の要素を参照する
- 表示・逆引き・`SelectedItem(s)` は同一 snapshot を参照する

**推奨修正**:  
値等価は `IndexOfItem` による逆引きだけに使用し、正から導出した object の適用可否は参照同一性を考慮してください。MAUI の `BindableObject` 側でも値等価によって更新が抑止されないことまで確認し、必要なら正規化専用の書き戻し方法を設けてください。

少なくとも以下の回帰テストを追加してください。

- 値等価だが別インスタンスの `SelectedItem` を設定し、source 内の実体が `SameAs` で返る
- 値等価な別インスタンス列へ `ItemsSource` を差し替え、新 snapshot の実体へ更新される
- `SelectedItems` でも各要素が source snapshot の実体になる

### 🟡 Minor: 複数の同名 indexer がある型で未解決フォールバックではなく例外になる

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsMemberProjection.cs:89`

**問題点**:  
`Type.GetProperty(member, ...)` は、同じ名前を持つプロパティが複数ある場合に `AmbiguousMatchException` を送出します。例えば `int` と `string` の2種類の public indexer を持つ型で `DisplayMember = "Item"` を指定すると、92行目の引数なし判定へ到達する前に例外になります。

indexer は仕様上の「引数なし readable property」ではないため、本来は解決不能として主表示を `ToString()`、副表示を無しへフォールバックすべきです。

**推奨修正**:  
`GetProperties(...)` の結果から、名前、public instance getter、引数なしという条件を順に絞り込んでください。複数 indexer を持つ fixture を追加し、主表示・副表示双方のフォールバックをテストしてください。

## 確認結果

依頼文に記載された以下の実行済み結果を前提にしました。

- iOS: 629 tests / 0 failures
- Android: 2676 tests / 0 failures
- MAUI: 491 tests / 0 failures
- 全 sample ビルド成功
- 承認済みモックとの視覚的乖離なし

`deviation.md` の4件は合意済み差分として扱い、指摘対象から除外しました。

## アクションプラン

1. MAUI の `SelectedItem(s)` を source snapshot の実体へ確実に再正規化する。
2. 参照同一性を検証する回帰テストを追加する。
3. リフレクション解決を同名 indexer に対して安全にする。

**指摘件数**: Critical 0 / Major 1 / Minor 1 / Suggestion 0  
**最終判定**: **CHANGES_REQUESTED**

## 突き合わせ結果 (ksn-orchestrator、2026-08-28)

ホスト側 review-001.md との突き合わせ。双方の指摘に重複なし。

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| 値等価な別インスタンスが source snapshot の実体へ再正規化されない (`PickerCell.cs` ApplySelectedItem/ApplySelectedItems) | 相方のみ (Major) | **採用** (Major) | 該当箇所特定・実害シナリオ (ItemsSource 差し替え後に旧 snapshot の object が公開値に残る) あり。「選択の正は index、公開値は再導出」の契約 (ADR-0029 / design) と整合する指摘 |
| 同名 indexer 複数の型で `GetProperty` が `AmbiguousMatchException` (`KsMemberProjection.cs`) | 相方のみ (Minor) | **採用** (Minor) | 再現条件が具体的 (int/string の2 indexer + `DisplayMember="Item"`)。spec のフォールバック契約 (解決不能→ToString) に反して例外になる |

- ホスト側のみの指摘 (Major 2 件・Minor 1 件・Suggestion 2 件) はホスト判定のまま処理
- 未解決・矛盾: なし
