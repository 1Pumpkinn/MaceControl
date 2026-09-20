package net.macecontrol.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tally of valid/invalid/enchanted maces found by a {@link MaceScanner} pass, plus where each one was. */
public class MaceScanResult {

    int totalValidMaces = 0;
    int invalidMaces = 0;
    final Set<Integer> maceNumbers = new HashSet<>();
    final Set<Integer> enchantedMaces = new HashSet<>();

    /** One colour-coded line per mace found, e.g. "Chest at world 10,64,-3 > bundle #2". */
    final List<String> locations = new ArrayList<>();

    public int getTotalValidMaces() {
        return totalValidMaces;
    }

    public int getInvalidMaces() {
        return invalidMaces;
    }

    public int getTotalMaces() {
        return totalValidMaces + invalidMaces;
    }

    public List<String> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public String getDetailsString(int enchantableMaces) {
        StringBuilder sb = new StringBuilder();
        sb.append(totalValidMaces).append(" valid maces");

        if (!maceNumbers.isEmpty()) {
            List<Integer> sorted = new ArrayList<>(maceNumbers);
            Collections.sort(sorted);

            List<String> maceList = new ArrayList<>();
            for (int number : sorted) {
                String label = "#" + number;
                if (enchantedMaces.contains(number)) {
                    label += " enchanted";
                } else if (number <= enchantableMaces) {
                    label += " enchantable";
                }
                maceList.add(label);
            }
            sb.append(" (").append(String.join(", ", maceList)).append(")");
        }

        if (invalidMaces > 0) {
            sb.append(", ").append(invalidMaces).append(" invalid");
        }

        return sb.toString();
    }
}