///*
// * TextInput.java
// *
// * Copyright (C) 2008  Pei Wang
// *
// * This file is part of Open-NARS.
// *
// * Open-NARS is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 2 of the License, or
// * (at your option) any later version.
// *
// * Open-NARS is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
// */
//package nars.op.in;
//
//import nars.NAR;
//import nars.Narsese;
//import nars.task.Task;
//import nars.task.flow.TaskQueue;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Collection;
//
///**
// * Process experience from a string into zero or more input tasks
// */
//public class TextInput extends TaskQueue {
//
//	public TextInput(@NotNull NAR nar, @NotNull String input) throws Narsese.NarseseException {
//		int n = nar.tasks(input,
//				(Collection<Task>) this, nar);
//		if (n == 0)
//			throw new Narsese.NarseseException("No tasks parsed: " + input);
//	}
//
//}
