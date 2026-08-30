# Delta: samples-maui (add-accessory-visibility-toggle)

## ADDED Requirements

### Requirement: Visibility デモの Header / Footer トグル

既存の Visibility デモ画面に、Section Header / Footer の表示トグルを実演するデモを追加する (SHALL)。Header 用・Footer 用の切り替え操作は独立した2操作とし、それぞれ対象 Section の Header / Footer だけを切り替える (SHALL)。文言・画面構成は samples-ios / samples-android と一字一句一致させる (sample-parity)。

#### Scenario: デモ操作で Header / Footer の表示が独立に切り替わる
- **GIVEN** Visibility デモ画面の Header / Footer トグルのデモ Section
- **WHEN** Header 用・Footer 用の切り替え操作をそれぞれ行う
- **THEN** 対象 Section の Header / Footer がそれぞれ独立に表示・非表示を切り替える
