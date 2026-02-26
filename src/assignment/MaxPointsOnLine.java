package assignment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Finds the maximum number of points that lie on the same straight line
public class MaxPointsOnLine {

    public static int maxPoints(int[][] points) {
        int totalPoints = points.length;
        if (totalPoints <= 2) return totalPoints;

        int globalMaxOnLine = 1;

        // Use each point as anchor and count slopes to other points
        for (int anchorIdx = 0; anchorIdx < totalPoints; anchorIdx++) {
            Map<NormalizedSlope, Integer> slopeFrequency = new HashMap<>();
            int duplicateCount = 0;
            int maxOnLineFromAnchor = 0;

            for (int otherIdx = anchorIdx + 1; otherIdx < totalPoints; otherIdx++) {
                int deltaX = points[otherIdx][0] - points[anchorIdx][0];
                int deltaY = points[otherIdx][1] - points[anchorIdx][1];

                // Same point, count as duplicate
                if (deltaX == 0 && deltaY == 0) {
                    duplicateCount++;
                    continue;
                }

                // Reduce slope to simplest fraction
                int divisor = gcd(deltaX, deltaY);
                deltaX /= divisor;
                deltaY /= divisor;

                // Normalize direction for consistent slope keys
                if (deltaX < 0) { deltaX = -deltaX; deltaY = -deltaY; }
                if (deltaX == 0) { deltaY = 1; }   // vertical
                if (deltaY == 0) { deltaX = 1; }   // horizontal

                NormalizedSlope slope = new NormalizedSlope(deltaX, deltaY);
                int updatedCount = slopeFrequency.getOrDefault(slope, 0) + 1;
                slopeFrequency.put(slope, updatedCount);
                maxOnLineFromAnchor = Math.max(maxOnLineFromAnchor, updatedCount);
            }

            // best slope count + anchor itself + duplicates
            globalMaxOnLine = Math.max(globalMaxOnLine, maxOnLineFromAnchor + 1 + duplicateCount);
        }

        return globalMaxOnLine;
    }

    // GCD using Euclidean algorithm
    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        if (a == 0) return b == 0 ? 1 : b;
        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }
        return a == 0 ? 1 : a;
    }

    public static void main(String[] args) {
        System.out.println(maxPoints(new int[][]{{1, 1}, {2, 2}, {3, 3}}));           // expected: 3
        System.out.println(maxPoints(new int[][]{{1, 1}, {3, 2}, {5, 3}, {4, 1}, {2, 3}, {1, 4}})); // expected: 4
    }

    // Slope as a normalized reduced fraction for use as HashMap key
    private static final class NormalizedSlope {
        final int deltaX, deltaY;

        NormalizedSlope(int deltaX, int deltaY) {
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof NormalizedSlope)) return false;
            NormalizedSlope that = (NormalizedSlope) other;
            return deltaX == that.deltaX && deltaY == that.deltaY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deltaX, deltaY);
        }
    }
}