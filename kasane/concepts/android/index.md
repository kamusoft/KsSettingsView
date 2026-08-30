# android 目次

Android 系統 (android/ ビルドルート) 固有の知識。カテゴリ定義と配置基準は [../rules.md](../rules.md) を参照。

## api/

- [api/android-native-host.md](api/android-native-host.md) — SettingsRootStore と Android View Host の構築・更新・ViewHolder 拡張境界
- [api/android-compose.md](api/android-compose.md) — Compose の Store / DSL 方式、identity、modifier、Theme 伝播

## architecture/

- [architecture/build-toolchain.md](architecture/build-toolchain.md) — android/ と samples/android/ の Gradle ビルドが要求する JDK の役割分担・成果物ターゲット (Java 17)・バージョン宣言の単一元 (libs.versions.toml)・MAUI binding など消費側の前提・ツールチェーン更新時に揃えるもの
