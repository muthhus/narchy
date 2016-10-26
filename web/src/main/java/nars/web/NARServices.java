package nars.web;

import io.undertow.server.handlers.PathHandler;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import spacegraph.web.Json;

import static nars.web.WebServer.socket;

/**
 * Created by me on 9/23/16.
 */
@Deprecated public class NARServices {

    public NARServices(NAR nar, PathHandler path) {

        path
                .addPrefixPath("/terminal", socket(new NarseseIOService(nar)))
                .addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
                .addPrefixPath("/active", socket(new ActiveConceptService(nar, 100, 64)));


    }
}
