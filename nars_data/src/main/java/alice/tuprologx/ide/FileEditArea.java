package alice.tuprologx.ide;

public interface FileEditArea
{
    /**
     * Set the saved flag for the theory contained in the edit area after
     * the last Save operation.
     *
     * @param flag <code>true</code> if the theory has been modified ,
     * <code>false</code> otherwise.
     */
    void setSaved(boolean flag);

    /**
     * Check if the theory in the edit area has been modified after the
     * last Save operation.
     *
     * @return <code>true</code> if the theory has been modified,
     * <code>false</code> otherwise.
     */
    boolean isSaved();

}
