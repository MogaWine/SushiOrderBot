package Utils;

import java.util.Comparator;

public class CustomMapComparator implements Comparator<String> {
        private static Integer intValue(String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        //TODO FIX 123a 1 2 3 123
        @Override
        public int compare(String s1, String s2) {
            Integer i1 = intValue(s1);
            Integer i2 = intValue(s2);
            if (i1 == null && i2 == null) {
                return s1.compareTo(s2);
            } else if (i1 == null) {
                return -1;
            } else if (i2 == null) {
                return 1;
            } else {
                return i1.compareTo(i2);
            }
        }
}
