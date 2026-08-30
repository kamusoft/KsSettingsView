using System.Collections.Generic;
using KsSettingsView.Maui;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// Section 装飾デモの style 選択・装飾プリセット選択と、観察対象のスイッチ 3 種を供給する ViewModel。
/// </summary>
public sealed class SectionDecorationDemoViewModel : SampleViewModel
{
    private SettingsViewStyle _style = SettingsViewStyle.Modern;
    private SectionDecorationPreset _preset = SectionDecorationPreset.Standard;

    private bool _airplaneMode;
    private bool _autoAppearance = true;
    private bool _trueTone = true;

    /// <summary>プリセット選択 UI に並べる全プリセット。</summary>
    public IReadOnlyList<SectionDecorationPreset> Presets { get; } = SectionDecorationPreset.All;

    /// <summary>選択中の style。</summary>
    public SettingsViewStyle Style
    {
        get => _style;
        set => Set(ref _style, value);
    }

    /// <summary>選択中の装飾プリセット。</summary>
    public SectionDecorationPreset Preset
    {
        get => _preset;
        set => Set(ref _preset, value);
    }

    /// <summary>機内モードの状態。</summary>
    public bool AirplaneMode
    {
        get => _airplaneMode;
        set => Set(ref _airplaneMode, value);
    }

    /// <summary>外観モードの「自動」の状態。</summary>
    public bool AutoAppearance
    {
        get => _autoAppearance;
        set => Set(ref _autoAppearance, value);
    }

    /// <summary>True Tone の状態。</summary>
    public bool TrueTone
    {
        get => _trueTone;
        set => Set(ref _trueTone, value);
    }
}
