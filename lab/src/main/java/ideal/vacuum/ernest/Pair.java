package ideal.vacuum.ernest;

/**
 * 
 * @author stagiaire
 *
 * @param <L>
 * @param <R>
 */
public class Pair<L, R> {
  public static <L, R> Pair<L, R> create(L left, R right) {
    return new Pair<>(left, right);
  }
  
  public final L mLeft;
  public final R mRight;
  
  private Pair(L left, R right) {
    this.mLeft = left;
    this.mRight = right;
  }

  public L getLeft() {
    return mLeft;
  }

  public R getRight() {
    return mRight;
  }
  
  public String toString() {
    return "(" + mLeft + ',' + mRight + ')';
  }
}
