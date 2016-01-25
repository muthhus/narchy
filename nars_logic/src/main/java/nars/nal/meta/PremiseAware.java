package nars.nal.meta;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public interface PremiseAware {
	@Nullable
	Term function(Compound args, PremiseMatch r);
}
