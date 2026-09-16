# セカンドオピニオン: maui-cell-appthemebinding-logical-child (code-001)
**相方**: codex / **label**: so-code-maui-cell-appthemebinding-logical-child / **日付**: 2026-09-16 / **対象**: 作業ツリーの未コミット変更全体 (maui/KsSettingsView.Maui、maui/KsSettingsView.Maui.Tests、maui/tests/KsSettingsView.MauiHost、samples/maui、kasane/concepts・handbook の追随分)
---
# レビュー結果: maui-cell-appthemebinding-logical-child

**日付**: 2026-09-16  
**判定**: **CHANGES_REQUESTED**

## サマリー

observable な既定コレクションでは、論理子の付け外し、binding の再評価、多重配置検査、native 更新経路が概ね仕様どおり実装されています。提出された 552 件成功のテスト結果と両 OS の外観切替証跡も確認しました。

ただし、正式に対応している非 observable `IList` の初回変換経路で論理所有が欠落する Major があり、中核契約を満たしません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`
- `kasane/handbook/cross/diagnostic-message-language.md`
- `kasane/handbook/cross/sample-parity.md`
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/runtime-behavior-verification.md`
- `kasane/handbook/cross/local-development-setup.md`
- `kasane/handbook/maui/integration-host-verification.md`
- `kasane/lessons/code-review.md`
- `kasane/lessons/process.md`

## 指摘事項

### [🟠 Major] 非 observable コレクションの表示要素が論理子にならない

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsLogicalChildOwnership.cs:58`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:205`

**問題点**: 所有の器は対象が `INotifyCollectionChanged` の場合にしか追加を検知せず、Host 接続前にも現在内容を再照合しません。一方、既存契約は素の `List<T>` を「接続時点の内容を静的描画する」入力として正式に扱っています。

例えば空の `List<Section>` を `Root` に設定した後、Host 接続前に Section を追加すると、controller は接続時にその Section を native へ送りますが、`Section.Parent` は null のままです。`Cells` を素の `List<CellBase>` にした場合も同様です。この状態では表示されている Section／Cell の `AppThemeBinding` と `DynamicResource` が祖先へ到達せず、変更の主目的が成立しません。

**推奨修正**: controller が現在のツリーを変換する直前に、Root と各 Section.Cells の論理所有を現在の snapshot と再照合してください。少なくとも次の回帰テストが必要です。

- 空の `List<Section>` を Root に設定後、接続前に Section を追加してから接続する
- 空の `List<CellBase>` を Cells に設定後、接続前に Cell を追加してから接続する
- いずれも Parent チェーンと `AppThemeBinding`／`DynamicResource` の再評価を確認する

接続前の追加時点から厳密に Parent を保証するなら、非 observable `IList` の変更は観測不能なので、別途設計上の解決が必要です。

### [🟡 Minor] 新しいコメントが公開 doc コメント規約と proposed ADR 制約に違反する

**該当箇所**: `maui/KsSettingsView.Maui/CellBase.cs:17`、`maui/KsSettingsView.Maui/Section.cs:25`、`maui/KsSettingsView.Maui/Internals/KsLogicalChildOwnership.cs:16`

**問題点**: `CellBase` と `Section` の公開 doc コメントに内部設計識別子 `cross/ADR-0032` が入っています。公開 doc コメントは利用者向け契約だけで自己完結させ、ADR ID を含めないことが handbook の明示規則です。静的 lint の advisory でも新規 2 箇所が検出されます。

また `cross/ADR-0032` は現時点で `proposed` であり、内部コメントで許容される「確定した設計判断」にもまだ該当しません。

**推奨修正**: 公開 doc コメントから ADR 参照を削除してください。内部コメントも自己完結した説明だけにするか、ADR が accepted になった後に必要最小限の箇所へ置いてください。

## アクションプラン

1. 非 observable Root／Cells の接続前変更を、初回変換前に論理所有へ反映する。
2. 上記経路の Parent と binding 再評価テストを追加する。
3. 新規の `cross/ADR-0032` ソースコメント参照を整理する。
4. 修正後に MAUI facade 全件テストを再実行し、実行件数を併記する。

**件数**: Critical 0 / Major 1 / Minor 1 / Suggestion 0



## 突き合わせ結果 (ホスト review-001 との照合、2026-09-16)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| Major: 非 observable コレクション (`List<T>`) を Root / Cells にし Host 接続前に要素を足すと論理子にならない | 指摘なし | **採用** (Major、ホスト側の見逃し) | 該当箇所と実害シナリオ (表示されるのに `AppThemeBinding` / `DynamicResource` が届かない) が具体的。素の `IList` は既存契約が正式に扱う入力 |
| Minor: 公開 doc コメント (`CellBase` / `Section`) と内部コメントに `cross/ADR-0032` (proposed) の参照 | 指摘なし (comment-policy lint の禁止 0 件のみ確認) | **採用** (Minor) | handbook cross/comment-policy.md の明示規則 (公開 doc に ADR ID を入れない) |

ホスト側のみの指摘 (Minor 3 / Suggestion 3) はホスト側判定のまま修正サイクルに含める。矛盾する指摘はなし。
