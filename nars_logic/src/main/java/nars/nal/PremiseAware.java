package nars.nal;

import nars.term.Term;
import nars.term.compound.Compound;
import org.jetbrains.annotations.Nullable;

public interface PremiseAware {
	@Nullable
	Term function(Compound args, PremiseMatch r);
}
