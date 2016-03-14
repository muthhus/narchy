Core
----
 * complete budget backpressure with accountability tools to ensure no inflation nor leak
  * stack-instrumented budget accounting guards? (maybe overkill)
 * complete Prolog plugin
 * complete Clojure / Core.Logic plugin
  * explore Narjure integrations
 * Bag's BLink budgets stored in unified fixed-size float[] array for improved cpu cache coherence, possibly with Unsafe management
 * ensure (via unit tests) the formation of stable, non-explosive/non-leaky/non-redundant temporal models from given input
 * fully compiled unification patterns for rules
 * scalable, distributed, and persistent memory (ex: apache ignite)
 * temporal interpolation "compression" on belief table overflow (instead of just discarding a task)
 * temporal belief table tuning
 * dynamically adjust concept size (ex: belief table sizes) according to budget and term features
 * question/quest tasks as specific subclass of task with feedback handlers
 * goal-driven self-testing framework
 * NAL9 tuning
  * abbreviation as compression codec(s)
  * multi-NAR architectures

Sensorimotor & Autonomics
-------------------------
 * tune NarQ, HSOM, Autoencoder parameters (and their combined usages, ex: in Rover, etc..)
 * refine sensor/goal/motor API
 * implement additional machine learning algorithms

GuiFX & Web
-----------
 * task control UI
 * complete desktop widgetization
 * LRI (logic resource identifier) web-browser prototype
 * bower invoked on build from gradle

Narsese
-------
 * equals character ("=") as shorthand syntax for <->
 * parse: (&&+0, a, b, ..., c )

I/O, Multimedia, & Sensors
--------------------------
 * live webcam/microphone application
 * time-series database integration, with IoT sources
 * multi-agent dialog chatbots (ex: irc)
 * VNC
 * text synthesizer control
 * interactive sonification

Web & P2P
---------
 * mesh I/O (ex: meshy)

Semantic Web
------------
 * OWL/RDF inference and demos completely loading several ontologies from supplied/online ontology index
 * KIF reasoner
 * FIPA ACL https://github.com/hypergraphdb/hypergraphdb/wiki/MessageStructure
