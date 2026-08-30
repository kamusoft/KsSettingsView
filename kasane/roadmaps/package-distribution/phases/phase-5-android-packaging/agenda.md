# phase-5-android-packaging

core / ui / compose を単一 module `android/kssettingsview` に統合し、`jp.kamusoft:kssettingsview` として Maven Central へ発行できる形にする (android/ADR-0016)。bridge は `android/kssettingsview-bridge` に改名し非公開のまま維持する。

## 論点

- source set 統合の手順 (Kotlin パッケージ名は維持) とテスト基盤の同居 (core の JUnit 5 と ui / compose の JUnit 4 + Robolectric)
- AGP namespace の単一化 (`jp.kamusoft.kssettingsview`) と R クラス参照 (android/ADR-0013) への影響
- ディレクトリ / project 名の改名 (`kssettingsview` / `kssettingsview-bridge`) と `android/settings.gradle.kts`
- catalog への `group` / version の集約 (phase-1 の catalog 上に乗せる)
- 発行の仕組み: `com.vanniktech.maven.publish` (POM メタデータ license / scm / developers、sources / javadoc jar、GPG 署名、Central Portal アップロード) の設定と、更新後の AGP との互換
- `jp.kamusoft` 名前空間の Central Portal 登録 (DNS TXT 検証、KsDialogs と共用)
- Sample の composite build: maven-publish 導入で自動置換が効くか、明示 `dependencySubstitution` を整理できるか
- MAUI binding csproj の `AndroidLibrary` aar パス (3 本 → 2 本) の追随
- 蒸留時の concepts 追随: public-identifiers.md の Maven 座標 (artifactId `ks-settingsview-*` → `kssettingsview`)、Android README
- 利用側の Kotlin 要件 (phase-1 からの申し送り、2026-08-21): Kotlin 2.4.10 化で生成物が要求する `kotlin-stdlib` の下限が上がった。POM / README の互換情報にライブラリの Kotlin・AGP・minSdk 要件を明記するか、stdlib 下限を意図的に下げるか

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
- [ ] **phase-9 からの申し送り** (2026-08-30): `kasane/concepts/cross/conventions/public-identifiers.md` の artifactId 規則を単一 artifact (`jp.kamusoft:kssettingsview`) へ改訂し (android/ADR-0016)、cross/ADR-0018 の配布先の表も追随させる。ルート README と `skills/` は既にこの座標を暫定値として書いている
