# Tasks: add-maui-native-bridge

## 1. Spike: binding toolchain の net10.0 疎通 (Decision 6)

- [x] 1.1 iOS: XcodeProject 形式 binding テンプレートの net10.0 最小スケルトンで成功ゲートを通す — (1) xcframework 生成 (2) binding assembly 生成 (3) C# compile/link (4) シミュレータで最小アプリ起動
- [x] 1.2 Android: AndroidGradleProject 形式 binding テンプレートの net10.0 最小スケルトンで成功ゲートを通す — (1) aar 生成 (2) binding assembly 生成 (3) C# compile/link (4) エミュレータで最小アプリ起動
- いずれかのゲートが失敗した場合は後続タスクへ進まず change を blocked とし、phase agenda へ差し戻す

## 2. iOS Store: replaceCells の追加 (→ Requirement: 複数 Cell 内容更新のバッチ適用 (replaceCells))

- [x] 2.1 `SettingsRootStore.replaceCells` を実装する (状態1回更新 + 状態更新後のバッチ内容更新配信経路の新設。Android の観察可能挙動と対称)
- [x] 2.2 ユニットテストを作成する (→ Scenario: 複数 Cell の更新が1バッチで配信される / 既知・未知 ID の混在では既知だけが適用・配信される / 存在しない ID は無視され適用0件なら配信しない / 空リストは no-op / 同一 ID の重複指定は最後の値が残る。Android `SettingsRootStoreTest` の対応ケースと対称にする)
- [x] 2.3 iOS Native Host のバッチ内容更新反映を実装する (→ Requirement: Native Host のバッチ内容更新反映)
- [x] 2.4 Host 反映のテストを作成する (→ Scenario: バッチ更新が表示へ反映される)

## 3. iOS Bridge ライブラリ (→ Requirement: Bridge の生成と Root 構築 / Native Host の生成と接続 / Bridge の lifecycle / Store 操作 1:1 の更新 API / Theme 適用)

- [x] 3.1 `ios/` 配下に Bridge ライブラリを新設する (`@objc` 互換の Builder [Section header/footer は text 限定]・LabelCell DTO・**Bridge 採番の canonical UUID 文字列 ID**・内部所有 Store。Decision 1 / 5 / 8 / 9)
- [x] 3.2 Native Host 生成 API と破棄 API を実装する (内部 Store 接続済み `KsSettingsViewController` の生成・公開。同時1 Host・破棄冪等・破棄後 no-op。Decision 7)
- [x] 3.3 `setRoot` + 更新 API 10種 + `setTheme` を Store 公開操作への変換として実装する (accessory は text 限定・Theme DTO は Theme 公開項目と 1:1。Decision 2 / 4 / 8)
- [x] 3.4 ユニットテストを作成する (→ Scenario: root 表示 / 全置換 / 採番 ID での後続操作 / 不正 ID no-op / Host 生成→setRoot / setRoot→Host 生成の状態復元 / 破棄冪等 / 破棄後 no-op / Cell 構造操作 / Section 構造操作 / identity 維持 / バッチ反映 / Theme 変更 / 同値 Theme)
- [x] 3.5 全12操作を parameterized test で検証する (→ Scenario: 全12操作が契約どおりに反映される。**観察可能な結果 (表示内容・通知) ベース**で、未知 ID・index clamp の境界も操作ごとに含める)

## 4. Android Bridge モジュール (→ 同上の Requirements)

- [x] 4.1 `android/` 配下に Bridge モジュールを新設する (JVM 互換の Builder [Section header/footer は text 限定]・LabelCell DTO・**Bridge 採番の canonical UUID 文字列 ID**・内部所有 Store。Decision 1 / 5 / 8 / 9)
- [x] 4.2 Native Host 生成 API と破棄 API を実装する (内部 Store を bind 済みの `KsSettingsView` の生成・公開。`Context` は生成引数で受け取り保持しない。同時1 Host・破棄冪等・破棄後 no-op。Decision 7)
- [x] 4.3 `setRoot` + 更新 API 10種 + `setTheme` を Store 公開操作への変換として実装する (accessory は text 限定・Theme DTO は Theme 公開項目と 1:1。Decision 2 / 4 / 8)
- [x] 4.4 ユニットテストを作成する (→ iOS 3.4 と同じ Scenario 群)
- [x] 4.5 全12操作を parameterized test で検証する (→ iOS 3.5 と同様)

## 5. MAUI Binding プロジェクト (→ Requirement: .NET binding からの呼び出し)

- [x] 5.1 iOS 用 Binding csproj (XcodeProject 参照・net10.0-ios) を `maui/` 配下に新設する
- [x] 5.2 Android 用 Binding csproj (AndroidGradleProject 参照・net10.0-android) を `maui/` 配下に新設する
- [x] 5.3 `KsSettingsView.slnx` に組み込み、C# から Bridge 公開 API (Builder・Host 生成・破棄・更新 API・setTheme) を参照するコードのビルドを確認する (→ Scenario: C# からの参照とビルド)
- [x] 5.4 検証ホスト (最小の iOS / Android integration host) を `maui/` 配下に**テスト資産として**新設し、C# → Builder → Host 取り付け → `setRoot` → LabelCell 表示を両 OS で確認する (→ Scenario: C# からの実行時疎通。後続フェーズ・SDK 更新時の回帰検証に再利用する)
