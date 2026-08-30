# Deviation: add-maui-native-bridge

- Binding csproj の形式 (proposal「What Changes」/ tasks 5.2): spec では「AndroidGradleProject 形式の Binding csproj」→ 指示により **Android のみ `android/gradlew` を Exec で直接呼ぶ方式を維持**。理由: SDK の init script が `rootProject.allprojects` の buildDirectory を単一パスへ束ねるため、project 依存を持つ現行4モジュール構成では Gradle validation エラーでビルド自体が失敗することを実測確認 (ksn-dual-research、証跡: exploration.md)。SDK 側の対応まで標準アイテムに載せられない。オーナー承認済み (2026-08-05)
  - 補足: iOS は同じ逸脱が「SDK 制約」として記録されていたが、並走調査の実験で反証されたため標準 `XcodeProject` 形式へ戻す (deviation ではなく実装修正で解消)
  - 補足: pack 経路は公式アイテム (`AndroidLibrary`) 経由で標準方式と共通。`dotnet pack` の成立は実測済み
