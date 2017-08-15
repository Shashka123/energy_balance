import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;


class Energy_Balance {


    // given numbers to place in rows and columns
    public static int[] copy_original_values;
    public static int[] original_values = {7, 0, -3, 8, -3, -1, 8, 0}; 

    // sets of locations with specified sums
    //(numbered from array with upper left 0, row first)

	public static int[][] addends = {
						{2, 3, 4, 5},     //resulta em 3
						{0, 1, 5, 7},     //resulta em 12
						{4, 6}            //resulta em 7
	                                };	

    // desired sums for each corresponding vector in addends
    public static int[] sums = {3, 12, 7};			  
	 
	
    public static int iterations = 0;

    static int EMPTY = 1 << 10;

    private int numFails = 0;

    public static void main(String[] args) {

        iterations = 0;
        Energy_Balance eb = new Energy_Balance();

        boolean answer = false;

        int[] result = null;
        if (args != null && args.length >= 1) {

            testcase mycase = new testcase(args[0]);
            original_values = mycase.original_values;
            result = new int[original_values.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = EMPTY;
            }
            answer = eb.FindMap(mycase.original_values, mycase.sums, mycase.addends, result);
        }
        else {
            result = new int[original_values.length];
            answer = eb.FindMap(original_values, sums, addends, result);
        }
        System.out.println();
        if (!answer) {
            System.out.println("No solution found!");
            return;
        }
        for (int i = 0; i < result.length; i++) {
            System.out.printf("Position %4d: %4s\n", i,
                              (result[i] == EMPTY ? "ANY" : Integer.toString(result[i])));
        }
    }

    private int checkARule(int[][] addends, int[] sums, int index, int[] values) {
        int sum = 0;
        for (int j = 0; j < addends[index].length; j++) {
            //System.out.printf("%3d+", original_values[addends[index][j]]);
            sum += values[addends[index][j]];
        }
        //System.out.printf("=%4d/%4d\n", sum, sums[index]);
        return sum;
    }

    private boolean checkRemainingRules(int[][] addends, int[] sums, int index, int[] values) {
        for (int i = index; i < addends.length; i++) {
            if (sums[i] != checkARule(addends, sums, i, values)) {
                return false;
            }
        }
        return true;

    }

    private void printSquare(int[] values) {
        System.out.println();
        int dim = (int)Math.sqrt(values.length);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < values.length / dim; j++) {
                System.out.printf("%4s",
                                  (values[i * dim + j] == EMPTY ? "ANY" : values[i * dim + j]));
            }
            System.out.printf("\n");
        }
    }


    private void printFailures() {
        numFails++;
        if (numFails % 10000 == 0) {
            System.out.printf("Number of trials %s\r", String.format("%,d", numFails));
        }
    }

    // returns a map from the locations in addends to values (not their indices)
    public boolean FindMap(int[] values, int[] sums, int[][] addends, int[] result) {
        iterations += 1;

        int combinationCount = 0;
        CombinationGenerator cg =
            new CombinationGenerator(values.length, addends[0].length);

        while (cg.hasMore()) {
            combinationCount += 1;
            int [] addIndices = cg.getNext();  //internal buffer exposed
            int sum = 0;
            for (int j = 0; j < addIndices.length; j++) {
                sum += values[addIndices[j]];
            }

            if (sum == sums[0]) {

                // now compute all permutations of indices and iterate through them

                // XXX: backtrack too difficult to maintain
                // thus each time a new shrunk solution space is provided

                for (int f = 0; f < Permutation.factorial(addIndices.length); f++) {
                    if (f > 0) {
                        Permutation.nextPermutation(addIndices);
                    }

                    /* base case
                       there could be rules(sums left) when you run out of remaining numbers
                       the case where square puzzles work. If you found a partial solution,
                       The values can run out with several rules remaining.
                    */

                    /* now this is really the last rule with values to accommodate */
                    if (sums.length == 1) {
                        for(int i = 0; i < addends[0].length; i++) {
                            result[addends[0][i]] = values[addIndices[i]];
                        }
                        return true;
                    }
                    int[] newValues = new int[values.length - addends[0].length];
                    int temp = 0;
                    for (int i = 0; i < values.length; i++) {
                        boolean inside = false;
                        for (int k = 0; k < addIndices.length; k++) {
                            if (addIndices[k] == i) {
                                inside = true;
                                break;
                            }
                        }
                        if (inside) continue;
                        newValues[temp++] = values[i];
                    }

                    int[][] newAddends = new int[addends.length - 1][];
                    for (int i = 1; i < addends.length; i++) {
                        newAddends[i - 1] = new int[addends[i].length];
                        System.arraycopy(addends[i], 0, newAddends[i-1], 0, addends[i].length);
                    }

                    int[] newSums = new int[sums.length - 1];
                    System.arraycopy(sums, 1, newSums, 0, sums.length - 1);

                    // remove what we have found.
                    // if it overlaps with other unsolved indices, remove those overlaps
                    boolean failed = false;
                    for (int i = 0; i < addends[0].length; i++) {
                        int index = addends[0][i];
                        for (int j = 0; j < newAddends.length; j++) {
                            for (int k = 0; k < newAddends[j].length; k++) {
                                if (newAddends[j][k] == index) {
                                    int[] copy = new int[newAddends[j].length - 1];
                                    System.arraycopy(newAddends[j], 0, copy, 0, k);
                                    System.arraycopy(newAddends[j], k+1, copy, k, newAddends[j].length-k-1);
                                    newAddends[j] = copy;
                                    newSums[j] -= values[addIndices[i]];
                                    if (newAddends[j].length == 0 && newSums[j] != 0) {
                                        failed = true;
                                    }
                                }
                            }
                        }
                    }
                    if (failed) {
                        continue; // have to continue with permutations
                    }

                    boolean noRulesLeft = false;

                    for (int i = 0; i < addends[0].length; i++) {
                        result[addends[0][i]] = values[addIndices[i]];
                    }


                    if (newValues.length == 0) {
                        noRulesLeft = checkRemainingRules(addends, sums, 1, result);
                        if (!noRulesLeft) {
                            System.out.println("Remaining rules check failed");
                            printSquare(result);//attempts saved here!
                            for (int i = 0; i < addends[0].length; i++) {
                                result[addends[0][i]] = EMPTY;
                            }
                            continue;
                        }
                        else { //no rules left, no values left, and additional checks pass
                            printSquare(result);
                            return true;
                        }
                    }

                    /* backtrack */

                    if (!FindMap(newValues, newSums, newAddends, result)) {
                        for (int i = 0; i < addends[0].length; i++) {
                            result[addends[0][i]] = EMPTY;
                        }
                        continue;
                    }
                    return true;
                } // end of permutation loop
            } //end if sum == sum[0]
        } //end of all combination groups
        // no solution found, return null array
        printFailures();
        return false;
    }
}

/**
 * @author Michael Gilleland - http://www.merriampark.com/comb.htm
 */

//--------------------------------------
// Systematically generate combinations.
//--------------------------------------


class CombinationGenerator {
    private static int id_seq = 0;
    public int id;
    private int[] a;
    private int [] output; // returns the result so the internal state is not overwritten by others
    public int n;
    public int r;
    private BigInteger numLeft;
    private BigInteger total;


      public static void main(String[] args) {

        int[] values = {-14, 1, 4, 15, -2, 15, -9, 6};
        CombinationGenerator cg = new CombinationGenerator(values.length, 3);
        while (cg.hasMore()) {
            int[] ind = cg.getNext();
            System.out.println("cg " + cg.id + " now " + Arrays.toString(ind));
        }
        CombinationGenerator cg2 = new CombinationGenerator(values.length, 5);
        while (cg2.hasMore()) {
            int[] ind = cg2.getNext();
            System.out.println("cg2 " + cg.id + " now " + Arrays.toString(ind));
        }


     }

    public CombinationGenerator (int n, int r) {
        if (r > n) {
            throw new IllegalArgumentException (r + " > " + n);
        }
        if (n < 1) {
            throw new IllegalArgumentException (n + " < " + 1);
        }
        this.n = n;
        this.r = r;
        this.id = id_seq++;
        a = new int[r];
        output = new int[r];
        BigInteger nFact = getFactorial (n);
        BigInteger rFact = getFactorial (r);
        BigInteger nminusrFact = getFactorial (n - r);
        total = nFact.divide (rFact.multiply (nminusrFact));
        reset ();
    }

    public void reset () {
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
            output[i] = i;
        }
        numLeft = new BigInteger (total.toString ());
    }

    //------------------------------------------------
    // Return number of combinations not yet generated
    //------------------------------------------------

    public BigInteger getNumLeft () {
        return numLeft;
    }

    //-----------------------------
    // Are there more combinations?
    //-----------------------------

    public boolean hasMore () {
        return numLeft.compareTo (BigInteger.ZERO) == 1;
    }

    //------------------------------------
    // Return total number of combinations
    //------------------------------------

    public BigInteger getTotal () {
        return total;
    }

    //------------------
    // Compute factorial
    //------------------

    private static BigInteger getFactorial (int n) {
        BigInteger fact = BigInteger.ONE;
        for (int i = n; i > 1; i--) {
            fact = fact.multiply (new BigInteger (Integer.toString (i)));
        }
        return fact;
    }

    //--------------------------------------------------------
    // Generate next combination (algorithm from Rosen p. 286)
    //--------------------------------------------------------

    public int[] getNext () {

        if (numLeft.equals (total)) {
            numLeft = numLeft.subtract (BigInteger.ONE);
            return output;
        }

        int i = r - 1;
        while (a[i] == n - r + i) {
            i--;
        }
        a[i] = a[i] + 1;
        for (int j = i + 1; j < r; j++) {
            a[j] = a[i] + j - i;
        }

        numLeft = numLeft.subtract (BigInteger.ONE);
        System.arraycopy(a, 0, output, 0, a.length);
        return output;

    }
}

class Permutation{

    public static boolean nextPermutation(int[] array) {
        // Find longest non-increasing suffix
        int i = array.length - 1;
        while (i > 0 && array[i - 1] >= array[i])
            i--;
        // Now i is the head index of the suffix
        // Are we at the last permutation already?
        if (i <= 0) {
            return false;
        }
        // Let array[i - 1] be the pivot
        // Find rightmost element that exceeds the pivot
        int j = array.length - 1;
        while (array[j] <= array[i - 1])
            j--;
        // Now the value array[j] will become the new pivot
        // Assertion: j >= i

        // Swap the pivot with j
        int temp = array[i - 1];
        array[i - 1] = array[j];
        array[j] = temp;

        // Reverse the suffix
        j = array.length - 1;
        while (i < j) {
            temp = array[i];
            array[i] = array[j];
            array[j] = temp;
            i++;
            j--;
        }
        // Successfully computed the next permutation
        return true;
    }

    public static int factorial(int n) {
        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }
}

class testcase {

    int[] original_values;
    int[][] addends;
    int[] sums;

    public void printRules() {
        for (int i = 0; i < addends.length; i++) {
            int j = 0;;
            int mysum = 0;
            for (; j < addends[i].length - 1; j++) {
                System.out.printf("%5d +",original_values[addends[i][j]]);
                mysum += original_values[addends[i][j]];
            }
            System.out.printf("%5d = %5d / %5d\n",
                              original_values[addends[i][j]],
                              mysum + original_values[addends[i][j]],
                              sums[i]);
        }
    }

    public testcase(String filename) {
        loadTest(filename);
        printRules();
    }

    private List<String> readFile(String filename) {
        List<String> records = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                records.add(line);
            }
            reader.close();
            return records;
       } catch (Exception e) {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return null;
       }
    }

    public boolean loadTest(String filename) {
        List<String> lines = readFile(filename);
        int i = 0;

        while (lines.get(i).startsWith("#")) {
            i++; //skipping original values comments
        }
        String original_strings = lines.get(i);
        System.out.println("original values: " + original_strings);
        String[] original_strings_split = original_strings.split(",");

        original_values = new int[original_strings_split.length];

        for (int j = 0; j < original_values.length; j++) {
            original_values[j] = Integer.parseInt(original_strings_split[j].trim());
        }
        System.out.println("original converted values: " + Arrays.toString(original_values));

        i++;
        while (lines.get(i).startsWith("#")) {
            i++; //skipping addends comments
        }

        ArrayList<String> addends_string = new ArrayList<String>();
        System.out.println("addends:");
        while (!lines.get(i).startsWith("#")) {
            addends_string.add(lines.get(i));
            System.out.println(lines.get(i));
            i++;
        }
        addends = new int[addends_string.size()][];
        System.out.println("addends converted:");
        for (int j = 0; j < addends_string.size(); j++) {
            String[] addends_string_array = addends_string.get(j).split(",");
            addends[j] = new int[addends_string_array.length];
            for (int k = 0; k < addends[j].length; k++) {
                addends[j][k] = Integer.parseInt(addends_string_array[k].trim());
            }
            System.out.println(Arrays.toString(addends[j]));
        }

        i++;
        while (lines.get(i).startsWith("#")) {
            i++;  //skipping sums comments
        }

        String sum_strings = lines.get(i);
        System.out.println("sum values: " + sum_strings);
        String[] sum_strings_split = sum_strings.split(",");

        sums = new int[sum_strings_split.length];

        for (int j = 0; j < sums.length; j++) {
            sums[j] = Integer.parseInt(sum_strings_split[j].trim());
        }
        System.out.println("sum converted values: " + Arrays.toString(sums));

        return true;

    }
}
