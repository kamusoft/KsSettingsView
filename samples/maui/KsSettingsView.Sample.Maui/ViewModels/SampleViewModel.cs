using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// デモページの ViewModel の共通基底。
/// </summary>
/// <remarks>
/// Cell の双方向バインド先になるプロパティは、値が実際に変わったときだけ変更通知を出す
/// (<see cref="Set{T}"/>)。受け付けられなかった操作 (複数選択の上限超過など) では Native から
/// 元の値がそのまま書き戻されるため、同値での通知を止めておくと「直近の操作」表示が
/// 実際の変更だけを拾える。
/// </remarks>
public abstract class SampleViewModel : INotifyPropertyChanged
{
    /// <inheritdoc/>
    public event PropertyChangedEventHandler? PropertyChanged;

    /// <summary>値が変わったときだけ書き換えて変更を通知する。</summary>
    /// <typeparam name="T">プロパティの型</typeparam>
    /// <param name="field">バッキングフィールド</param>
    /// <param name="value">新しい値</param>
    /// <param name="propertyName">変更を通知するプロパティ名</param>
    /// <returns>書き換えたなら true</returns>
    protected bool Set<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(field, value))
        {
            return false;
        }

        field = value;
        OnPropertyChanged(propertyName);
        return true;
    }

    /// <summary>プロパティの変更を通知する。</summary>
    /// <param name="propertyName">変更を通知するプロパティ名</param>
    protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
}
