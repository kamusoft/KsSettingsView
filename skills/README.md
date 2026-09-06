# KsSettingsView Skills

Agent Skills for building settings screens with KsSettingsView, published in English and Japanese ([日本語版の索引: README_ja.md](README_ja.md)).

## Skills

| Name | For | What it does | en | ja |
|---|---|---|---|---|
| `kssettingsview-ios` | iOS (Swift) | Build a settings screen with the SwiftUI declarative DSL or the UIKit host: 12 built-in cells, CustomCell, live updates through a store, and `Theme` / `CellStyle` styling. | [en](en/kssettingsview-ios/SKILL.md) | [ja](ja/kssettingsview-ios/SKILL.md) |
| `kssettingsview-android` | Android (Kotlin) | Build a settings screen with the Jetpack Compose declarative DSL or the View host: 12 built-in cells, CustomCell, live updates through a store, and `Theme` / `CellStyle` styling. | [en](en/kssettingsview-android/SKILL.md) | [ja](ja/kssettingsview-android/SKILL.md) |
| `kssettingsview-maui` | .NET MAUI (XAML / C#) | Build a settings page with the MAUI `SettingsView` control over the native settings list: 12 built-in cells, CustomCell, two-way bindings, `ItemsSource` / `ItemTemplate`, headers and footers, and list styling. | [en](en/kssettingsview-maui/SKILL.md) | [ja](ja/kssettingsview-maui/SKILL.md) |
| `kssettingsview-aiforms-migration` | .NET MAUI migration | Move a settings page off `AiForms.Maui.SettingsView` (the .NET MAUI release): a mapping of the old public API onto KsSettingsView, with the replacement approach for what is gone. Copy it together with `kssettingsview-maui`, which covers the KsSettingsView API itself. | [en](en/kssettingsview-aiforms-migration/SKILL.md) | [ja](ja/kssettingsview-aiforms-migration/SKILL.md) |

## How to install

**1. Get a copy of this repository.** Clone it anywhere outside your own project (or download the ZIP from GitHub and unpack it, then set `KSSV` to the unpacked directory):

```bash
git clone https://github.com/kamusoft/KsSettingsView.git
KSSV="$PWD/KsSettingsView"
```

`KSSV` now holds the path to the clone, and the copy commands below read it — so run them in the same shell session.

**2. Copy the Skill directories you want into your own project.** Replace `<your-project-root>` with the root of your project, `<lang>` with `en` or `ja`, and `<skill-name>` with a name from the table above (for example `kssettingsview-ios`). Repeat the `cp` line for each Skill you want.

If your agent tool reads Skills from `.agents/skills/`, copy them there — that is the common first choice, since agent tools increasingly read Skills from that location:

```bash
cd <your-project-root>
mkdir -p .agents/skills
cp -R "$KSSV/skills/<lang>/<skill-name>" .agents/skills/
```

If you use Claude Code, copy them to `.claude/skills/` instead, which is where it reads Skills from (or symlink `.claude/skills` to `.agents/skills`):

```bash
cd <your-project-root>
mkdir -p .claude/skills
cp -R "$KSSV/skills/<lang>/<skill-name>" .claude/skills/
```

Copy the whole directory, not single files: a Skill is `SKILL.md` plus its `references/`, and the copied directory is fully self-contained — the documents hold no links pointing outside it.

**3. Restart your agent** (or start a new session) so it picks up the newly copied Skills — a running session does not see them.

## One language only

Copy Skills from **one** language tree only. `en/` and `ja/` are translations of the same content and carry the same `name`, so copying both puts two Skills with the same name into one project.
