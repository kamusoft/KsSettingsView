## ADDED Requirements

### Requirement: Android 公開ライブラリの明示 API 境界

Maven 公開対象の Android 本体 module は Kotlin Explicit API mode の Strict を使用し、production source の公開宣言に明示的な visibility と、Kotlin が要求する型を SHALL 持たせる。テスト source と Maven 非公開の Bridge module はこの強制の対象外とする。

#### Scenario: 公開宣言の明示不足を拒否する

- **GIVEN** Android 本体 module の production source に公開する意図の宣言がある
- **WHEN** 必要な visibility または型を明示せず debug または release の Kotlin compilation を行う
- **THEN** compilation は Explicit API Strict の診断により失敗する

#### Scenario: 明示済みの公開面を両 variant でコンパイルできる

- **GIVEN** Android 本体 module の公開宣言に必要な visibility と型が明示されている
- **WHEN** debug と release の Kotlin compilation を行う
- **THEN** どちらも Explicit API Strict の診断なしで成功する

#### Scenario: 対象外の compilation に Strict が波及しない

- **GIVEN** 本体 module の test source と Bridge module に暗黙 public 宣言がある
- **WHEN** 本体テストと Bridge の Kotlin compilation を行う
- **THEN** 本体 production compilation に設定した Explicit API Strict を理由とする診断は発生しない

### Requirement: 公開 API 差分の限定

Android 本体 module は、変更前に外部公開されていた宣言を SHALL 維持する。ただし `KsCellRegistry.viewTypeOf`、`KsCellRegistry.isRegistered`、`SettingsRootStore.preview` の 3 宣言は意図した可視性引き下げとして除外する。`public` の明示だけを理由とする新しい公開 ABI は SHALL 生じさせない。

#### Scenario: release AAR の公開 ABI 差分が意図した降格だけになる

- **GIVEN** 変更前の release AAR から同一手順で列挙・正規化した公開 ABI 一覧がある
- **WHEN** 変更後の release AAR の公開 ABI 一覧と比較する
- **THEN** 差分は `viewTypeOf`、`isRegistered`、`preview` の internal 化に伴う変化だけで、他の公開宣言に増減がない

### Requirement: Cell Registry の利用者向け公開面

Android 本体 module は、独自 Cell 登録と標準 Cell 一括登録に必要な Registry API を外部利用者へ SHALL 公開する。一方、Adapter の内部照会とテスト・診断用照会は外部利用者へ SHALL 公開しない。

#### Scenario: 外部利用者が Cell を登録できる

- **GIVEN** Android 本体 module に依存する外部 Kotlin code がある
- **WHEN** `KsCellRegistry.register`、`strictMode`、`CELL_VIEW_TYPE_MIN`、標準 Cell 一括登録 API、`CellViewHolder` を使用して compilation する
- **THEN** 公開 API として参照できる

#### Scenario: 外部利用者が内部照会を参照できない

- **GIVEN** Android 本体 module に依存する外部 Kotlin code がある
- **WHEN** `KsCellRegistry.viewTypeOf` または `KsCellRegistry.isRegistered` を参照して compilation する
- **THEN** internal な宣言への参照として compilation が失敗する

### Requirement: SettingsRootStore の生成境界

Android 本体 module は `SettingsRootStore` の public コンストラクタを利用者向けの生成経路として SHALL 維持し、Preview / Test 用 factory は外部利用者へ SHALL 公開しない。

#### Scenario: 外部利用者が通常コンストラクタで Store を生成できる

- **GIVEN** Android 本体 module に依存する外部 Kotlin code がある
- **WHEN** `SettingsRoot` と任意の `Theme` を渡して `SettingsRootStore` を生成する
- **THEN** public コンストラクタを参照して compilation が成功する

#### Scenario: 外部利用者が Preview factory を参照できない

- **GIVEN** Android 本体 module に依存する外部 Kotlin code がある
- **WHEN** `SettingsRootStore.preview` を参照して compilation する
- **THEN** internal な宣言への参照として compilation が失敗する
