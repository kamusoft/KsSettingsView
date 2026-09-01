# https 解決検証 (2026-09-01)

一時消費者プロジェクト (リポジトリ外) から https URL + exact tag 指定で解決し、3 module の公開型を各 1 つ参照するコードを iOS Simulator 向けにビルドして成功。

## Package.resolved
```json
{
  "pins" : [
    {
      "identity" : "kssettingsview-spm",
      "kind" : "remoteSourceControl",
      "location" : "https://github.com/kamusoft/KsSettingsView-SPM",
      "state" : {
        "revision" : "8661e38f1805101e12798ca8243ff7b709098a99",
        "version" : "0.1.0-alpha.1"
      }
    }
  ],
  "version" : 2
}
```

## ビルド結果
```
xcodebuild build -scheme SpmConsumer -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
** BUILD SUCCEEDED **
```

## 検証用 tag の後始末 (2026-09-01)

```
$ git ls-remote --tags origin | wc -l
0
$ gh api repos/kamusoft/KsSettingsView-SPM/tags --jq length
0
```

prerelease tag `0.1.0-alpha.1` は https 解決検証の完了後に削除済み。リモートに tag は存在しない。

## 一時消費者プロジェクトのソース (検証に使用した全量)

### Package.swift
```swift
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "SpmConsumer",
    platforms: [.iOS(.v16)],
    products: [.library(name: "SpmConsumer", targets: ["SpmConsumer"])],
    dependencies: [
        .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "0.1.0-alpha.1")
    ],
    targets: [
        .target(
            name: "SpmConsumer",
            dependencies: [.product(name: "KsSettingsView", package: "KsSettingsView-SPM")])
    ]
)
```

### Sources/SpmConsumer/Consumer.swift
```swift
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

// 3 module それぞれの公開型を最低 1 つ参照する (配線検証)
public enum ConsumerCheck {
    public static let core: Any.Type = CellTitleAlignment.self
    public static let ui: Any.Type = KsSettingsViewStyle.self
    public static let swiftUI: Any.Type = DSLIdentityHint.self
}
```
