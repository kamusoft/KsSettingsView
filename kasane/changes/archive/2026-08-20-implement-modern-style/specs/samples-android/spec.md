# Delta: samples-android (implement-modern-style)

## ADDED Requirements

### Requirement: style と Section 装飾のデモ

Android サンプルアプリは、Classic / Modern を実行時に切り替えられるデモを提供する (SHALL)。デモは Header / Footer 付き Section・icon 付き Cell・単一 Cell の Section を含み、Modern の箱描画と separator 規則を目視確認できる (SHALL)。Theme の Section 装飾4属性を変更した表示を確認する手段 (プリセット切替等) を提供する (SHALL)。

#### Scenario: style を切り替えて見比べる
- **GIVEN** デモ画面を表示している
- **WHEN** style 切替操作を行う
- **THEN** 同じ設定内容が Classic / Modern の装飾で切り替わる

#### Scenario: 4属性の変更を確認する
- **GIVEN** Modern で表示中のデモ画面
- **WHEN** Section 装飾のプリセット (margin / radius / border の異なる組) を切り替える
- **THEN** 箱の余白・角丸・ボーダーが切り替わって表示される
