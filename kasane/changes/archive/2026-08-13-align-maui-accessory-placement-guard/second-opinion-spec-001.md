# セカンドオピニオン: align-maui-accessory-placement-guard (spec-001)
**相方**: codex / **日付**: 2026-08-13 / **対象**: 提案一式 (proposal / specs/maui-core / tasks)
---
# レビュー結果: align-maui-accessory-placement-guard

**日付**: 2026-08-12  
**判定**: **NEEDS_DISCUSSION**

## サマリー

値確定前 guard の導入方針は、現行コードと先行変更の規律に整合しています。一方、構造バッチ失敗後の公開コレクション状態と Root 再構築時の「既存配置」の定義が未決定です。このままでは、仕様どおり実装しても公開ツリーと表示状態が分離したままになったり、実装者によって Root 再構築の許否が変わったりします。

指摘件数: Critical 0 / Major 3 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] バッチ失敗後の公開コレクション状態が規定されていない

**該当箇所**: [spec.md:11](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:11)、[spec.md:33](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:33)、[RangeAddCollection.cs:19](maui/KsSettingsView.Maui.Tests/Support/RangeAddCollection.cs:19)、[KsSettingsController.cs:392](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:392)

**問題点**:  
仕様は失敗時に native・対応表・実体・論理所有を変更しないとしていますが、`Root` / `Cells` の公開コレクション自体については何も定めていません。現行経路では、RangeAdd は要素を追加してから通知し、`Root` の差し替えも BindableProperty の値確定後に `RebuildRoot` が検査します。そのため例外後も公開コレクションには拒否された要素が残り得ます。

Scenario 名の「1件も入れないまま」とも読み方が衝突し、変更理由で問題視している「公開値と表示状態の分離」が構造変更では残ります。

**推奨修正**:  
次のどちらを契約にするか決定してください。

- 公開コレクションの変更は残るが、controller/native には反映されない。利用者がコレクションを修復する必要がある。
- 公開コレクションまでロールバックし、例外前のツリーへ戻す。

後者なら再入通知を含む実装設計と、`Root` 値・コレクション内容・購読状態まで確認するテストタスクが必要です。

### [🟠 Major] Root 再構築における「既存配置との重複」と releasing 意味論が曖昧

**該当箇所**: [proposal.md:20](kasane/changes/align-maui-accessory-placement-guard/proposal.md:20)、[spec.md:11](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:11)、[KsSettingsController.cs:961](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:961)、[AccessoryViewTests.cs:646](maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:646)

**問題点**:  
Requirement は Root 再構築でも「既存配置との重複」を拒否すると読めます。一方、Non-Goals が strict と明記するのは Replace バッチだけです。

現行 `RebuildRoot` は新しいツリー内部の重複を検査した後、旧登録をすべて解除します。このため次の区別が未決定です。

- 同じ Section・同じ slot・同じ View を含む Root 再構築
- 旧 Root から外れる Section の View を、新しい Section へ移す再構築
- Root accessory と新しい Section accessory の衝突

すべての旧配置を「既存配置」とすると、既存テストで保証されている同じ Section の正常な Root 再構築まで拒否します。逆に releasing を全面許容すると、Requirement の「既存配置との重複」は満たしません。

**推奨修正**:  
Root 再構築について、少なくとも「同一 slot の継続」「旧ツリーから解放される slot からの移動」「再構築後も残る外部 slot との衝突」を分けて許否を規定し、それぞれ Scenario を追加してください。

### [🟠 Major] Requirement が列挙する構造経路を tasks と Scenario が検証できない

**該当箇所**: [spec.md:11](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:11)、[spec.md:13](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:13)、[tasks.md:12](kasane/changes/align-maui-accessory-placement-guard/tasks.md:12)、[tasks.md:18](kasane/changes/align-maui-accessory-placement-guard/tasks.md:18)

**問題点**:  
Requirement は Section の追加・差し替え・Root 再構築・未接続状態からの Host 接続を対象にしていますが、予定されている異常系テストは RangeAdd と単一 Section 追加だけです。

これらは実際には別経路です。

- Add / Replace: `EnsureSectionsAreNotPlaced`
- Root 再構築: `EnsureTreeHasNoDuplicates`
- 初回接続: `SetTheme` 後に `RebuildRoot`

タスク2.1だけでは Root 再構築や接続時検査に影響せず、仕様の一部が未実装でも全予定テストが成功できます。また、「先頭の候補は正常、後続候補だけ既存配置と衝突」というケースがなく、全件検査前に1件目を挿入する退行も検出できません。

**推奨修正**:  
次を個別 Scenario／テストタスクとして追加してください。

- ReplaceSections の重複失敗
- Root 差し替えまたは Reset の重複失敗
- 未接続中に作られた重複ツリーの初回接続失敗
- RangeAdd の後半要素だけが既存配置と衝突し、先頭要素も挿入されないケース

### [🟡 Minor] Footer 4プロパティの対称性に回帰検出力がない

**該当箇所**: [spec.md:7](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:7)、[tasks.md:6](kasane/changes/align-maui-accessory-placement-guard/tasks.md:6)、[tasks.md:16](kasane/changes/align-maui-accessory-placement-guard/tasks.md:16)

**問題点**:  
実装タスクは Header/Footer の4プロパティを対象にしますが、失敗時原子性のテストは `Section.HeaderView` と `RootHeaderView` のみです。片方の Footer だけ guard 配線が漏れても受け入れテストが通ります。

**推奨修正**:  
Section/Root × Header/Footer を parameterized test で回し、公開値・論理親・lease・gateway 呼び出しを同じ基準で検証してください。

### [🟡 Minor] 「gateway 呼び出しなし」の観測範囲が接続経路では曖昧

**該当箇所**: [spec.md:9](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:9)、[spec.md:13](kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md:13)、[KsSettingsController.cs:196](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:196)

**問題点**:  
初回接続ではツリー検査前に `SetTheme` が呼ばれます。「gateway 呼び出しなし」を全呼び出しゼロと解釈すると現行接続経路と衝突しますが、構造・accessory 更新だけを指すなら適合できます。

**推奨修正**:  
「失敗した操作に由来する構造/accessory gateway 呼び出しは発生しない」のように対象を限定するか、初回接続でも全 gateway 呼び出し前に検査する契約だと明記してください。

## アクションプラン

1. バッチ失敗後の公開コレクションをロールバックするか決定する。
2. Root 再構築の releasing／同一 slot 継続規則を明文化する。
3. Add・Replace・Rebuild・初回接続を個別 Scenario とテストへ割り当てる。
4. Header/Footer のテストを対称化する。

上記1・2が設計判断を要するため、現時点では実装開始前に提案を修正して再レビューするのが妥当です。静的レビューのため、ビルド・テストは実行していません。


## 突き合わせ結果

| 指摘 | 採否 | 対応 |
|---|---|---|
| Major 1 (バッチ失敗後の公開コレクション状態が未規定) | **採用** | content 側と同一の契約 (ロールバックしない・native/対応表へ反映しない) を Requirement 本文と Non-Goals に明文化。Scenario 3.3 相当でコレクション残留も固定 |
| Major 2 (Root 再構築の「既存配置」定義と releasing が曖昧) | **採用** | 再構築の検査対象を「新ツリー内部の相互重複 + 再構築をまたいで残る配置 (root accessory)」に限定。同一 Section 継続を許容 Scenario で固定。null を経ない所有者間移動は本変更では規定しない (保証経路は null 解除) と明記 — RetireAccessoryView が論理所有を解かない現行設計への踏み込みはスコープ外 |
| Major 3 (構造経路ごとの Scenario / テスト不足) | **採用** | Replace 失敗・Root 再構築 (内部重複 / root accessory 衝突)・Host 接続時検出・後続要素のみ衝突の Scenario とテストタスク (3.4〜3.8) を追加 |
| Minor 1 (Footer 4プロパティの対称性) | **採用** | 4対象マトリクスを Requirement 本文に明記、テスト 3.1 を parameterized (TestCaseSource) 指定に変更 |
| Minor 2 (gateway 呼び出しなしの観測範囲) | **採用** | 「失敗した操作に由来する native gateway の呼び出し (構造・accessory・内容の更新)」に限定する文言へ修正。Host 接続 Scenario は例外送出のみを主張 |

未解決: なし (全指摘採用。設計判断 2 件は現行挙動と content 側前例の明文化で決着)

## 確認ラウンド (turn 2) と最終突き合わせ

相方の再判定: 解消 3 (Major 1 / Minor 1 / Minor 2)、未解消 2 (Major 2 / Major 3)、新規 Major 1 (再収束契約未規定)。残り 3 件も採用し反映:

| 指摘 | 採否 | 対応 |
|---|---|---|
| Major 2 残 (「解放される配置は衝突相手にならない」SHALL と移動未規定の矛盾) | **採用** | SHALL の保証を「同一 Section・同一 slot・同一 View の継続配置」に限定し、null を経ない所有者間移動は保証対象外 (現行挙動) と明記 |
| Major 3 残 (複数件 Replace の部分更新検出不能) | **採用** | Replace Scenario を「先頭非衝突・後続のみ衝突の複数件差し替え」へ改訂、テスト 3.5 に複数件 Replace 用コレクション追加を明記 |
| 新規 Major (再収束方法未規定) | **採用** | 「呼び出し元による Root 全体再構築 (再代入 / Reset) で再収束できる」を Requirement に追加、Scenario + テスト 3.5b を追加 (現行 RebuildRoot で成立する挙動の固定) |

最終状態: 全 8 指摘採用・未解決なし。以降のラウンドは回さず実装フェーズへ (実装後の code-review 系列で再び相方レビューが入る)。
