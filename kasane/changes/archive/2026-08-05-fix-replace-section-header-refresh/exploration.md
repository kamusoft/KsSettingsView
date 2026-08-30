# Exploration: fix-replace-section-header-refresh

- 起票日: 2026-08-04
- 起票経緯: add-maui-native-bridge 実装中 (グループ3・iOS Bridge) にワーカーが発見した既存バグの簡易起票
- 探索完了日: 2026-08-05 (方針確定・実装ハンドオフ)

## 課題

iOS で `replaceSection` により header text を変更しても、実描画の Section header (supplementary view の UILabel) が更新されない。

- `applyReplaceSection` は `applyFullSnapshot` へフォールバックするが、Section identity が同じままだと diffable data source が supplementary view を作り直さないため、header 表示が古いまま残る (1秒待っても更新されないことを実測確認済み)
- Store の model 側は正しく更新される (表示だけが取りこぼす)
- Bridge 由来ではなく、`SettingsRootStore` を直接使う SwiftUI 利用者にも当たる既存挙動
- 既存テスト `ApplyDiffTests.test_applyDiff_replaceSection` は identity 保持の慣習 (`Section(id: s1.id, ...)`) で書かれており、item 数の検証のみでこの表示の取りこぼしを検出していない
- add-maui-native-bridge の Bridge 契約テスト (`KsBridgeOperationContractTests`) では、誤った挙動を固定しないよう header text を変えないケースで検証している

### 探索での追加発見 (2026-08-05)

この取りこぼしは `replaceSection` 固有ではなく **`.full` diff でも同じ**。`.full` で同一 Section ID のまま header text だけ変えた場合も supplementary は再構成されない。根っこは `applyFullSnapshot` が「identity 不変 + header/footer 変化」を reload 対象として扱っていないこと (`KsSettingsViewController.swift` の `applyFullSnapshot`)。

## 検討した選択肢 (却下案と理由を含む)

- **A案 (採用): `applyFullSnapshot` 内で旧/新 visible projection を突き合わせ、header/footer が変化した同一 ID Section を `snapshot.reloadSections` に積む**
  - `replaceSection` と `.full` の両経路が一度に直る。条件付き reload なので header 不変ケースに副作用 (無駄な cell 再構成・ちらつき) が出ない
- B案 (却下): `applyReplaceSection` だけ対象 Section を無条件 `reloadSections`
  - 最も単純だが `.full` 経路の同種取りこぼしが残り、header 不変でも section 内全 cell が reload されちらつく
- C案 (却下): apply 後に `invalidateSupplementaryElements(ofKind:at:)` で supplementary のみ再要求
  - cell に触れない最軽量だが、indexPath 計算 + kind 列挙で経路が増え、UIKit の再要求挙動への依存が強い

## 決定事項

- 修正方針は A案 (ユーザー承認済み・2026-08-05)
- 「header text を変える replaceSection が表示へ反映される」テストを追加して固定する
- 実機検証は **iPhone 17 / iOS 26.5 シミュレータ** で行う (ユーザー指定)
- Android 側の対称挙動の確認は実装フェーズの検証項目として残す

## 実装フェーズの追加判断・既知の制限 (2026-08-05、review-001 Minor 対応で記録)

- **view 形式 accessory の扱い**: `SectionAccessory.==` は `.view` ケースをケース一致のみで等価と扱う
  (中身は比較不能) ため、差分検出では `.view` の中身変化を捉えられない。`replaceSection` 経由に限り
  「`.view` が絡む場合は対象 Section を強制 reload」する補助条件を付けた (置換の意図が明示されているため)
- **既知の制限 1**: `.full` diff で同一 Section ID のまま `.view` header/footer の中身だけを差し替えた場合は
  修正後も表示に反映されない (等価比較不能のため検出せず、無条件 reload の副作用も避ける判断)。
  `.view` の中身更新は `updateAccessory` 経由が正
- **既知の制限 2**: `.view` accessory を持つ Section への `replaceSection` は、内容が実質不変でも
  対象 Section の全 Cell を reload する (編集中 Cell の first responder が失われ得る)。
  Section 全体置換という操作の意味論上、許容する

## ADR 候補

- 未起票 (局所的なバグ修正であり、覆すコスト高 / 境界越え / 将来制約のいずれにも該当しないため ADR は起こさない)

## 未決の論点

- なし (Android 対称性の確認は実装フェーズの検証項目)

## UI 素材

- なし (既存描画のバグ修正のため mock 不要。検証はシミュレータ実描画で行う)

## 変更級の推奨: S (確定)

バグ修正・局所的。触るのは `applyFullSnapshot` 1 箇所 + テスト追加。ただし diffable data source の supplementary view 更新経路に触るため、既存の表示テストでの回帰確認は必須。
