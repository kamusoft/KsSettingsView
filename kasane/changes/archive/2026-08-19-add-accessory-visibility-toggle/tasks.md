# Tasks: add-accessory-visibility-toggle

## 1. Native iOS

- [x] 1.1 `Section` に `isHeaderVisible` / `isFooterVisible` (既定 `true`) を追加し、手動 `==` / `hash(into:)` に参加させる (→ Requirement: Section の Header / Footer 表示トグル [ios])
- [x] 1.2 表示判定へ AND 合成を織り込む — `supplementaryModes` / `makeHeaderBoundaryItem` / `shouldShowFooter` (→ Requirement: 同上)
- [x] 1.3 内容不在判定を「nil または空 text」へ統一する — header 側の空 text 判定を追加 (→ Requirement: 内容不在の統一判定 [ios])
- [x] 1.4 存在判定を高さ解決に先行させる — header 不在時の `Section.headerHeight` / `Theme.headerHeight` による supplementary 生成を廃止し、逆契約を固定していた既存テスト (KsSettingsViewControllerTests の headerHeight40 + header nil 系) を新契約へ反転する (→ Requirement: 高さ解決は存在判定の後に適用する)
- [x] 1.5 `Section` を手動再構築している箇所を全列挙し (visible projection 構築・Store の各 Cell 操作・accessory 更新・SwiftUI Section modifiers 等)、トグル値の保持を織り込む (→ Requirement: トグルの独立性と保持 [ios])
- [x] 1.6 SwiftUI 宣言 DSL 対応 — `ksSection` 等の構築 API にトグル引数を追加し resolved `Section` へ転写、`DSLDiffCalculator` にトグル変化の検出を追加する (→ Requirement: 宣言 DSL のトグル指定と Store 経路との対称性 [ios])
- [x] 1.7 iOS ユニットテスト追加 — トグル AND / 等価性参加 / replaceSection 反映 / 空 text 統一 / 高さ先行の全組み合わせ / 独立性・Cell 操作をまたぐ保持・非表示中更新 / DSL 転写・再評価反映・Store/DSL 対称テスト (core/ADR-0018) (→ 全 Scenario [ios])

## 2. Native Android

- [x] 2.1 `Section` (data class) に `isHeaderVisible` / `isFooterVisible` (既定 `true`) を追加する (→ Requirement: Section の Header / Footer 表示トグル [android])
- [x] 2.2 `flatten` へ AND 合成と「null または空 text」の統一判定を織り込む (→ Requirement: 同上 / 内容不在の統一判定 [android])
- [x] 2.3 Compose 宣言 DSL 対応 — `DSLScope.Section` にトグル引数を追加し resolved `Section` へ転写、DSL 差分検出にトグル変化の検出を追加する (→ Requirement: 宣言 DSL のトグル指定と Store 経路との対称性 [android])
- [x] 2.4 Android ユニットテスト追加 — トグル AND / 等価性参加 / replaceSection 反映 / 空 text 非表示化 / 独立性・Cell 操作をまたぐ保持・非表示中更新 / DSL 転写・再評価反映・Store/DSL 対称テスト (core/ADR-0018) (→ 全 Scenario [android])

## 3. Bridge / Binding

- [x] 3.1 iOS `KsBridgeSection` に `isHeaderVisible` / `isFooterVisible` を追加し `makeSection` で伝搬する (→ Requirement: KsBridgeSection の可視トグル輸送)
- [x] 3.2 Android `KsBridgeSection` に同フィールドを追加し `makeSection` で伝搬する (→ Requirement: 同上)
- [x] 3.3 iOS Binding の `ApiDefinition.cs` へ 2 プロパティを追加し、生成される managed API を検証する (→ Requirement: 同上)
- [x] 3.4 Android Binding で生成される managed プロパティ名を確認・固定する (→ Requirement: 同上)
- [x] 3.5 Bridge の変換テスト追加 — DTO → core Section の伝搬と既定値 (→ 両 Scenario [maui-bridge])

## 4. MAUI facade

- [x] 4.1 facade `Section` に BindableProperty `IsHeaderVisible` / `IsFooterVisible` (既定 `true`) を追加する (→ Requirement: Section.IsHeaderVisible / IsFooterVisible の公開)
- [x] 4.2 PropertyChanged を `IsVisible` / `HeaderHeight` と同じ ReplaceSection バッチ分岐 (KsSettingsController) へ相乗りさせる (→ Requirement: 実行時トグル変更の反映経路)
- [x] 4.3 per-TFM gateway の Section → `KsBridgeSection` 変換に 2 フィールドを追加し、初期構築・挿入・置換の全経路で輸送されることを確認する (→ Requirement: KsBridgeSection の可視トグル輸送)
- [x] 4.4 net10.0 ユニットテスト追加 — fake gateway で初期構築時反映・実行時変更の置換配信・既定値・Cell identity と内容の保持 (既存 SectionVisibilityTests 系へ) (→ 全 Scenario [maui-core])

## 5. Samples (sample-parity)

- [x] 5.1 iOS Visibility デモへ Header 用・Footer 用の独立トグルのデモを追加する (→ Requirement: Visibility デモの Header / Footer トグル [samples-ios])
- [x] 5.2 Android Visibility デモへ同デモを追加する (→ Requirement: 同上 [samples-android])
- [x] 5.3 MAUI Visibility デモへ同デモを追加する (→ Requirement: 同上 [samples-maui])
- [x] 5.4 3 platform の文言・画面構成の一字一句一致を確認する (sample-parity 規約)

## 6. 検証

- [x] 6.1 全テストスイート実行 (ios / android / maui)
- [x] 6.2 3 platform でサンプルを起動し、トグル切り替えと対称化3件 (空 text 非表示 / header 不在 + Section.headerHeight 非生成 / header 不在 + Theme.headerHeight 非生成) をスクリーンショットで記録する
