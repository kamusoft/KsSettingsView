# 0022: View 配置プロパティの多重配置検査は値確定前 guard で行い、失敗時は旧状態を保全する

- status: accepted
- date: 2026-08-13
- change: align-maui-accessory-placement-guard (先行: add-maui-custom-cell)

## Context

facade の View 配置プロパティ (`CustomCell.Content`・`Section.HeaderView` / `FooterView`・`SettingsView.RootHeaderView` / `RootFooterView`) は同一 View インスタンスの多重配置を `InvalidOperationException` で拒否するが、検査のタイミングと失敗後の状態は当初プロパティごとに異なっていた。値確定後に検査すると、BindableProperty の値確定と propertyChanged の所有付け替えが先に走り、失敗時に公開値・論理所有・native 表示が分離して元へ戻せない。構造変更バッチ (Section / Cell の追加・差し替え・Root 再構築) でも、途中の 1 件で例外にすると native と対応表が部分更新のまま残る。

## Decision

配置検査の規律を全 View 配置プロパティで統一する:

1. **値確定前検査**: 配置済み所有者のプロパティ設定は `validateValue` から controller の guard (`IKsCellContentGuard` / `IKsAccessoryViewGuard`) へ尋ね、失敗時は値を確定させない。公開値・論理所有・platform 実体・native 表示のいずれも動かない
2. **例外は送出で表す**: validateValue の false 返却は使わない (BindableProperty が `ArgumentException` へ変換し、公開契約の例外型が壊れる)
3. **バッチは native 前に全件検査**: 構造変更バッチ内の相互重複と既存配置との重複は、native gateway を 1 件も呼ぶ前に検査する。どの位置の要素が衝突しても部分更新を残さない
4. **公開コレクションはロールバックしない**: 失敗後の `Root` / `Section.Cells` は呼び出し元の操作後のまま残り、native・対応表には反映されない。回復経路は呼び出し元による Root 全体再構築 (再代入 / Reset)
5. **guard の参照は弱く持つ**: 所有者 (model) 側が保持する guard (controller) への参照は `WeakReference` にする。model→controller 参照を弱参照とする既存規律 (`KsWeakPropertySubscription`) と同軸
6. 未参加の所有者には guard が差し込まれず検査相手がいないため、多重配置は変換経路への参加時点 (Host 未接続で参加した場合は Host 接続時) まで持ち越される

## Consequences

- 失敗した設定操作は公開値と表示が食い違わず、利用者は例外を握りつぶしても壊れた状態を持ち越さない
- バッチ失敗後は公開コレクションと表示が意図的に分離する (非ロールバックの帰結)。再収束は全体再構築で保証される
- 新しい View 配置プロパティを追加するときは、guard の差し込み (登録時) と取り外し (登録解除時)、および取り外しを固定する回帰テストまでが一組になる
- (出典: 実装結果) guard を強参照で配線すると外部保持の model 経由で SettingsView がリークする。`CustomCell.ContentGuard` (先行実装) には強参照のままのリークが残っており、別変更で追随予定

## Alternatives Considered

- **A: 値確定後の検査 (accessory の当初実装)** — 却下。propertyChanged 前の `ReassignIfFree` が旧 View の論理所有を先に解き、例外後に公開値・所有・表示が三様に分離する。元へ戻す手段がない
- **B: validateValue の false 返却** — 却下。BindableProperty 側が `ArgumentException` に変換し、多重配置が公開契約どおりの例外型で観測できない
- **C: 失敗時に公開コレクションをロールバック** — 却下。コレクション変更イベントの処理中に再入的な逆操作が必要になり、購読者から見た通知列が壊れる。content 側の既存契約 (非ロールバック) とも不整合
- **D: guard を強参照で保持** — 却下。外部 (ViewModel 等) が Section / Cell を保持したまま SettingsView を破棄すると、guard → controller → SettingsView の経路で回収されない (leak テストで実測)
