package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author TODO: Juhee Hong
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;
    private int[] mergedCheck;
    //private int compare;
    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.
        board.setViewingPerspective(side);

        for (int j = 0; j < board.size(); j++){
            mergedCheck = new int[board.size()];
            if (columnEmpty(j)) {
                continue;
            } else if (oneValueColumn(j)) {
                boolean checkchanged = oneValueColMoveUp(j);
                if (checkchanged) {
                    changed= true;
                }
            } else {
                for (int i = board.size()-2; i >= 0; i--) {
                    int k = hasNumberAbove(j, i);
                    Tile current = board.tile(j, i);
                    if (current != null && k != -1) {
                        Tile compare = board.tile(j, k);

                        if (compare.value() == current.value() && mergedCheck[k] == 0) {
                            //merge
                            int a = topmostAvailPlace(k, j, "top");
                            //int b = topmostAvailPlace(i, j, "bottom");
                            board.move(j, a, compare);
                            //board.move(j, b, current); //problematic
                            board.move(j, a, current);
                            changed = true;
                            mergedCheck[k] = 1;
                            score += current.value() + compare.value();
                        } else if (compare.value() == current.value()) {
                            if (board.tile(j, k-1) == null) {
                                board.move(j, topmostAvailPlace(i, j, "bottom"), current);
                                changed = true;
                            }
                        } else if (compare.value() != current.value()) {
                            //first make sure compare goes up all the way to the top.
                            int a = topmostAvailPlaceNotEqualVal(k, j);
                            board.move(j, a, compare);
                            int b = topmostAvailPlaceNotEqualVal(i, j);
                            board.move(j, b, current);
                            changed = true;
                        }
                    }
                }
            }
        }

        board.setViewingPerspective(Side.NORTH);

        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    private int topmostAvailPlace(int i, int j, String which) {
        //for when current.val == compare.val, regardless of merge status --> not to sure abt this condition
        //find where there's an already merged place above current i, if any.
        // also make sure that you don't run into other numbers so check that the
        // place to move is null, not another number.
        if (which.equals("top")) {
            return topmostAvailPlaceNotEqualVal(i, j);
        }
        for (int x = mergedCheck.length-1; x > i; x--){
            if (mergedCheck[x] == 1) {
                return (x-1);
            }
        }
        return mergedCheck.length-1;
    }

    private int topmostAvailPlaceNotEqualVal(int i, int j) {
        //for when current.val != compare.val
        //check if there are any spaces above current tile (j, i), starting from
        //right above (a null) until tile(j, i) is no longer null
        int lastsaved = i;
        for (int x = i+1; x < board.size(); x++) {
            if (board.tile(j, x) == null) {
                lastsaved = x;
            }
            else {
                break;
            }
        }
        return lastsaved;
    }

    private boolean oneValueColMoveUp(int j) {
        for (int i = board.size()-2; i >= 0; i--) {
            Tile t = board.tile(j, i);
            if (t != null) {
                board.move(j, board.size()-1, t);
                return true;//changed
            }
        }
        return false; //not changed
    }

    private int hasNumberAbove(int j, int i) {
        //returns the i-coordinate of first number (not null) above cell at current i
        for (int k = i+1; k < board.size(); k++) {
            if (board.tile(j, k) != null) {
                return k;
            }
        }
        return -1;
    }
    private boolean columnEmpty(int j) {
        //checks if the entire column is empty
        for (int i = 0; i < board.size(); i++) {
            if (board.tile(j, i) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean oneValueColumn(int j) {
        // checks if the entire column has just 1 value
        int count = 0;
        for (int i = 0; i < board.size(); i++) {
            if (board.tile(j, i) != null) {
                count += 1;
            }
        }
        if (count == 1) {
            return true;
        } else return false;
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        // TODO: Fill in this function.
        for (int i = 0; i < b.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                if (b.tile(i, j) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        // TODO: Fill in this function.
        for (int i = 0; i < b.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                if (b.tile(i, j) != null && b.tile(i, j).value() == MAX_PIECE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        // TODO: Fill in this function.
        if (emptySpaceExists(b)) {
            return true;
        }
        for (int i = 0; i < b.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                if (i >= 0 && i < b.size()-1) {
                    if (b.tile(i, j).value() == b.tile(i+1, j).value()) {
                        return true;
                    }
                }
                if (j >= 0 && j < b.size()-1) {
                    if (b.tile(i,j).value() == b.tile(i, j+1).value()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
