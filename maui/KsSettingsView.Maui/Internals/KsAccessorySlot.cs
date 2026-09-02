namespace KsSettingsView.Internals;

/// <summary>
/// accessory を置ける 1 箇所を指す座標。
/// </summary>
/// <remarks>
/// Root の header・footer と、Section ごとの header・footer が対象になる。Section は
/// インスタンスの同一性で区別する。
/// </remarks>
/// <param name="Target">accessory の位置</param>
/// <param name="Section">Section を対象にするときの Section。Root 対象では null</param>
internal readonly record struct KsAccessorySlot(KsAccessoryTarget Target, Section? Section);
