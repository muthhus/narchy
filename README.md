**NARchy** derives from [OpenNARS](https://github.com/opennars/opennars2), the open-source version of [NARS](https://sites.google.com/site/narswang/home), a general-purpose AI system, designed in the framework of a reasoning system.

![NARchy Logo](https://bitbucket.org/seh/narchy/raw/master/doc/narchy.jpg)

Theory
------

Non-Axiomatic Reasoning System ([NARS](https://sites.google.com/site/narswang/home)) processes tasks imposed by its environment, which may include human users or other computer systems. Tasks can arrive at any time, and there is no restriction on their contents as far as they can be expressed in __Narsese__, the I/O language of NARS.

There are several types of __tasks__:

 * **Judgment** - To process it means to accept it as the system's belief, as well as to derive new beliefs and to revise old beliefs accordingly.
 * **Question** -  To process it means to find the best answer to it according to current beliefs.
 * **Goal** - To process it means to carry out some system operations to realize it.

### By default, NARS makes *no assumptions* about the belief or desire values of input.
#### How to choose proper inputs and interpret possible outputs for each application is an *open problem* to be solved by its users.

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
 * Gradle

References
----------
http://code.google.com/p/open-nars/wiki/ProjectStatus

An (outdated) HTML user manual:
 * http://www.cis.temple.edu/~pwang/Implementation/NARS/NARS-GUI-Guide.html

The project home page:
 * https://code.google.com/p/open-nars/

Discussion Group:
 * https://groups.google.com/forum/?fromgroups#!forum/open-nars
