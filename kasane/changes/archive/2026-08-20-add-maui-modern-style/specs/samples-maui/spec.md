# Delta: samples-maui (add-maui-modern-style)

## ADDED Requirements

### Requirement: SectionDecoration デモページ

MAUI サンプルに Section 装飾デモページを追加し、メニューから遷移できるようにする (SHALL)。ページは Classic / Modern の style 切替操作と、Section 装飾4属性の preset 切替操作を持つ (SHALL)。文言・画面構成 (Section / Cell 構成・preset の内容) は samples-ios / samples-android の SectionDecorationDemo と一字一句一致させる (sample-parity、cross/ADR-0016) (SHALL)。サンプル用の新しい色既定は追加せず、既存の MAUI サンプル共通 Theme を下地に使う (SHALL)。

#### Scenario: メニューからデモページへ遷移できる
- **GIVEN** サンプルアプリのメニュー画面
- **WHEN** Section 装飾デモの項目を選ぶ
- **THEN** デモページが表示される

#### Scenario: style 切替操作で表示が切り替わる
- **GIVEN** デモページ (native サンプルと同じ初期状態 — 初期 style は Modern)
- **WHEN** style 切替操作で Classic を選ぶ
- **THEN** 設定 list が Classic の見え方に切り替わる (Modern へ戻すと Modern の装飾に戻る)

#### Scenario: preset 切替で装飾が変わる
- **GIVEN** デモページ (Modern 表示)
- **WHEN** preset 切替操作で別の preset を選ぶ
- **THEN** 選んだ preset の4属性が適用され、装飾の見え方が変わる
