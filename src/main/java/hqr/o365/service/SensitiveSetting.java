package hqr.o365.service;

final class SensitiveSetting {
    private SensitiveSetting() {
    }

    static boolean isSensitive(String key) {
        return key != null && (key.contains("PASSWORD") || key.contains("SECRET")
                || key.contains("TOKEN") || key.endsWith("KEY") || key.endsWith("AESKEY"));
    }
}
