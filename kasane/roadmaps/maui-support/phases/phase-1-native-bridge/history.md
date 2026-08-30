# phase-1-native-bridge 議論履歴

## 2026-08-04: Bridge の位置づけ (DSL 方式 / Store 方式の二分法への収め方)

- 背景: 旧 add-maui-bridge は Store を介さず直接 `controller.applyDiff` を呼ぶ設計で、現行 `declarative-ui-bridge.md` の「両方式は `SettingsRootStore → Native Host` へ収束する」保証を迂回する第三経路になる問題を確認。
- 選択肢: A) Bridge 内部所有 Store を持つ DSL 方式の類型として位置づけ、Bridge API を Store 公開操作へ変換 / B) 旧案のまま直接 applyDiff / C) Native Store handle を C# に公開 (Store 方式)。
- 判断軸: 現行契約との整合、Store 保証 (replaceCells・Theme 分離・状態復元) の獲得、Bridge API 表面積、実装コスト、将来拡張余地。
- 採用: **A案**。B は保証の再実装で実質 Store の再発明になるため却下。C は API 表面積が大きく、高頻度更新ユースケースが出た時の将来拡張として保留。
- ADR: [maui/ADR-0001](../../../../decisions/maui/0001-maui-bridge-dsl-variant-internal-store.md) (proposed) を起票。

## 2026-08-04: DTO 契約の現行化 (Bridge API の形)

- 背景: 旧案は union 型 `KsSettingsRootDiffDTO` (Diff 全10ケース) + `applyDiff` 1本。ADR-0001 で Bridge は Store 操作への変換と決まったため再検討。iOS Store の公開操作11個を実査し、`replaceCells` バッチが Android のみの非対称契約であることを確認。
- 選択肢: A) union DTO 継続 + replaceCells ケース追加 / B) Store 公開操作と 1:1 のメソッド群。
- 判断軸: interop 境界での表現 (`@objc` は enum associated value 不可)、変換層の薄さ、API 表面積。
- 採用: **B案** (12メソッド)。さらに `replaceCells` は「Bridge 内部で iOS だけループ」ではなく **iOS Store 本体へ追加して対称化** (オーナー指示: ループで誤魔化す意味は無い、additive で破壊的変更ではない)。
- ADR: [maui/ADR-0002](../../../../decisions/maui/0002-bridge-api-per-store-operation.md) (proposed) を起票。

## 2026-08-04: ユーザー操作通知の集約方式

- 背景: Native → C# の操作通知経路。interop callback は GC ハンドルの寿命管理がコストになる。
- 選択肢: A) 単一 delegate/listener 集約 (旧案) / B) Cell 単位の callback 登録 / C) 単一メソッド + シリアライズ済みイベント。
- 判断軸: interop ハンドル寿命管理、型安全性、C# 側配送、Cell 種別追加時の影響。
- 採用: **A案**。B はハンドル数が Cell 数に比例しリークテスト対象が爆発、C は型安全性を失うため却下。delegate が Cell フェーズごとに伸びるのは addXxxCell と同じ additive リズムとして許容。
- ADR: [maui/ADR-0003](../../../../decisions/maui/0003-single-interaction-delegate.md) (proposed) を起票。

## 2026-08-04: EntryCell 高頻度更新向け直行パス + debounce の要否

- 背景: 旧案は `updateCellValue` 直行パス + 200ms debounce を先行実装する計画だった。懸念は「Native 入力 → C# 更新 → replaceCell で Native へ戻る」エコーの頻度。
- 選択肢: A) 特別パスなし + CellBase の同値チェックでエコー抑止 / B) 旧案どおり直行パス + debounce。
- 判断軸: ADR-0002 (Store 操作 1:1) との整合、エコー抑止の根本性、値の即時性、実装量。
- 採用: **A案**。updateCellValue は Store 公開操作に存在せず例外経路になる。入力は人間のタイプ速度で interop コストは無視でき、SwiftUI/Compose の TwoWay 経路も同じループを毎キーストロークで回して問題化していない。debounce は取りこぼし窓を作るため却下。同値チェックの実装は phase-2 へ引き継ぎ。ADR は起票せず (ADR-0002 の帰結の範囲であり、将来の追加も additive で可逆なため)。

## 2026-08-04: Theme / CellStyle の C# 公開型 (styling 契約との整合)

- 背景: `style-resolution.md` は「KsColor / KsFont のような中間表現を置かず各 platform の型と慣例を直接公開する」と定める。MAUI は単一 C# API で両 OS を相手にするため型の選択が必要。setTheme 自体は ADR-0002 の12メソッドに含まれ Store applyTheme への素通しで契約を満たす。
- 選択肢: A) MAUI 慣例型 (Microsoft.Maui.Graphics.Color 等) を公開し interop 境界でプリミティブへ marshalling / B) 共通論理型 (KsColor 的) を導入 / C) platform 別 Theme を条件コンパイルで公開。
- 判断軸: styling 契約との整合 (規則の趣旨 = platform 慣例の直接公開)、MAUI 利用者の体験 (XAML で書けるか)、interop 実装。
- 採用: **A案**。「MAUI platform の慣例 = MAUI 型」であり規則の趣旨の MAUI への適用。interop DTO は非公開の輸送表現で禁止対象外。B は契約に正面から抵触、C は `#if` 分岐だらけで XAML から書けないため却下。platform 固有項目は接頭辞付き nullable。
- ADR: [maui/ADR-0004](../../../../decisions/maui/0004-maui-idiomatic-types-for-styling.md) (proposed) を起票。

## 2026-08-04: Binding csproj の .NET 10 構成

- 背景: 旧案の Binding 形式 (Native Library Interop: XcodeProject / AndroidGradleProject) は標準スキルの方式そのままで代替比較の分岐なし。変わったのはターゲット net9.0 → net10.0 のみ。
- 採用: 形式は踏襲。net10.0 の toolchain 検証は議論で結論が出ない性質のため phase-1 実装の先頭 spike タスク (最小スケルトンのビルド疎通) とする。問題が出たら agenda に差し戻し。ADR は起票せず (小さな決定)。

## 2026-08-04: LabelCell 疎通範囲 (残り Cell のインターフェース先行定義の是非)

- 背景: 旧案は「LabelCell のみ実体実装、他13種はインターフェース定義のみ先行」。縦1本の疎通方針は妥当だが、先行定義の是非を再検討。
- 採用: LabelCell のみ実装は踏襲、**先行インターフェース定義は廃止**。理由: CustomCell は phase-5 で全面再検討、Picker 系は phase-4 の選択面論点でシグネチャが動くため推測定義は腐る。ADR-0002 (メソッド 1:1) / ADR-0003 (delegate additive) の「実装フェーズで足す」リズムとも一貫しない。全体像の見通しは spec とロードマップが担う。ADR は起票せず (ADR-0002/0003 の帰結の範囲)。
- これで phase-1 の論点は全て解消。次は ksn-propose での変更提案化。