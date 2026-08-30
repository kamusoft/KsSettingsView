# Tasks: align-maui-accessory-placement-guard

## 1. 値確定前ガードの導入

- [x] 1.1 accessory 用の内部 guard の口を用意する (`IKsCellContentGuard` と同型。Section 用は置き場所 target 付き、Root 用は SettingsView の controller 直結でよい)。検査失敗は `InvalidOperationException` の送出で表す — validateValue の false 返却は BindableProperty が `ArgumentException` に変換するため使わない (CustomCell.ContentProperty の前例に従う) (→ Requirement: 同一 View インスタンスの多重配置は例外になる)
- [x] 1.2 `Section.HeaderViewProperty` / `FooterViewProperty` に `validateValue` を追加し、guard 未設定 (未参加) 時は素通しする (→ Scenario: 失敗した差し替えでは公開値と旧状態が一切動かない・未参加の所有者に持ち越された重複は参加時点で弾かれる)
- [x] 1.3 `SettingsView.RootHeaderViewProperty` / `RootFooterViewProperty` に `validateValue` を追加する (`_controller` 未初期化タイミングに備え null 条件で呼ぶ) (→ Scenario: Root accessory の失敗した差し替えでも同様に旧状態が残る)
- [x] 1.4 controller に guard 実装を追加し (`EnsureAccessoryViewIsNotPlaced` へ委譲)、`RegisterSection` で差し込み・`UnregisterSection` / `ClearRegistrations` で外す (custom.ContentGuard の配線と対称) (→ 同上)

## 2. バッチ検査の対称化

- [x] 2.1 `EnsureSectionsAreNotPlaced` にバッチ内 accessory View の数えあげ (`HashSet<View>` + `AddSeenView`) を追加し、`EnsureCellsAreNotPlaced` の in-batch seen と対称にする。Cell の Content との交差もバッチ内で数える (→ Scenario: 追加バッチ内で accessory View が重複すると 1 件も入れないまま例外)

## 3. 回帰テスト (AccessoryViewTests へ CustomCellContentTests の鏡像を追加)

- [x] 3.1 失敗した accessory 差し替えの原子性: Section / Root × Header / Footer の **4対象を parameterized test** (`TestCaseSource(Targets)` の既存流儀) で回し、例外後に公開値・論理親・lease・表示が旧状態のまま・`gateway.Calls` が空・相手側配置も無傷であることを検証 (`AFailedContentReplacementKeepsTheCurrentContent` の鏡像) (→ Scenario: 失敗した差し替えでは公開値と旧状態が一切動かない)
- [x] 3.2 失敗した `RootHeaderView` 差し替え (CustomCell.Content の View を設定): 同上の検証 (`AFailedContentReplacementWithAnAccessoryViewKeepsTheCurrentContent` の鏡像) (→ Scenario: Root accessory の失敗した差し替えでも同様に旧状態が残る)
- [x] 3.3 同じ `HeaderView` を持つ 2 Section の一括追加 (RangeAdd): 例外後に `gateway.Calls` が空・対応表未登録・既存配置無傷。公開コレクションはロールバックされないことも固定 (`AddingCellsThatShareAContentViewThrowsBeforeAnyInsert` の鏡像) (→ Scenario: 追加バッチ内で accessory View が重複すると 1 件も入れないまま例外)
- [x] 3.4 一括追加の後続要素だけが既存配置と衝突するケース: 先頭の衝突しない Section も native・対応表へ入らないことを検証 (→ Scenario: 追加バッチの後続要素だけが既存配置と衝突しても先頭要素は入らない)
- [x] 3.5 複数件 Replace (先頭非衝突・後続のみ既配置 View と衝突) のケース: 例外後に先頭の差し替えも未適用・表示・対応表が差し替え前のまま・`gateway.Calls` が空 (複数件 Replace イベントを発行するテスト用コレクションが無ければ RangeAddCollection の流儀で追加する) (→ Scenario: 差し替えバッチの重複でも同様に 1 件も適用されない)
- [x] 3.5b バッチ失敗後の再収束: 失敗で分離した `Root` から衝突要素を除いた新コレクションを再代入し、残した Section が表示・登録されること (→ Scenario: 失敗したバッチは Root の全体再構築で再収束できる)
- [x] 3.6 Root 再構築: ①新ツリー内の重複、②root accessory との衝突の両方で、現在の木・実体が無傷のまま例外 (`RebuildingWithADuplicateContentThrowsWithoutTouchingTheCurrentTree` の鏡像) (→ Scenario: Root 再構築内の重複は現在の木に触れないまま例外・Root 再構築の新ツリーが root accessory と衝突すると現在の木に触れないまま例外)
- [x] 3.7 同一 Section を含む Root 再構築の非退行: 継続配置が例外にならず表示が保たれる (→ Scenario: 同一 Section を含む Root 再構築は引き続き成立する)
- [x] 3.8 未参加 Section に既配置 View を設定 → `Root` へ追加で例外 (`gateway.Calls` 空・既存配置無傷)、および未接続のまま構築した重複ツリーが Host 接続時に例外になること (→ Scenario: 未参加の所有者に持ち越された重複は参加時点で弾かれる・未接続のまま構築された重複は Host 接続時に弾かれる)
- [x] 3.9 成功経路の非退行: 同一 slot の View 差し替え・null 解除後の再利用・Host 切断中の検査 (`DuplicatePlacementIsDetectedWithoutAGateway`) が引き続き通ることを確認する (→ Scenario: null 解除後の再利用は許容される)

## 4. 全体確認

- [x] 4.1 maui テストスイート全通し — ベースライン 400 件 (2026-08-13 時点・全合格) に対する退行ゼロ + 追加テスト全合格
