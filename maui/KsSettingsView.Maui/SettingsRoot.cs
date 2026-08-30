using System.Collections.ObjectModel;

namespace KsSettingsView.Maui;

/// <summary>
/// <see cref="SettingsView.Root"/> の既定コレクション。Section を順に保持する observable な器。
/// </summary>
public class SettingsRoot : ObservableCollection<Section>
{
}
