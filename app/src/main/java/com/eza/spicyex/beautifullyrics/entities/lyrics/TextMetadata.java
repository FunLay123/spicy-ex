package com.eza.spicyex.beautifullyrics.entities.lyrics;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class TextMetadata {
    @SerializedName("Text")
    public String text;
    @Nullable
    @SerializedName(value = "RomanizedText", alternate = {"romanizedText", "RomanisedText", "romanisedText"})
    public String romanizedText;
    @Nullable
    @SerializedName(value = "TranslatedText", alternate = {"translatedText", "Translation", "translation"})
    public String translatedText;
}
