# KsSettingsView Skills

KsSettingsView で設定画面を作るための Agent Skills。英語版と日本語版を用意している ([English index: README.md](README.md))。

## 収録 Skill

| name | 対象 | 概要 | en | ja |
|---|---|---|---|---|
| `kssettingsview-ios` | iOS (Swift) | SwiftUI の宣言的 DSL または UIKit ホストで設定画面を作る。組み込み 12 種の Cell、CustomCell、Store による表示中の更新、`Theme` / `CellStyle` のスタイル指定を扱う。 | [en](en/kssettingsview-ios/SKILL.md) | [ja](ja/kssettingsview-ios/SKILL.md) |
| `kssettingsview-android` | Android (Kotlin) | Jetpack Compose の宣言的 DSL または View ホストで設定画面を作る。組み込み 12 種の Cell、CustomCell、Store による表示中の更新、`Theme` / `CellStyle` のスタイル指定を扱う。 | [en](en/kssettingsview-android/SKILL.md) | [ja](ja/kssettingsview-android/SKILL.md) |
| `kssettingsview-maui` | .NET MAUI (XAML / C#) | Native の設定 list を描画する MAUI の `SettingsView` コントロールで設定ページを作る。組み込み 12 種の Cell、CustomCell、双方向バインド、`ItemsSource` / `ItemTemplate`、Header / Footer、list 外観を扱う。 | [en](en/kssettingsview-maui/SKILL.md) | [ja](ja/kssettingsview-maui/SKILL.md) |
| `kssettingsview-aiforms-migration` | .NET MAUI の移行 | `AiForms.Maui.SettingsView` (.NET MAUI 版) からの移行。旧公開 API の KsSettingsView への対応表と、廃止されたものの代替手段を示す。KsSettingsView 自体の API は `kssettingsview-maui` が扱うため、そちらと併せてコピーする。 | [en](en/kssettingsview-aiforms-migration/SKILL.md) | [ja](ja/kssettingsview-aiforms-migration/SKILL.md) |

## コピー手順

**1. 本リポジトリを手元に取得する。** 自分のプロジェクトの外側の任意の場所へ clone する (または GitHub から ZIP をダウンロードして展開し、`KSSV` に展開先を設定する):

```bash
git clone https://github.com/kamusoft/KsSettingsView.git
KSSV="$PWD/KsSettingsView"
```

`KSSV` に clone 先のパスが入る。以降のコピーコマンドはこれを参照するので、同じシェルセッションで続けて実行する。

**2. 使いたい Skill のディレクトリを自分のプロジェクトへコピーする。** `<your-project-root>` は自分のプロジェクトのルート、`<lang>` は `en` または `ja`、`<skill-name>` は上の表の name (例: `kssettingsview-ios`) に置き換える。複数入れる場合は `cp` の行を Skill の数だけ繰り返す。

使っているエージェントツールが `.agents/skills/` を読むなら、そこへコピーする (各種エージェントツールが共通して読むようになってきている場所であり、コピー先の第一候補):

```bash
cd <your-project-root>
mkdir -p .agents/skills
cp -R "$KSSV/skills/<lang>/<skill-name>" .agents/skills/
```

Claude Code を使う場合は、Claude Code が読む `.claude/skills/` へコピーする (または `.claude/skills` を `.agents/skills` への symlink にする):

```bash
cd <your-project-root>
mkdir -p .claude/skills
cp -R "$KSSV/skills/<lang>/<skill-name>" .claude/skills/
```

ファイル単位ではなくディレクトリごとコピーする。Skill は `SKILL.md` と `references/` の組であり、コピーしたディレクトリだけで完結する — 文書からディレクトリの外へ張られたリンクは無い。

**3. エージェントを再起動する** (またはセッションを開き直す)。起動中のセッションはコピーした Skill を認識しない。

## 言語は片方だけ

コピーするのは `en/` `ja/` の**どちらか一方**だけにする。両者は同一内容の翻訳ペアで `name` も同じため、両方をコピーすると同名の Skill が 2 つ入ることになる。
