# Tasks: implement-modern-style

## 1. iOS: 箱描画機構のスパイクと Theme API

- [x] 1.1 スパイク: decoration の frame 補正 (Cell 行のみを覆う) と layoutAttributes 経由の値輸送が self-sizing・挿入/削除アニメーションと両立することをサンプルで検証する (→ design Decision 2 / Risks)
- [x] 1.2 `Theme` に4属性 (`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`) を optional で追加し、値等価性に参加させる (→ Requirement: Theme の Section 装飾4属性 [ios])
- [x] 1.3 style 別の既定値解決 (Modern: 承認モックの値 / Classic: 上下 0、border 実効 0・透明) を実装する (→ 同上)

## 2. iOS: Modern 装飾の実装

- [x] 2.1 `.insetGrouped` を廃し、Modern の sectionProvider で `section.contentInsets` に margin を反映する (→ Requirement: Modern の Section 箱描画 [ios]、ios/ADR-0003)
- [x] 2.2 layout subclass + decoration view で Cell 行範囲のみの角丸背景・ボーダーを描画する。Header / Footer / Root H/F は箱に含めない (→ 同上)
- [x] 2.3 Theme 変更・SettingsRootDiff・style 切替で装飾が再評価されることを保証する (→ Requirement: 実行時の Theme 変更 / 構造変更後の追従 / style 切替の整合 [ios])
- [x] 2.4 `separatorConfiguration` に Modern 分岐 (箱の上下端なし・中間のみ・icon 非依存) を追加する (→ Requirement: Modern の separator 規則 [ios])
- [x] 2.5 Classic の sectionProvider に `sectionMargin` 上下成分を反映する (→ Requirement: Classic への sectionMargin 上下適用 [ios])
- [x] 2.6 合成契約 (ボーダー最前面・先頭/末尾 Cell と押下背景の箱形状 clip・CellStyle.backgroundColor の前面描画) を実装する (→ Requirement: 箱と Cell 背景の合成 [ios]、design Decision 7)

## 3. Android: decoration の Theme 駆動化

- [x] 3.1 `Theme` に4属性を optional で追加し、値等価性 (data class) に参加させる。style 別の既定値解決を実装する (→ Requirement: Theme の Section 装飾4属性 [android])
- [x] 3.2 `ModernSectionDecoration` のハードコード寸法を Theme 解決値に置換し、ボーダー描画 (onDraw) を追加する (→ Requirement: Modern の Section 箱描画 [android])
- [x] 3.3 箱と inset の対象から Section Header / Footer 行を除外する (→ 同上)
- [x] 3.4 セクション内の中間 separator を `onDrawOver` で描画する (箱の上下端なし・icon 非依存・1物理 pixel) (→ Requirement: Modern の separator 規則 [android])
- [x] 3.5 `ClassicSectionDecoration` に `sectionMargin` 上下成分の offset を追加する (→ Requirement: Classic への sectionMargin 上下適用 [android])
- [x] 3.6 Theme 変更・style 切替 (Compose ラッパ経由含む) で装飾が再評価されることを保証する (→ Requirement: 実行時の Theme 変更 / style 切替の整合 [android])
- [x] 3.7 合成契約 (ボーダー最前面・箱形状 clip) と、オフスクリーン Section 端の箱延長 (可視 child のみ集計の欠陥修正) を実装する (→ Requirement: 箱と Cell 背景の合成 / 長い Section の箱端描画 [android]、design Decision 7・8)

## 4. サンプル

- [x] 4.1 iOS サンプルに style 切替 + 装飾プリセット切替のデモを追加する (→ Requirement: style と Section 装飾のデモ [samples-ios])
- [x] 4.2 Android サンプルに同デモを追加する (→ Requirement: style と Section 装飾のデモ [samples-android])
- [x] 4.3 iOS / Android デモの文言・Section / Cell 構成・プリセット値を突き合わせ、一字一句・構成一致を確認する (→ concepts/cross/conventions/sample-parity.md)

## 5. テスト

- [x] 5.1 iOS: 4属性の既定値解決・値等価性・style 切替・separator 規則 (先頭/末尾/中間/単一 Cell)・Header/Footer 箱外・margin の Section 単位適用・可視 Cell 0件・負値正規化・合成契約 (ボーダー可視/角丸 clip/押下) のテストを追加する。既存の `appearance(for: .modern) == .insetGrouped` テストを新契約へ置換する (→ settings-view-ios-ui 全 Requirement)
- [x] 5.2 Android: 4属性の既定値解決・値等価性 (PaddingValues 委譲)・箱範囲 (Header/Footer 除外)・separator 描画 (背景色付き Cell 含む)・Classic 上下 margin・margin の Section 単位適用・可視 Cell 0件・負値正規化・合成契約・viewport より長い Section のスクロール (偽の箱端が出ない) のテストを追加する (→ settings-view-android-ui 全 Requirement)

## 6. 視覚照合

- [x] 6.1 iOS サンプルのスクリーンショットを承認モック (ui/mock/approved.png) と照合し、乖離を潰す
- [x] 6.2 Android サンプルのスクリーンショットを承認モックと照合し、乖離を潰す (既定値は platform 既定のため寸法差は許容、構造 [箱・Header/Footer 外・separator] の一致を確認)
