**NARchy** derives from [OpenNARS](https://github.com/opennars/opennars2), the open-source version of [NARS](https://sites.google.com/site/narswang/home), a general-purpose AI system, designed in the framework of a reasoning system.

![NARchy Logo](https://bitbucket.org/seh/narchy/raw/master/doc/narchy.jpg)

Theory
------

Non-Axiomatic Reasoning System ([NARS](https://sites.google.com/site/narswang/home))
processes **Tasks** imposed by and perceived from its environment,
which may include human or animal users, and other computer systems.

Tasks can arrive at any time, and there is no restriction on their contents as far as they can be expressed in __Narsese__, the I/O language of NARS.

###### Beliefs - represent a specified amount of factual evidence with which to revise existing knowledge and derive novel conclusions.
###### Questions - find the best matching answer(s) according to active beliefs.
###### Goals - invoke system operations in order to satisfy desire.

#### By default, NARS makes *no assumptions* about the meaning or truth value of input beliefs and goals.
###### How to choose proper inputs and interpret possible outputs for each application is an *open problem* to be solved by its users.

![Inference](https://raw.githubusercontent.com/automenta/narchy/skynet1/doc/derivation_pipeline.png)

As a reasoning system, the [architecture of NARS](http://www.cis.temple.edu/~pwang/Implementation/NARS/architecture.pdf) consists of a **memory**, an **inference engine**, and a **control system**.

The **memory** manages a collection of concepts, a list of operators, and a buffer for new tasks. Each concept is identified by a term, and contains tasks and beliefs directly on the term, as well as links to related tasks and terms.

The **deriver** applies various type of inference, according to a set of built-in rules. Each inference rule derives certain new tasks from a given task and a belief that are related to the same concept.

The **control** determines the cyclical activity of the system:

 1. Select tasks in the buffer to insert into the corresponding concepts, which may include the creation of new concepts and beliefs, as well as direct processing on the tasks.
 2. Select a concept from the memory, then select a task and a belief from the concept.
 3. Feed the task and the belief to the inference engine to produce derived tasks.
 4. Add the derived tasks into the task buffer, and send report to the environment if a task provides a best-so-far answer to an input question, or indicates the realization of an input goal.
 5. Return the processed belief, task, and concept back to memory with feedback.

All choices in steps 1 and 2 are **probabilistic**,
in the sense that all the items (tasks, beliefs, or concepts)
within the scope of the selection are referenced with
varying priority budgets.

When a new item is produced, its priority value is determined
according to its parent items and the conditions of the process which
produces it.

At step 5, the priority values of all the involved items
are adjusted, according to the immediate feedback of the
current cycle.

----

How NARchy Reasons Differently From OpenNARS
-------------------------------------------

The most significant difference is NARchy's completely redesigned Temporal Logic (NAL7) system
which uses numeric time differences embedded within temporal compounds.  These allow for 
arbitrary resolution in measuring and interpolating time as opposed to arbitrarily discretized
time intervals.  A concept's beliefs and goals co-locate all temporal and non-temporal varieties of 
its form into separate eternal and temporal belief tables which can not compete with each other
yet support each other when evaluating truth value.

NARchy removes the separate Parallel and Sequential term operator variations of Conjunctions,
 Equivalences, and Implications by using unified continuous-time Conjunction, Implication and 
 Equivalence operators, sharing all derivation rules with their eternal-time root types.  
 This reduces the number of derivation rules necessary and smooths some discontinuities and 
 edge cases that multiple temporal and non-temporal operator types necessitated.
 
In order to fully utilize this added temporal expressiveness, temporal belief tables were
redesigned to support evaluation of concept truth value at any point in time using a 
generalized microsphere interpolation "revection" algorithm which combines revision (interpolation) and
projection (extrapolation).  Temporal revision can be thought of as lossy compression, in that
tasks (as data points in truth-time space) can be merged to empty room for incoming data.  The
 1D "microsphere interpolation" algorithm was chosen and adapted with support for
  varying "illumination" intensity (set to truth confidence values).  The top eternal
  belief/goal, if exists, is applied as the "background" light source in which
  temporal beliefs shine their frequency "color" to the evaluated time point.

In keeping with a design preference for unity and balanced spectral continuity, negations are 
 also handled
 differently through the elimination of all Negation concepts.  Instead, a concept stores
 its complete frequency spectrum within itself and Negation is handled automaticaly and
 transparently during derivation and user-input/output.  Subterms may be negated, and this
 results in unique compounds, but the top-level term of a task is always stored un-negated.
 This ultimately can result in less concepts (since a negation of a concept doesn't exist separately)
 and eliminates the possibility of a concept contradicting the beliefs of its negation which
 otheriwse would be stored in separate belief tables.  It also
 supports smooth and balanced revection across the 0.5 "maybe" midpoint of the frequency range,
 in both temporal and eternal modes.
 
 NARchy's deriver follows a continued evolution from its beginnings in the OpenNARS 1.6..1.7 versions
 which featured the Termutator to manage the traversal of the space of possible permutations
 while obeying AIKR principles according to operating parameter limits.  It has some 
 additional features including inline 
 term rewrite functions (ex: set operations and 2nd-layer subtitutions) and integration of
  the temporal functions necessary to appropriately "temporalize" derivations according
  to the timing of premise components.
 
 Disjunctions are only virtual operators as perceivable by input and displayed on output. They 
 are converted immediately to negated conjunction of negations via DeMorgan's laws.  By preferring 
 the conjunction representation,
 temporal information can not be lost through conversion to or from the non-temporal Disjunction type.
 
 The default bag type is a Buffered Auto-Forgetting CurveBag which accumulate updates between 
 "commits" in which the changes are later applied.  Buffering supports rapid high-frequency 
 inter-concept activation without needing continuous bag data structure maintenance.  Auto-forgetting
 removes the need for arbitrary forgetting rates, instead replacing with a continuous forgetting
 applied in a balanced proportion to the bag activation pressure relative to the existing
 bag's mass.  Bags also share key maps where possible (between all tasks in a concept, and
 between both termlink and tasklink bags in a concept), reducing memory usage.
 
 A central, concurrent concept index (cache) provides access to all inactive concepts.  The capacity
 of the index can be adjusted in various ways including maximum size, maximum "weight", and weak/soft
 references.  This cache can also serve as an asynchronous reader and writer to longer-term caches 
 which persist on disk or in a database.  The concurrent abilities of this index support
 arbitrarily parallelized reasoner operations along with concurrent concept data structures.
  While not yet entirely synchronization-free, this becomes less important as the number
   of concepts generally exceeds greatly the number of threads.
 
 A compact byte-level codec for terms and tasks allows all concept data to be serialized to
 disk, off-heap memory, or network streams.  It is optionally compressed with Snappy compression
 algorithm which offers a tradeoff of speed and size savings.
 
 An adaptive concept "policy" system manages the allowed capacity of the different concept
 data structures according to activity, term complexity, confidence levels, or other heuristics.  
 This can be used, for example, to allow atomic concepts to support more termlinks than compounds, 
 or to allow more beliefs for a concept which has higher confidence values.  It also allows for
 shrinking capacities when a concept is deactivated, acting as another form of lossy
 concept compression which removes less essential components.
 
 A sensor/motor "NAgent" API for wrapping a NAR reasoner and attaching various sensor and
 motor concepts with specific abilities for transducing input to beliefs
 and effecting behaviors from goals.  This can be used to easily interface a NAR as a 
 reinforcement-learning agent with a specific environment or interface.   It also has support for
 Reward sensor concept which can be desired and focused as the object of procedural questions
 and future predictions with respect to the sensor and motor concepts of its context.
   
 The InterNARS is a multi-agent p2p mesh network protocol allowing individual NAR peers to communicate
  asynchronously and remotely through messages containing serialized tasks.  In the InterNARS, 
  peers learn to intelligently route their own and others' communications according to the
  budget and/or truth heuristics inherent in the reasoning itself.  Another peer's beliefs can 
  be corroborated, doubted, augmented, summarized, misrepresented, or ignored.  Their questions
  can be answered, reiterated, or answered with more questions.  Goals can be obeyed, reinforced,
  or disobeyed.  The semantics of the various NAL operator and task punctuations covers the range
  of "performatives" offered by
  classical multi-agent communication protocols like FIPA and ACL, but perhaps in a more
  natural way, and enhanced with the added expressiveness of shades of NAL truth and budget. 
   
   
 _Many other changes remain to be documented._
 

----

 * A comprehensive description of NARS [Rigid Flexibility: The Logic of Intelligence](http://www.springer.com/west/home/computer/artificial?SGWID=4-147-22-173659733-0) and [Non-Axiomatic Logic: A Model of Intelligent Reasoning](http://www.worldscientific.com/worldscibooks/10.1142/8665).
 * Papers discussing aspects of the system: [available here](http://www.cis.temple.edu/~pwang/papers.html)
 * Introduction: [The Logic of Intelligence](http://www.cis.temple.edu/~pwang/Publication/logic_intelligence.pdf)
 * High-level engineering plan: [From NARS to a Thinking Machine](http://www.cis.temple.edu/~pwang/Publication/roadmap.pdf)
 * Core Logic: [From Inheritance Relation to Non-Axiomatic Logic](http://www.cis.temple.edu/~pwang/Publication/inheritance_nal.pdf)
 * Semantics: [Experience-Grounded Semantics: A theory for intelligent systems](http://www.cis.temple.edu/~pwang/Publication/semantics.pdf)
 * Memory & Control: [Computation and Intelligence in Problem Solving](http://www.cis.temple.edu/~pwang/Writing/computation.pdf)

[![](https://badge.imagelayers.io/automenta/narchy:latest.svg)](https://imagelayers.io/?images=automenta/narchy:latest 'Docker badge imagelayers.io')

Contents
--------
 * **nal** - Logic Reasoner
 * **guifx** - JavaFX GUI
 * **app** - Application-level and supporting tools
 * **web** - Web server and client
 * **lab** - Experiments & demos
 * **util** - Non-NARS specific supporting utilities
 * **logic** - Non-NARS specific supporting logic
 * **perf** - JMH benchmarks

Requirements
------------
 * Java 9 (OpenJDK or Oracle JDK)
 * Maven

References
----------
http://code.google.com/p/open-nars/wiki/ProjectStatus

An (outdated) HTML user manual:
 * http://www.cis.temple.edu/~pwang/Implementation/NARS/NARS-GUI-Guide.html

The project home page:
 * https://code.google.com/p/open-nars/

Discussion Group:
 * https://groups.google.com/forum/?fromgroups#!forum/open-nars
