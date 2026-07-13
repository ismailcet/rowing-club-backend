package com.rowingclub.app.common;


public enum MemberLevel {

    BASLANGIC("Başlangıç", 0),
    GELISEN("Gelişen", 5),
    ORTA("Orta Seviye", 15),
    ILERI("İleri Seviye", 30),
    UZMAN("Uzman", 50),
    PROFESYONEL("Profesyonel", 80);

    private final String label;
    private final int minSessions;

    MemberLevel(String label, int minSessions) {
        this.label = label;
        this.minSessions = minSessions;
    }

    public String label() {
        return label;
    }

    public int minSessions() {
        return minSessions;
    }

    public int number() {
        return ordinal() + 1;
    }

    /** Sonraki seviye; en üstteyse null. */
    public MemberLevel next() {
        MemberLevel[] all = values();
        return ordinal() < all.length - 1 ? all[ordinal() + 1] : null;
    }

    /** Katılınan ders sayısına karşılık gelen seviye. */
    public static MemberLevel of(int attendedCount) {
        MemberLevel result = BASLANGIC;
        for (MemberLevel level : values()) {
            if (attendedCount >= level.minSessions) {
                result = level;
            }
        }
        return result;
    }

    public static int maxNumber() {
        return values().length;
    }
}