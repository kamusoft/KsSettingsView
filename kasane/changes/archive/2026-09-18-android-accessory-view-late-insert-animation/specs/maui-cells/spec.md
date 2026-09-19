# Delta Spec: maui-cells (android-accessory-view-late-insert-animation)

## ADDED Requirements

### Requirement: CustomCell の Content は Native Host の最初の表示に含まれる

SettingsView が表示される時点で CustomCell に設定されている Content は、Native Host が view 階層へ取り付けられた後の最初の表示に含まれなければならない (SHALL)。取り付け後に Content が後から行へ入る形で現れてはならない (SHALL NOT)。

この保証は、初回の表示と、Handler の切断・再接続の後の表示の両方で成り立つ (SHALL)。Content は Native Host を生成する時点で native の設定ツリーの現在状態に含まれている (SHALL)。取り付けの通知 (`Loaded`) を待って Content を配信してはならない (SHALL NOT)。

再接続時に複数の CustomCell の Content を届けるときは、1 回のバッチで届ける (SHALL)。

埋め込み view の安定性 (Content のインスタンスが変わらないかぎり埋め込み view は差し替わらない) と、実行時の Content の差し替えの反映は、既存の Requirement「内容変化の live 反映と Content の差し替え」のとおりであり、変わらない。

#### Scenario: 初回の表示で Content が最初から行に入っている
- **GIVEN** Content を設定した CustomCell を含む SettingsView
- **WHEN** Handler が接続され Native Host が生成される
- **THEN** Native Host の生成時点で、native の設定ツリーの現在状態はその Cell の埋め込み view を持つ

#### Scenario: 再接続でも Content は Native Host の生成までに届く
- **GIVEN** Content を表示した後に Handler が切断された、複数の CustomCell を含む SettingsView
- **WHEN** Handler が再接続され Native Host が生成される
- **THEN** すべての CustomCell の埋め込み view が Native Host の生成時点で native の設定ツリーの現在状態に含まれ、その配信は 1 回のバッチで行われている

#### Scenario: 取り付けの通知では Content を配信し直さない
- **GIVEN** Content を設定した CustomCell を含む SettingsView の Handler が接続され、Native Host が生成された状態
- **WHEN** view 階層への取り付けの通知 (`Loaded`) が届く
- **THEN** Content の native への配信は新たに発生しない
