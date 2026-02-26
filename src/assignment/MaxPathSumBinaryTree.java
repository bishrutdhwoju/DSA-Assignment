package assignment;

// Finds the maximum path sum in a binary tree using post-order DFS
public class MaxPathSumBinaryTree {
    // Tracks the best path sum found so far
    private static int globalMaxSum;

    // Resets global best and starts the recursive search
    public static int maxPathSum(TreeNode root) {
        globalMaxSum = Integer.MIN_VALUE;
        computeMaxGain(root);
        return globalMaxSum;
    }

    // Returns the max one-sided gain from this node downward
    private static int computeMaxGain(TreeNode node) {
        if (node == null) return 0;

        // Only take positive gains from left and right subtrees
        int leftGain = Math.max(0, computeMaxGain(node.left));
        int rightGain = Math.max(0, computeMaxGain(node.right));

        // Check if path through this node (both sides) is a new best
        int pathThroughNode = node.val + leftGain + rightGain;
        globalMaxSum = Math.max(globalMaxSum, pathThroughNode);

        // Return best one-sided path to the parent
        return node.val + Math.max(leftGain, rightGain);
    }

    public static void main(String[] args) {
        // Tree: [-10, 9, 20, null, null, 15, 7] -> expected: 42
        TreeNode root = new TreeNode(-10);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);
        System.out.println(maxPathSum(root));
    }

    // Simple binary tree node
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }
    }
}