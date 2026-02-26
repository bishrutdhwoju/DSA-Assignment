package assignment;

import java.util.Arrays;

// Computes max profit from at most K stock buy/sell transactions using DP
public class StockMaxProfitKTransactions {

    public static int maxProfit(int maxTransactions, int[] dailyPrices) {
        int totalDays = dailyPrices.length;
        if (totalDays == 0 || maxTransactions == 0) return 0;

        // If enough transactions, just sum all positive differences
        if (maxTransactions >= totalDays / 2) {
            int unlimitedProfit = 0;
            for (int day = 1; day < totalDays; day++) {
                if (dailyPrices[day] > dailyPrices[day - 1]) {
                    unlimitedProfit += dailyPrices[day] - dailyPrices[day - 1];
                }
            }
            return unlimitedProfit;
        }

        // DP: previousRoundProfit = best with (t-1) transactions, currentRoundProfit = best with t
        int[] previousRoundProfit = new int[totalDays];
        int[] currentRoundProfit = new int[totalDays];

        for (int transaction = 1; transaction <= maxTransactions; transaction++) {
            // Best effective buy price considering prior transactions
            int bestBuyOpportunity = -dailyPrices[0];
            currentRoundProfit[0] = 0;

            for (int day = 1; day < totalDays; day++) {
                // Max of: carry forward, or sell today at best buy opportunity
                currentRoundProfit[day] = Math.max(currentRoundProfit[day - 1], dailyPrices[day] + bestBuyOpportunity);
                bestBuyOpportunity = Math.max(bestBuyOpportunity, previousRoundProfit[day - 1] - dailyPrices[day]);
            }

            // Swap arrays for next round
            int[] swapTemp = previousRoundProfit;
            previousRoundProfit = currentRoundProfit;
            currentRoundProfit = swapTemp;
            Arrays.fill(currentRoundProfit, 0);
        }

        return previousRoundProfit[totalDays - 1];
    }

    public static void main(String[] args) {
        // prices=[2000,4000,1000], k=2 -> profit=2000
        System.out.println(maxProfit(2, new int[]{2000, 4000, 1000}));
    }
}