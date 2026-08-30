# Delta Spec: samples-maui — AccessoryViewsDemoPage の追加

## ADDED Requirements

### Requirement: AccessoryViewsDemoPage を MAUI 固有区分に追加する

MAUI サンプルの一覧ページに「MAUI 固有」の区分を新設し、`AccessoryViewsDemoPage` への項目を配置する (SHALL)。この区分は既存のパリティ対象デモ群と視覚的に区別される (sample-parity の「デモ対象の公開 API が存在しない platform」例外 — native への追随義務を負わない)。

#### Scenario: 一覧の MAUI 固有区分から遷移できる

- **GIVEN** サンプルアプリの一覧ページ
- **WHEN** 「MAUI 固有」区分の AccessoryViews 項目を選択する
- **THEN** AccessoryViewsDemoPage へ遷移し、一覧の項目文言とページタイトルが一致している

### Requirement: AccessoryViewsDemoPage は本 change の公開挙動を確認できる

ページは次の7項目を実際に操作・確認できる構成とする (SHALL): (1) RootHeaderView / RootFooterView の表示 (2) Section の HeaderView / FooterView の表示 (バインディング付き View を含む) (3) text / view 併存時の View 優先と、View 解除での text フォールバック (4) View の新インスタンス差し替え (5) サイズが変わる内容変化への高さ追従 (6) HeaderHeight 固定時のはみ出し分の非表示 (7) Handler の切断・再接続をまたぐ view accessory の復元。

項目 (7) は**同一の SettingsView インスタンス**で Handler の切断・再接続が起きる操作手順で確認できること (SHALL)。現行メニューは選択のたびに新しい Page を生成するため、単純な「戻って再選択」では新規インスタンスの初期表示となり復元の検証にならない — 子ページの push → pop、または保持した同一 Page インスタンスへの再遷移など、facade インスタンスが維持される手順を用いる。

#### Scenario: 7項目が1ページで確認できる

- **GIVEN** AccessoryViewsDemoPage
- **WHEN** ページ内の各項目を操作する (トグル・差し替えボタン・値変更・切断再接続の操作)
- **THEN** 上記 (1)〜(7) の挙動がそれぞれ目視確認できる

#### Scenario: 復元確認は同一インスタンスで行われる

- **GIVEN** view accessory と切断中変更の識別表示を持つ AccessoryViewsDemoPage
- **WHEN** 同一 SettingsView インスタンスのまま Handler の切断 → 再接続を起こす操作を行う (子ページ push → pop 等)
- **THEN** 再接続後に view accessory が再表示され、切断中に行った変更も反映されていることが、同一インスタンスであることの表示とともに確認できる
