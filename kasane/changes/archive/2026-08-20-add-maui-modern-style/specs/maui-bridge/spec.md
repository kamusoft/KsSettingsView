# Delta: maui-bridge (add-maui-modern-style)

## ADDED Requirements

### Requirement: Theme DTO の Section 装飾4属性輸送

Theme の輸送 (`KsThemeSnapshot` および両 OS の `KsBridgeTheme`) は Section 装飾4属性を7フィールドで運ぶ (SHALL): margin はフラットな論理4成分 (top / leading / bottom / trailing)、加えて cornerRadius・borderWidth・borderColor (ARGB int)。facade は `SectionMargin` が null なら4成分すべてを null、非 null なら4成分すべてを設定する (all-or-none) (SHALL)。Bridge の resolve は4成分から native の方向対応型 (iOS `NSDirectionalEdgeInsets` / Android `PaddingValues(start, end)`) を組み立て、null フィールドは native Theme の未指定として写す (SHALL)。DTO 上で4成分の一部だけが null の場合 (Bridge API を直接使う利用者が作り得る)、margin 全体を未指定として解決する (SHALL、両 OS 同一)。入れ子 DTO は追加しない。

#### Scenario: 4成分が方向対応型へ組み立てられる
- **GIVEN** margin の論理4成分を設定した `KsBridgeTheme`
- **WHEN** native `Theme` へ resolve する
- **THEN** leading 成分は方向対応型の leading / start へ、trailing 成分は trailing / end へ写る

#### Scenario: null は未指定として resolve される
- **GIVEN** 4属性のフィールドをすべて null にした `KsBridgeTheme`
- **WHEN** native `Theme` へ resolve する
- **THEN** native Theme の `sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor` は未指定 (nil / null) である

#### Scenario: 部分 null の margin は全体を未指定として解決する
- **GIVEN** margin の trailing 成分だけを null にした `KsBridgeTheme`
- **WHEN** native `Theme` へ resolve する
- **THEN** `sectionMargin` は未指定 (nil / null) になる (両 OS で同一の結果)

#### Scenario: borderColor が platform 色へ変換される
- **GIVEN** ARGB int の `sectionBorderColor` を設定した `KsBridgeTheme`
- **WHEN** native `Theme` へ resolve する
- **THEN** 既存の色変換規則 (KsBridgeColor 相当) で platform の色型へ変換される

### Requirement: style の設定操作の輸送

gateway は style 設定操作 (`SetStyle`) を持ち、Bridge (両 OS) は新設の style 設定 API で native の style 可変プロパティ (iOS `KsSettingsViewController.style` / Android `KsSettingsView.style`) へ適用する (SHALL)。値は enum の序数 int (Classic = 0 / Modern = 1) で輸送し、定義域外の序数は Classic へ正規化する (SHALL — 非 nullable・既定 Classic の facade 契約に寄せる)。この操作は Store を経由しない (native 側でも style は Store 外の View / Controller プロパティであり対称。maui/ADR-0002 の Store 操作 1:1 の枠外である旨を注記する)。

#### Scenario: SetStyle が native の style へ適用される
- **GIVEN** 表示中の Bridge 構成 (既定 Classic)
- **WHEN** style 設定 API に Modern を渡す
- **THEN** native の SettingsView / Controller の style が Modern になり、Modern の装飾で再描画される

#### Scenario: 序数の対応が両 OS で一致する
- **GIVEN** 序数 0 と 1
- **WHEN** 両 OS の Bridge で style へ変換する
- **THEN** 0 は Classic、1 は Modern に解決され、対応は両 OS で一致する

#### Scenario: 定義域外の序数は Classic へ正規化される
- **GIVEN** 0 / 1 以外の序数 (例: 2 や -1)
- **WHEN** 両 OS の Bridge で style へ変換する
- **THEN** Classic に解決される

### Requirement: style の Host 再生成をまたぐ保持

Bridge (両 OS) は現在の style を Host の外のフィールドで保持し、Host の生成時 (`makeHost*`) に適用する (SHALL)。Host 生成前に受けた style 設定も保持し、後続の生成で適用する (SHALL)。`releaseHost()` 後の再生成でも style は失われない (SHALL) — Theme は Store 経由で新 Host へ復元されるが style は Store 外のため、Bridge 保持で対称の生存性を与える。facade の controller は現在の style を保持し、gateway の初回接続時に配信する (SHALL)。gateway は Native Host の解放をまたいで作り直されない (既存 lifecycle) ため、接続時配信は初回のみで足りる。

#### Scenario: Host 生成前の style 設定が生成時に適用される
- **GIVEN** Host 未生成の Bridge
- **WHEN** style 設定 API に Modern を渡してから Host を生成する
- **THEN** 生成された Host は Modern の装飾で表示される

#### Scenario: Host 再生成をまたいで style が維持される
- **GIVEN** Modern を適用済みの表示中 Bridge 構成
- **WHEN** `releaseHost()` 後に再度 Host を生成する
- **THEN** 新しい Host も Modern の装飾で表示される

#### Scenario: gateway 初回接続時に style が配信される (net10.0 ユニットテスト)
- **GIVEN** `ListStyle = Modern` の SettingsView
- **WHEN** fake gateway を接続する
- **THEN** 接続時に gateway は Modern の style 設定操作を受け取る
