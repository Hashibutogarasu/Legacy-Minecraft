package wily.legacy.core.utils;

import java.util.ArrayList;
import java.util.List;

public class ModVersionUtils {
    public static boolean isNewerVersion(String actualVersion, String previous, int limitCount) {
        List<Integer> v = getParsedVersion(actualVersion);
        List<Integer> v1 = getParsedVersion(previous);
        int size = limitCount <= 0 ? v.size() : Math.min(limitCount, v.size());
        for (int i = 0; i < size; i++) {
            if (v.get(i) > (v1.size() <= i ? 0 : v1.get(i))) return true;
        }
        return false;
    }

    public static List<Integer> getParsedVersion(String version) {
        List<Integer> parsedVersion = new ArrayList<>();
        String[] versions = version.split("[.\\-]");
        for (String s : versions) {
            int value;
            try {
                value = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                value = 0;
            }
            parsedVersion.add(value);
        }
        return parsedVersion;
    }

    public static boolean isNewerVersion(String actualVersion, String previous) {
        return isNewerVersion(actualVersion, previous, 2);
    }
}
