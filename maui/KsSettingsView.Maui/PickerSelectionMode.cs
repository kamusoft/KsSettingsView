namespace KsSettingsView.Maui;

/// <summary><see cref="PickerCell"/> の選択モード。</summary>
public enum PickerSelectionMode
{
    /// <summary>1 項目だけを選ぶ。選択状態は <see cref="PickerCell.SelectedIndex"/> が持つ。</summary>
    Single,

    /// <summary>複数項目を選ぶ。選択状態は <see cref="PickerCell.SelectedIndices"/> が持つ。</summary>
    Multiple,
}
