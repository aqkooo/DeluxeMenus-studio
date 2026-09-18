package com.ejyqyl.dmstudio.service.head;

import com.google.gson.annotations.SerializedName;

/**
 * Record representing a player head / HeadDatabase item with offline textures.
 */
public record HeadEntry(
        String id,
        @SerializedName("name_ru") String nameRu,
        @SerializedName("name_en") String nameEn,
        String category,
        String value,
        String textureHash,
        String iconPngBase64
) {
    public String getLocalizedName(boolean isRu) {
        if (isRu) {
            return nameRu != null && !nameRu.isBlank() ? nameRu : nameEn;
        }
        return nameEn != null && !nameEn.isBlank() ? nameEn : nameRu;
    }
}
