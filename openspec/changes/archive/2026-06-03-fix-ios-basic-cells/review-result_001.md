# レビュー結果 - fix-ios-basic-cells

**レビュー日時**: 2026年06月03日  
**レビュワー**: sdd-reviewer  
**変更提案ID**: fix-ios-basic-cells

## サマリー

iOS 基本 Cell 7 種の実機レビューで判明した 4 つの UI 不具合（① ヘッダー固定・空フッター帯、② CheckboxCell のチェック UI、③ RadioCell の横スライド消失、④ SimpleCheckCell の左側チェック）を是正する変更。spec delta（`cell-types-basic` / `settings-view-ios-ui`）と design.md の Decision 1〜4 を実装と照合した結果、**いずれも仕様・設計通りに実装されており、オリジナル `AiForms.Maui.SettingsView` の挙動にも忠実**である。

検証実績:
- ビルド: `xcodebuild build -scheme KsSettingsViewUI -destination 'iOS Simulator,iPhone 17'` → **BUILD SUCCEEDED**
- テスト: `xcodebuild test -scheme KsSettingsView-Package -only-testing:KsSettingsViewUITests` → **101 tests, 0 failures**（TEST SUCCEEDED）

主要な所見:
- ① ヘッダー非固定（`pinToVisibleBounds = false`）・footer 出し分け（`supplementaryModes`）は design Decision 1 通り。`static` ヘルパ化されテスト可能。
- ② `KsCheckBoxView` は座標比（22/52→38/68→76/30）・線幅（辺長/10）・CornerRadius 3 / BorderWidth 2 までオリジナル `CheckBox.Draw` を忠実に再現。
- ③④ `KsCheckmarkAccessoryView` の alpha フェード + 常設 customView accessory で、横スライド回避と「初回即時 / 変化時 animate」のチラつき制御が両立。RadioCell / SimpleCheckCell で共通ヘルパ化。
- retain cycle なし（tapHandler は `self` を捕捉せず）。`prepareForReuse` も適切。
- openspec タスクは 7.2（アーカイブ時反映）を除き全完了。7.2 は意図的にアーカイブ工程に残す設計で問題なし。

Critical / Major 指摘はなし。下記は軽微な改善提案（Minor / Suggestion）のみ。

**判定: `APPROVED`**

---

## 指摘事項

#### [🟡 Minor] `traitCollectionDidChange(_:)` は iOS 17 で deprecated

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCheckBoxView.swift:76`

**問題点**:
ダークモード追従のため `traitCollectionDidChange(_:)` をオーバーライドしているが、本 API は iOS 17 で deprecated（`registerForTraitChanges(_:handler:)` が推奨）。デプロイメントターゲットが iOS 16 のため現状は機能し、ビルド警告も `#available(iOS 13.0, *)` ガードで実害はないが、将来的に deprecation 警告の温床になりうる。`#available(iOS 13.0, *)` ガード自体も iOS 16 ターゲットでは恒真で不要。

**推奨修正**:
将来 iOS 17+ へ最低ターゲットを上げる際に `registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (view: KsCheckBoxView, _) in ... }` へ移行する。今回は対象外（このままで動作する）。冗長な `if #available(iOS 13.0, *)` は削除してよい。

#### [🔵 Suggestion] unchecked 時の枠線色が accent 固定（ダークモード視認性）

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCheckBoxView.swift:62, 80`

**問題点**:
design.md の Risks に「未チェック枠はセパレータ相当のシステムカラーを使うなど Theme と整合させる」という open question があるが、実装は checked / unchecked いずれも枠線を `accentColor` 固定にしている。ただしオリジナル `CheckboxCellView.cs`（`ChangeCheckColor` で `BorderColor = accent`）も枠線は常に accent であり、**オリジナル忠実性を優先した本実装は妥当**。spec も枠色を規定していないため要件違反ではない。

**推奨修正**:
現状維持で問題なし。将来ダークモードで未チェック枠が見えづらいという実機フィードバックが出た場合のみ、未チェック時の枠を `.separator` 相当へ切り替える検討を行う（design の open question として記録済み）。

#### [🔵 Suggestion] テスト用 `accessoryType` 拡張と SDK プロパティの名称衝突

**該当箇所**: `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift:334-345`

**問題点**:
テスト内 `private extension UICellAccessory { var accessoryType: KsTestAccessoryKind }` は、iOS 17+ SDK で公開された実在プロパティ `UICellAccessory.accessoryType`（`UICellAccessory.AccessoryType` を返す）と同名。production 側（`CheckboxCellView`/`RadioCellView`/`SimpleCheckCellView` の `_hasTrailing*Accessory`）は SDK の本物の `accessoryType` を使い `customView === checkBoxView` の同一性判定を行っており（ビルド・テストとも成功で確認済み）正しく機能している。一方テスト側ヘルパは文字列 `String(describing:)` ベースの簡易判定で、production と判定ロジックが二重化している。混乱の元になりうる。

**推奨修正**:
テストの簡易ヘルパ（`KsTestAccessoryKind`）は他 Cell（Command の disclosure 判定など）でのみ使い、CheckBox/Checkmark の常設判定は production アクセサ（`_hasTrailingCheckBoxAccessory` / `_hasTrailingCheckmarkAccessory`）に一本化されている点をコメントで明示するとよい。動作上の問題はないため任意。

---

## アクションプラン

優先度順（いずれも任意・ブロッカーなし）:

1. （任意）iOS 17+ へ最低ターゲットを引き上げる際に `KsCheckBoxView` の trait 監視を `registerForTraitChanges` へ移行し、不要な `if #available(iOS 13.0, *)` を削除する。
2. （任意）ダークモードでの未チェック枠視認性は実機フィードバック待ち。design の open question として継続記録。
3. （任意）テストの accessoryType ヘルパと production アクセサの役割分担をコメントで明示。
4. アーカイブ時に tasks.md 7.2（本体 spec への MODIFIED 反映）を実施する。

---

## 判定結果

**ステータス**: `APPROVED`

- ビルド成功 / 全 101 テスト成功。
- spec delta（`cell-types-basic` の CheckboxCell / RadioCell / SimpleCheckCell、`settings-view-ios-ui` の classic スタイル）と実装が一致。
- design.md Decision 1〜4 を忠実に実装。オリジナル `AiForms.Maui.SettingsView`（`CheckboxCellView.cs` / `SimpleCheckCellView.cs`）の挙動とも整合。
- メモリリーク・retain cycle なし。`prepareForReuse` による状態リセットと「初回即時 / 変化時フェード」のチラつき制御も妥当。
- Android 側・公開 API（Cell 構造 / DSL / コンストラクタ）への破壊的変更なし。
- Critical / Major 指摘なし。残る指摘は Minor / Suggestion のみで、いずれもマージ後に任意対応可能。

orchestrator は本ステータス（APPROVED）に基づき次工程（検証 / アーカイブ）へ進めてよい。
