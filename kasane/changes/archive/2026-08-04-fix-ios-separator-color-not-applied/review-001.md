# レビュー結果: fix-ios-separator-color-not-applied (001 回目)

**日付**: 2026-08-04
**判定**: APPROVED

## サマリー

`separatorConfiguration(for:base:)` の先頭で `config.color = currentTheme.separatorColor` を設定する 1 行の修正であり、proposal で合意済みの設計と完全に一致する。可視性・インセット規則・公開 API には触れておらず、Non-Goals も守られている。iOS テストは全件 green (Core 83 + SwiftUI 68 + UI 401 = 552 件 / 0 failures、iPhone 17 / iOS 26.5)。

レビュー側で実行時経路を実測し (下記「独立検証」)、初期表示・`applyTheme` 後のいずれでも **実際に描画される separator view の色が Theme の色になる** ことを確認した。proposal が挙げていた「`reconfigureItems` による separator 再評価が UIKit 依存」というリスクは、iOS 26.5 では解消していることが実測で確認できた。

Critical / Major の指摘はない。

## 独立検証

一時的なプローブテスト (レビュー後に削除、ワーキングツリーは原状復帰済み) で、`UIWindow` に載せた `KsSettingsViewController` の view 階層から実描画の separator view を採取して色を実測した:

- 初期 bind 後: `_UICollectionViewListSeparatorView` × 4 すべてが `(0.9, 0.855, 0.725, 1)` = 指定した Theme の separatorColor
- `applyTheme(Theme(separatorColor: (0, 0.5, 0.25)))` 後: 同 4 view すべてが `(0, 0.5, 0.25, 1)` に追従

すなわち「`itemSeparatorHandler` が `reconfigureItems` 後に再評価される」ことが実測で裏付けられ、What Changes の 2 要件 (初期表示・実行時変更) はいずれも実挙動として満たされている。

## 指摘事項

### [🔵 Suggestion] 実行時 Theme 変更のテストは「描画への反映」までは固定していない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` の
`test_separatorConfiguration_applyTheme後は新Themeのセパレータ色に追従する` /
`test_separatorConfiguration_Store経由のTheme変更でセパレータ色が追従する`

**問題点**: 両テストは `applyTheme` 後に `separatorConfiguration(...)` を**直接呼んで**返り値を見ている。この経路は `currentTheme` を読むだけなので、実質的に「`applyTheme` が `currentTheme` を差し替えたか」の検証であり、tasks 3.2 の括弧書きが意図した「`reconfigureItems` による separator 再評価の UIKit 依存を固定する」効果は持たない (tasks 1.2 が警告していた代理値観測に近い構図)。UIKit 側が将来 `reconfigureItems` で separator を再評価しなくなった場合、これらのテストは green のまま実画面だけが追従しなくなる。

なお現時点の実挙動は上記「独立検証」で確認済みのため、**本 change の受け入れを妨げるものではない**。

**推奨修正**: 恒久テストで固定するなら view 階層を辿って実描画 separator の色を見る方法があるが、判別に private クラス名 (`_UICollectionViewListSeparatorView`) を使う必要があり脆い。恒久化はせず、tasks 4.1 の Simulator 視覚確認に「表示後に Theme を差し替えて separator 色が追従すること」を 1 手順足して証跡を残す方が費用対効果が高い (実行時挙動の完了判定は `concepts/cross/conventions/runtime-behavior-verification.md` の規律に沿う)。

### [🔵 Suggestion] 既定 Theme 時の dark appearance での見え方 (蒸留時のメモ推奨)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:621`

**問題点**: 修正前の separator 色は UIKit 既定 (dark appearance では暗色へ追従する動的色) だったが、修正後は `Theme.defaultSeparatorColor` = 固定の `#C8C7CC` 相当になる。proposal の Impact が述べる「差は 2/255 程度」は light appearance 限定の比較であり、dark appearance では動的追従が失われる点までは書かれていない。

ただし `Theme.defaultBackgroundColor` (白) / `cellBackgroundColor` の既定 `.white` が示すとおり、本ライブラリの Theme 既定値はもとより light 固定であり、list 下地が白のまま separator だけ暗色になる従来の方が不整合だった。したがって**本変更の方向が正しく、修正は不要**。

**推奨修正**: コード修正は不要。蒸留 (`concepts/core/styling/list-appearance.md` の「iOS は `Theme.separatorColor` を適用しない」記述の更新) の際に、「iOS の separator 色は Theme 由来の固定色であり system appearance には追従しない」ことを併せて書き残すと、利用者向けの誤解を防げる。

## 確認した観点

- **仕様充足**: proposal の What Changes / Non-Goals と diff が一致。公開 API 変更なし (`separatorConfiguration` は internal のまま)、可視性・インセット規則・`PickerListViewController`・Android / MAUI に変更なし
- **tasks の真正性**: グループ 1〜3 のチェックはいずれも実体を伴う。3.5 の全件回帰はレビュー側でも再実行して確認 (552 件 / 0 failures)。グループ 4 (Simulator 視覚確認) は未実施のままだが、オーケストレーター実施分のため指摘しない
- **足場の書き換え**: `git diff` 上、本 change のアーティファクトに実装中の書き換えなし (proposal.md / tasks.md は本 change 新規作成分)
- **設計の一貫性**: separator は list レベルの属性で `CellStyle` に対応フィールドがなく、`EffectiveStyle.separatorColor` も `theme.separatorColor` をそのまま持つだけのため、`currentTheme.separatorColor` の直接参照は `concepts/core/styling/style-resolution.md` の解決順と矛盾しない
- **適用位置**: 早期 `return config` を含む全経路で色が乗るよう、可視性・インセット判定より前に代入している。セクション情報が取れない防御パスでも Theme 色が適用される点は妥当
- **コメント規約 (L-001 / L-002)**: 今回追加した行 (本体 3 行・テスト doc コメント) は `scripts/comment-policy-lint.py` の禁止に 1 件も該当しない。同ファイルの既存 67 件は本 change 以前からの債務でありレビュー範囲外
- **既存契約の非破壊**: 罫線インセット規則テスト群、`PickerSelectionScreenTests` の separatorColor 系を含め全件 green

## アクションプラン

1. (任意) tasks 4.1 の Simulator 視覚確認に「実行時の Theme 差し替え後も separator 色が追従する」手順を追加し、スクリーンショット証跡を残す
2. (蒸留フェーズ) `concepts/core/styling/list-appearance.md` の iOS separator に関する記述を更新する際、system appearance に追従しない固定色である旨を併記する
